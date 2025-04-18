package com.abada.engine.parser;

import com.abada.engine.core.ProcessDefinition;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * Parses BPMN XML into Abada process definition metadata.
 */
public class BpmnParser {

    public static class TaskMeta {
        public String name;
        public String assignee;
        public List<String> candidateUsers = List.of();
        public List<String> candidateGroups = List.of();
    }

    public static class SequenceFlow {
        private final String from;
        private final String to;

        public SequenceFlow(String id, String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }
    }

    public ProcessDefinition parse(InputStream bpmnXml) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(bpmnXml);
            doc.getDocumentElement().normalize();

            Node process = doc.getElementsByTagName("process").item(0);
            NamedNodeMap attrs = process.getAttributes();
            String id = attrs.getNamedItem("id").getNodeValue();
            String name = attrs.getNamedItem("name").getNodeValue();

            Map<String, TaskMeta> userTasks = new HashMap<>();
            List<SequenceFlow> flows = new ArrayList<>();
            String startEventId = null;

            NodeList nodes = doc.getElementsByTagName("process").item(0).getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node el = nodes.item(i);
                if (el.getNodeType() != Node.ELEMENT_NODE) continue;

                String tag = el.getNodeName();
                NamedNodeMap attr = el.getAttributes();
                String elId = attr.getNamedItem("id").getNodeValue();

                switch (tag) {
                    case "startEvent" -> startEventId = elId;
                    case "userTask" -> {
                        TaskMeta meta = new TaskMeta();
                        meta.name = attr.getNamedItem("name").getNodeValue();
                        if (attr.getNamedItem("assignee") != null)
                            meta.assignee = attr.getNamedItem("assignee").getNodeValue();
                        if (attr.getNamedItem("candidateUsers") != null) {
                            String candidates = attr.getNamedItem("candidateUsers").getNodeValue();
                            if (!candidates.isBlank())
                                meta.candidateUsers = Arrays.asList(candidates.split("\\s*,\\s*"));
                        }
                        if (attr.getNamedItem("candidateGroups") != null) {
                            String groups = attr.getNamedItem("candidateGroups").getNodeValue();
                            if (!groups.isBlank())
                                meta.candidateGroups = Arrays.asList(groups.split("\\s*,\\s*"));
                        }
                        userTasks.put(elId, meta);
                    }
                    case "sequenceFlow" -> {
                        String from = attr.getNamedItem("sourceRef").getNodeValue();
                        String to = attr.getNamedItem("targetRef").getNodeValue();
                        flows.add(new SequenceFlow(elId, from, to));
                    }
                }
            }

            return new ProcessDefinition(id, name, startEventId, userTasks, flows);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse BPMN", e);
        }
    }
}
