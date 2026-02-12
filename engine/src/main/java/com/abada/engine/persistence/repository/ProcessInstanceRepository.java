package com.abada.engine.persistence.repository;

import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, String> {}

