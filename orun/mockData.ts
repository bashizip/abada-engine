import { ProcessInstance, Job, Variable, ActivityInstance, ProcessDefinition } from './types';

export const PROCESS_DEFINITIONS: ProcessDefinition[] = [
  { id: 'pd-1', name: 'Order Processing', key: 'order-process', version: 3 },
  { id: 'pd-2', name: 'User Onboarding', key: 'user-onboarding', version: 1 },
  { id: 'pd-3', name: 'Payment Reconciliation', key: 'payment-recon', version: 5 },
  { id: 'pd-4', name: 'Inventory Restock', key: 'inventory-restock', version: 2 },
];

const generateInstances = (): ProcessInstance[] => {
  const instances: ProcessInstance[] = [];
  const statuses: ('RUNNING' | 'COMPLETED' | 'FAILED' | 'SUSPENDED' | 'CANCELLED')[] = ['RUNNING', 'RUNNING', 'COMPLETED', 'FAILED', 'SUSPENDED', 'RUNNING', 'CANCELLED'];
  
  for (let i = 0; i < 25; i++) {
    const def = PROCESS_DEFINITIONS[i % PROCESS_DEFINITIONS.length];
    const status = statuses[i % statuses.length];
    const start = new Date(Date.now() - Math.random() * 1000000000);
    const end = status === 'COMPLETED' || status === 'CANCELLED' ? new Date(start.getTime() + Math.random() * 5000000) : undefined;
    
    instances.push({
      id: `inst-${1000 + i}`,
      definitionId: def.id,
      definitionName: def.name,
      status: status,
      startedBy: ['system', 'admin', 'jdoe', 'msmith'][Math.floor(Math.random() * 4)],
      startTime: start.toISOString(),
      endTime: end?.toISOString(),
      currentActivity: status === 'RUNNING' ? 'Validate Order' : undefined,
      duration: end ? `${Math.floor((end.getTime() - start.getTime()) / 60000)}m` : undefined
    });
  }
  return instances;
};

export const MOCK_INSTANCES = generateInstances();

export const MOCK_JOBS: Job[] = [
  {
    id: 'job-501',
    processInstanceId: 'inst-1003',
    processDefinitionName: 'Order Processing',
    activityId: 'act-payment',
    activityName: 'Charge Credit Card',
    exceptionMessage: 'Connection timed out: Payment Gateway Unreachable (503)',
    retries: 0,
    failureTime: new Date(Date.now() - 1000 * 60 * 5).toISOString()
  },
  {
    id: 'job-502',
    processInstanceId: 'inst-1012',
    processDefinitionName: 'Inventory Restock',
    activityId: 'act-update-erp',
    activityName: 'Update ERP System',
    exceptionMessage: 'OptimisticLockingException: Row was updated or deleted by another transaction',
    retries: 1,
    failureTime: new Date(Date.now() - 1000 * 60 * 30).toISOString()
  },
  {
    id: 'job-503',
    processInstanceId: 'inst-1018',
    processDefinitionName: 'User Onboarding',
    activityId: 'act-email',
    activityName: 'Send Welcome Email',
    exceptionMessage: 'SMTP Error: Invalid recipient address format',
    retries: 2,
    failureTime: new Date(Date.now() - 1000 * 60 * 120).toISOString()
  }
];

export const MOCK_VARIABLES: Variable[] = [
  { name: 'amount', type: 'Double', value: 1250.50 },
  { name: 'currency', type: 'String', value: 'USD' },
  { name: 'customerCheck', type: 'Boolean', value: true },
  { name: 'orderId', type: 'String', value: 'ORD-2023-8891' },
  { name: 'riskScore', type: 'Integer', value: 42 },
  { name: 'metadata', type: 'Json', value: '{"source": "web", "campaign": "summer_sale"}' },
];

export const MOCK_ACTIVITY_HISTORY: ActivityInstance[] = [
  { id: 'ai-1', activityId: 'start', activityName: 'Order Received', startTime: new Date(Date.now() - 3600000).toISOString(), endTime: new Date(Date.now() - 3590000).toISOString(), type: 'startEvent', status: 'COMPLETED' },
  { id: 'ai-2', activityId: 'validate', activityName: 'Validate Order', startTime: new Date(Date.now() - 3590000).toISOString(), endTime: new Date(Date.now() - 3500000).toISOString(), type: 'serviceTask', status: 'COMPLETED' },
  { id: 'ai-3', activityId: 'check-fraud', activityName: 'Check Fraud', startTime: new Date(Date.now() - 3500000).toISOString(), endTime: new Date(Date.now() - 3400000).toISOString(), type: 'serviceTask', status: 'COMPLETED' },
  { id: 'ai-4', activityId: 'payment', activityName: 'Charge Credit Card', startTime: new Date(Date.now() - 3400000).toISOString(), type: 'serviceTask', status: 'FAILED' },
];