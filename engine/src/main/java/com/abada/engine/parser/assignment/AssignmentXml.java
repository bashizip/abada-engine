package com.abada.engine.parser.assignment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

/** Secure, namespace-aware XML view used only by deployment translators. */
public final class AssignmentXml {
    private final Document document;

    private AssignmentXml(Document document) { this.document = document; }

    public static AssignmentXml parse(String source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return new AssignmentXml(factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception exception) {
            throw new IllegalArgumentException("BPMN XML cannot be parsed securely", exception);
        }
    }

    public Optional<Element> elementById(String id) {
        NodeList all = document.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (id.equals(element.getAttribute("id"))) return Optional.of(element);
        }
        return Optional.empty();
    }

    public List<Element> elements() {
        NodeList all = document.getElementsByTagNameNS("*", "*");
        List<Element> result = new ArrayList<>(all.getLength());
        for (int i = 0; i < all.getLength(); i++) result.add((Element) all.item(i));
        return List.copyOf(result);
    }

    static Optional<Element> firstChild(Element parent, String namespace, String localName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && namespace.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) return Optional.of(element);
        }
        return Optional.empty();
    }
}
