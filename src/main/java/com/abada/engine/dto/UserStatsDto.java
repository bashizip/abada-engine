package com.abada.engine.dto;

import com.abada.engine.core.model.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents user-specific statistics and activity data.
 * This is the payload returned by the `GET /v1/tasks/stats` endpoint.
 */
public record UserStatsDto(
        // Quick stats
        QuickStats quickStats,
        
        // Recent tasks assigned to user (last 5-10)
        List<RecentTask> recentTasks,
        
        // Tasks grouped by status
        Map<TaskStatus, Integer> tasksByStatus,
        
        // Overdue tasks (CLAIMED for more than 7 days)
        List<OverdueTask> overdueTasks,
        
        // Process activity
        ProcessActivity processActivity
) {
    
    public record QuickStats(
            int activeTasks,        // Tasks in CLAIMED status by user
            int completedTasks,     // Tasks in COMPLETED status by user
            int runningProcesses,   // Process instances that have tasks for user
            int availableTasks      // Tasks user can claim (AVAILABLE + eligible)
    ) {}
    
    public record RecentTask(
            String id,
            String name,
            String taskDefinitionKey,
            TaskStatus status,
            Instant startDate,
            String processInstanceId
    ) {}
    
    public record OverdueTask(
            String id,
            String name,
            String taskDefinitionKey,
            Instant startDate,
            long daysOverdue,
            String processInstanceId
    ) {}
    
    public record ProcessActivity(
            List<RecentProcess> recentlyStartedProcesses,  // Processes with user's tasks
            int activeProcessCount,                        // Count of active processes with user's tasks
            double completionRate                          // Completion rate for user's processes
    ) {}
    
    public record RecentProcess(
            String id,
            String processDefinitionId,
            Instant startDate,
            String currentActivityId
    ) {}
}
