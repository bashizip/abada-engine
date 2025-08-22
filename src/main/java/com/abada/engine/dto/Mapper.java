package com.abada.engine.dto;

import com.abada.engine.core.ProcessInstance;

public class Mapper {

  public   static class  ProcessInstanceMapper {
        public static ProcessInstanceDTO toDto(ProcessInstance pi) {
            return new ProcessInstanceDTO(
                    pi.getId(),
                    pi.getCurrentActivityId(),
                    pi.getVariables(),
                    pi.isWaitingForUserTask(),
                    pi.isCompleted()
            );
        }
    }

}
