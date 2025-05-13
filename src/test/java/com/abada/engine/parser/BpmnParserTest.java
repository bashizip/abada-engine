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
        InputStream xmlStream = BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn");
        CamundaSchemaValidator.validate(xmlStream);

        // Re-open stream for parsing
        xmlStream = BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn");
        parsed = new BpmnParser().parse(xmlStream);
    }

    @Test
    void testProcessIdAndName() {
        assertEquals("recipe_cook", parsed.getId());
        assertNotNull(parsed.getStartEventId());
    }

    @Test
    void testUserTasksExist() {
        assertTrue(parsed.isUserTask("Activity_0b1232f"));
        assertTrue(parsed.isUserTask("Activity_1lzaw3z"));
    }

    @Test
    void testAssigneeAndGroupsParsed() {
        assertEquals("alice", parsed.getTaskAssignee("Activity_1lzaw3z"));
        assertTrue(parsed.getCandidateGroups("Activity_1lzaw3z").contains("cuistos"));
    }

    @Test
    void testFlowGraphConstruction() {
        assertFalse(parsed.getNextActivities("Activity_0b1232f").isEmpty());
        assertFalse(parsed.getNextActivities("Gateway_0ih95cn").isEmpty());
    }
}
