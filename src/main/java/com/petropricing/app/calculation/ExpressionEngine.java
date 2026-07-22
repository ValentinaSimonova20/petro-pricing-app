package com.petropricing.app.calculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safe arithmetic evaluator for MVP formulas: + - * / ( ) and numeric literals / variables.
 */
public class ExpressionEngine {

    public static class EvaluationException extends Exception {
        private final boolean unsupported;

        public EvaluationException(String message, boolean unsupported) {
            super(message);
            this.unsupported = unsupported;
        }

        public boolean isUnsupported() {
            return unsupported;
        }
    }

    private static final Pattern UNSUPPORTED = Pattern.compile(
            "(?i)(if\\s*\\(|тогда|иначе|case\\s+|when\\s+|&&|\\|\\||==|!=|<>|>=|<=|[а-яА-Я]{3,})"
    );

    private static final Pattern VAR_TOKEN = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");

    public BigDecimal evaluate(String expression, Map<String, BigDecimal> variables) throws EvaluationException {
        if (expression == null || expression.isBlank()) {
            throw new EvaluationException("Пустое выражение формулы", false);
        }
        String expr = expression.trim();
        if (UNSUPPORTED.matcher(expr).find() && containsNonAsciiLogic(expr)) {
            // Cyrillic words or logical ops beyond arithmetic
            if (expr.matches("(?s).*[а-яА-Я].*") || expr.contains("&&") || expr.contains("||")
                    || expr.toLowerCase(Locale.ROOT).contains("if(")) {
                throw new EvaluationException("Выражение содержит конструкции вне MVP-движка", true);
            }
        }

        String substituted = substituteVariables(expr, variables);
        try {
            return new Parser(substituted).parseExpression().stripTrailingZeros();
        } catch (EvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException("Ошибка разбора выражения: " + e.getMessage(), false);
        }
    }

    private boolean containsNonAsciiLogic(String expr) {
        return expr.matches("(?s).*[а-яА-Я].*")
                || expr.contains("&&")
                || expr.contains("||")
                || expr.toLowerCase(Locale.ROOT).contains("if(");
    }

    private String substituteVariables(String expression, Map<String, BigDecimal> variables) throws EvaluationException {
        Matcher m = VAR_TOKEN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        List<String> missing = new ArrayList<>();
        while (m.find()) {
            String name = m.group();
            // skip pure numbers that somehow matched - VAR_TOKEN won't
            if (variables.containsKey(name)) {
                BigDecimal val = variables.get(name);
                m.appendReplacement(sb, Matcher.quoteReplacement(val.toPlainString()));
            } else if (isFunctionOrKeyword(name)) {
                throw new EvaluationException("Неподдерживаемая функция/конструкция: " + name, true);
            } else {
                missing.add(name);
            }
        }
        m.appendTail(sb);
        if (!missing.isEmpty()) {
            throw new EvaluationException("Неизвестные переменные: " + String.join(", ", missing), false);
        }
        return sb.toString();
    }

    private boolean isFunctionOrKeyword(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals("if") || lower.equals("min") || lower.equals("max")
                || lower.equals("abs") || lower.equals("round") || lower.equals("pow");
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s.replace(" ", "");
        }

        BigDecimal parseExpression() throws EvaluationException {
            BigDecimal value = parseTerm();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') {
                    pos++;
                    value = value.add(parseTerm());
                } else if (c == '-') {
                    pos++;
                    value = value.subtract(parseTerm());
                } else {
                    break;
                }
            }
            if (pos != s.length()) {
                throw new EvaluationException("Неожиданный символ у позиции " + pos + ": '" + s.charAt(pos) + "'", false);
            }
            return value;
        }

        private BigDecimal parseTerm() throws EvaluationException {
            BigDecimal value = parseFactor();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '*') {
                    pos++;
                    value = value.multiply(parseFactor(), MathContext.DECIMAL64);
                } else if (c == '/') {
                    pos++;
                    BigDecimal divisor = parseFactor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new EvaluationException("Деление на ноль", false);
                    }
                    value = value.divide(divisor, 10, RoundingMode.HALF_UP);
                } else {
                    break;
                }
            }
            return value;
        }

        private BigDecimal parseFactor() throws EvaluationException {
            if (pos >= s.length()) {
                throw new EvaluationException("Неожиданный конец выражения", false);
            }
            char c = s.charAt(pos);
            if (c == '+') {
                pos++;
                return parseFactor();
            }
            if (c == '-') {
                pos++;
                return parseFactor().negate();
            }
            if (c == '(') {
                pos++;
                BigDecimal value = parseExpressionInner();
                if (pos >= s.length() || s.charAt(pos) != ')') {
                    throw new EvaluationException("Ожидалась закрывающая скобка", false);
                }
                pos++;
                return value;
            }
            return parseNumber();
        }

        private BigDecimal parseExpressionInner() throws EvaluationException {
            BigDecimal value = parseTerm();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') {
                    pos++;
                    value = value.add(parseTerm());
                } else if (c == '-') {
                    pos++;
                    value = value.subtract(parseTerm());
                } else {
                    break;
                }
            }
            return value;
        }

        private BigDecimal parseNumber() throws EvaluationException {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.' ) {
                    pos++;
                } else {
                    break;
                }
            }
            if (start == pos) {
                throw new EvaluationException("Ожидалось число у позиции " + pos, false);
            }
            try {
                return new BigDecimal(s.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new EvaluationException("Некорректное число: " + s.substring(start, pos), false);
            }
        }
    }
}
