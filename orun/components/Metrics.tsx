import React, { useState, useEffect } from 'react';
import { Card } from './ui/Common.tsx';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line } from 'recharts';
import { Cpu, HardDrive, Activity, Zap, TrendingUp, Database } from 'lucide-react';
import { API_BASE_URL } from '../config';

// Types for system metrics
interface SystemMetrics {
    cpu: {
        usage: number;
        cores: number;
    };
    memory: {
        used: number;
        max: number;
        heapUsed: number;
        heapMax: number;
    };
    threads: {
        active: number;
        queued: number;
        peak: number;
    };
    jvm: {
        uptime: number;
        version: string;
    };
}

// Mock historical data for CPU/Memory trends (last 30 data points)
const generateMockHistory = () => {
    const history = [];
    const now = Date.now();
    for (let i = 29; i >= 0; i--) {
        history.push({
            time: new Date(now - i * 10000).toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' }),
            cpu: Math.random() * 30 + 20, // 20-50%
            memory: Math.random() * 20 + 40, // 40-60%
        });
    }
    return history;
};

export const Metrics: React.FC = () => {
    const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
    const [history, setHistory] = useState(generateMockHistory());
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Fetch system metrics from API
    const fetchMetrics = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/v1/metrics/system`, {
                headers: {
                    'X-User': 'alice',
                    'X-Groups': 'customers,managers',
                },
            });

            if (!response.ok) {
                // If endpoint doesn't exist yet, use mock data
                if (response.status === 404) {
                    setMetrics({
                        cpu: { usage: 32.5, cores: 8 },
                        memory: {
                            used: 2048 * 1024 * 1024,
                            max: 4096 * 1024 * 1024,
                            heapUsed: 512 * 1024 * 1024,
                            heapMax: 1024 * 1024 * 1024
                        },
                        threads: { active: 24, queued: 3, peak: 45 },
                        jvm: { uptime: 3456789, version: '17.0.9' }
                    });
                    setError(null);
                    return;
                }
                throw new Error(`Failed to fetch metrics: ${response.statusText}`);
            }

            const data = await response.json();
            setMetrics(data);
            setError(null);
        } catch (err) {
            // Use mock data on error
            setMetrics({
                cpu: { usage: 32.5, cores: 8 },
                memory: {
                    used: 2048 * 1024 * 1024,
                    max: 4096 * 1024 * 1024,
                    heapUsed: 512 * 1024 * 1024,
                    heapMax: 1024 * 1024 * 1024
                },
                threads: { active: 24, queued: 3, peak: 45 },
                jvm: { uptime: 3456789, version: '17.0.9' }
            });
            setError(null);
        } finally {
            setLoading(false);
        }
    };

    // Auto-refresh metrics every 5 seconds
    useEffect(() => {
        fetchMetrics();
        const interval = setInterval(() => {
            fetchMetrics();
            // Update history with new data point
            setHistory(prev => {
                const newHistory = [...prev.slice(1)];
                newHistory.push({
                    time: new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' }),
                    cpu: metrics?.cpu.usage || Math.random() * 30 + 20,
                    memory: metrics ? (metrics.memory.used / metrics.memory.max) * 100 : Math.random() * 20 + 40,
                });
                return newHistory;
            });
        }, 5000);

        return () => clearInterval(interval);
    }, [metrics]);

    // Format bytes to human readable
    const formatBytes = (bytes: number): string => {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
    };

    // Format uptime to human readable
    const formatUptime = (ms: number): string => {
        const seconds = Math.floor(ms / 1000);
        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        return `${days}d ${hours}h ${minutes}m`;
    };

    if (loading) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-bold text-slate-100">System Metrics</h1>
                <div className="text-slate-400">Loading metrics...</div>
            </div>
        );
    }

    if (!metrics) {
        return (
            <div className="space-y-6">
                <h1 className="text-2xl font-bold text-slate-100">System Metrics</h1>
                <div className="text-red-400">Failed to load metrics</div>
            </div>
        );
    }

    const memoryUsagePercent = (metrics.memory.used / metrics.memory.max) * 100;
    const heapUsagePercent = (metrics.memory.heapUsed / metrics.memory.heapMax) * 100;

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-100">System Metrics</h1>
                <div className="text-sm text-slate-400">
                    Auto-refresh every 5s
                </div>
            </div>

            {/* Key System Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
                <Card className="p-6">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-slate-400 text-sm flex items-center gap-2">
                                <Cpu size={16} />
                                CPU Usage
                            </p>
                            <p className="text-3xl font-bold text-slate-100 mt-2">
                                {metrics.cpu.usage.toFixed(1)}%
                            </p>
                            <p className="text-xs text-slate-500 mt-1">{metrics.cpu.cores} cores available</p>
                        </div>
                        <div className={`px-2 py-1 rounded text-xs font-medium ${metrics.cpu.usage > 80 ? 'bg-red-500/20 text-red-400' :
                                metrics.cpu.usage > 50 ? 'bg-yellow-500/20 text-yellow-400' :
                                    'bg-emerald-500/20 text-emerald-400'
                            }`}>
                            {metrics.cpu.usage > 80 ? 'HIGH' : metrics.cpu.usage > 50 ? 'MODERATE' : 'NORMAL'}
                        </div>
                    </div>
                </Card>

                <Card className="p-6">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-slate-400 text-sm flex items-center gap-2">
                                <HardDrive size={16} />
                                Memory Usage
                            </p>
                            <p className="text-3xl font-bold text-slate-100 mt-2">
                                {memoryUsagePercent.toFixed(1)}%
                            </p>
                            <p className="text-xs text-slate-500 mt-1">
                                {formatBytes(metrics.memory.used)} / {formatBytes(metrics.memory.max)}
                            </p>
                        </div>
                        <div className={`px-2 py-1 rounded text-xs font-medium ${memoryUsagePercent > 85 ? 'bg-red-500/20 text-red-400' :
                                memoryUsagePercent > 70 ? 'bg-yellow-500/20 text-yellow-400' :
                                    'bg-emerald-500/20 text-emerald-400'
                            }`}>
                            {memoryUsagePercent > 85 ? 'HIGH' : memoryUsagePercent > 70 ? 'MODERATE' : 'NORMAL'}
                        </div>
                    </div>
                </Card>

                <Card className="p-6">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-slate-400 text-sm flex items-center gap-2">
                                <Activity size={16} />
                                Thread Pool
                            </p>
                            <p className="text-3xl font-bold text-slate-100 mt-2">
                                {metrics.threads.active}
                            </p>
                            <p className="text-xs text-slate-500 mt-1">
                                {metrics.threads.queued} queued · {metrics.threads.peak} peak
                            </p>
                        </div>
                        <div className={`px-2 py-1 rounded text-xs font-medium ${metrics.threads.queued > 10 ? 'bg-yellow-500/20 text-yellow-400' :
                                'bg-emerald-500/20 text-emerald-400'
                            }`}>
                            {metrics.threads.queued > 10 ? 'QUEUED' : 'HEALTHY'}
                        </div>
                    </div>
                </Card>

                <Card className="p-6">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-slate-400 text-sm flex items-center gap-2">
                                <Zap size={16} />
                                JVM Uptime
                            </p>
                            <p className="text-2xl font-bold text-slate-100 mt-2">
                                {formatUptime(metrics.jvm.uptime)}
                            </p>
                            <p className="text-xs text-slate-500 mt-1">
                                Java {metrics.jvm.version}
                            </p>
                        </div>
                    </div>
                </Card>
            </div>

            {/* Historical Trends */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card className="p-6">
                    <h3 className="text-lg font-medium text-slate-200 mb-6 flex items-center gap-2">
                        <TrendingUp size={18} />
                        CPU Usage Trend (Last 5min)
                    </h3>
                    <div className="h-[280px] w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={history}>
                                <defs>
                                    <linearGradient id="colorCpu" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
                                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.1} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                                <XAxis
                                    dataKey="time"
                                    stroke="#94a3b8"
                                    tick={{ fontSize: 11 }}
                                    interval="preserveStartEnd"
                                    tickFormatter={(value, index) => index % 5 === 0 ? value : ''}
                                />
                                <YAxis stroke="#94a3b8" domain={[0, 100]} unit="%" />
                                <Tooltip
                                    contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', color: '#f1f5f9' }}
                                    formatter={(value: number) => `${value.toFixed(1)}%`}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="cpu"
                                    stroke="#3b82f6"
                                    fillOpacity={1}
                                    fill="url(#colorCpu)"
                                    name="CPU"
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </Card>

                <Card className="p-6">
                    <h3 className="text-lg font-medium text-slate-200 mb-6 flex items-center gap-2">
                        <Database size={18} />
                        Memory Usage Trend (Last 5min)
                    </h3>
                    <div className="h-[280px] w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={history}>
                                <defs>
                                    <linearGradient id="colorMemory" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.8} />
                                        <stop offset="95%" stopColor="#10b981" stopOpacity={0.1} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                                <XAxis
                                    dataKey="time"
                                    stroke="#94a3b8"
                                    tick={{ fontSize: 11 }}
                                    interval="preserveStartEnd"
                                    tickFormatter={(value, index) => index % 5 === 0 ? value : ''}
                                />
                                <YAxis stroke="#94a3b8" domain={[0, 100]} unit="%" />
                                <Tooltip
                                    contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', color: '#f1f5f9' }}
                                    formatter={(value: number) => `${value.toFixed(1)}%`}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="memory"
                                    stroke="#10b981"
                                    fillOpacity={1}
                                    fill="url(#colorMemory)"
                                    name="Memory"
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </Card>
            </div>

            {/* JVM Heap Details */}
            <Card className="p-6">
                <h3 className="text-lg font-medium text-slate-200 mb-4">JVM Heap Memory</h3>
                <div className="space-y-4">
                    <div>
                        <div className="flex justify-between text-sm mb-2">
                            <span className="text-slate-400">Heap Usage</span>
                            <span className="text-slate-300">
                                {formatBytes(metrics.memory.heapUsed)} / {formatBytes(metrics.memory.heapMax)}
                                <span className="ml-2 text-slate-500">({heapUsagePercent.toFixed(1)}%)</span>
                            </span>
                        </div>
                        <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
                            <div
                                className={`h-full transition-all duration-300 ${heapUsagePercent > 85 ? 'bg-red-500' :
                                        heapUsagePercent > 70 ? 'bg-yellow-500' :
                                            'bg-emerald-500'
                                    }`}
                                style={{ width: `${heapUsagePercent}%` }}
                            />
                        </div>
                    </div>

                    <div>
                        <div className="flex justify-between text-sm mb-2">
                            <span className="text-slate-400">Total Memory</span>
                            <span className="text-slate-300">
                                {formatBytes(metrics.memory.used)} / {formatBytes(metrics.memory.max)}
                                <span className="ml-2 text-slate-500">({memoryUsagePercent.toFixed(1)}%)</span>
                            </span>
                        </div>
                        <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
                            <div
                                className={`h-full transition-all duration-300 ${memoryUsagePercent > 85 ? 'bg-red-500' :
                                        memoryUsagePercent > 70 ? 'bg-yellow-500' :
                                            'bg-emerald-500'
                                    }`}
                                style={{ width: `${memoryUsagePercent}%` }}
                            />
                        </div>
                    </div>
                </div>
            </Card>
        </div>
    );
};
