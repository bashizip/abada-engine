package com.abada.engine.core;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AtomicRuntimeCommandContractTest {
    @Test
    void everyPublicRuntimeMutationDeclaresTheAtomicCommandBoundary() throws Exception {
        List<Method> commands = List.of(
                AbadaEngine.class.getMethod("deploy", InputStream.class),
                AbadaEngine.class.getMethod("startProcess", String.class, String.class, Map.class),
                AbadaEngine.class.getMethod("claim", String.class, String.class, List.class),
                AbadaEngine.class.getMethod("completeTask", String.class, String.class, List.class, Map.class),
                AbadaEngine.class.getMethod("failTask", String.class),
                AbadaEngine.class.getMethod("failProcess", String.class),
                AbadaEngine.class.getMethod("cancelProcessInstance", String.class, String.class),
                AbadaEngine.class.getMethod("suspendProcessInstance", String.class, boolean.class),
                AbadaEngine.class.getMethod("updateProcessVariables", String.class, Map.class),
                AbadaEngine.class.getMethod("resumeFromEvent", String.class, String.class, Map.class),
                EventManager.class.getMethod("correlateMessage", String.class, String.class, Map.class),
                EventManager.class.getMethod("broadcastSignal", String.class, Map.class),
                ExternalTaskCommandService.class.getMethod("fetchAndLock", com.abada.engine.dto.FetchAndLockRequest.class),
                ExternalTaskCommandService.class.getMethod("complete", String.class, Map.class),
                ExternalTaskCommandService.class.getMethod("handleFailure", String.class, com.abada.engine.dto.ExternalTaskFailureDto.class),
                ExternalTaskCommandService.class.getMethod("extendLock", String.class, com.abada.engine.dto.ExtendLockRequest.class),
                ExternalTaskCommandService.class.getMethod("setRetries", String.class, int.class),
                TimerJobCommandService.class.getMethod("execute", String.class, String.class, java.time.Instant.class),
                TimerJobCommandService.class.getMethod("recordFailure", String.class, String.class),
                IdempotencyService.class.getMethod("execute", String.class, String.class, Object.class,
                        java.util.function.Supplier.class));

        assertThat(commands)
                .allSatisfy(method -> assertThat(method.isAnnotationPresent(AtomicRuntimeCommand.class))
                        .as(method.toGenericString()).isTrue());
    }
}
