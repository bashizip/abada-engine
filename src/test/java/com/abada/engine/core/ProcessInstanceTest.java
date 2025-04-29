package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessInstanceTest {


    @Test
    void testAdvanceThroughProcess() throws IOException {
        ParsedProcessDefinition definition = createSimpleDefinition();

        ProcessInstance instance = new ProcessInstance(definition);

        assertThat(instance.getCurrentActivityId()).isEqualTo("startEvent1");

        String next = instance.advance();
        assertThat(next).isEqualTo("userTask1");

        next = instance.advance();
        assertThat(next).isEqualTo("endEvent1");

        next = instance.advance();
        assertThat(next).isNull();
        assertThat(instance.isCompleted()).isTrue();
    }

    @Test
    void testIsWaitingForUserTask() throws IOException {
        ParsedProcessDefinition definition = createSimpleDefinition();
        ProcessInstance instance = new ProcessInstance(definition);

        instance.advance(); // move to userTask1
        assertThat(instance.isWaitingForUserTask()).isTrue();
    }

    @Test
    void testReloadedInstance() throws IOException {
        ParsedProcessDefinition definition = createSimpleDefinition();
        ProcessInstance reloadedInstance = new ProcessInstance(
                "custom-id-123",
                definition,
                "userTask1"
        );

        assertThat(reloadedInstance.getId()).isEqualTo("custom-id-123");
        assertThat(reloadedInstance.getCurrentActivityId()).isEqualTo("userTask1");
        assertThat(reloadedInstance.isWaitingForUserTask()).isTrue();
    }

    private ParsedProcessDefinition createSimpleDefinition() throws IOException {
        Map<String, BpmnParser.TaskMeta> userTasks = Map.of(
                "userTask1", new BpmnParser.TaskMeta("Approve Something", null, List.of(), List.of())
        );

        List<BpmnParser.SequenceFlow> flows = List.of(
                new BpmnParser.SequenceFlow("seq1","startEvent1", "userTask1"),
                new BpmnParser.SequenceFlow("seq2","userTask1", "endEvent1")
        );



        return new ParsedProcessDefinition(
                "test-process",
                "Test Process",
                "startEvent1",
                userTasks,
                flows,
                BpmnTestUtils.loadBpmnAsString("test-process.bpmn")
        );
    }

}
