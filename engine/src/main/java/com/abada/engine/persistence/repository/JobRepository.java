package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {

    /**
     * Finds all jobs that are due to be executed at or before the given timestamp.
     * @param timestamp The current time.
     * @return A list of due jobs.
     */
    List<JobEntity> findByExecutionTimestampLessThanEqual(Instant timestamp);
}
