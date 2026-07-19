package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.EventSubscriptionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscriptionEntity, String> {
    boolean existsByProcessInstanceIdAndActivityId(String processInstanceId, String activityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EventSubscriptionEntity> findFirstByEventTypeAndEventNameAndCorrelationKeyAndConsumedAtIsNull(
            EventSubscriptionEntity.Type type, String eventName, String correlationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EventSubscriptionEntity> findByEventTypeAndEventNameAndConsumedAtIsNullOrderByIdAsc(
            EventSubscriptionEntity.Type type, String eventName);
}
