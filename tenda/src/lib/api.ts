import { refreshToken, keycloak } from '@/auth/keycloakClient';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://localhost/api';

export interface ApiResponse<T> {
  data?: T;
  error?: any;
  status: number;
}

export type TaskStatus = 'AVAILABLE' | 'CLAIMED' | 'COMPLETED' | 'FAILED';

export interface TaskDetailsDto {
  id: string;
  name: string;
  assignee?: string;
  status: TaskStatus;
  candidateGroups?: string[];
  processInstanceId: string;
  variables?: Record<string, any>;
  startDate?: string;
  endDate?: string;
}

export interface ProcessInstanceDTO {
  id: string;
  currentActivityId: string;
  variables: Record<string, any>;
  isCompleted: boolean;
  startDate: string;
  endDate?: string;
}

export interface ProcessDefinition {
  id: string;
  name: string;
  documentation?: string;
  bpmnXml?: string;
}

export interface UserStatsDto {
  quickStats: {
    activeTasks: number;
    completedTasks: number;
    runningProcesses: number;
    availableTasks: number;
  };
  recentTasks: Array<{
    id: string;
    name: string;
    taskDefinitionKey: string;
    status: TaskStatus;
    startDate: string;
    processInstanceId: string;
  }>;
  tasksByStatus: {
    AVAILABLE: number;
    CLAIMED: number;
    COMPLETED: number;
    FAILED: number;
  };
  overdueTasks: Array<{
    id: string;
    name: string;
    taskDefinitionKey: string;
    startDate: string;
    daysOverdue: number;
    processInstanceId: string;
  }>;
  processActivity: {
    recentlyStartedProcesses: Array<{
      id: string;
      processDefinitionId: string;
      startDate: string;
      currentActivityId: string | null;
    }>;
    activeProcessCount: number;
    completionRate: number;
  };
}

class ApiClient {
  private async getAuthHeaders(): Promise<HeadersInit> {
    await refreshToken(30);
    const headers: Record<string, string> = {};

    if (keycloak.token) {
      headers['Authorization'] = `Bearer ${keycloak.token}`;
    }

    return headers;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    try {
      const isFormData = options.body instanceof FormData;
      const authHeaders = await this.getAuthHeaders();

      const requestHeaders: Record<string, string> = {
        ...authHeaders,
        ...(options.headers as Record<string, string>),
      };

      if (!isFormData) {
        requestHeaders['Content-Type'] = 'application/json';
      }

      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: requestHeaders,
      });

      if (response.ok) {
        if (response.status === 204 || response.headers.get('Content-Length') === '0') {
          return { data: undefined, status: response.status };
        }
        const data = await response.json();
        return { data, status: response.status };
      }

      let errorPayload: any = `HTTP ${response.status}: ${response.statusText}`;
      try {
        const errorData = await response.json();
        errorPayload = errorData;
        if (response.status === 400) {
          console.error("API Error (400):", errorData);
        }
      } catch (e) {
        // Not a JSON error response
      }
      return { status: response.status, error: errorPayload };

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Network error';
      console.error("Network Error:", errorMessage);
      return {
        status: 0,
        error: errorMessage,
      };
    }
  }

  // Task endpoints
  async getTasks(filters?: {
    status?: string;
  }): Promise<ApiResponse<TaskDetailsDto[]>> {
    const queryParams = new URLSearchParams();
    if (filters?.status) queryParams.append('status', filters.status);
    const queryString = queryParams.toString();

    return this.request(`/v1/tasks${queryString ? `?${queryString}` : ''}`);
  }

  async getTask(id: string): Promise<ApiResponse<TaskDetailsDto>> {
    return this.request(`/v1/tasks/${id}`);
  }

  async claimTask(taskId: string): Promise<ApiResponse<{ status: string; taskId: string }>> {
    return this.request(`/v1/tasks/claim?taskId=${encodeURIComponent(taskId)}`, { method: 'POST' });
  }

  async completeTask(taskId: string, variables?: Record<string, any>): Promise<ApiResponse<{ status: string; taskId: string }>> {
    const endpoint = `/v1/tasks/complete?taskId=${encodeURIComponent(taskId)}`;
    return this.request(endpoint, {
      method: 'POST',
      body: variables ? JSON.stringify(variables) : undefined,
    });
  }

  async failTask(taskId: string): Promise<ApiResponse<{ status: string; taskId: string }>> {
    const endpoint = `/v1/tasks/fail?taskId=${encodeURIComponent(taskId)}`;
    return this.request(endpoint, { method: 'POST' });
  }

  // Process endpoints
  async getProcessDefinitions(): Promise<ApiResponse<ProcessDefinition[]>> {
    return this.request('/v1/processes');
  }

  async getProcessDefinition(id: string): Promise<ApiResponse<ProcessDefinition>> {
    return this.request(`/v1/processes/${id}`);
  }

  async getProcessInstances(): Promise<ApiResponse<ProcessInstanceDTO[]>> {
    return this.request('/v1/processes/instances');
  }

  async startProcess(processId: string, variables?: Record<string, any>): Promise<ApiResponse<{ processInstanceId: string }>> {
    const endpoint = `/v1/processes/start?processId=${encodeURIComponent(processId)}`;
    return this.request(endpoint, {
      method: 'POST',
      body: variables ? JSON.stringify(variables) : undefined,
    });
  }

  async failProcessInstance(instanceId: string): Promise<ApiResponse<{ status: string; processInstanceId: string }>> {
    const endpoint = `/v1/processes/instance/${encodeURIComponent(instanceId)}/fail`;
    return this.request(endpoint, { method: 'POST' });
  }

  async deployProcess(file: File): Promise<ApiResponse<{ status: string }>> {
    const formData = new FormData();
    formData.append('file', file);

    return this.request('/v1/processes/deploy', {
      method: 'POST',
      body: formData,
    });
  }

  // User stats endpoint
  async getUserStats(): Promise<ApiResponse<UserStatsDto>> {
    return this.request('/v1/tasks/user-stats');
  }
}

export const apiClient = new ApiClient();
