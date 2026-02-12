import { ToastAction } from "@/components/ui/toast";

interface ApiErrorToastProps {
  error: any;
  defaultMessage: string;
}

export function ApiErrorToast({ error, defaultMessage }: ApiErrorToastProps) {
  // This console.error is kept as it's useful for debugging all error types.
  console.error("API Error:", error);

  const title = defaultMessage;
  let description = null;

  // This is the correct way to differentiate the error types.
  if (error instanceof Error) {
    // For standard JavaScript errors, we only want the message.
    description = error.message;
  } else if (typeof error === 'object' && error !== null) {
    // For our API's JSON responses, we display the full object.
    description = (
      <pre className="mt-2 w-[340px] rounded-md bg-slate-950 p-4">
        <code className="text-white">{JSON.stringify(error, null, 2)}</code>
      </pre>
    );
  } else if (error) {
    // For any other case, display the error as a string.
    description = String(error);
  }

  return {
    variant: "destructive" as const,
    title: title,
    description: description,
    action: <ToastAction altText="Dismiss">Dismiss</ToastAction>,
  };
}
