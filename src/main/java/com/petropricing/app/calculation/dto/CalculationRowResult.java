package com.petropricing.app.calculation.dto;

import com.petropricing.app.calculation.CalculationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalculationRowResult {
    private Long forecastId;
    private Integer rowId;
    private LocalDate period;
    private String clientId;
    private String mtrNsiCode;
    private String contract;
    private CalculationStatus status;
    private String statusReason;
    private String selectedFormulaKey;
    private List<String> candidateFormulaKeys = new ArrayList<>();
    private String formulaText;
    private BigDecimal price;
    private String currency;
    private boolean extendedValidity;
    private List<String> warnings = new ArrayList<>();
    private List<ComponentBreakdown> components = new ArrayList<>();

    public Long getForecastId() {
        return forecastId;
    }

    public void setForecastId(Long forecastId) {
        this.forecastId = forecastId;
    }

    public Integer getRowId() {
        return rowId;
    }

    public void setRowId(Integer rowId) {
        this.rowId = rowId;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMtrNsiCode() {
        return mtrNsiCode;
    }

    public void setMtrNsiCode(String mtrNsiCode) {
        this.mtrNsiCode = mtrNsiCode;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public CalculationStatus getStatus() {
        return status;
    }

    public void setStatus(CalculationStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getSelectedFormulaKey() {
        return selectedFormulaKey;
    }

    public void setSelectedFormulaKey(String selectedFormulaKey) {
        this.selectedFormulaKey = selectedFormulaKey;
    }

    public List<String> getCandidateFormulaKeys() {
        return candidateFormulaKeys;
    }

    public void setCandidateFormulaKeys(List<String> candidateFormulaKeys) {
        this.candidateFormulaKeys = candidateFormulaKeys;
    }

    public String getFormulaText() {
        return formulaText;
    }

    public void setFormulaText(String formulaText) {
        this.formulaText = formulaText;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isExtendedValidity() {
        return extendedValidity;
    }

    public void setExtendedValidity(boolean extendedValidity) {
        this.extendedValidity = extendedValidity;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<ComponentBreakdown> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentBreakdown> components) {
        this.components = components;
    }

    public int getCandidateCount() {
        return candidateFormulaKeys == null ? 0 : candidateFormulaKeys.size();
    }
}
