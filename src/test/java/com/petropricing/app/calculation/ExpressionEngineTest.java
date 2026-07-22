package com.petropricing.app.calculation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionEngineTest {

    private final ExpressionEngine engine = new ExpressionEngine();

    @Test
    void evaluatesSimpleArithmetic() throws Exception {
        BigDecimal result = engine.evaluate("A + B * 2", Map.of(
                "A", new BigDecimal("10"),
                "B", new BigDecimal("3")
        ));
        assertEquals(new BigDecimal("16"), result);
    }

    @Test
    void failsOnUnknownVariable() {
        ExpressionEngine.EvaluationException ex = assertThrows(
                ExpressionEngine.EvaluationException.class,
                () -> engine.evaluate("A + B", Map.of("A", BigDecimal.ONE))
        );
        assertTrue(ex.getMessage().contains("B"));
    }
}
