package com.petropricing.app.calculation;

import com.petropricing.app.calculation.dto.ComponentBreakdown;
import com.petropricing.app.domain.CurrencyRate;
import com.petropricing.app.domain.FormulaComponent;
import com.petropricing.app.domain.QuoteMapping;
import com.petropricing.app.domain.QuoteValue;
import com.petropricing.app.domain.TermType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ValueResolver {

    public static class ResolveOutcome {
        private final List<ComponentBreakdown> components;
        private final Map<String, BigDecimal> variables;
        private final CalculationStatus failureStatus;
        private final String failureReason;

        private ResolveOutcome(List<ComponentBreakdown> components,
                               Map<String, BigDecimal> variables,
                               CalculationStatus failureStatus,
                               String failureReason) {
            this.components = components;
            this.variables = variables;
            this.failureStatus = failureStatus;
            this.failureReason = failureReason;
        }

        public static ResolveOutcome ok(List<ComponentBreakdown> components, Map<String, BigDecimal> variables) {
            return new ResolveOutcome(components, variables, null, null);
        }

        public static ResolveOutcome fail(List<ComponentBreakdown> components,
                                          CalculationStatus status,
                                          String reason) {
            return new ResolveOutcome(components, Map.of(), status, reason);
        }

        public boolean isOk() {
            return failureStatus == null;
        }

        public List<ComponentBreakdown> getComponents() {
            return components;
        }

        public Map<String, BigDecimal> getVariables() {
            return variables;
        }

        public CalculationStatus getFailureStatus() {
            return failureStatus;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    private static final List<String> QUOTE_TYPE_PRIORITY = List.of("Факт", "ОФ", "ППР");

    private final Map<String, TermType> termTypesByCode;
    private final List<QuoteMapping> quoteMappings;
    private final List<QuoteValue> quotes;
    private final List<CurrencyRate> rates;

    public ValueResolver(List<TermType> termTypes,
                         List<QuoteMapping> quoteMappings,
                         List<QuoteValue> quotes,
                         List<CurrencyRate> rates) {
        this.termTypesByCode = new HashMap<>();
        if (termTypes != null) {
            for (TermType t : termTypes) {
                if (t.getTypeCode() != null) {
                    termTypesByCode.put(t.getTypeCode().trim(), t);
                }
            }
        }
        this.quoteMappings = quoteMappings == null ? List.of() : quoteMappings;
        this.quotes = quotes == null ? List.of() : quotes;
        this.rates = rates == null ? List.of() : rates;
    }

    public ResolveOutcome resolve(List<FormulaComponent> components, LocalDate period) {
        List<ComponentBreakdown> breakdown = new ArrayList<>();
        Map<String, BigDecimal> variables = new HashMap<>();

        if (components == null || components.isEmpty()) {
            return ResolveOutcome.fail(breakdown, CalculationStatus.MISSING_COMPONENTS,
                    "У формулы нет компонентов (термов)");
        }

        List<FormulaComponent> ordered = new ArrayList<>(components);
        ordered.sort(Comparator.comparing(FormulaComponent::getTermNumber, Comparator.nullsLast(Integer::compareTo)));

        for (FormulaComponent component : ordered) {
            ComponentBreakdown item = new ComponentBreakdown();
            String varName = blankToNull(component.getVariableName());
            item.setVariableName(varName);
            item.setTermTypeCode(component.getTermTypeCode());

            TermType termType = termTypesByCode.get(
                    component.getTermTypeCode() == null ? null : component.getTermTypeCode().trim()
            );
            String category = termType != null ? termType.getCategory() : null;
            item.setCategory(category);

            CategoryKind kind = classify(category, component.getTermTypeCode(), component);

            CalculationStatus failStatus = null;
            String failReason = null;

            switch (kind) {
                case CONSTANT -> {
                    if (component.getValueAmount() == null) {
                        item.setResolved(false);
                        item.setNote("Пустое значение константы");
                        failStatus = CalculationStatus.MISSING_CONSTANT;
                        failReason = "Нет значения константы для переменной " + varName;
                    } else {
                        item.setValue(component.getValueAmount());
                        item.setSource("formula_components.Значение");
                        item.setResolved(true);
                    }
                }
                case QUOTE -> {
                    QuoteResolve qr = resolveQuote(component, period);
                    item.setSource(qr.source);
                    item.setValueDate(qr.valueDate);
                    item.setNote(qr.note);
                    if (qr.value != null) {
                        item.setValue(qr.value);
                        item.setResolved(true);
                    } else {
                        item.setResolved(false);
                        failStatus = qr.status;
                        failReason = qr.note;
                    }
                }
                case FX -> {
                    FxResolve fx = resolveFx(component, period);
                    item.setSource(fx.source);
                    item.setValueDate(fx.valueDate);
                    item.setNote(fx.note);
                    if (fx.value != null) {
                        item.setValue(fx.value);
                        item.setResolved(true);
                    } else {
                        item.setResolved(false);
                        failStatus = CalculationStatus.MISSING_FX;
                        failReason = fx.note;
                    }
                }
                case SKIP -> {
                    // grouping / price-list / unknown without required value — skip variable binding
                    if (component.getValueAmount() != null) {
                        item.setValue(component.getValueAmount());
                        item.setSource("formula_components.Значение (fallback)");
                        item.setResolved(true);
                        item.setNote("Категория '" + category + "': использовано числовое значение терма");
                    } else {
                        item.setResolved(false);
                        item.setNote("Категория '" + category + "' пропущена (нет значения)");
                    }
                }
            }

            breakdown.add(item);
            if (failStatus != null) {
                return ResolveOutcome.fail(breakdown, failStatus, failReason);
            }
            if (varName != null && item.isResolved() && item.getValue() != null) {
                variables.put(varName, item.getValue());
            }
        }

        return ResolveOutcome.ok(breakdown, variables);
    }

    private enum CategoryKind { CONSTANT, QUOTE, FX, SKIP }

    private CategoryKind classify(String category, String typeCode, FormulaComponent component) {
        String cat = category == null ? "" : category.toLowerCase(Locale.ROOT);
        if (cat.contains("котиров")) return CategoryKind.QUOTE;
        if (cat.contains("курс")) return CategoryKind.FX;
        if (cat.contains("констант") || cat.contains("фикс")) return CategoryKind.CONSTANT;
        if ("5".equals(typeCode)) return CategoryKind.FX;
        if (component.getQuoteName() != null && !component.getQuoteName().isBlank()) {
            // heuristic: named quote/rate
            if (cat.contains("курс") || looksLikeFxName(component.getQuoteName())) {
                return CategoryKind.FX;
            }
            return CategoryKind.QUOTE;
        }
        if (component.getValueAmount() != null) return CategoryKind.CONSTANT;
        if (cat.contains("групп") || cat.contains("прайс")) return CategoryKind.SKIP;
        return CategoryKind.SKIP;
    }

    private boolean looksLikeFxName(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        return n.contains("USD") || n.contains("EUR") || n.contains("CNY") || n.contains("CUR") || n.contains("FX");
    }

    private static class QuoteResolve {
        BigDecimal value;
        String source;
        LocalDate valueDate;
        String note;
        CalculationStatus status;
    }

    private QuoteResolve resolveQuote(FormulaComponent component, LocalDate period) {
        QuoteResolve out = new QuoteResolve();
        String quoteName = blankToNull(component.getQuoteName());
        String origin = blankToNull(component.getQuoteOrigin());
        if (quoteName == null) {
            out.status = CalculationStatus.MISSING_QUOTE;
            out.note = "У терма не задано имя котировки";
            return out;
        }

        QuoteMapping mapping = findMapping(quoteName, origin);
        if (mapping == null) {
            out.status = CalculationStatus.MISSING_QUOTE_MAPPING;
            out.note = "Нет маппинга котировки: " + quoteName
                    + (origin == null ? "" : " / origin=" + origin);
            return out;
        }
        String quoteCode = blankToNull(mapping.getDlId());
        if (quoteCode == null) {
            out.status = CalculationStatus.MISSING_QUOTE_MAPPING;
            out.note = "В маппинге пустой ID в озере данных для " + quoteName;
            return out;
        }

        LocalDate targetDate = period;
        QuoteValue best = null;
        for (String type : QUOTE_TYPE_PRIORITY) {
            best = findQuote(quoteCode, targetDate, type);
            if (best != null) break;
        }
        if (best == null) {
            // nearest date same month fallback
            for (String type : QUOTE_TYPE_PRIORITY) {
                best = findNearestQuoteInMonth(quoteCode, targetDate, type);
                if (best != null) break;
            }
        }
        if (best == null) {
            out.status = CalculationStatus.MISSING_QUOTE;
            out.note = "Нет котировки " + quoteName + " (" + quoteCode + ") на период " + targetDate
                    + " (приоритет Факт→ОФ→ППР)";
            return out;
        }

        out.value = best.getQuoteVal();
        out.valueDate = best.getQuoteDate();
        out.source = "quotes." + best.getQuoteType() + " / " + quoteCode;
        out.note = "quote_type=" + best.getQuoteType();
        return out;
    }

    private QuoteMapping findMapping(String quoteName, String origin) {
        QuoteMapping byBoth = null;
        QuoteMapping byName = null;
        for (QuoteMapping m : quoteMappings) {
            if (!Objects.equals(blankToNull(m.getQuoteName()), quoteName)) continue;
            if (origin != null && Objects.equals(blankToNull(m.getQuoteOrigin()), origin)) {
                byBoth = m;
                break;
            }
            if (byName == null) byName = m;
        }
        return byBoth != null ? byBoth : byName;
    }

    private QuoteValue findQuote(String quoteCode, LocalDate date, String quoteType) {
        for (QuoteValue q : quotes) {
            if (!Objects.equals(blankToNull(q.getQuoteCode()), quoteCode)) continue;
            if (!Objects.equals(blankToNull(q.getQuoteType()), quoteType)) continue;
            if (date != null && Objects.equals(q.getQuoteDate(), date)) {
                return q;
            }
        }
        return null;
    }

    private QuoteValue findNearestQuoteInMonth(String quoteCode, LocalDate period, String quoteType) {
        if (period == null) return null;
        QuoteValue best = null;
        long bestDist = Long.MAX_VALUE;
        for (QuoteValue q : quotes) {
            if (!Objects.equals(blankToNull(q.getQuoteCode()), quoteCode)) continue;
            if (!Objects.equals(blankToNull(q.getQuoteType()), quoteType)) continue;
            if (q.getQuoteDate() == null) continue;
            if (q.getQuoteDate().getYear() != period.getYear() || q.getQuoteDate().getMonth() != period.getMonth()) {
                continue;
            }
            long dist = Math.abs(q.getQuoteDate().toEpochDay() - period.toEpochDay());
            if (dist < bestDist) {
                bestDist = dist;
                best = q;
            }
        }
        return best;
    }

    private static class FxResolve {
        BigDecimal value;
        String source;
        LocalDate valueDate;
        String note;
    }

    private FxResolve resolveFx(FormulaComponent component, LocalDate period) {
        FxResolve out = new FxResolve();
        String currency = extractCurrency(component);
        if (currency == null) {
            out.note = "Не удалось определить валюту курса для переменной " + component.getVariableName();
            return out;
        }
        LocalDate target = period;
        CurrencyRate exact = findRate(currency, target);
        if (exact == null) {
            exact = findNearestRateInMonth(currency, target);
        }
        if (exact == null) {
            out.note = "Нет курса " + currency + " на период " + target;
            return out;
        }
        out.value = exact.getCurrencyValue();
        out.valueDate = exact.getCalday();
        out.source = "currency_rates." + currency;
        out.note = "calday=" + exact.getCalday() + (Objects.equals(exact.getCalday(), target) ? "" : " (ближайший день месяца)");
        return out;
    }

    private String extractCurrency(FormulaComponent component) {
        if (component.getTermCurrency() != null && !component.getTermCurrency().isBlank()) {
            return component.getTermCurrency().trim().toUpperCase(Locale.ROOT);
        }
        String name = component.getQuoteName();
        if (name != null) {
            String upper = name.toUpperCase(Locale.ROOT);
            for (String c : List.of("USD", "EUR", "CNY", "RUB")) {
                if (upper.contains(c)) return c;
            }
        }
        String var = component.getVariableName();
        if (var != null) {
            String upper = var.toUpperCase(Locale.ROOT);
            for (String c : List.of("USD", "EUR", "CNY", "RUB")) {
                if (upper.contains(c)) return c;
            }
        }
        return null;
    }

    private CurrencyRate findRate(String currency, LocalDate date) {
        for (CurrencyRate r : rates) {
            if (!currency.equalsIgnoreCase(blankToNull(r.getCurrencyName()))) continue;
            if (date != null && Objects.equals(r.getCalday(), date)) return r;
        }
        return null;
    }

    private CurrencyRate findNearestRateInMonth(String currency, LocalDate period) {
        if (period == null) return null;
        CurrencyRate best = null;
        long bestDist = Long.MAX_VALUE;
        for (CurrencyRate r : rates) {
            if (!currency.equalsIgnoreCase(blankToNull(r.getCurrencyName()))) continue;
            if (r.getCalday() == null) continue;
            if (r.getCalday().getYear() != period.getYear() || r.getCalday().getMonth() != period.getMonth()) continue;
            long dist = Math.abs(r.getCalday().toEpochDay() - period.toEpochDay());
            if (dist < bestDist) {
                bestDist = dist;
                best = r;
            }
        }
        return best;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
