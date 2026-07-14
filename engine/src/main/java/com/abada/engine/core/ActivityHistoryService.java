package com.abada.engine.core;

import com.abada.engine.persistence.entity.ActivityHistoryEntity;
import com.abada.engine.persistence.repository.ActivityHistoryRepository;
import com.abada.engine.security.Identity;
import com.abada.engine.security.IdentityContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ActivityHistoryService {
    private final ActivityHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public ActivityHistoryService(ActivityHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String eventType, ProcessInstance instance, String activityId, Map<String, ?> details) {
        record(eventType, instance == null ? null : instance.getId(),
                instance == null ? null : instance.getDefinition().getId(), activityId, details);
    }

    public void record(String eventType, String processInstanceId, String processDefinitionId,
            String activityId, Map<String, ?> details) {
        ActivityHistoryEntity history = new ActivityHistoryEntity();
        history.setEventType(eventType);
        history.setProcessInstanceId(processInstanceId);
        history.setProcessDefinitionId(processDefinitionId);
        history.setActivityId(activityId);
        history.setActor(IdentityContext.get().map(Identity::username).orElse("system"));
        var spanContext = Span.current().getSpanContext();
        history.setTraceId(spanContext.isValid() ? spanContext.getTraceId() : null);
        try {
            history.setDetailsJson(objectMapper.writeValueAsString(details == null ? Map.of() : details));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize activity history", ex);
        }
        repository.save(history);
    }
}
