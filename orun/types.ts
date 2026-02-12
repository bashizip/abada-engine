export type ProcessStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SUSPENDED' | 'CANCELLED';

export interface ProcessDefinition {
  id: string;
  name: string;
  key: string;
  version: number;
  bpmnXml?: string;
}

export interface ProcessInstance {
  id: string;
  definitionId: string;
  definitionName: string;
  status: ProcessStatus;
  startedBy: string;
  startTime: string; // ISO String
  endTime?: string; // ISO String
  currentActivity?: string;
  duration?: string;
}

export interface Job {
  id: string;
  processInstanceId: string;
  processDefinitionName: string;
  activityId: string;
  activityName: string;
  exceptionMessage: string;
  retries: number;
  dueDate?: string;
  failureTime: string;
}

export interface Variable {
  name: string;
  type: 'String' | 'Integer' | 'Long' | 'Double' | 'Float' | 'Boolean' | 'Json';
  value: any;
}

export interface ActivityInstance {
  id: string;
  activityId: string;
  activityName: string;
  startTime: string;
  endTime?: string;
  type: 'startEvent' | 'endEvent' | 'userTask' | 'serviceTask' | 'gateway';
  status: 'COMPLETED' | 'RUNNING' | 'FAILED';
}

export interface MetricPoint {
  time: string;
  value: number;
}
