package com.abada.engine.parser;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BpmnParser {

    public ParsedProcessDefinition parse(InputStream bpmnXml) {
        try {
            BpmnModelInstance model = Bpmn.readModelFromStream(bpmnXml);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Bpmn.writeModelToStream(out, model);
            String rawXml = out.toString(StandardCharsets.UTF_8);

            Process process = model.getModelElementsByType(Process.class).stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No <process> found"));

            String id = process.getId();
            String name = process.getName();

            String startEventId = model.getModelElementsByType(StartEvent.class).stream()
                    .findFirst()
                    .map(BaseElement::getId)
                    .orElse(null);

            if (startEventId == null) {
                throw new RuntimeException("No <startEvent> found");
            }

            Map<String, TaskMeta> userTasks = new HashMap<>();
            for (UserTask task : model.getModelElementsByType(UserTask.class)) {
                TaskMeta meta = new TaskMeta();
                meta.name = task.getName();
                meta.assignee = task.getCamundaAssignee();

                String candidates = task.getCamundaCandidateUsers();
                if (candidates != null && !candidates.isBlank()) {
                    meta.candidateUsers = Arrays.asList(candidates.split("\\s*,\\s*"));
                }

                String groups = task.getCamundaCandidateGroups();
                if (groups != null && !groups.isBlank()) {
                    meta.candidateGroups = Arrays.asList(groups.split("\\s*,\\s*"));
                }

                userTasks.put(task.getId(), meta);
            }

            List<SequenceFlow> flows = new ArrayList<>();
            for (org.camunda.bpm.model.bpmn.instance.SequenceFlow flow : model.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.SequenceFlow.class)) {
                flows.add(new SequenceFlow(flow.getId(), flow.getSource().getId(), flow.getTarget().getId()));
            }

            return new ParsedProcessDefinition(id, name, startEventId, userTasks, flows, rawXml);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BPMN", e);
        }
    }
}
