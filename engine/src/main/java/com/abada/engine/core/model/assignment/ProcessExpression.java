package com.abada.engine.core.model.assignment;

import java.io.Serializable;

/** A literal or dynamic value retained in a compiled process definition. */
public sealed interface ProcessExpression extends Serializable
        permits LiteralExpression, DynamicExpression {
    String source();

    default boolean dynamic() {
        return this instanceof DynamicExpression;
    }
}
