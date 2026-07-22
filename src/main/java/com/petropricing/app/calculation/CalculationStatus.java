package com.petropricing.app.calculation;

public enum CalculationStatus {
    OK,
    OK_EXTENDED_VALIDITY,
    OK_MULTI_FORMULA,
    NO_FORMULA,
    SPOT_NO_FORMULA,
    INACTIVE_ONLY,
    MISSING_COMPONENTS,
    MISSING_QUOTE,
    MISSING_QUOTE_MAPPING,
    MISSING_FX,
    MISSING_CONSTANT,
    AMBIGUOUS_MATERIAL_SCOPE,
    EXPRESSION_ERROR,
    UNSUPPORTED_EXPRESSION,
    DATA_INCONSISTENT,
    INTERNAL_ERROR;

    public boolean isSuccess() {
        return this == OK || this == OK_EXTENDED_VALIDITY || this == OK_MULTI_FORMULA;
    }
}
