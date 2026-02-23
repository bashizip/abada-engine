package com.abada.engine.persistence.repository;


import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinitionEntity, String> {}