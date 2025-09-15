package com.abada.engine.parser;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;

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
                meta.setId(task.getId());
                meta.setName(task.getName());
                meta.setAssignee(task.getCamundaAssignee());

                String candidates = task.getCamundaCandidateUsers();
                if (candidates != null && !candidates.isBlank()) {
                    meta.setCandidateUsers(Arrays.asList(candidates.split("\s*,\s*")));
                }

                String groups = task.getCamundaCandidateGroups();
                if (groups != null && !groups.isBlank()) {
                    meta.setCandidateGroups( Arrays.asList(groups.split("\s*,\s*")));
                }

                userTasks.put(task.getId(), meta);
            }

            List<SequenceFlow> flows = new ArrayList<>();
            for (org.camunda.bpm.model.bpmn.instance.SequenceFlow flow : model.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.SequenceFlow.class)) {
                flows.add(new SequenceFlow(
                        flow.getId(),
                        flow.getSource().getId(),
                        flow.getTarget().getId(),flow.getName(),
                        flow.getConditionExpression() != null ? flow.getConditionExpression().getRawTextContent() : null,
                        flow.isImmediate()));
            }

            Map<String, GatewayMeta> gateways = new HashMap<>();
            for (ExclusiveGateway gateway : model.getModelElementsByType(ExclusiveGateway.class)) {
                gateways.put(gateway.getId(), new GatewayMeta(gateway.getId(), GatewayMeta.Type.EXCLUSIVE, gateway.getDefault() != null ? gateway.getDefault().getId() : null));
            }
            for (InclusiveGateway gateway : model.getModelElementsByType(InclusiveGateway.class)) {
                gateways.put(gateway.getId(), new GatewayMeta(gateway.getId(), GatewayMeta.Type.INCLUSIVE, gateway.getDefault() != null ? gateway.getDefault().getId() : null));
            }

            Map<String, Object> endEvents = new HashMap<>();
            for (EndEvent endEvent : model.getModelElementsByType(EndEvent.class)) {
                endEvents.put(endEvent.getId(), null);
            }

            ParsedProcessDefinition definition = new ParsedProcessDefinition(id, name, startEventId, userTasks, flows, gateways, endEvents, rawXml);
            for (SequenceFlow flow : flows) {
                definition.addOutgoing(flow.getSourceRef(), flow);
            }
            return definition;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BPMN", e);
        }
    }
}