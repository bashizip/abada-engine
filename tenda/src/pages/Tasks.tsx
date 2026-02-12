import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Search } from 'lucide-react';
import { apiClient, TaskDetailsDto, TaskStatus } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { formatDistanceToNow, getTaskStatusColors } from '@/lib/utils';
import { ApiErrorToast } from '@/components/ApiErrorToast';

export default function Tasks() {
  const [tasks, setTasks] = useState<TaskDetailsDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<TaskStatus | 'all'>('all');
  const { toast } = useToast();

  const fetchTasks = async () => {
    setLoading(true);
    const response = await apiClient.getTasks({ status: statusFilter === 'all' ? undefined : statusFilter });
    if (response.data) {
      setTasks(response.data);
    } else {
      toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to fetch tasks" }));
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchTasks();
  }, [statusFilter]);

  const getStatusLabel = (status: TaskStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'Completed';
      case 'CLAIMED':
        return 'Claimed';
      case 'AVAILABLE':
        return 'Available';
      case 'FAILED':
        return 'Failed';
      default:
        return status;
    }
  };

  const filteredTasks = tasks.filter(task => {
    return task.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      task.assignee?.toLowerCase().includes(searchTerm.toLowerCase());
  });

  return (
    <Layout>
      <div className="container mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-foreground">My Tasks</h1>
            <p className="text-sm text-muted-foreground mt-1">Manage and track your assigned tasks</p>
          </div>

        </div>

        {/* Filters */}
        <Card className="mb-6">
          <CardContent className="pt-6">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                  <Input
                    placeholder="Search tasks..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10 text-sm placeholder:text-muted-foreground"
                  />
                </div>
              </div>
              <div className="w-full md:w-48">
                <Select value={statusFilter} onValueChange={(value) => setStatusFilter(value as TaskStatus | 'all')}>
                  <SelectTrigger>
                    <SelectValue placeholder="Status: All" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">Status: All</SelectItem>
                    <SelectItem value="AVAILABLE">Available</SelectItem>
                    <SelectItem value="CLAIMED">Claimed</SelectItem>
                    <SelectItem value="COMPLETED">Completed</SelectItem>
                    <SelectItem value="FAILED">Failed</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Task Table */}
        <Card>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-border">
                  <tr>
                    <th className="text-left p-4 text-sm font-semibold text-muted-foreground">TASK NAME</th>
                    <th className="text-left p-4 text-sm font-semibold text-muted-foreground">ASSIGNEE</th>
                    <th className="text-left p-4 text-sm font-semibold text-muted-foreground">CANDIDATE GROUPS</th>
                    <th className="text-left p-4 text-sm font-semibold text-muted-foreground">START DATE</th>
                    <th className="text-left p-4 text-sm font-semibold text-muted-foreground">END DATE</th>
                    <th className="text-left p-4 text-sm font-semibold text-muted-foreground">STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={6} className="text-center p-8 text-muted-foreground">
                        Loading tasks...
                      </td>
                    </tr>
                  ) : filteredTasks.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center p-8 text-muted-foreground">
                        No tasks found
                      </td>
                    </tr>
                  ) : (
                    filteredTasks.map((task) => (
                      <tr key={task.id} className="border-b border-border hover:bg-muted/50">
                        <td className="p-4">
                          <Link
                            to={`/tasks/${task.id}`}
                            className="text-sm font-semibold text-foreground hover:text-primary"
                          >
                            {task.name}
                          </Link>
                        </td>
                        <td className="p-4 text-sm text-muted-foreground">
                          {task.assignee || '-'}
                        </td>
                        <td className="p-4 text-sm text-muted-foreground">
                          {task.candidateGroups?.join(', ') || '-'}
                        </td>
                        <td className="p-4 text-sm text-muted-foreground">
                          {formatDistanceToNow(task.startDate)}
                        </td>
                        <td className="p-4 text-sm text-muted-foreground">
                          {formatDistanceToNow(task.endDate)}
                        </td>
                        <td className="p-4">
                          <Badge className={getTaskStatusColors(task.status)}>
                            {getStatusLabel(task.status)}
                          </Badge>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}
