package com.abada.engine.parser;

import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class BpmnSchemaValidatorTest {

    @Test
    void testValidBpmnXmlPasses() {
        InputStream xmlStream = BpmnTestUtils.loadBpmnStream("recipe-cook-test.bpmn");
        assertDoesNotThrow(() -> CamundaSchemaValidator.validate(xmlStream));
    }


/*    @Test
    void testInvalidBpmnXmlFails() {
        InputStream xmlStream = BpmnTestUtils.loadBpmnStream("invalid.bpmn");
        assertThrows(SAXException.class, () -> BpmnSchemaValidator.validate(xmlStream));
    }*/
}
