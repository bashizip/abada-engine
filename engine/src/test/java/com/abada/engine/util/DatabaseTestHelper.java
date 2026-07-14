package com.abada.engine.util;

import com.abada.engine.persistence.repository.ExternalTaskRepository;
import com.abada.engine.persistence.repository.ActivityHistoryRepository;
import com.abada.engine.persistence.repository.EventSubscriptionRepository;
import com.abada.engine.persistence.repository.IdempotencyRecordRepository;
import com.abada.engine.persistence.repository.JobRepository;
import com.abada.engine.persistence.repository.ProcessDefinitionRepository;
import com.abada.engine.persistence.repository.ProcessInstanceRepository;
import com.abada.engine.persistence.repository.TaskRepository;
import com.abada.engine.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseTestHelper {

    private final ExternalTaskRepository externalTaskRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final JobRepository jobRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;
    private final ProcessDefinitionRepository processDefinitionRepository;
    private final OutboxEventRepository outboxEventRepository;

    public DatabaseTestHelper(ExternalTaskRepository externalTaskRepository,
            ActivityHistoryRepository activityHistoryRepository,
            EventSubscriptionRepository eventSubscriptionRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            JobRepository jobRepository, ProcessInstanceRepository processInstanceRepository,
            TaskRepository taskRepository, ProcessDefinitionRepository processDefinitionRepository,
            OutboxEventRepository outboxEventRepository) {
        this.externalTaskRepository = externalTaskRepository;
        this.activityHistoryRepository = activityHistoryRepository;
        this.eventSubscriptionRepository = eventSubscriptionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.jobRepository = jobRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
        this.processDefinitionRepository = processDefinitionRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void cleanup() {
        outboxEventRepository.deleteAll();
        activityHistoryRepository.deleteAll();
        eventSubscriptionRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        externalTaskRepository.deleteAll();
        jobRepository.deleteAll();
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
        processDefinitionRepository.deleteAll();
    }
}
