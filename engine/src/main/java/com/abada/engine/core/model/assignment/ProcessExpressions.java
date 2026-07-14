package com.abada.engine.core.model.assignment;

/** Parsing helpers shared by BPMN dialect adapters. */
public final class ProcessExpressions {
    private ProcessExpressions() {}

    public static ProcessExpression parse(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Assignment expression cannot be empty");
        }
        String value = source.trim();
        if (value.startsWith("${") || value.endsWith("}")) return new DynamicExpression(value);
        return new LiteralExpression(value);
    }
}
