package com.abada.engine.dto;

import com.abada.engine.core.ProcessInstance;

public class Mapper {

  public   static class  ProcessInstanceMapper {
        public static ProcessInstanceDTO toDto(ProcessInstance pi) {
            String currentToken = (pi.getActiveTokens() != null && !pi.getActiveTokens().isEmpty()) ? pi.getActiveTokens().get(0) : null;
            return new ProcessInstanceDTO(
                    pi.getId(),
                    currentToken,
                    pi.getVariables(),
                    pi.isWaitingForUserTask(),
                    pi.isCompleted(),
                    pi.getStartDate(),
                    pi.getEndDate()
            );
        }
    }

}
