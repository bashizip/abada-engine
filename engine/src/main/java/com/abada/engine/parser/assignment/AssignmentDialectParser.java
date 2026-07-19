package com.abada.engine.parser.assignment;

import com.abada.engine.core.model.assignment.UserTaskAssignment;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.util.Optional;

/** A deployment-time translator from one BPMN assignment dialect to the canonical model. */
public interface AssignmentDialectParser {
    String profileId();
    Optional<UserTaskAssignment> parse(UserTask task, AssignmentXml xml);
}
