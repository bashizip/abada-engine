package com.abada.engine.core.model;

import java.io.Serializable;
import java.util.List;

public class TaskMeta implements Serializable {
    public String id;
    public String name;
    public String assignee;
    public List<String> candidateUsers;
    public List<String> candidateGroups;
    public String formKey;
    public String dueDate;
    public String followUpDate;
    public String priority;
    public String documentation;

    // Future fields (e.g., listeners, multi-instance, conditions) can be added here.
}
