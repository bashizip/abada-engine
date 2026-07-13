package com.abada.engine.parser;

import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.exception.ProcessEngineException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SupportedBpmnValidatorTest {
    @Test
    void rejectsUnsupportedReceiveTask() {
        String xml = process("<bpmn:receiveTask id=\"unsupported\" />",
                "<bpmn:sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"unsupported\"/>" +
                "<bpmn:sequenceFlow id=\"f2\" sourceRef=\"unsupported\" targetRef=\"end\"/>");

        ProcessEngineException error = assertThrows(ProcessEngineException.class, () -> parse(xml));
        assertTrue(error.getMessage().contains("receiveTask(unsupported)"));
    }

    @Test
    void executesJavascriptAndPublishesBindingsAsVariables() {
        String xml = process("<bpmn:scriptTask id=\"script\" scriptFormat=\"javascript\"><bpmn:script>variables.put('result', input * 2);</bpmn:script></bpmn:scriptTask>",
                "<bpmn:sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"script\"/>" +
                "<bpmn:sequenceFlow id=\"f2\" sourceRef=\"script\" targetRef=\"end\"/>");

        ProcessInstance instance = new ProcessInstance(parse(xml));
        instance.setVariable("input", 21);
        instance.advance();

        assertEquals(42.0, ((Number) instance.getVariable("result")).doubleValue());
        assertTrue(instance.isCompleted());
    }

    private static com.abada.engine.core.model.ParsedProcessDefinition parse(String xml) {
        return new BpmnParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String process(String node, String flows) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="test">
                  <bpmn:process id="support-test" isExecutable="true">
                    <bpmn:startEvent id="start"/>
                    %s
                    <bpmn:endEvent id="end"/>
                    %s
                  </bpmn:process>
                </bpmn:definitions>
                """.formatted(node, flows);
    }
}
