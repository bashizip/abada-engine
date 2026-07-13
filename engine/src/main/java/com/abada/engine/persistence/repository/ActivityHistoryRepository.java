package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.ActivityHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityHistoryRepository extends JpaRepository<ActivityHistoryEntity, String> {
    List<ActivityHistoryEntity> findByProcessInstanceIdOrderByOccurredAtAsc(String processInstanceId);
}
