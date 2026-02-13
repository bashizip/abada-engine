import { useState, useEffect, createContext, useContext } from "react";
import type { ReactNode } from "react";
import { AuthState, initialAuthState, type User } from "@/lib/auth";
import {
  keycloak,
  initKeycloak,
  refreshToken,
  getUserFromToken,
  hasOrunAdminRole,
} from "@/keycloak/keycloakClient";

interface AuthContextType extends AuthState {
  login: () => Promise<void>;
  logout: () => void;
  loading: boolean;
  isOrunAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>(initialAuthState);
  const [loading, setLoading] = useState(false);
  const [isOrunAdmin, setIsOrunAdmin] = useState(false);

  const login = async (): Promise<void> => {
    try {
      await keycloak.login();
    } catch (error) {
      console.error("Login failed", error);
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
        const hasRole = authenticated
          ? hasOrunAdminRole(keycloak.tokenParsed)
          : false;
        if (authenticated && !hasRole && import.meta.env.DEV) {
          const parsed = keycloak.tokenParsed as any;
          console.warn("Authenticated user lacks orun-admin access", {
            username: parsed?.preferred_username,
            realmRoles: parsed?.realm_access?.roles,
            groups: parsed?.groups,
            resourceAccess: parsed?.resource_access,
          });
        }

        setAuthState({
          user,
          token: keycloak.token ?? null,
          isAuthenticated: Boolean(authenticated),
        });
        setIsOrunAdmin(hasRole);

        if (authenticated) {
          refreshInterval = window.setInterval(() => {
            refreshToken(30);
          }, 30000);
        }
      } catch (error) {
        if (!mounted) return;
        console.error("Authentication failed", error);
        setAuthState(initialAuthState);
        setIsOrunAdmin(false);
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
    <AuthContext.Provider
      value={{
        ...authState,
        login,
        logout,
        loading,
        isOrunAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuthContext must be used within an AuthProvider");
  }
  return context;
}
