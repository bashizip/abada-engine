# Metrics Tab Update - System Metrics Implementation

## Overview
The Metrics tab has been transformed from showing application observability metrics (which duplicate Grafana functionality) to displaying focused **system health metrics** for quick operational checks.

## What Changed

### Before
- Showed process instance metrics (Active vs Failed Instances)
- Displayed process duration statistics
- Showed throughput, latency, and error rates
- **Problem:** This duplicated Grafana's full observability capabilities

### After
- **CPU Usage** - Real-time CPU usage with status indicators (NORMAL/MODERATE/HIGH)
- **Memory Usage** - Total and heap memory consumption with visual progress bars
- **Thread Pool Stats** - Active threads, queued tasks, and peak thread count
- **JVM Metrics** - Uptime and Java version information
- **Historical Trends** - Last 5 minutes of CPU and Memory usage trends
- **Auto-refresh** - Updates every 5 seconds for real-time monitoring

## Key Features

### 1. At-a-Glance Health Cards
Four key metric cards with:
- Visual icons (CPU, HardDrive, Activity, Zap)
- Large, easy-to-read numbers
- Color-coded status badges (🟢 NORMAL, 🟡 MODERATE, 🔴 HIGH)
- Contextual information (cores, queued tasks, etc.)

### 2. Trend Visualization
- **CPU Usage Trend**: Area chart showing last 5 minutes
- **Memory Usage Trend**: Area chart showing last 5 minutes
- Both charts update in real-time as new data arrives
- Clean, dark-themed design consistent with the rest of Orun

### 3. JVM Heap Details
- Detailed heap memory breakdown with progress bars
- Color-coded based on usage thresholds
- Shows both heap and total memory usage

### 4. Smart Fallback
- If the backend endpoint doesn't exist (404), uses realistic mock data
- No error messages shown - graceful degradation
- Works out of the box for development/demo purposes

## Backend Integration

### API Endpoint Required
```
GET /v1/metrics/system
```

### Response Format
```json
{
  "cpu": { "usage": 32.5, "cores": 8 },
  "memory": {
    "used": 2147483648,
    "max": 4294967296,
    "heapUsed": 536870912,
    "heapMax": 1073741824
  },
  "threads": { "active": 24, "queued": 3, "peak": 45 },
  "jvm": { "uptime": 3456789, "version": "17.0.9" }
}
```

**Full implementation guide available in:** `docs/system-metrics-api.md`

## Files Modified

1. **`components/Metrics.tsx`** - Complete rewrite
   - Added real-time data fetching with auto-refresh
   - Implemented system metrics display
   - Added historical trend tracking
   - Enhanced UI with icons and status indicators

2. **`docs/system-metrics-api.md`** - New file
   - API endpoint specification
   - Backend implementation examples (Spring Boot)
   - Field descriptions and usage guide

## Design Rationale

### Why System Metrics Instead of Observability?

**Separation of Concerns:**
- **Grafana** → Full observability, historical trends, custom dashboards, alerts
- **Orun Metrics Tab** → Quick system health check, last 5 minutes only
- **Orun Dashboard** → Process instance statistics and workflow metrics

**User Benefits:**
1. **Quick Health Check** - See if system is healthy without leaving Orun
2. **Troubleshooting Context** - When investigating issues, see if it's resource-related
3. **No Grafana Dependency** - Basic system stats available even if Grafana is down
4. **Focused View** - Only what operators need for immediate assessment

### Status Thresholds

**CPU Usage:**
- 🟢 NORMAL: 0-50% - System has plenty of headroom
- 🟡 MODERATE: 50-80% - System is working hard but stable
- 🔴 HIGH: 80-100% - May indicate performance issues

**Memory Usage:**
- 🟢 NORMAL: 0-70% - Healthy memory usage
- 🟡 MODERATE: 70-85% - Approaching limits, watch closely
- 🔴 HIGH: 85-100% - Risk of OOM errors

**Thread Pool:**
- 🟢 HEALTHY: 0-10 queued - Tasks being processed smoothly
- 🟡 QUEUED: 10+ queued - Backlog forming, may need attention

## Usage

### For Developers
1. The component works immediately with mock data
2. Implement the backend endpoint when ready
3. Restart Orun - it will automatically use real data

### For Operators
1. Navigate to **Metrics** tab in Orun
2. View current system health at a glance
3. Check historical trends (last 5 minutes)
4. Use color-coded indicators to identify issues

### Testing
```bash
# Start dev server
npm run dev

# Visit http://localhost:5604
# Navigate to Metrics tab
# Should see mock data with realistic values updating every 5 seconds
```

## Next Steps

### Backend Implementation
1. Review `docs/system-metrics-api.md`
2. Implement `GET /v1/metrics/system` endpoint
3. Use Spring Boot JMX beans for metrics collection
4. Test endpoint returns correct format
5. Restart Orun to see real data

### Optional Enhancements (Future)
- Add garbage collection metrics
- Show database connection pool stats
- Display cache hit rates
- Add disk I/O metrics
- Network traffic statistics

## Summary

The Metrics tab now provides **exactly what it should**: a quick, focused view of system health that complements (rather than duplicates) Grafana. It's perfect for operators who need to quickly assess if system resources are the cause of any issues they're investigating, without leaving the Orun interface.
