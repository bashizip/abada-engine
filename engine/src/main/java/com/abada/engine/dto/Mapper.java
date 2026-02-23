package com.abada.engine.dto;

import com.abada.engine.core.ProcessInstance;

public class Mapper {

    public static class ProcessInstanceMapper {
        public static ProcessInstanceDTO toDto(ProcessInstance pi) {
            String currentToken = (pi.getActiveTokens() != null && !pi.getActiveTokens().isEmpty())
                    ? pi.getActiveTokens().get(0)
                    : null;
            return new ProcessInstanceDTO(
                    pi.getId(),
                    pi.getDefinition().getId(),
                    pi.getDefinition().getName(),
                    currentToken,
                    pi.getStatus(),
                    pi.isSuspended(),
                    pi.getStartDate(),
                    pi.getEndDate(),
                    "system", // This will be populated from the database entity in the future
                    pi.getVariables());
        }
    }

}
