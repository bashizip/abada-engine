import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, Star, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { apiClient, TaskDetailsDto, TaskStatus } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { formatDistanceToNow, getTaskStatusColors } from '@/lib/utils';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog";
import { ApiErrorToast } from '@/components/ApiErrorToast';
import JSONInput from 'react-json-editor-ajrm';
import locale from 'react-json-editor-ajrm/locale/en';

export default function TaskDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [task, setTask] = useState<TaskDetailsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [isClaimConfirmOpen, setIsClaimConfirmOpen] = useState(false);
  const [isCompleteConfirmOpen, setIsCompleteConfirmOpen] = useState(false);
  const [isFailConfirmOpen, setIsFailConfirmOpen] = useState(false);
  const [completionVariables, setCompletionVariables] = useState({});
  const [isValidating, setIsValidating] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    if (id) {
      fetchTask(id);
    }
  }, [id]);

  const fetchTask = async (taskId: string) => {
    setLoading(true);
    const response = await apiClient.getTask(taskId);
    setLoading(false);
    if (response.data) {
      setTask(response.data);
    } else {
      toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to fetch task details" }));
    }
  };

  const handleClaim = async () => {
    if (!task) return;

    setActionLoading(true);
    const response = await apiClient.claimTask(task.id);
    setActionLoading(false);
    setIsClaimConfirmOpen(false);

    if (response.data?.status === 'Claimed') {
      toast({
        title: "Task claimed",
        description: "You have successfully claimed this task",
      });
      fetchTask(task.id);
    } else {
      toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to claim task" }));
    }
  };

  const handleComplete = async () => {
    if (!task) return;

    setActionLoading(true);
    const response = await apiClient.completeTask(task.id, completionVariables);
    setActionLoading(false);
    setIsCompleteConfirmOpen(false);

    if (response.data?.status === 'Completed') {
      toast({
        title: "Task completed",
        description: "Task has been marked as completed",
      });
      navigate('/tasks');
    } else {
      toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to complete task" }));
    }
  };

  const handleFail = async () => {
    if (!task) return;

    setActionLoading(true);
    const response = await apiClient.failTask(task.id);
    setActionLoading(false);
    setIsFailConfirmOpen(false);

    if (response.data?.status === 'Failed') {
      toast({
        title: "Task failed",
        description: "Task has been marked as failed",
      });
      fetchTask(task.id);
    } else {
      toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to fail task" }));
    }
  };

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

  if (loading) {
    return (
      <Layout>
        <div className="container mx-auto px-6 py-8">
          <div className="text-center">
            <p className="text-muted-foreground">Loading task details...</p>
          </div>
        </div>
      </Layout>
    );
  }

  if (!task) {
    return (
      <Layout>
        <div className="container mx-auto px-6 py-8">
          <div className="text-center">
            <p className="text-muted-foreground">Task not found</p>
            <Button onClick={() => navigate('/tasks')} className="mt-4">
              Back to Tasks
            </Button>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="container mx-auto px-6 py-8 max-w-4xl">
        <div className="mb-6">
          <Button
            variant="ghost"
            onClick={() => navigate('/tasks')}
            className="mb-4"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Tasks
          </Button>

          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-foreground">Task Details</h1>
              <p className="text-muted-foreground mt-1">
                View and manage the details of the selected task.
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-8">
          {/* Task Information */}
          <div>
            <h2 className="text-xl font-semibold text-foreground mb-4">Task Information</h2>
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Task ID</p>
                <p className="font-medium text-foreground">#{task.id}</p>
              </div>

              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Task Name</p>
                <p className="font-medium text-foreground">{task.name}</p>
              </div>

              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Assignee</p>
                <p className="font-medium text-foreground">{task.assignee || 'Unassigned'}</p>
              </div>

              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Status</p>
                <Badge className={getTaskStatusColors(task.status)}>{getStatusLabel(task.status)}</Badge>
              </div>

              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Start Date</p>
                <p className="font-medium text-foreground">{formatDistanceToNow(task.startDate)}</p>
              </div>

              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">End Date</p>
                <p className="font-medium text-foreground">{formatDistanceToNow(task.endDate)}</p>
              </div>
            </div>
          </div>

          {/* Process Information */}
          <div>
            <h2 className="text-xl font-semibold text-foreground mb-4">Process Information</h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-muted p-4 rounded-lg">
                <p className="text-sm text-muted-foreground mb-1">Process ID</p>
                <p className="font-medium text-foreground">#{task.processInstanceId}</p>
              </div>
            </div>
          </div>

          {/* Variables */}
          <div>
            <h2 className="text-xl font-semibold text-foreground mb-4">Variables</h2>
            <div className="bg-slate-800 p-4 rounded-lg">
              <pre className="text-sm font-mono text-white overflow-x-auto">
                <code className="text-white">
                  {JSON.stringify(task.variables || {}, null, 2)}
                </code>
              </pre>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3 pt-4">
            <Button
              variant="outline"
              onClick={() => setIsClaimConfirmOpen(true)}
              disabled={actionLoading || task.status !== 'AVAILABLE'}
              className="bg-blue-500 text-white border-blue-500 hover:bg-blue-600 hover:border-blue-600"
            >
              <Star className="mr-2 h-4 w-4" />
              Claim
            </Button>

            <Button
              onClick={() => {
                setIsCompleteConfirmOpen(true);
                setIsValidating(false);
              }}
              disabled={actionLoading || task.status !== 'CLAIMED'}
              className="bg-green-500 text-white hover:bg-green-600"
            >
              <CheckCircle className="mr-2 h-4 w-4" />
              Complete
            </Button>

            <Button
              variant="destructive"
              onClick={() => setIsFailConfirmOpen(true)}
              disabled={actionLoading || task.status === 'COMPLETED' || task.status === 'FAILED'}
            >
              <XCircle className="mr-2 h-4 w-4" />
              Fail
            </Button>
          </div>
        </div>
      </div>
      <Dialog open={isClaimConfirmOpen} onOpenChange={setIsClaimConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Are you sure?</DialogTitle>
            <DialogDescription>
              This will claim the task for the current user.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleClaim} disabled={actionLoading}>
              {actionLoading ? 'Claiming...' : 'Confirm'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog open={isCompleteConfirmOpen} onOpenChange={(open) => {
        setIsCompleteConfirmOpen(open);
        if (!open) setIsValidating(false);
      }}>
        <DialogContent className="sm:max-w-[625px]">
          <DialogHeader>
            <DialogTitle>Complete Task</DialogTitle>
            <DialogDescription>
              Enter any variables to pass to the process upon completion.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <div onKeyDown={() => setIsValidating(true)} onPaste={() => setIsValidating(true)}>
              <JSONInput
                id='completion-variables-editor'
                placeholder={completionVariables}
                locale={locale}
                onChange={(data: any) => {
                  setIsValidating(false);
                  if (data.error === false) {
                    setCompletionVariables(data.jsObject);
                  }
                }}
                height='250px'
                width='100%'
                theme="dark_vscode_tribute"
                waitAfterKeyPress={5000}
              />
            </div>
            {isValidating && (
              <div className="flex items-center mt-2 text-sm text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Validating JSON...
              </div>
            )}
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleComplete} disabled={actionLoading}>
              {actionLoading ? 'Completing...' : 'Confirm & Complete'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog open={isFailConfirmOpen} onOpenChange={setIsFailConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Are you sure?</DialogTitle>
            <DialogDescription>
              This will mark the task as failed.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleFail} disabled={actionLoading} variant="destructive">
              {actionLoading ? 'Failing...' : 'Confirm'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Layout>
  );
}
