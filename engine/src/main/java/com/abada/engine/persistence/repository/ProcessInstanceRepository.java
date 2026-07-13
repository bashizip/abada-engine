package com.abada.engine.persistence.repository;

import com.abada.engine.core.model.ProcessStatus;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProcessInstanceEntity p WHERE p.id = :instanceId")
    Optional<ProcessInstanceEntity> findByIdForUpdate(@Param("instanceId") String instanceId);

    @Query("""
            SELECT p.processDefinitionId AS processDefinitionId, COUNT(p) AS instanceCount
            FROM ProcessInstanceEntity p
            WHERE p.status IN :activeStatuses
            GROUP BY p.processDefinitionId
            """)
    List<ActiveProcessCount> countActiveProcessesByDefinitionId(
            @Param("activeStatuses") Collection<ProcessStatus> activeStatuses);

    interface ActiveProcessCount {
        String getProcessDefinitionId();
        long getInstanceCount();
    }
}
