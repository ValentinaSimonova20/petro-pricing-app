package com.petropricing.app.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "formula")
public class Formula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "formula_key")
    private String formulaKey;

    @Lob
    @Column(name = "formula_text")
    private String formulaText;

    @Column(name = "material_group_m")
    private String materialGroupM;

    @Column(name = "business_partner")
    private String businessPartner;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_on")
    private LocalDate createdOn;

    @Column(name = "inactive")
    private String inactive;

    @Column(name = "price_type")
    private String priceType;

    @Column(name = "currency_document")
    private String currencyDocument;

    @Column(name = "material_code")
    private String materialCode;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_name")
    private String clientName;

    public Long getId() {
        return id;
    }

    public String getFormulaKey() {
        return formulaKey;
    }

    public void setFormulaKey(String formulaKey) {
        this.formulaKey = formulaKey;
    }

    public String getFormulaText() {
        return formulaText;
    }

    public void setFormulaText(String formulaText) {
        this.formulaText = formulaText;
    }

    public String getMaterialGroupM() {
        return materialGroupM;
    }

    public void setMaterialGroupM(String materialGroupM) {
        this.materialGroupM = materialGroupM;
    }

    public String getBusinessPartner() {
        return businessPartner;
    }

    public void setBusinessPartner(String businessPartner) {
        this.businessPartner = businessPartner;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
    }

    public String getInactive() {
        return inactive;
    }

    public void setInactive(String inactive) {
        this.inactive = inactive;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public String getCurrencyDocument() {
        return currencyDocument;
    }

    public void setCurrencyDocument(String currencyDocument) {
        this.currencyDocument = currencyDocument;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
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

