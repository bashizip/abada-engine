package com.abada.engine.bpmn.migration;

import com.abada.engine.bpmn.compatibility.*;
import com.abada.engine.parser.BpmnParser;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Deterministic Camunda assignment to Abada-native XML migration. */
public final class BpmnMigrationService {
    private static final String BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String CAMUNDA = BpmnCompatibilityDetector.CAMUNDA_NAMESPACE;
    private static final String ABADA = BpmnCompatibilityDetector.ABADA_NAMESPACE;

    public BpmnMigrationResult migrate(InputStream input) {
        try {
            String original = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var parsed = new BpmnParser().parseDetailed(stream(original), BpmnParseOptions.defaults());
            Document document = document(original);
            NodeList tasks = document.getElementsByTagNameNS(BPMN, "userTask");
            for (int i = 0; i < tasks.getLength(); i++) migrateTask(document, (Element) tasks.item(i));
            Element root = document.getDocumentElement();
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:abada", ABADA);
            root.removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "camunda");
            String migrated = serialize(document);
            var verified = new BpmnParser().parseDetailed(stream(migrated),
                    new BpmnParseOptions(List.of(CompatibilityProfiles.STANDARD, CompatibilityProfiles.ABADA_NATIVE),
                            true, true));
            return new BpmnMigrationResult(original, migrated,
                    new CompatibilityReport(verified.report().detectedProfiles(),
                            List.of(new CompatibilityMapping("camunda-7 user-task assignment",
                                    "abada-native-1 assignment", verified.definition().getId(),
                                    "Assignment directives migrated deterministically.")), verified.report().issues()));
        } catch (BpmnValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw BpmnValidationException.single(new BpmnValidationIssue(BpmnErrorCodes.MIGRATION_UNCERTAIN,
                    ValidationSeverity.ERROR, "BPMN migration could not preserve semantics: " + exception.getMessage(),
                    null, null, CAMUNDA, null, "Remove unsupported directives before migration."));
        }
    }

    private void migrateTask(Document document, Element task) {
        String assignee = task.getAttributeNS(CAMUNDA, "assignee");
        String users = task.getAttributeNS(CAMUNDA, "candidateUsers");
        String groups = task.getAttributeNS(CAMUNDA, "candidateGroups");
        if (assignee.isBlank() && users.isBlank() && groups.isBlank()) return;
        Element extensions = firstChild(task, BPMN, "extensionElements");
        if (extensions == null) {
            extensions = document.createElementNS(BPMN, "bpmn:extensionElements");
            task.insertBefore(extensions, task.getFirstChild());
        }
        Element assignment = document.createElementNS(ABADA, "abada:assignment");
        if (!assignee.isBlank()) assignment.setAttribute("assignee", assignee);
        if (!users.isBlank()) assignment.setAttribute("candidateUsers", users);
        if (!groups.isBlank()) assignment.setAttribute("candidateGroups", groups);
        assignment.setAttribute("strategy", assignee.isBlank() ? "claim" : "direct");
        extensions.appendChild(assignment);
        task.removeAttributeNS(CAMUNDA, "assignee");
        task.removeAttributeNS(CAMUNDA, "candidateUsers");
        task.removeAttributeNS(CAMUNDA, "candidateGroups");
    }

    private Element firstChild(Element parent, String namespace, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element element && namespace.equals(element.getNamespaceURI())
                    && name.equals(element.getLocalName())) return element;
        return null;
    }

    private Document document(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(stream(xml));
    }

    private String serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        StringWriter result = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(result));
        return result.toString();
    }

    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
