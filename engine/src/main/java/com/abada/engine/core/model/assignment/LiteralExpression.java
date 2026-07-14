package com.abada.engine.core.model.assignment;

import java.util.Objects;

public record LiteralExpression(String source) implements ProcessExpression {
    public LiteralExpression {
        source = Objects.requireNonNull(source, "source").trim();
        if (source.isEmpty()) throw new IllegalArgumentException("Literal assignment value cannot be empty");
    }
}
