package com.abada.engine.dto;

import java.util.List;

public record ActivityInstanceTree(
        String instanceId,
        List<ChildActivityInstance> childActivityInstances) {
}
