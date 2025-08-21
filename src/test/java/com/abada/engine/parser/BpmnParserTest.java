package com.abada.engine.parser;


import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class BpmnParserTest {

    private ParsedProcessDefinition parsed;

    @BeforeEach
    void setUp() throws Exception {
        InputStream xmlStream = BpmnTestUtils.loadBpmnStream("recipe-cook-test.bpmn");
        CamundaSchemaValidator.validate(xmlStream);
        // Re-open stream for parsing
        xmlStream = BpmnTestUtils.loadBpmnStream("recipe-cook-test.bpmn");
        parsed = new BpmnParser().parse(xmlStream);
    }

    @Test
    void testProcessIdAndName() {
        assertEquals("recipe-cook", parsed.getId());
        assertNotNull(parsed.getStartEventId());
    }

    @Test
    void testUserTasksExist() {
        assertTrue(parsed.isUserTask("choose-recipe"));
        assertTrue(parsed.isUserTask("cook-recipe"));
    }

    @Test
    void testAssigneeAndGroupsParsed() {
     //   assertEquals("bob", parsed.getTaskAssignee("choose-recipe"));
        assertTrue(parsed.getCandidateGroups("cook-recipe").contains("cuistos"));
    }

    @Test
    void testFlowGraphConstruction() {
        assertFalse(parsed.getNextActivities("choose-recipe").isEmpty());
        assertFalse(parsed.getNextActivities("cook-recipe").isEmpty());
    }
}
