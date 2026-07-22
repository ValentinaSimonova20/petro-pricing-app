package com.petropricing.app.web;

import com.petropricing.app.calculation.CalculationService;
import com.petropricing.app.calculation.dto.CalculationRowResult;
import com.petropricing.app.calculation.dto.CalculationRunSummary;
import com.petropricing.app.domain.CalculationResult;
import com.petropricing.app.domain.DemandForecast;
import com.petropricing.app.importer.BulkUploadResult;
import com.petropricing.app.importer.BulkUploadService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadController {

    private static final int RESULTS_LIMIT = 200;

    private final BulkUploadService bulkUploadService;
    private final DemandForecastRepository demandForecastRepository;
    private final FormulaRepository formulaRepository;
    private final FormulaComponentRepository formulaComponentRepository;
    private final TermTypeRepository termTypeRepository;
    private final QuoteValueRepository quoteValueRepository;
    private final QuoteMappingRepository quoteMappingRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final MaterialGroupRepository materialGroupRepository;
    private final CalculationService calculationService;
    private final CalculationResultRepository calculationResultRepository;

    public UploadController(
            BulkUploadService bulkUploadService,
            DemandForecastRepository demandForecastRepository,
            FormulaRepository formulaRepository,
            FormulaComponentRepository formulaComponentRepository,
            TermTypeRepository termTypeRepository,
            QuoteValueRepository quoteValueRepository,
            QuoteMappingRepository quoteMappingRepository,
            CurrencyRateRepository currencyRateRepository,
            MaterialGroupRepository materialGroupRepository,
            CalculationService calculationService,
            CalculationResultRepository calculationResultRepository
    ) {
        this.bulkUploadService = bulkUploadService;
        this.demandForecastRepository = demandForecastRepository;
        this.formulaRepository = formulaRepository;
        this.formulaComponentRepository = formulaComponentRepository;
        this.termTypeRepository = termTypeRepository;
        this.quoteValueRepository = quoteValueRepository;
        this.quoteMappingRepository = quoteMappingRepository;
        this.currencyRateRepository = currencyRateRepository;
        this.materialGroupRepository = materialGroupRepository;
        this.calculationService = calculationService;
        this.calculationResultRepository = calculationResultRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        populate(model, List.of(), null, "upload");
        return "index";
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public String upload(
            @RequestParam("files") MultipartFile[] files,
            Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        BulkUploadResult result = bulkUploadService.importAll(files);
        populate(model, result.getLines(), null, "upload");
        if ("true".equals(hxRequest)) {
            return "index :: page";
        }
        return "index";
    }

    @PostMapping("/calculate")
    public String calculate(
            @RequestParam(value = "period", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period,
            Model model,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        CalculationRunSummary summary = calculationService.calculateAll(period);
        populate(model, List.of(
                "Расчёт завершён: подбор формулы "
                        + String.format(java.util.Locale.ROOT, "%.0f", summary.getFormulaFoundRate()) + "%, "
                        + "цена по формуле "
                        + String.format(java.util.Locale.ROOT, "%.0f", summary.getPriceGivenFormulaRate()) + "%, "
                        + "полный набор компонентов "
                        + String.format(java.util.Locale.ROOT, "%.0f", summary.getComponentsResolvedRate()) + "% "
                        + "(" + summary.getTotalRows() + " строк, " + summary.getElapsedMs() + " ms)"
        ), summary, "results");
        if ("true".equals(hxRequest)) {
            return "index :: page";
        }
        return "index";
    }

    @GetMapping("/calculate/{id}/breakdown")
    public String breakdown(@PathVariable("id") Long id, Model model) {
        CalculationRowResult row = calculationService.loadBreakdown(id);
        DemandForecast forecast = null;
        if (row != null && row.getForecastId() != null) {
            forecast = demandForecastRepository.findById(row.getForecastId()).orElse(null);
        }
        model.addAttribute("breakdown", row);
        model.addAttribute("forecast", forecast);
        return "fragments/breakdown :: breakdown";
    }

    private void populate(Model model, List<String> uploadLines, CalculationRunSummary summaryOverride, String activeTab) {
        Map<String, Long> fileCounts = new HashMap<>();
        fileCounts.put("ssp", demandForecastRepository.count());
        fileCounts.put("formulas", formulaRepository.count());
        fileCounts.put("formula_components", formulaComponentRepository.count());
        fileCounts.put("term_types", termTypeRepository.count());
        fileCounts.put("quotes", quoteValueRepository.count());
        fileCounts.put("quote_mapping", quoteMappingRepository.count());
        fileCounts.put("currency_rates", currencyRateRepository.count());
        fileCounts.put("material_groups", materialGroupRepository.count());

        long loadedFiles = fileCounts.values().stream().filter(c -> c != null && c > 0).count();
        boolean readyToCalculate = fileCounts.getOrDefault("ssp", 0L) > 0
                && fileCounts.getOrDefault("formulas", 0L) > 0;

        CalculationRunSummary summary = summaryOverride;
        if (summary == null && calculationResultRepository.count() > 0) {
            summary = calculationService.loadSummaryFromDb();
        }

        List<CalculationResult> calcEntities = calculationResultRepository.findAll(
                PageRequest.of(0, RESULTS_LIMIT, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();

        Map<Long, DemandForecast> forecastById = new HashMap<>();
        for (DemandForecast f : demandForecastRepository.findAll()) {
            forecastById.put(f.getId(), f);
        }

        List<CalcRowView> calcRows = new ArrayList<>();
        for (CalculationResult r : calcEntities) {
            DemandForecast f = r.getForecastId() == null ? null : forecastById.get(r.getForecastId());
            String customer = f != null ? f.getCustomer() : r.getClientId();
            String material = f != null
                    ? ((f.getMtrNsiName() != null ? f.getMtrNsiName() : "") +
                    (f.getMtrNsiCode() != null ? " (" + f.getMtrNsiCode() + ")" : "")).trim()
                    : r.getMtrNsiCode();
            calcRows.add(new CalcRowView(r, customer, material, f != null ? f.getForecast() : null));
        }
        calcRows.sort(Comparator.comparingInt(CalcRowView::getSortRank)
                .thenComparing(v -> v.getResult().getId(), Comparator.nullsLast(Comparator.reverseOrder())));

        model.addAttribute("uploadLines", uploadLines);
        model.addAttribute("fileCounts", fileCounts);
        model.addAttribute("loadedFiles", loadedFiles);
        model.addAttribute("readyToCalculate", readyToCalculate);
        model.addAttribute("periods", demandForecastRepository.findDistinctPeriods());
        model.addAttribute("calcRows", calcRows);
        model.addAttribute("calculationSummary", summary);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("hasResults", calculationResultRepository.count() > 0);
    }
}
