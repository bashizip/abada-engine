package com.abada.engine.runtime;



import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ProcessRegistry {

    private final Map<String, DeployedProcessDefinition> definitions = new ConcurrentHashMap<>();

    public void register(DeployedProcessDefinition definition) {
        definitions.put(definition.getId(), definition);
    }

    public DeployedProcessDefinition getById(String id) {
        return definitions.get(id);
    }

    public boolean exists(String id) {
        return definitions.containsKey(id);
    }

    public void clear() {
        definitions.clear();
    }
}