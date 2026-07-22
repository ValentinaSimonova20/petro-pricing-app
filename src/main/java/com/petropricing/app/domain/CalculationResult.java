package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "calculation_result")
public class CalculationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "forecast_id")
    private Long forecastId;

    @Column(name = "row_id")
    private Integer rowId;

    @Column(name = "period")
    private LocalDate period;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "mtr_nsi_code")
    private String mtrNsiCode;

    @Column(name = "contract")
    private String contract;

    @Column(name = "status")
    private String status;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "selected_formula_key")
    private String selectedFormulaKey;

    @Lob
    @Column(name = "candidate_formula_keys")
    private String candidateFormulaKeys;

    @Lob
    @Column(name = "formula_text")
    private String formulaText;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "currency")
    private String currency;

    @Column(name = "extended_validity")
    private Boolean extendedValidity;

    @Column(name = "candidate_count")
    private Integer candidateCount;

    @Lob
    @Column(name = "warnings_json")
    private String warningsJson;

    @Lob
    @Column(name = "components_json")
    private String componentsJson;

    public Long getId() {
        return id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getCandidateFormulaKeys() {
        return candidateFormulaKeys;
    }

    public void setCandidateFormulaKeys(String candidateFormulaKeys) {
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

    public Boolean getExtendedValidity() {
        return extendedValidity;
    }

    public void setExtendedValidity(Boolean extendedValidity) {
        this.extendedValidity = extendedValidity;
    }

    public Integer getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(Integer candidateCount) {
        this.candidateCount = candidateCount;
    }

    public String getWarningsJson() {
        return warningsJson;
    }

    public void setWarningsJson(String warningsJson) {
        this.warningsJson = warningsJson;
    }

    public String getComponentsJson() {
        return componentsJson;
    }

    public void setComponentsJson(String componentsJson) {
        this.componentsJson = componentsJson;
    }
}
