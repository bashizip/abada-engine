# User-task assignment semantics

Assignments are compiled into `UserTaskAssignment` with literal or dynamic expressions. Expressions are evaluated exactly once when the task is created. `${a.b}` performs deterministic map-path lookup against process variables.

An assignee resolves to zero or one identifier; multiple values fail the command. Candidate expressions may resolve to one value or a collection. Values are trimmed, empty values discarded, and duplicates removed while preserving order. Groups are stored as groups and are never expanded at deployment.

A directly assigned task is `CLAIMED`; candidates remain stored for audit and reassignment. Claiming an assigned task is rejected. An available task may be claimed by a candidate user or group member. Only its current assignee may unclaim it; unclaim preserves candidates and returns it to `AVAILABLE`.

The durable audit stream records `TASK_CREATED`, `TASK_ASSIGNED`, `TASK_CLAIMED`, `TASK_UNCLAIMED`, and task completion events.

