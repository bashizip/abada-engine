# System Metrics API Endpoint

## Overview

The Metrics tab in Abada Orun now displays **system-level metrics** rather than application observability metrics (which are covered by Grafana). This provides a quick operational health check of the Abada engine without navigating to Grafana.

## API Endpoint

### GET `/v1/metrics/system`

Returns real-time system metrics for the Abada engine.

**Headers:**
```
X-User: alice
X-Groups: customers,managers
```

**Response (200 OK):**

```json
{
  "cpu": {
    "usage": 32.5,
    "cores": 8
  },
  "memory": {
    "used": 2147483648,
    "max": 4294967296,
    "heapUsed": 536870912,
    "heapMax": 1073741824
  },
  "threads": {
    "active": 24,
    "queued": 3,
    "peak": 45
  },
  "jvm": {
    "uptime": 3456789,
    "version": "17.0.9"
  }
}
```

**Response (404 Not Found):**
If the endpoint is not implemented, the UI will gracefully fall back to mock data.

## Field Descriptions

### CPU Metrics
- **`cpu.usage`** (number): Current CPU usage as a percentage (0-100)
- **`cpu.cores`** (number): Number of available CPU cores

### Memory Metrics
- **`memory.used`** (number): Total used memory in bytes
- **`memory.max`** (number): Maximum available memory in bytes
- **`memory.heapUsed`** (number): Used JVM heap memory in bytes
- **`memory.heapMax`** (number): Maximum JVM heap memory in bytes

### Thread Pool Metrics
- **`threads.active`** (number): Currently active threads in the thread pool
- **`threads.queued`** (number): Tasks queued waiting for thread pool
- **`threads.peak`** (number): Peak thread count since JVM start

### JVM Metrics
- **`jvm.uptime`** (number): JVM uptime in milliseconds
- **`jvm.version`** (string): Java version (e.g., "17.0.9")

## Implementation Guide (Backend)

### Spring Boot / Java Example

You can use Spring Boot Actuator or implement a custom endpoint:

```java
@RestController
@RequestMapping("/v1/metrics")
public class SystemMetricsController {
    
    @GetMapping("/system")
    public SystemMetrics getSystemMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        return SystemMetrics.builder()
            .cpu(CpuMetrics.builder()
                .usage(getProcessCpuLoad(osBean))
                .cores(osBean.getAvailableProcessors())
                .build())
            .memory(MemoryMetrics.builder()
                .used(memoryBean.getHeapMemoryUsage().getUsed() + 
                      memoryBean.getNonHeapMemoryUsage().getUsed())
                .max(memoryBean.getHeapMemoryUsage().getMax() + 
                     memoryBean.getNonHeapMemoryUsage().getMax())
                .heapUsed(memoryBean.getHeapMemoryUsage().getUsed())
                .heapMax(memoryBean.getHeapMemoryUsage().getMax())
                .build())
            .threads(ThreadMetrics.builder()
                .active(threadBean.getThreadCount())
                .queued(0) // Get from your thread pool executor
                .peak(threadBean.getPeakThreadCount())
                .build())
            .jvm(JvmMetrics.builder()
                .uptime(runtimeBean.getUptime())
                .version(System.getProperty("java.version"))
                .build())
            .build();
    }
    
    private double getProcessCpuLoad(OperatingSystemMXBean osBean) {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean)
                .getProcessCpuLoad() * 100;
        }
        return 0.0;
    }
}
```

### Using Spring Boot Actuator

If you're using Spring Boot Actuator, you can leverage existing metrics:

1. Add dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

2. Create endpoint that aggregates actuator metrics:
```java
@RestController
@RequestMapping("/v1/metrics")
public class SystemMetricsController {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @GetMapping("/system")
    public SystemMetrics getSystemMetrics() {
        // Fetch from actuator metrics
        double cpuUsage = meterRegistry.get("system.cpu.usage").gauge().value() * 100;
        long heapUsed = (long) meterRegistry.get("jvm.memory.used")
            .tag("area", "heap").gauge().value();
        // ... etc
    }
}
```

## UI Behavior

### Auto-Refresh
- The UI automatically refreshes metrics every **5 seconds**
- Historical trend shows the last **5 minutes** (30 data points)

### Status Indicators
The UI provides color-coded status indicators:

**CPU Usage:**
- 🟢 NORMAL: 0-50%
- 🟡 MODERATE: 50-80%
- 🔴 HIGH: 80-100%

**Memory Usage:**
- 🟢 NORMAL: 0-70%
- 🟡 MODERATE: 70-85%
- 🔴 HIGH: 85-100%

**Thread Pool:**
- 🟢 HEALTHY: 0-10 queued tasks
- 🟡 QUEUED: 10+ queued tasks

### Graceful Degradation
If the API endpoint returns 404 or encounters an error, the UI will:
1. Fall back to mock data for demonstration purposes
2. Show all visualizations normally
3. Not display error messages (to avoid confusion during development)

## Differences from Grafana

This metrics tab is **not** meant to replace Grafana. Instead, it provides:

✅ **Quick Health Check** - At-a-glance system status without leaving Orun
✅ **System Metrics Only** - CPU, Memory, Threads, JVM stats
✅ **Last 5 Minutes** - Recent trends for immediate troubleshooting
✅ **Simple UI** - Clean, focused view for operators

❌ **No Historical Data** - Use Grafana for long-term trends
❌ **No Custom Dashboards** - Use Grafana for detailed analysis  
❌ **No Business Metrics** - Process instance stats are on the Dashboard tab
❌ **No Alerting** - Use Grafana for alerts and notifications

## Testing

You can test the UI without implementing the backend endpoint:
1. The UI will automatically use mock data if the endpoint returns 404
2. Mock data shows realistic values that update every 5 seconds
3. All visualizations and features work with mock data

Once you implement the endpoint, simply restart the Orun container and it will automatically start using real data.
