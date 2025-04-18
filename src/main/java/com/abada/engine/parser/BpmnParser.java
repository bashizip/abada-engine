package com.abada.engine.parser;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class BpmnParser {

    /**
     * Represents a parsed BPMN process containing basic elements like
     * start event, user tasks, and sequence flows.
     */
    public static class ParsedProcess {
        public String id; // ID of the BPMN process
        public String name; // Name of the BPMN process
        public String startEventId; // ID of the start event

        // Map of user task IDs to task metadata
        public Map<String, TaskMeta> userTasks = new LinkedHashMap<>();
        // List of sequence flows defining transitions between elements
        public List<SequenceFlow> sequenceFlows = new ArrayList<>();
    }

    /**
     * Holds metadata for a single user task.
     */
    public static class TaskMeta {
        public String name;
        public String assignee;
        public List<String> candidateUsers = new ArrayList<>();
        public List<String> candidateGroups = new ArrayList<>();
    }

    /**
     * Represents a single sequence flow connection between two BPMN elements.
     */
    public static class SequenceFlow {
        public String id; // ID of the sequence flow
        public String sourceRef; // Source element ID
        public String targetRef; // Target element ID

        public SequenceFlow(String id, String sourceRef, String targetRef) {
            this.id = id;
            this.sourceRef = sourceRef;
            this.targetRef = targetRef;
        }
    }

    /**
     * Parses a BPMN XML file input stream and extracts key process elements.
     *
     * @param bpmnInputStream InputStream of a BPMN XML file
     * @return ParsedProcess object with extracted BPMN data
     * @throws Exception if parsing fails
     */
    public ParsedProcess parse(InputStream bpmnInputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(bpmnInputStream);
        document.getDocumentElement().normalize();

        Element definitions = document.getDocumentElement();
        NodeList processes = definitions.getElementsByTagName("process");

        if (processes.getLength() == 0) throw new RuntimeException("No <process> found");

        Element process = (Element) processes.item(0);
        ParsedProcess parsed = new ParsedProcess();
        parsed.id = process.getAttribute("id");
        parsed.name = process.getAttribute("name");

        NodeList children = process.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element el = (Element) node;
            switch (el.getTagName()) {
                case "startEvent" -> parsed.startEventId = el.getAttribute("id");
                case "userTask" -> {
                    TaskMeta meta = new TaskMeta();
                    meta.name = el.getAttribute("name");
                    meta.assignee = el.getAttribute("assignee");

                    String candidates = el.getAttribute("candidateUsers");
                    if (!candidates.isBlank())
                        meta.candidateUsers = List.of(candidates.split(","));

                    String groups = el.getAttribute("candidateGroups");
                    if (!groups.isBlank())
                        meta.candidateGroups = List.of(groups.split(","));

                    parsed.userTasks.put(el.getAttribute("id"), meta);
                }
                case "sequenceFlow" -> parsed.sequenceFlows.add(new SequenceFlow(
                        el.getAttribute("id"),
                        el.getAttribute("sourceRef"),
                        el.getAttribute("targetRef")
                ));
            }
        }
        return parsed;
    }
}
