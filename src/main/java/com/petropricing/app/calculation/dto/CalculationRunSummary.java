package com.petropricing.app.calculation.dto;

import com.petropricing.app.calculation.CalculationStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculationRunSummary {
    private int totalRows;
    private int successRows;
    private int formulaFoundRows;
    private int componentsResolvedRows;
    private long elapsedMs;
    private Map<String, Integer> statusCounts = new LinkedHashMap<>();
    private List<String> topReasons = new ArrayList<>();
    private int warningRows;
    private int multiFormulaRows;
    private int extendedValidityRows;

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessRows() {
        return successRows;
    }

    public void setSuccessRows(int successRows) {
        this.successRows = successRows;
    }

    public int getFormulaFoundRows() {
        return formulaFoundRows;
    }

    public void setFormulaFoundRows(int formulaFoundRows) {
        this.formulaFoundRows = formulaFoundRows;
    }

    public int getComponentsResolvedRows() {
        return componentsResolvedRows;
    }

    public void setComponentsResolvedRows(int componentsResolvedRows) {
        this.componentsResolvedRows = componentsResolvedRows;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public List<String> getTopReasons() {
        return topReasons;
    }

    public void setTopReasons(List<String> topReasons) {
        this.topReasons = topReasons;
    }

    public int getWarningRows() {
        return warningRows;
    }

    public void setWarningRows(int warningRows) {
        this.warningRows = warningRows;
    }

    public int getMultiFormulaRows() {
        return multiFormulaRows;
    }

    public void setMultiFormulaRows(int multiFormulaRows) {
        this.multiFormulaRows = multiFormulaRows;
    }

    public int getExtendedValidityRows() {
        return extendedValidityRows;
    }

    public void setExtendedValidityRows(int extendedValidityRows) {
        this.extendedValidityRows = extendedValidityRows;
    }

    public double getSuccessRate() {
        return totalRows == 0 ? 0.0 : (100.0 * successRows / totalRows);
    }

    /** Подбор формулы к строке спроса. */
    public double getFormulaFoundRate() {
        return totalRows == 0 ? 0.0 : (100.0 * formulaFoundRows / totalRows);
    }

    /** Расчёт цены по найденной формуле. */
    public double getPriceGivenFormulaRate() {
        return formulaFoundRows == 0 ? 0.0 : (100.0 * successRows / formulaFoundRows);
    }

    /** Полный набор значений компонентов из котировок/курсов/констант (доля от всех строк спроса). */
    public double getComponentsResolvedRate() {
        return totalRows == 0 ? 0.0 : (100.0 * componentsResolvedRows / totalRows);
    }

    public int countOf(CalculationStatus status) {
        return statusCounts.getOrDefault(status.name(), 0);
    }
}
