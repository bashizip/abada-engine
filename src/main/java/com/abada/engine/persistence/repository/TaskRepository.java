package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {
    List<TaskEntity> findByProcessInstanceId(String processInstanceId);
}
