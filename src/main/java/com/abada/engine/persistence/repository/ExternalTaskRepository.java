package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.ExternalTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalTaskRepository extends JpaRepository<ExternalTaskEntity, String> {

    /**
     * Finds an available external task for a given topic that is either OPEN
     * or has an expired lock.
     *
     * @param topicName The topic to search for.
     * @param status The OPEN status.
     * @param topicName2 The topic to search for (repeated for the OR clause).
     * @param now The current time, to check for expired locks.
     * @return An Optional containing an available task, if one exists.
     */
    Optional<ExternalTaskEntity> findFirstByTopicNameAndStatusOrTopicNameAndLockExpirationTimeLessThan(String topicName, ExternalTaskEntity.Status status, String topicName2, Instant now);
}
