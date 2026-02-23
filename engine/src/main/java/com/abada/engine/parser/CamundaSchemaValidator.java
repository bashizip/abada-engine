package com.abada.engine.parser;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.io.InputStream;
import java.util.Collection;

public class CamundaSchemaValidator {

    public static void validate(InputStream bpmnXmlStream) throws Exception {
        BpmnModelInstance model = Bpmn.readModelFromStream(bpmnXmlStream);

        // Simple structural assertions
        Collection<Process> processes = model.getModelElementsByType(Process.class);
        if (processes.isEmpty()) {
            throw new RuntimeException("No <process> element found in BPMN");
        }

        for (Process process : processes) {
            Collection<StartEvent> starts = process.getChildElementsByType(StartEvent.class);
            if (starts.isEmpty()) {
                throw new RuntimeException("Process '" + process.getId() + "' has no <startEvent>");
            }

            Collection<UserTask> tasks = process.getChildElementsByType(UserTask.class);
            if (tasks.isEmpty()) {
                throw new RuntimeException("Process '" + process.getId() + "' has no <userTask>");
            }

            Collection<SequenceFlow> flows = process.getChildElementsByType(SequenceFlow.class);
            if (flows.isEmpty()) {
                throw new RuntimeException("Process '" + process.getId() + "' has no <sequenceFlow>");
            }
        }
    }
}
