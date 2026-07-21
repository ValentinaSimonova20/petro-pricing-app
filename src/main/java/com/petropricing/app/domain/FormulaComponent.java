package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "formula_component")
public class FormulaComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "formula_key")
    private String formulaKey;

    @Column(name = "term_number")
    private Integer termNumber;

    @Column(name = "condition_type")
    private String conditionType;

    @Column(name = "variable_name")
    private String variableName;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "term_type_code")
    private String termTypeCode;

    @Column(name = "value_amount")
    private BigDecimal valueAmount;

    @Column(name = "term_currency")
    private String termCurrency;

    @Column(name = "quote_name")
    private String quoteName;

    @Column(name = "quote_kind")
    private String quoteKind;

    @Column(name = "calc_rule")
    private Integer calcRule;

    @Column(name = "currency_period_rule_code")
    private String currencyPeriodRuleCode;

    @Column(name = "currency_period_rule")
    private String currencyPeriodRule;

    @Column(name = "term_calc_kind")
    private Integer termCalcKind;

    @Column(name = "factor1")
    private Integer factor1;

    @Column(name = "factor2")
    private Integer factor2;

    @Column(name = "quote_origin")
    private String quoteOrigin;

    public Long getId() {
        return id;
    }

    public String getFormulaKey() {
        return formulaKey;
    }

    public void setFormulaKey(String formulaKey) {
        this.formulaKey = formulaKey;
    }

    public Integer getTermNumber() {
        return termNumber;
    }

    public void setTermNumber(Integer termNumber) {
        this.termNumber = termNumber;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public String getTermTypeCode() {
        return termTypeCode;
    }

    public void setTermTypeCode(String termTypeCode) {
        this.termTypeCode = termTypeCode;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public void setValueAmount(BigDecimal valueAmount) {
        this.valueAmount = valueAmount;
    }

    public String getTermCurrency() {
        return termCurrency;
    }

    public void setTermCurrency(String termCurrency) {
        this.termCurrency = termCurrency;
    }

    public String getQuoteName() {
        return quoteName;
    }

    public void setQuoteName(String quoteName) {
        this.quoteName = quoteName;
    }

    public String getQuoteKind() {
        return quoteKind;
    }

    public void setQuoteKind(String quoteKind) {
        this.quoteKind = quoteKind;
    }

    public Integer getCalcRule() {
        return calcRule;
    }

    public void setCalcRule(Integer calcRule) {
        this.calcRule = calcRule;
    }

    public String getCurrencyPeriodRuleCode() {
        return currencyPeriodRuleCode;
    }

    public void setCurrencyPeriodRuleCode(String currencyPeriodRuleCode) {
        this.currencyPeriodRuleCode = currencyPeriodRuleCode;
    }

    public String getCurrencyPeriodRule() {
        return currencyPeriodRule;
    }

    public void setCurrencyPeriodRule(String currencyPeriodRule) {
        this.currencyPeriodRule = currencyPeriodRule;
    }

    public Integer getTermCalcKind() {
        return termCalcKind;
    }

    public void setTermCalcKind(Integer termCalcKind) {
        this.termCalcKind = termCalcKind;
    }

    public Integer getFactor1() {
        return factor1;
    }

    public void setFactor1(Integer factor1) {
        this.factor1 = factor1;
    }

    public Integer getFactor2() {
        return factor2;
    }

    public void setFactor2(Integer factor2) {
        this.factor2 = factor2;
    }

    public String getQuoteOrigin() {
        return quoteOrigin;
    }

    public void setQuoteOrigin(String quoteOrigin) {
        this.quoteOrigin = quoteOrigin;
    }
}

