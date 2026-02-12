import { useState, useEffect, useRef } from 'react';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog";
import { Label } from '@/components/ui/label';
import { Upload, Eye, Loader2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiClient, ProcessDefinition } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { ApiErrorToast } from '@/components/ApiErrorToast';
import JSONInput from 'react-json-editor-ajrm';
import locale from 'react-json-editor-ajrm/locale/en';

export default function Processes() {
  const [processes, setProcesses] = useState<ProcessDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedProcess, setSelectedProcess] = useState<ProcessDefinition | null>(null);
  const [variables, setVariables] = useState({});
  const [isValidating, setIsValidating] = useState(false);
  const [startingProcess, setStartingProcess] = useState(false);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const { toast } = useToast();
  const validationTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    fetchProcesses();
  }, []);

  const fetchProcesses = async () => {
    setLoading(true);
    try {
      const response = await apiClient.getProcessDefinitions();
      if (response.data) {
        setProcesses(response.data);
      } else {
        setProcesses([]);
        toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to fetch processes" }));
      }
    } catch (error) {
      toast(ApiErrorToast({ error: error, defaultMessage: "Failed to fetch processes" }));
    } finally {
      setLoading(false);
    }
  };

  const handleStartProcess = async () => {
    if (!selectedProcess) return;

    setStartingProcess(true);
    try {
      const response = await apiClient.startProcess(selectedProcess.id, variables);
      if (response.data) {
        toast({
          title: "Process started",
          description: `Process instance ${response.data.processInstanceId} created successfully`,
        });
        setIsConfirmOpen(false);
        setVariables({});
      } else {
        toast(ApiErrorToast({ error: response.error, defaultMessage: "Failed to start process" }));
      }
    } catch (error) {
      toast(ApiErrorToast({ error: error, defaultMessage: "Failed to start process" }));
    } finally {
      setStartingProcess(false);
    }
  };

  const handleOpenConfirmDialog = (process: ProcessDefinition) => {
    setSelectedProcess(process);
    setIsConfirmOpen(true);
    setIsValidating(false);
  }

  return (
    <Layout>
      <div className="container mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-foreground">Process List</h1>
            <p className="text-sm text-muted-foreground mt-1">Browse and start available processes.</p>
          </div>
          <Link to="/processes/upload">
            <Button className="bg-accent hover:bg-accent/90">
              <Upload className="mr-2 h-4 w-4" />
              Deploy Process
            </Button>
          </Link>
        </div>

        {/* Process Grid */}
        {loading ? (
          <div className="text-center p-8">Loading processes...</div>
        ) : processes.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            {processes.map((process) => (
              <Card key={process.id} className="hover:shadow-md transition-shadow flex flex-col">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-lg font-semibold">{process.name}</CardTitle>
                  </div>
                  <CardDescription className="text-sm text-muted-foreground pt-2">{process.documentation || 'No documentation available.'}</CardDescription>
                </CardHeader>
                <CardContent className="mt-auto flex items-center space-x-2 pt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 border-info text-info hover:bg-info hover:text-info-foreground text-sm font-medium"
                    onClick={() => handleOpenConfirmDialog(process)}
                  >
                    Start
                  </Button>
                  <Link to={`/processes/${process.id}`} className="flex-1">
                    <Button variant="secondary" size="sm" className="w-full">
                      <Eye className="mr-2 h-4 w-4" />
                      View
                    </Button>
                  </Link>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <div className="text-center p-8 border rounded-md">
            <h3 className="text-lg font-semibold">No Processes Found</h3>
            <p className="text-muted-foreground">There are no available processes to display.</p>
          </div>
        )}
      </div>

      {selectedProcess && (
        <Dialog open={isConfirmOpen} onOpenChange={(open) => {
          setIsConfirmOpen(open);
          if (!open) setIsValidating(false);
        }}>
          <DialogContent className="sm:max-w-[625px]">
            <DialogHeader>
              <DialogTitle>Start Process: {selectedProcess.name}</DialogTitle>
              <DialogDescription>
                Enter any initial variables to pass to the process.
              </DialogDescription>
            </DialogHeader>
            <div className="py-4">
              <Label htmlFor="variables-editor" className="text-sm font-medium">Initial Variables (JSON format)</Label>
              <div className="mt-2">
                <div onKeyDown={() => setIsValidating(true)} onPaste={() => setIsValidating(true)}>
                  <JSONInput
                    id='variables-editor'
                    placeholder={variables}
                    locale={locale}
                    onChange={(data: any) => {
                      if (validationTimeoutRef.current) {
                        clearTimeout(validationTimeoutRef.current);
                      }
                      validationTimeoutRef.current = setTimeout(() => {
                        setIsValidating(false);
                        if (data.error === false) {
                          setVariables(data.jsObject);
                        }
                      }, 2000);
                    }}
                    height='250px'
                    width='100%'
                    theme="dark_vscode_tribute"
                  />
                </div>
                {isValidating && (
                  <div className="flex items-center mt-2 text-sm text-muted-foreground">
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Validating JSON...
                  </div>
                )}
              </div>
            </div>
            <DialogFooter>
              <DialogClose asChild>
                <Button variant="outline">Cancel</Button>
              </DialogClose>
              <Button onClick={handleStartProcess} disabled={startingProcess}>
                {startingProcess ? 'Starting...' : 'Confirm & Start'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </Layout>
  );
}
