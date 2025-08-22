package com.abada.engine.util;


import java.util.Map;

public final class ConditionEvaluator {

    // Evaluate a condition against variables. Returns false on any error.
    public static boolean evaluate(String rawExpr, Map<String, Object> vars) {
        if (rawExpr == null || rawExpr.isBlank()) return false;

        // 1) Normalize Camunda/EL `${...}` to a plain JS expression
        String expr = rawExpr.trim();
        // Remove wrapping <![CDATA[ ... ]]> if present
        expr = expr.replaceAll("^<!\\[CDATA\\[|\\]\\]>$", "").trim();
        // If it's ${...}, extract the inside
        var m = java.util.regex.Pattern.compile("^\\$\\{(.*)}$").matcher(expr);
        if (m.find()) {
            expr = m.group(1).trim();
        }

        // Optional: simple operator aliases often seen in EL
        expr = expr.replaceAll("\\band\\b", "&&")
                .replaceAll("\\bor\\b", "||")
                .replaceAll("\\beq\\b", "==")
                .replaceAll("\\bne\\b", "!=");

        // 2) Create a fresh engine per call (Nashorn engine objects are not threadsafe)
        javax.script.ScriptEngine engine =
                new javax.script.ScriptEngineManager().getEngineByName("JavaScript");

        // 3) Bind variables (Boolean, Number, String map cleanly into Nashorn)
        if (vars != null) {
            for (var e : vars.entrySet()) {
                engine.put(e.getKey(), e.getValue());
            }
        }

        try {
            System.out.println("Evaluating expression: " + expr + " with variables: " + vars);
            Object result = engine.eval(expr);
            if (result instanceof Boolean) return (Boolean) result;
            if (result == null) return false;
            // Best‑effort coercion (e.g., number/string truthiness)
            if (result instanceof Number) return ((Number) result).doubleValue() != 0d;
            return Boolean.parseBoolean(String.valueOf(result));
        } catch (Exception ex) {
            // TODO: replace with your logger
            // log.warn("Condition eval failed for [{}] with vars {}", rawExpr, vars, ex);
            return false;
        }
    }

    private ConditionEvaluator() {}
}
