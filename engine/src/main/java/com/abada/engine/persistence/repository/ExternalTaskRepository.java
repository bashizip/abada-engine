package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.ExternalTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ExternalTaskRepository extends JpaRepository<ExternalTaskEntity, String> {

    /**
     * Finds an available external task for a given topic that is either OPEN
     * or has an expired lock.
     *
     * @param topicName  The topic to search for.
     * @param status     The OPEN status.
     * @param topicName2 The topic to search for (repeated for the OR clause).
     * @param now        The current time, to check for expired locks.
     * @return An Optional containing an available task, if one exists.
     */
    @Query(value = "select * from external_tasks where topic_name = :topic "
            + "and (status = 'OPEN' or (status = 'LOCKED' and lock_expiration_time <= :now)) "
            + "order by id limit 1 for update skip locked", nativeQuery = true)
    List<ExternalTaskEntity> findAvailableForUpdate(@Param("topic") String topic, @Param("now") Instant now);

    default Optional<ExternalTaskEntity> findFirstAvailableForUpdate(String topic, Instant now) {
        return findAvailableForUpdate(topic, now).stream().findFirst();
    }

    boolean existsByProcessInstanceIdAndActivityIdAndStatusIn(
            String processInstanceId, String activityId, List<ExternalTaskEntity.Status> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from ExternalTaskEntity task where task.id = :id")
    Optional<ExternalTaskEntity> findByIdForUpdate(@Param("id") String id);

    @Query("select task from ExternalTaskEntity task where "
            + "(task.status = com.abada.engine.persistence.entity.ExternalTaskEntity.Status.FAILED "
            + "or (task.retries is not null and task.retries <= 0)) "
            + "and (:withException = false or task.exceptionMessage is not null) "
            + "and (:active = false or (task.retries is not null and task.retries >= 0))")
    Page<ExternalTaskEntity> findIncidents(@Param("withException") boolean withException,
            @Param("active") boolean active, Pageable pageable);
}
