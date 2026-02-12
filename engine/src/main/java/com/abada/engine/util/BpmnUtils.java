package com.abada.engine.util;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.parser.BpmnParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Utility class to help load BPMN resources for testing.
 */
public final class BpmnUtils {

    private BpmnUtils() {
        // Utility class, no instantiation
    }

    /**
     * Loads a BPMN XML file as InputStream from the test resources.
     * @param filename the BPMN file name under src/test/resources/bpmn/
     * @return InputStream of the BPMN XML
     */
    public static InputStream loadBpmnStream(String filename) {
        InputStream stream = BpmnUtils.class.getClassLoader().getResourceAsStream("bpmn/" + filename);
        if (stream == null) {
            throw new IllegalArgumentException("BPMN file not found: " + filename);
        }
        return stream;
    }

    /**
     * Loads a BPMN XML file fully as a String.
     * @param filename the BPMN file name under src/test/resources/bpmn/
     * @return String content of the BPMN XML
     */
    public static String loadBpmnAsString(String filename) {
        try (InputStream stream = loadBpmnStream(filename);
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BPMN file as String: " + filename, e);
        }
    }


    /**
     * Parses a BPMN file from the test resources folder and returns the parsed process definition.
     *
     * @param filename the name of the BPMN file (e.g., "claim-test.bpmn")
     * @return the parsed ParsedProcessDefinition
     */
    public static ParsedProcessDefinition parse(String filename) {
        try (InputStream stream = loadBpmnStream(filename)) {
            return new BpmnParser().parse(stream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read BPMN test file: " + filename, e);
        }
    }
}
