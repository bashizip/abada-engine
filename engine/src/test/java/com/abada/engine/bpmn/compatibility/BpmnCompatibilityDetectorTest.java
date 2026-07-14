package com.abada.engine.bpmn.compatibility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmnCompatibilityDetectorTest {
    @Test
    void detectsStandardAbadaAndCamundaNamespacesDeterministically() {
        String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                + "xmlns:abada=\"https://abada.io/schema/bpmn\" "
                + "xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\"/>";

        var detection = new BpmnCompatibilityDetector().detect(xml);

        assertThat(detection.profiles()).containsExactlyInAnyOrder(
                CompatibilityProfiles.STANDARD, CompatibilityProfiles.ABADA_NATIVE, CompatibilityProfiles.CAMUNDA_7);
        assertThat(detection.namespaces()).contains(BpmnCompatibilityDetector.ABADA_NAMESPACE,
                BpmnCompatibilityDetector.CAMUNDA_NAMESPACE);
    }

    @Test
    void defaultsIncludeAllProfilesAndUnknownProfilesHaveStableCode() {
        assertThat(BpmnParseOptions.defaults().compatibilityProfiles()).containsExactlyElementsOf(CompatibilityProfiles.DEFAULT);
        assertThatThrownBy(() -> new BpmnParseOptions(List.of("unknown"), false, false))
                .isInstanceOf(BpmnValidationException.class)
                .satisfies(error -> assertThat(((BpmnValidationException) error).getIssues().getFirst().code())
                        .isEqualTo(BpmnErrorCodes.UNKNOWN_PROFILE));
    }
}
