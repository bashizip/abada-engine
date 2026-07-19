package com.abada.engine.core.model.assignment;

import java.util.Objects;

public record DynamicExpression(String source) implements ProcessExpression {
    public DynamicExpression {
        source = Objects.requireNonNull(source, "source").trim();
        if (!source.startsWith("${") || !source.endsWith("}") || source.substring(2, source.length() - 1).isBlank()) {
            throw new IllegalArgumentException("Dynamic assignment expression must use non-empty ${...} syntax");
        }
    }
}
