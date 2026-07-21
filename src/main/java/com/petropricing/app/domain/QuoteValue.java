package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_value")
public class QuoteValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_type")
    private String quoteType;

    @Column(name = "quote_name")
    private String quoteName;

    @Column(name = "tech_quote_name")
    private String techQuoteName;

    @Column(name = "quote_code")
    private String quoteCode;

    @Column(name = "quote_date")
    private LocalDate quoteDate;

    @Column(name = "quote_currency")
    private String quoteCurrency;

    @Column(name = "quote_val")
    private BigDecimal quoteVal;

    @Column(name = "tech_load_ts")
    private LocalDateTime techLoadTs;

    public Long getId() {
        return id;
    }

    public String getQuoteType() {
        return quoteType;
    }

    public void setQuoteType(String quoteType) {
        this.quoteType = quoteType;
    }

    public String getQuoteName() {
        return quoteName;
    }

    public void setQuoteName(String quoteName) {
        this.quoteName = quoteName;
    }

    public String getTechQuoteName() {
        return techQuoteName;
    }

    public void setTechQuoteName(String techQuoteName) {
        this.techQuoteName = techQuoteName;
    }

    public String getQuoteCode() {
        return quoteCode;
    }

    public void setQuoteCode(String quoteCode) {
        this.quoteCode = quoteCode;
    }

    public LocalDate getQuoteDate() {
        return quoteDate;
    }

    public void setQuoteDate(LocalDate quoteDate) {
        this.quoteDate = quoteDate;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }

    public BigDecimal getQuoteVal() {
        return quoteVal;
    }

    public void setQuoteVal(BigDecimal quoteVal) {
        this.quoteVal = quoteVal;
    }

    public LocalDateTime getTechLoadTs() {
        return techLoadTs;
    }

    public void setTechLoadTs(LocalDateTime techLoadTs) {
        this.techLoadTs = techLoadTs;
    }
}

