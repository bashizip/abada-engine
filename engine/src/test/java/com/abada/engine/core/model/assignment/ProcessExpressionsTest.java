package com.abada.engine.core.model.assignment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessExpressionsTest {
    @Test void distinguishesLiteralAndDynamicValues() {
        assertThat(ProcessExpressions.parse(" alice ")).isEqualTo(new LiteralExpression("alice"));
        assertThat(ProcessExpressions.parse(" ${request.owner} ")).isEqualTo(new DynamicExpression("${request.owner}"));
    }

    @Test void rejectsMalformedDynamicValues() {
        assertThatThrownBy(() -> ProcessExpressions.parse("${}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProcessExpressions.parse("${owner"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
