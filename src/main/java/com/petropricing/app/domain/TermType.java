package com.petropricing.app.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "term_type")
public class TermType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_label")
    private String typeLabel;

    @Column(name = "category")
    private String category;

    @Column(name = "fixed_term_kind")
    private String fixedTermKind;

    @Column(name = "description")
    private String description;

    @Column(name = "value_source")
    private String valueSource;

    public Long getId() {
        return id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFixedTermKind() {
        return fixedTermKind;
    }

    public void setFixedTermKind(String fixedTermKind) {
        this.fixedTermKind = fixedTermKind;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValueSource() {
        return valueSource;
    }

    public void setValueSource(String valueSource) {
        this.valueSource = valueSource;
    }
}

