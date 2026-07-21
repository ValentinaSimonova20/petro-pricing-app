package com.petropricing.app.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "quote_mapping")
public class QuoteMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_origin")
    private String quoteOrigin;

    @Column(name = "quote_kind")
    private String quoteKind;

    @Column(name = "quote_name")
    private String quoteName;

    @Column(name = "dl_id")
    private String dlId;

    @Column(name = "dl_id2")
    private String dlId2;

    @Column(name = "quote_currency")
    private String quoteCurrency;

    public Long getId() {
        return id;
    }

    public String getQuoteOrigin() {
        return quoteOrigin;
    }

    public void setQuoteOrigin(String quoteOrigin) {
        this.quoteOrigin = quoteOrigin;
    }

    public String getQuoteKind() {
        return quoteKind;
    }

    public void setQuoteKind(String quoteKind) {
        this.quoteKind = quoteKind;
    }

    public String getQuoteName() {
        return quoteName;
    }

    public void setQuoteName(String quoteName) {
        this.quoteName = quoteName;
    }

    public String getDlId() {
        return dlId;
    }

    public void setDlId(String dlId) {
        this.dlId = dlId;
    }

    public String getDlId2() {
        return dlId2;
    }

    public void setDlId2(String dlId2) {
        this.dlId2 = dlId2;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }
}

