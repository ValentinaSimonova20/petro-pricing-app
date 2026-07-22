package com.petropricing.app.calculation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petropricing.app.calculation.dto.CalculationRowResult;
import com.petropricing.app.calculation.dto.CalculationRunSummary;
import com.petropricing.app.calculation.dto.ComponentBreakdown;
import com.petropricing.app.domain.CalculationResult;
import com.petropricing.app.domain.DemandForecast;
import com.petropricing.app.domain.Formula;
import com.petropricing.app.repository.CalculationResultRepository;
import com.petropricing.app.repository.CurrencyRateRepository;
import com.petropricing.app.repository.DemandForecastRepository;
import com.petropricing.app.repository.FormulaComponentRepository;
import com.petropricing.app.repository.FormulaRepository;
import com.petropricing.app.repository.MaterialGroupRepository;
import com.petropricing.app.repository.QuoteMappingRepository;
import com.petropricing.app.repository.QuoteValueRepository;
import com.petropricing.app.repository.TermTypeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalculationService {

    private final DemandForecastRepository demandForecastRepository;
    private final FormulaRepository formulaRepository;
    private final FormulaComponentRepository formulaComponentRepository;
    private final MaterialGroupRepository materialGroupRepository;
    private final TermTypeRepository termTypeRepository;
    private final QuoteMappingRepository quoteMappingRepository;
    private final QuoteValueRepository quoteValueRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final CalculationResultRepository calculationResultRepository;
    private final ObjectMapper objectMapper;
    private final ExpressionEngine expressionEngine = new ExpressionEngine();

    public CalculationService(
            DemandForecastRepository demandForecastRepository,
            FormulaRepository formulaRepository,
            FormulaComponentRepository formulaComponentRepository,
            MaterialGroupRepository materialGroupRepository,
            TermTypeRepository termTypeRepository,
            QuoteMappingRepository quoteMappingRepository,
            QuoteValueRepository quoteValueRepository,
            CurrencyRateRepository currencyRateRepository,
            CalculationResultRepository calculationResultRepository,
            ObjectMapper objectMapper
    ) {
        this.demandForecastRepository = demandForecastRepository;
        this.formulaRepository = formulaRepository;
        this.formulaComponentRepository = formulaComponentRepository;
        this.materialGroupRepository = materialGroupRepository;
        this.termTypeRepository = termTypeRepository;
        this.quoteMappingRepository = quoteMappingRepository;
        this.quoteValueRepository = quoteValueRepository;
        this.currencyRateRepository = currencyRateRepository;
        this.calculationResultRepository = calculationResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CalculationRunSummary calculateAll(LocalDate periodFilter) {
        long started = System.currentTimeMillis();
        calculationResultRepository.deleteAll();

        List<DemandForecast> rows = demandForecastRepository.findAll();
        if (periodFilter != null) {
            rows = rows.stream()
                    .filter(r -> periodFilter.equals(r.getPeriod()))
                    .collect(Collectors.toList());
        }

        FormulaMatcher matcher = new FormulaMatcher(
                formulaRepository.findAll(),
                materialGroupRepository.findAll()
        );
        ValueResolver resolver = new ValueResolver(
                termTypeRepository.findAll(),
                quoteMappingRepository.findAll(),
                quoteValueRepository.findAll(),
                currencyRateRepository.findAll()
        );

        List<CalculationRowResult> results = new ArrayList<>();
        for (DemandForecast row : rows) {
            try {
                results.add(calculateOne(row, matcher, resolver));
            } catch (Exception e) {
                CalculationRowResult failed = baseResult(row);
                failed.setStatus(CalculationStatus.INTERNAL_ERROR);
                failed.setStatusReason("Внутренняя ошибка: " + e.getMessage());
                results.add(failed);
            }
        }

        List<CalculationResult> entities = results.stream().map(this::toEntity).toList();
        calculationResultRepository.saveAll(entities);

        CalculationRunSummary summary = CalculationMetrics.build(results);
        summary.setElapsedMs(System.currentTimeMillis() - started);
        return summary;
    }

    private CalculationRowResult calculateOne(DemandForecast row,
                                              FormulaMatcher matcher,
                                              ValueResolver resolver) {
        CalculationRowResult result = baseResult(row);

        if (row.getPeriod() == null || isBlank(row.getClientId()) || isBlank(row.getMtrNsiCode())) {
            result.setStatus(CalculationStatus.DATA_INCONSISTENT);
            result.setStatusReason("Пустые обязательные поля строки (period / client_id / mtr_nsi_code)");
            return result;
        }

        FormulaMatcher.MatchResult match = matcher.match(row);
        List<String> candidateKeys = match.getCandidates().stream()
                .map(c -> c.getFormula().getFormulaKey())
                .filter(k -> k != null)
                .toList();
        result.setCandidateFormulaKeys(candidateKeys);

        if (match.getSelected() == null) {
            if (match.isHadInactiveOnly()) {
                result.setStatus(CalculationStatus.INACTIVE_ONLY);
                result.setStatusReason("Подходящие формулы есть только среди неактивных");
            } else if (FormulaMatcher.isSpot(row.getContract())) {
                result.setStatus(CalculationStatus.SPOT_NO_FORMULA);
                result.setStatusReason("Строка Spot: подходящая формула не найдена");
            } else {
                result.setStatus(CalculationStatus.NO_FORMULA);
                result.setStatusReason("Не найдена формула по client_id + материал/группа + периоду");
            }
            return result;
        }

        FormulaMatcher.MatchCandidate selected = match.getSelected();
        Formula formula = selected.getFormula();
        result.setSelectedFormulaKey(formula.getFormulaKey());
        result.setFormulaText(formula.getFormulaText());
        result.setCurrency(formula.getCurrencyDocument());
        result.setExtendedValidity(selected.isExtendedValidity());

        if (selected.isExtendedValidity()) {
            result.getWarnings().add("Подбор с продлением срока действия (valid_to < period)");
        }
        if (match.getCandidates().size() > 1) {
            result.getWarnings().add("Найдено несколько формул; выбрана с самой поздней датой создания");
        }

        var components = formulaComponentRepository.findByFormulaKey(formula.getFormulaKey());
        ValueResolver.ResolveOutcome resolved = resolver.resolve(components, row.getPeriod());
        result.setComponents(resolved.getComponents());

        if (!resolved.isOk()) {
            result.setStatus(resolved.getFailureStatus());
            result.setStatusReason(resolved.getFailureReason());
            return result;
        }

        try {
            var price = expressionEngine.evaluate(formula.getFormulaText(), resolved.getVariables());
            result.setPrice(price);
            if (match.getCandidates().size() > 1) {
                result.setStatus(CalculationStatus.OK_MULTI_FORMULA);
                result.setStatusReason("Цена посчитана; выбрана одна из " + match.getCandidates().size() + " формул");
            } else if (selected.isExtendedValidity()) {
                result.setStatus(CalculationStatus.OK_EXTENDED_VALIDITY);
                result.setStatusReason("Цена посчитана с продлением срока действия формулы");
            } else {
                result.setStatus(CalculationStatus.OK);
                result.setStatusReason("Цена посчитана успешно");
            }
        } catch (ExpressionEngine.EvaluationException e) {
            if (e.isUnsupported()) {
                result.setStatus(CalculationStatus.UNSUPPORTED_EXPRESSION);
            } else {
                result.setStatus(CalculationStatus.EXPRESSION_ERROR);
            }
            result.setStatusReason(e.getMessage());
            result.setPrice(null);
        }

        return result;
    }

    private CalculationRowResult baseResult(DemandForecast row) {
        CalculationRowResult result = new CalculationRowResult();
        result.setForecastId(row.getId());
        result.setRowId(row.getRowId());
        result.setPeriod(row.getPeriod());
        result.setClientId(row.getClientId());
        result.setMtrNsiCode(row.getMtrNsiCode());
        result.setContract(row.getContract());
        return result;
    }

    private CalculationResult toEntity(CalculationRowResult row) {
        CalculationResult entity = new CalculationResult();
        entity.setForecastId(row.getForecastId());
        entity.setRowId(row.getRowId());
        entity.setPeriod(row.getPeriod());
        entity.setClientId(row.getClientId());
        entity.setMtrNsiCode(row.getMtrNsiCode());
        entity.setContract(row.getContract());
        entity.setStatus(row.getStatus() == null ? null : row.getStatus().name());
        entity.setStatusReason(row.getStatusReason());
        entity.setSelectedFormulaKey(row.getSelectedFormulaKey());
        entity.setFormulaText(row.getFormulaText());
        entity.setPrice(row.getPrice());
        entity.setCurrency(row.getCurrency());
        entity.setExtendedValidity(row.isExtendedValidity());
        entity.setCandidateCount(row.getCandidateCount());
        entity.setCandidateFormulaKeys(writeJson(row.getCandidateFormulaKeys()));
        entity.setWarningsJson(writeJson(row.getWarnings()));
        entity.setComponentsJson(writeJson(row.getComponents()));
        return entity;
    }

    public CalculationRunSummary loadSummaryFromDb() {
        List<CalculationResult> all = calculationResultRepository.findAll();
        List<CalculationRowResult> rows = all.stream().map(this::fromEntityLight).toList();
        return CalculationMetrics.build(rows);
    }

    public List<CalculationResult> loadPreview(int limit) {
        return calculationResultRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();
    }

    public CalculationRowResult loadBreakdown(Long id) {
        return calculationResultRepository.findById(id)
                .map(this::fromEntityFull)
                .orElse(null);
    }

    private CalculationRowResult fromEntityLight(CalculationResult entity) {
        CalculationRowResult row = new CalculationRowResult();
        row.setForecastId(entity.getForecastId());
        row.setRowId(entity.getRowId());
        row.setPeriod(entity.getPeriod());
        row.setClientId(entity.getClientId());
        row.setMtrNsiCode(entity.getMtrNsiCode());
        row.setContract(entity.getContract());
        if (entity.getStatus() != null) {
            try {
                row.setStatus(CalculationStatus.valueOf(entity.getStatus()));
            } catch (IllegalArgumentException ignored) {
                row.setStatus(CalculationStatus.INTERNAL_ERROR);
            }
        }
        row.setStatusReason(entity.getStatusReason());
        row.setSelectedFormulaKey(entity.getSelectedFormulaKey());
        row.setPrice(entity.getPrice());
        row.setCurrency(entity.getCurrency());
        row.setExtendedValidity(Boolean.TRUE.equals(entity.getExtendedValidity()));
        row.setCandidateFormulaKeys(readStringList(entity.getCandidateFormulaKeys()));
        row.setWarnings(readStringList(entity.getWarningsJson()));
        return row;
    }

    private CalculationRowResult fromEntityFull(CalculationResult entity) {
        CalculationRowResult row = fromEntityLight(entity);
        row.setFormulaText(entity.getFormulaText());
        row.setComponents(readComponents(entity.getComponentsJson()));
        return row;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<ComponentBreakdown> readComponents(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ComponentBreakdown>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
