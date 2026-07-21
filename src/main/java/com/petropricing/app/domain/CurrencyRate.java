package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "currency_rate")
public class CurrencyRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_type")
    private String versionType;

    @Column(name = "currency_name")
    private String currencyName;

    @Column(name = "calmonth")
    private String calmonth;

    @Column(name = "calday")
    private LocalDate calday;

    @Column(name = "currency_value")
    private BigDecimal currencyValue;

    public Long getId() {
        return id;
    }

    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public String getCalmonth() {
        return calmonth;
    }

    public void setCalmonth(String calmonth) {
        this.calmonth = calmonth;
    }

    public LocalDate getCalday() {
        return calday;
    }

    public void setCalday(LocalDate calday) {
        this.calday = calday;
    }

    public BigDecimal getCurrencyValue() {
        return currencyValue;
    }

    public void setCurrencyValue(BigDecimal currencyValue) {
        this.currencyValue = currencyValue;
    }
}

