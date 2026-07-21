package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "demand_forecast")
public class DemandForecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row_id")
    private Integer rowId;

    @Column(name = "period")
    private LocalDate period;

    @Column(name = "customer_asv")
    private String customerAsv;

    @Column(name = "customer")
    private String customer;

    @Column(name = "mtr_nsi_code")
    private String mtrNsiCode;

    @Column(name = "mtr_nsi_name")
    private String mtrNsiName;

    @Column(name = "contract")
    private String contract;

    @Column(name = "currency")
    private String currency;

    @Column(name = "forecast")
    private BigDecimal forecast;

    @Column(name = "country")
    private String country;

    @Column(name = "region")
    private String region;

    @Column(name = "market")
    private String market;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_name")
    private String clientName;

    public Long getId() {
        return id;
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

    public String getCustomerAsv() {
        return customerAsv;
    }

    public void setCustomerAsv(String customerAsv) {
        this.customerAsv = customerAsv;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getMtrNsiCode() {
        return mtrNsiCode;
    }

    public void setMtrNsiCode(String mtrNsiCode) {
        this.mtrNsiCode = mtrNsiCode;
    }

    public String getMtrNsiName() {
        return mtrNsiName;
    }

    public void setMtrNsiName(String mtrNsiName) {
        this.mtrNsiName = mtrNsiName;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getForecast() {
        return forecast;
    }

    public void setForecast(BigDecimal forecast) {
        this.forecast = forecast;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}

