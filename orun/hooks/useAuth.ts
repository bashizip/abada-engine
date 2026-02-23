import { useAuthContext } from "@/components/AuthProvider";

export function useAuth() {
  return useAuthContext();
}
