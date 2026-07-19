package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.ActivityHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityHistoryRepository extends JpaRepository<ActivityHistoryEntity, String> {
    List<ActivityHistoryEntity> findByProcessInstanceIdOrderByOccurredAtAsc(String processInstanceId);
    Page<ActivityHistoryEntity> findByProcessInstanceId(String processInstanceId, Pageable pageable);
}
