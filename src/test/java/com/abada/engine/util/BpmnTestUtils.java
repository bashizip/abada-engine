package com.abada.engine.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Utility class to help load BPMN resources for testing.
 */
public final class BpmnTestUtils {

    private BpmnTestUtils() {
        // Utility class, no instantiation
    }

    /**
     * Loads a BPMN XML file as InputStream from the test resources.
     * @param filename the BPMN file name under src/test/resources/bpmn/
     * @return InputStream of the BPMN XML
     */
    public static InputStream loadBpmnStream(String filename) {
        InputStream stream = BpmnTestUtils.class.getClassLoader().getResourceAsStream("bpmn/" + filename);
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
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BPMN file as String: " + filename, e);
        }
    }
}
