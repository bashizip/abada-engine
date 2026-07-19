package com.abada.engine.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MutationIdempotencyContractTest {

    private static final List<Class<?>> PUBLIC_CONTROLLERS = List.of(
            CockpitController.class,
            EventController.class,
            ExternalTaskController.class,
            JobController.class,
            ProcessController.class,
            TaskController.class);

    @Test
    void everyPublicMutationAcceptsAnOptionalIdempotencyKey() {
        for (Class<?> controller : PUBLIC_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMutation(method)) continue;

                assertThat(hasIdempotencyKeyHeader(method))
                        .as("%s.%s must accept Idempotency-Key", controller.getSimpleName(), method.getName())
                        .isTrue();
            }
        }
    }

    private boolean isMutation(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private boolean hasIdempotencyKeyHeader(Method method) {
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestHeader header
                        && "Idempotency-Key".equals(header.value())
                        && !header.required()) {
                    return true;
                }
            }
        }
        return false;
    }
}
