package com.petropricing.app.web;

import com.petropricing.app.domain.CalculationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalcRowView {
    private final CalculationResult result;
    private final String customer;
    private final String materialName;
    private final BigDecimal forecastTons;
    private final String badgeKind;

    public CalcRowView(CalculationResult result, String customer, String materialName, BigDecimal forecastTons) {
        this.result = result;
        this.customer = customer;
        this.materialName = materialName;
        this.forecastTons = forecastTons;
        this.badgeKind = mapBadge(result.getStatus());
    }

    private static String mapBadge(String status) {
        if (status == null) return "error";
        return switch (status) {
            case "OK", "OK_EXTENDED_VALIDITY" -> "ok";
            case "OK_MULTI_FORMULA" -> "warn";
            default -> "error";
        };
    }

    public CalculationResult getResult() {
        return result;
    }

    public String getCustomer() {
        return customer;
    }

    public String getMaterialName() {
        return materialName;
    }

    public BigDecimal getForecastTons() {
        return forecastTons;
    }

    public String getBadgeKind() {
        return badgeKind;
    }

    public String getBadgeLabel() {
        return switch (badgeKind) {
            case "ok" -> "посчитано";
            case "warn" -> "неск. формул";
            default -> "ошибка";
        };
    }

    public BigDecimal getAmount() {
        if (result.getPrice() == null || forecastTons == null) return null;
        return result.getPrice().multiply(forecastTons).setScale(2, RoundingMode.HALF_UP);
    }

    public int getSortRank() {
        return switch (badgeKind) {
            case "error" -> 0;
            case "warn" -> 1;
            default -> 2;
        };
    }
}
