package com.abada.engine.core;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.context.UserContextProvider;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class ServiceTaskTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Mock
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("service-task-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Engine should execute a JavaDelegate service task and update variables")
    void shouldExecuteJavaDelegate() {
        // 1. Start the process
        ProcessInstance pi = abadaEngine.startProcess("ServiceTaskTestProcess");

        // 2. Assert that the process has passed through the service task and is now at the user task
        ProcessInstance updatedPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertFalse(updatedPi.isCompleted(), "Process should be waiting at the user task");
        assertEquals(1, updatedPi.getActiveTokens().size());
        assertEquals("UserTask_1", updatedPi.getActiveTokens().get(0));

        // 3. Assert that the delegate was executed by checking the process variables
        assertEquals(true, updatedPi.getVariable("delegateExecuted"), "The delegate should have set this variable to true");
        assertEquals(1, updatedPi.getVariable("counter"), "The delegate should have incremented the counter to 1");
    }
}
