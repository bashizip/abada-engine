import { useState, useEffect, createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import { AuthState, initialAuthState, User } from '@/lib/auth';
import { keycloak, initKeycloak, refreshToken, getUserFromToken } from '@/auth/keycloakClient';
import { useToast } from '@/hooks/use-toast';
import { ApiErrorToast } from './ApiErrorToast';

interface AuthContextType extends AuthState {
  login: () => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>(initialAuthState);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const login = async (): Promise<void> => {
    try {
      await keycloak.login();
    } catch (error) {
      toast(ApiErrorToast({ error, defaultMessage: "Login failed" }));
    }
  };

  const logout = () => {
    keycloak.logout({ redirectUri: window.location.origin });
  };

  useEffect(() => {
    let refreshInterval: number | undefined;
    let mounted = true;

    const startAuth = async () => {
      setLoading(true);
      try {
        const authenticated = await initKeycloak();
        if (!mounted) return;

        const user: User | null = authenticated
          ? getUserFromToken(keycloak.tokenParsed)
          : null;

        setAuthState({
          user,
          token: keycloak.token ?? null,
          isAuthenticated: Boolean(authenticated),
        });

        if (authenticated) {
          toast({
            title: "Welcome back!",
            description: user ? `Logged in as ${user.username}` : "Logged in",
          });
          refreshInterval = window.setInterval(() => {
            refreshToken(30);
          }, 30000);
        }
      } catch (error) {
        if (!mounted) return;
        toast(ApiErrorToast({ error, defaultMessage: "Authentication failed" }));
        setAuthState(initialAuthState);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    startAuth();

    return () => {
      mounted = false;
      if (refreshInterval) window.clearInterval(refreshInterval);
    };
  }, []);

  return (
    <AuthContext.Provider value={{
      ...authState,
      login,
      logout,
      loading,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider');
  }
  return context;
}
