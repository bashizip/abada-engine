package com.abada.engine.persistence.repository;

import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.persistence.entity.TaskEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    List<TaskEntity> findByProcessInstanceId(String processInstanceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TaskEntity t WHERE t.id = :taskId")
    Optional<TaskEntity> findByIdForUpdate(@Param("taskId") String taskId);
    
    // User-specific queries
    List<TaskEntity> findByAssigneeAndStatus(String assignee, TaskStatus status);
    
    List<TaskEntity> findByAssigneeOrderByStartDateDesc(String assignee);
    
    @Query("SELECT t FROM TaskEntity t WHERE t.assignee = :assignee ORDER BY t.startDate DESC")
    List<TaskEntity> findRecentTasksByAssignee(@Param("assignee") String assignee);
    
    @Query("SELECT t FROM TaskEntity t WHERE t.assignee = :assignee AND t.status = :status AND t.startDate < :cutoffDate")
    List<TaskEntity> findOverdueTasksByAssignee(@Param("assignee") String assignee, 
                                               @Param("status") TaskStatus status, 
                                               @Param("cutoffDate") Instant cutoffDate);
    
    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.assignee = :assignee AND t.status = :status")
    long countByAssigneeAndStatus(@Param("assignee") String assignee, @Param("status") TaskStatus status);
    
    @Query("SELECT t FROM TaskEntity t WHERE t.assignee = :assignee OR :assignee MEMBER OF t.candidateUsers")
    List<TaskEntity> findTasksForUser(@Param("assignee") String assignee);
}
