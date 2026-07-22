package com.petropricing.app.calculation;

import com.petropricing.app.domain.DemandForecast;
import com.petropricing.app.domain.Formula;
import com.petropricing.app.domain.MaterialGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FormulaMatcher {

    public static class MatchCandidate {
        private final Formula formula;
        private final boolean extendedValidity;

        public MatchCandidate(Formula formula, boolean extendedValidity) {
            this.formula = formula;
            this.extendedValidity = extendedValidity;
        }

        public Formula getFormula() {
            return formula;
        }

        public boolean isExtendedValidity() {
            return extendedValidity;
        }
    }

    public static class MatchResult {
        private final List<MatchCandidate> candidates;
        private final MatchCandidate selected;
        private final boolean hadInactiveOnly;

        public MatchResult(List<MatchCandidate> candidates, MatchCandidate selected, boolean hadInactiveOnly) {
            this.candidates = candidates;
            this.selected = selected;
            this.hadInactiveOnly = hadInactiveOnly;
        }

        public List<MatchCandidate> getCandidates() {
            return candidates;
        }

        public MatchCandidate getSelected() {
            return selected;
        }

        public boolean isHadInactiveOnly() {
            return hadInactiveOnly;
        }
    }

    private final Map<String, Set<String>> groupsByMaterial;
    private final List<Formula> formulas;

    public FormulaMatcher(List<Formula> formulas, List<MaterialGroup> materialGroups) {
        this.formulas = formulas == null ? List.of() : formulas;
        this.groupsByMaterial = new HashMap<>();
        if (materialGroups != null) {
            for (MaterialGroup g : materialGroups) {
                String code = normalizeCode(g.getCodeNsi());
                String hname = blankToNull(g.getHname());
                if (code == null || hname == null) continue;
                groupsByMaterial.computeIfAbsent(code, k -> new HashSet<>()).add(hname);
            }
        }
    }

    public MatchResult match(DemandForecast row) {
        List<MatchCandidate> active = new ArrayList<>();
        boolean sawInactiveMatch = false;

        String clientId = blankToNull(row.getClientId());
        String material = normalizeCode(row.getMtrNsiCode());
        LocalDate period = row.getPeriod();

        for (Formula formula : formulas) {
            if (!Objects.equals(blankToNull(formula.getClientId()), clientId)) {
                continue;
            }
            if (!materialMatches(formula, material)) {
                continue;
            }

            boolean inactive = isInactive(formula.getInactive());
            ValidityCheck validity = checkValidity(formula, period);
            if (!validity.matches) {
                continue;
            }

            if (inactive) {
                sawInactiveMatch = true;
                continue;
            }

            active.add(new MatchCandidate(formula, validity.extended));
        }

        active.sort(Comparator
                .comparing((MatchCandidate c) -> c.getFormula().getCreatedOn(), Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(c -> c.getFormula().getFormulaKey(), Comparator.nullsLast(String::compareTo)));

        MatchCandidate selected = active.isEmpty() ? null : active.get(0);
        return new MatchResult(active, selected, sawInactiveMatch && active.isEmpty());
    }

    private boolean materialMatches(Formula formula, String materialCode) {
        String materialScope = normalizeCode(formula.getMaterialCode());
        String groupScope = blankToNull(formula.getMaterialGroupM());

        if (materialScope == null && groupScope == null) {
            return false;
        }
        if (materialCode == null) {
            return false;
        }
        if (materialScope != null && groupScope != null) {
            return materialScope.equals(materialCode)
                    || groupsByMaterial.getOrDefault(materialCode, Set.of()).contains(groupScope);
        }
        if (materialScope != null) {
            return materialScope.equals(materialCode);
        }
        return groupsByMaterial.getOrDefault(materialCode, Set.of()).contains(groupScope);
    }

    private static class ValidityCheck {
        final boolean matches;
        final boolean extended;

        ValidityCheck(boolean matches, boolean extended) {
            this.matches = matches;
            this.extended = extended;
        }
    }

    private ValidityCheck checkValidity(Formula formula, LocalDate period) {
        if (period == null) {
            return new ValidityCheck(false, false);
        }
        LocalDate from = toLocalDate(formula.getValidFrom());
        LocalDate to = formula.getValidTo();

        if (from != null && period.isBefore(from)) {
            return new ValidityCheck(false, false);
        }
        if (to != null && period.isAfter(to)) {
            // MVP: extend validity to forecast period
            return new ValidityCheck(true, true);
        }
        return new ValidityCheck(true, false);
    }

    private static boolean isInactive(String inactive) {
        if (inactive == null) return false;
        String v = inactive.trim();
        return v.equalsIgnoreCase("X") || v.equalsIgnoreCase("true") || v.equals("1");
    }

    private static LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeCode(String s) {
        String v = blankToNull(s);
        if (v == null) return null;
        // strip trailing .0 from numeric strings
        if (v.matches("^-?\\d+\\.0+$")) {
            v = v.substring(0, v.indexOf('.'));
        }
        return v;
    }

    public static boolean isSpot(String contract) {
        return contract != null && contract.trim().equalsIgnoreCase("Spot");
    }
}
