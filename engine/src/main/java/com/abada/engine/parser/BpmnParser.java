package com.abada.engine.parser;

import com.abada.engine.core.model.*;
import com.abada.engine.core.model.SequenceFlow;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;

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
            String documentation = process.getDocumentations().stream()
                    .findFirst()
                    .map(Documentation::getTextContent)
                    .orElse(null);

            String startEventId = model.getModelElementsByType(StartEvent.class).stream()
                    .findFirst()
                    .map(BaseElement::getId)
                    .orElse(null);

            if (startEventId == null) {
                throw new RuntimeException("No <startEvent> found");
            }

            Map<String, TaskMeta> userTasks = new HashMap<>();
            for (UserTask userTask : model.getModelElementsByType(UserTask.class)) {
                TaskMeta meta = new TaskMeta();
                meta.setId(userTask.getId());
                meta.setName(userTask.getName());
                meta.setAssignee(userTask.getCamundaAssignee());

                String candidates = userTask.getCamundaCandidateUsers();
                if (candidates != null && !candidates.isBlank()) {
                    meta.setCandidateUsers(Arrays.asList(candidates.split("\\s*,\\s*")));
                }

                String groups = userTask.getCamundaCandidateGroups();
                if (groups != null && !groups.isBlank()) {
                    meta.setCandidateGroups(Arrays.asList(groups.split("\\s*,\\s*")));
                }
                userTasks.put(userTask.getId(), meta);
            }

            Map<String, ServiceTaskMeta> serviceTasks = new HashMap<>();
            for (ServiceTask serviceTask : model.getModelElementsByType(ServiceTask.class)) {
                String className = serviceTask.getCamundaClass();
                String topicName = serviceTask.getCamundaTopic();

                if (className != null && !className.isBlank()) {
                    serviceTasks.put(serviceTask.getId(),
                            new ServiceTaskMeta(serviceTask.getId(), serviceTask.getName(), className, null));
                } else if (topicName != null && !topicName.isBlank()) {
                    serviceTasks.put(serviceTask.getId(),
                            new ServiceTaskMeta(serviceTask.getId(), serviceTask.getName(), null, topicName));
                }
            }

            List<SequenceFlow> flows = new ArrayList<>();
            for (org.camunda.bpm.model.bpmn.instance.SequenceFlow flow : model
                    .getModelElementsByType(org.camunda.bpm.model.bpmn.instance.SequenceFlow.class)) {
                flows.add(new SequenceFlow(
                        flow.getId(),
                        flow.getSource().getId(),
                        flow.getTarget().getId(), flow.getName(),
                        flow.getConditionExpression() != null ? flow.getConditionExpression().getRawTextContent()
                                : null,
                        flow.isImmediate()));
            }

            Map<String, GatewayMeta> gateways = new HashMap<>();
            for (ExclusiveGateway gateway : model.getModelElementsByType(ExclusiveGateway.class)) {
                gateways.put(gateway.getId(), new GatewayMeta(gateway.getId(), GatewayMeta.Type.EXCLUSIVE,
                        gateway.getDefault() != null ? gateway.getDefault().getId() : null));
            }
            for (InclusiveGateway gateway : model.getModelElementsByType(InclusiveGateway.class)) {
                gateways.put(gateway.getId(), new GatewayMeta(gateway.getId(), GatewayMeta.Type.INCLUSIVE,
                        gateway.getDefault() != null ? gateway.getDefault().getId() : null));
            }
            for (ParallelGateway gateway : model.getModelElementsByType(ParallelGateway.class)) {
                gateways.put(gateway.getId(), new GatewayMeta(gateway.getId(), GatewayMeta.Type.PARALLEL, null));
            }

            Map<String, EventMeta> events = new HashMap<>();
            for (IntermediateCatchEvent event : model.getModelElementsByType(IntermediateCatchEvent.class)) {
                if (!event.getEventDefinitions().isEmpty()) {
                    EventDefinition eventDefinition = event.getEventDefinitions().iterator().next();

                    if (eventDefinition instanceof MessageEventDefinition) {
                        MessageEventDefinition messageEventDef = (MessageEventDefinition) eventDefinition;
                        String messageName = messageEventDef.getMessage().getName();
                        events.put(event.getId(), new EventMeta(event.getId(), event.getName(),
                                EventMeta.EventType.MESSAGE, messageName));
                    } else if (eventDefinition instanceof TimerEventDefinition) {
                        TimerEventDefinition timerEventDef = (TimerEventDefinition) eventDefinition;
                        if (timerEventDef.getTimeDuration() != null) {
                            String duration = timerEventDef.getTimeDuration().getTextContent();
                            events.put(event.getId(),
                                    new EventMeta(event.getId(), event.getName(), EventMeta.EventType.TIMER, duration));
                        }
                    } else if (eventDefinition instanceof SignalEventDefinition) {
                        SignalEventDefinition signalEventDef = (SignalEventDefinition) eventDefinition;
                        String signalName = signalEventDef.getSignal().getName();
                        events.put(event.getId(),
                                new EventMeta(event.getId(), event.getName(), EventMeta.EventType.SIGNAL, signalName));
                    }
                }
            }

            Map<String, Object> endEvents = new HashMap<>();
            for (EndEvent endEvent : model.getModelElementsByType(EndEvent.class)) {
                endEvents.put(endEvent.getId(), endEvent);
            }

            // Extract candidate starter groups and users from the process element
            List<String> candidateStarterGroups = null;
            List<String> candidateStarterUsers = null;

            String starterGroups = process.getAttributeValueNs("http://camunda.org/schema/1.0/bpmn",
                    "candidateStarterGroups");
            if (starterGroups != null && !starterGroups.isBlank()) {
                candidateStarterGroups = Arrays.asList(starterGroups.split("\\s*,\\s*"));
            }

            String starterUsers = process.getAttributeValueNs("http://camunda.org/schema/1.0/bpmn",
                    "candidateStarterUsers");
            if (starterUsers != null && !starterUsers.isBlank()) {
                candidateStarterUsers = Arrays.asList(starterUsers.split("\\s*,\\s*"));
            }

            ParsedProcessDefinition definition = new ParsedProcessDefinition(id, name, documentation, startEventId,
                    userTasks, serviceTasks, flows, gateways, events, endEvents, rawXml, candidateStarterGroups,
                    candidateStarterUsers);
            for (SequenceFlow flow : flows) {
                definition.addOutgoing(flow.getSourceRef(), flow);
            }
            return definition;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BPMN", e);
        }
    }
}
