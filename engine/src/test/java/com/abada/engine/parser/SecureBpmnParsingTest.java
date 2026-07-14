package com.abada.engine.parser;

import com.abada.engine.bpmn.compatibility.BpmnErrorCodes;
import com.abada.engine.bpmn.compatibility.BpmnValidationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureBpmnParsingTest {
    @Test void rejectsDoctypeAndExternalEntityInput() {
        String xml = """
            <?xml version="1.0"?><!DOCTYPE definitions [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="test">
              <process id="p"><startEvent id="s"/><endEvent id="e"/><sequenceFlow id="f" sourceRef="s" targetRef="e"/></process>
            </definitions>
            """;
        assertThatThrownBy(() -> parse(xml)).isInstanceOf(RuntimeException.class);
    }

    @Test void rejectsInputBeyondBoundedDeploymentLimitWithStableCode() {
        byte[] bytes = new byte[BpmnParser.MAX_DEPLOYMENT_BYTES + 1];
        assertThatThrownBy(() -> new BpmnParser().parse(new ByteArrayInputStream(bytes)))
                .isInstanceOf(BpmnValidationException.class)
                .satisfies(error -> assertThat(((BpmnValidationException) error).getIssues().getFirst().code())
                        .isEqualTo(BpmnErrorCodes.XML_SECURITY));
    }

    private void parse(String xml) {
        new BpmnParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
