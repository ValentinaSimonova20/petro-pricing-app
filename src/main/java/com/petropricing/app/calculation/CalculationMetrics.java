package com.petropricing.app.calculation;

import com.petropricing.app.calculation.dto.CalculationRowResult;
import com.petropricing.app.calculation.dto.CalculationRunSummary;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CalculationMetrics {

    private CalculationMetrics() {
    }

    public static CalculationRunSummary build(List<CalculationRowResult> results) {
        CalculationRunSummary summary = new CalculationRunSummary();
        summary.setTotalRows(results.size());

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> reasonCounts = new HashMap<>();
        int success = 0;
        int formulaFound = 0;
        int componentsResolved = 0;
        int warningRows = 0;
        int multi = 0;
        int extended = 0;

        for (CalculationRowResult row : results) {
            CalculationStatus status = row.getStatus();
            String key = status == null ? "UNKNOWN" : status.name();
            statusCounts.merge(key, 1, Integer::sum);

            if (status != null && status.isSuccess()) {
                success++;
            }
            boolean hasFormula = row.getSelectedFormulaKey() != null
                    || row.getCandidateCount() > 0;
            if (hasFormula) {
                formulaFound++;
            }
            if (status != null && isComponentsResolved(status)) {
                componentsResolved++;
            }
            if (row.getWarnings() != null && !row.getWarnings().isEmpty()) {
                warningRows++;
            }
            if (row.getCandidateCount() > 1) {
                multi++;
            }
            if (row.isExtendedValidity()) {
                extended++;
            }
            if (row.getStatusReason() != null && !row.getStatusReason().isBlank()) {
                reasonCounts.merge(row.getStatusReason(), 1, Integer::sum);
            }
        }

        summary.setSuccessRows(success);
        summary.setFormulaFoundRows(formulaFound);
        summary.setComponentsResolvedRows(componentsResolved);
        summary.setStatusCounts(statusCounts);
        summary.setWarningRows(warningRows);
        summary.setMultiFormulaRows(multi);
        summary.setExtendedValidityRows(extended);
        summary.setTopReasons(
                reasonCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(8)
                        .map(e -> e.getValue() + "× " + e.getKey())
                        .collect(Collectors.toList())
        );
        return summary;
    }

    /** Полный набор значений компонентов: резолв прошёл (цена могла не посчитаться из‑за выражения). */
    private static boolean isComponentsResolved(CalculationStatus status) {
        return status.isSuccess()
                || status == CalculationStatus.EXPRESSION_ERROR
                || status == CalculationStatus.UNSUPPORTED_EXPRESSION;
    }
}
