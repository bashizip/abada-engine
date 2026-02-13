import { API_BASE_URL } from "../config";
import {
  ProcessDefinition,
  ProcessInstance,
  Job,
  Variable,
  ActivityInstance,
} from "../types";
import { calculateDuration } from "../utils";
import { keycloak, refreshToken } from "@/keycloak/keycloakClient";

// API Response types (what the backend actually returns)
interface ApiProcessInstance {
  id: string;
  processDefinitionId: string;
  processDefinitionName: string;
  currentActivityId?: string;
  status: "RUNNING" | "COMPLETED" | "FAILED" | "SUSPENDED" | "CANCELLED";
  suspended: boolean;
  startDate: string;
  endDate?: string;
  startedBy: string;
  variables: Record<string, any>;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `API Error: ${response.status} ${response.statusText} - ${errorText}`,
    );
  }
  // Check if response has content before trying to parse JSON
  const text = await response.text();
  return text ? JSON.parse(text) : ({} as T);
}

async function getAuthHeaders(
  extraHeaders?: Record<string, string>,
): Promise<HeadersInit> {
  await refreshToken(30);
  const headers: Record<string, string> = {
    ...(extraHeaders || {}),
  };

  if (keycloak.token) {
    headers["Authorization"] = `Bearer ${keycloak.token}`;
  }

  return headers;
}

// Transform API response to frontend types
function transformProcessInstance(
  apiInstance: ApiProcessInstance,
): ProcessInstance {
  const duration = apiInstance.endDate
    ? calculateDuration(apiInstance.startDate, apiInstance.endDate)
    : undefined;

  return {
    id: apiInstance.id,
    definitionId: apiInstance.processDefinitionId,
    definitionName: apiInstance.processDefinitionName,
    status: apiInstance.status,
    startedBy: apiInstance.startedBy,
    startTime: apiInstance.startDate,
    endTime: apiInstance.endDate,
    currentActivity: apiInstance.currentActivityId,
    duration,
  };
}

export const api = {
  // Process Definitions
  getProcessDefinitions: async (): Promise<ProcessDefinition[]> => {
    const response = await fetch(`${API_BASE_URL}/v1/processes`, {
      headers: await getAuthHeaders(),
    });
    return handleResponse<ProcessDefinition[]>(response);
  },

  getProcessDefinition: async (id: string): Promise<ProcessDefinition> => {
    const response = await fetch(`${API_BASE_URL}/v1/processes/${id}`, {
      headers: await getAuthHeaders(),
    });
    return handleResponse<ProcessDefinition>(response);
  },

  // Process Instances
  getProcessInstances: async (): Promise<ProcessInstance[]> => {
    const response = await fetch(`${API_BASE_URL}/v1/processes/instances`, {
      headers: await getAuthHeaders(),
    });
    const apiInstances = await handleResponse<ApiProcessInstance[]>(response);
    return apiInstances.map(transformProcessInstance);
  },

  getProcessInstance: async (id: string): Promise<ProcessInstance> => {
    const response = await fetch(
      `${API_BASE_URL}/v1/processes/instances/${id}`,
      {
        headers: await getAuthHeaders(),
      },
    );
    const apiInstance = await handleResponse<ApiProcessInstance>(response);
    return transformProcessInstance(apiInstance);
  },

  cancelProcessInstance: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/v1/process-instances/${id}`, {
      method: "DELETE",
      headers: await getAuthHeaders(),
    });
    return handleResponse<void>(response);
  },

  suspendProcessInstance: async (
    id: string,
    suspended: boolean,
  ): Promise<void> => {
    console.log(`[API] Suspending instance ${id}, suspended=${suspended}`);
    const url = `${API_BASE_URL}/v1/process-instances/${id}/suspension`;
    console.log(`[API] Request URL: ${url}`);
    console.log(`[API] Request body:`, { suspended });

    const response = await fetch(url, {
      method: "PUT",
      headers: await getAuthHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ suspended }),
    });

    console.log(
      `[API] Response status: ${response.status} ${response.statusText}`,
    );
    const result = await handleResponse<void>(response);
    console.log(`[API] Suspend operation completed successfully`);
    return result;
  },

  getActivityInstances: async (id: string): Promise<ActivityInstance[]> => {
    const response = await fetch(
      `${API_BASE_URL}/v1/process-instances/${id}/activity-instances`,
      {
        headers: await getAuthHeaders(),
      },
    );
    const data = await handleResponse<{
      childActivityInstances: ActivityInstance[];
    }>(response);
    return data.childActivityInstances || [];
  },

  // Jobs
  getJobs: async (): Promise<Job[]> => {
    const response = await fetch(`${API_BASE_URL}/v1/jobs`, {
      headers: await getAuthHeaders(),
    });
    return handleResponse<Job[]>(response);
  },

  retryJob: async (jobId: string, retries: number = 3): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/v1/jobs/${jobId}/retries`, {
      method: "POST",
      headers: await getAuthHeaders({
        "Content-Type": "application/json",
      }),
      body: JSON.stringify({ retries }),
    });
    return handleResponse<void>(response);
  },

  getJobStacktrace: async (jobId: string): Promise<string> => {
    const response = await fetch(
      `${API_BASE_URL}/v1/jobs/${jobId}/stacktrace`,
      {
        headers: await getAuthHeaders(),
      },
    );
    return response.text();
  },

  // Variables
  getProcessVariables: async (id: string): Promise<Variable[]> => {
    const response = await fetch(
      `${API_BASE_URL}/v1/process-instances/${id}/variables`,
      {
        headers: await getAuthHeaders(),
      },
    );
    const data =
      await handleResponse<Record<string, { value: any; type: string }>>(
        response,
      );
    return Object.entries(data).map(([name, info]) => ({
      name,
      value: info.value,
      type: info.type as any,
    }));
  },

  updateProcessVariable: async (
    id: string,
    variableName: string,
    value: any,
    type: string,
  ): Promise<void> => {
    const response = await fetch(
      `${API_BASE_URL}/v1/process-instances/${id}/variables`,
      {
        method: "PATCH",
        headers: await getAuthHeaders({
          "Content-Type": "application/json",
        }),
        body: JSON.stringify({
          modifications: {
            [variableName]: { value, type },
          },
        }),
      },
    );
    return handleResponse<void>(response);
  },
};
