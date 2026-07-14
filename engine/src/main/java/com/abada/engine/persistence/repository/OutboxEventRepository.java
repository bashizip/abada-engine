package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    @Query(value = "select * from outbox_events where published_at is null "
            + "and (next_attempt_at is null or next_attempt_at <= :now) "
            + "and (lease_expires_at is null or lease_expires_at <= :now) "
            + "order by occurred_at limit :batchSize for update skip locked", nativeQuery = true)
    List<OutboxEventEntity> findDispatchableForUpdate(
            @Param("now") Instant now, @Param("batchSize") int batchSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OutboxEventEntity event where event.id = :id")
    Optional<OutboxEventEntity> findByIdForUpdate(@Param("id") String id);

    long countByPublishedAtIsNull();

    List<OutboxEventEntity> findByAggregateIdOrderByOccurredAt(String aggregateId);
}
