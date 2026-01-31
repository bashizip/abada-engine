# Keycloak Integration with Frontend Apps

## Frontend OAuth2/OIDC Flow

### Tenda App Integration (React)

```javascript
// src/config/keycloak.ts
export const keycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'abada-dev',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'abada-frontend',
};

// src/auth/keycloakAdapter.ts
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak(keycloakConfig);

export const initKeycloak = async () => {
  try {
    const authenticated = await keycloak.init({
      onLoad: 'login-required',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256',
    });
    
    if (!authenticated) {
      console.warn('Not authenticated');
    } else {
      console.log('User authenticated:', keycloak.tokenParsed);
    }
    
    // Token refresh every 30 seconds
    setInterval(() => {
      keycloak.refreshToken(30)
        .success(refreshed => {
          if (refreshed) {
            console.log('Token refreshed');
          }
        })
        .error(() => {
          console.error('Failed to refresh token');
          keycloak.logout();
        });
    }, 30000);
    
    return keycloak;
  } catch (error) {
    console.error('Keycloak init failed:', error);
    throw error;
  }
};

// src/api/apiClient.ts
import axios from 'axios';
import { keycloak } from './keycloakAdapter';

const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:5601/abada/api',
});

apiClient.interceptors.request.use(config => {
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export default apiClient;
```

### Orun App Integration (Vue)

```javascript
// src/keycloak.ts
import Keycloak from 'keycloak-js';

const keycloakInstance = new Keycloak({
  url: import.meta.env.VITE_KC_URL,
  realm: import.meta.env.VITE_KC_REALM,
  clientId: import.meta.env.VITE_KC_CLIENT_ID,
});

export const initKeycloak = async () => {
  try {
    const authenticated = await keycloakInstance.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
    });
    return authenticated;
  } catch (error) {
    console.error('Keycloak initialization failed', error);
    throw error;
  }
};

export default keycloakInstance;

// src/api/axiosInstance.ts
import axios from 'axios';
import keycloak from '@/keycloak';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

axiosInstance.interceptors.request.use((config) => {
  if (keycloak && keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

export default axiosInstance;
```

### Angular Integration

```typescript
// src/app/auth/keycloak.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  keycloak: Keycloak | undefined;

  constructor(private http: HttpClient) {}

  init(): Promise<boolean> {
    this.keycloak = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    });

    return this.keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }

  login() {
    this.keycloak?.login();
  }

  logout() {
    this.keycloak?.logout();
  }

  getToken() {
    return this.keycloak?.token;
  }

  isTokenExpired() {
    return this.keycloak?.isTokenExpired();
  }

  refreshToken() {
    return this.keycloak?.refreshToken(30);
  }
}

// src/app/auth/auth.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { KeycloakService } from './keycloak.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private keycloakService: KeycloakService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.keycloakService.getToken();
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });
    }
    return next.handle(request);
  }
}
```

## Environment Variables for Frontend

### Development
```env
REACT_APP_KEYCLOAK_URL=http://localhost:8080
REACT_APP_KEYCLOAK_REALM=abada-dev
REACT_APP_KEYCLOAK_CLIENT_ID=abada-frontend
REACT_APP_API_URL=http://localhost:5601/abada/api
REACT_APP_REDIRECT_URI=http://localhost:5602
```

### Production
```env
REACT_APP_KEYCLOAK_URL=https://keycloak.example.com
REACT_APP_KEYCLOAK_REALM=abada-prod
REACT_APP_KEYCLOAK_CLIENT_ID=abada-frontend-prod
REACT_APP_API_URL=https://api.example.com/api
REACT_APP_REDIRECT_URI=https://tenda.example.com
```

## Token Validation Endpoint (Optional)

If frontend needs to validate JWT locally:

```javascript
// utils/jwtValidator.ts
import jwtDecode from 'jwt-decode';

export interface DecodedToken {
  sub: string;
  preferred_username: string;
  groups: string[];
  realm_access: {
    roles: string[];
  };
  iss: string;
  exp: number;
  iat: number;
}

export const validateToken = (token: string): boolean => {
  try {
    const decoded = jwtDecode<DecodedToken>(token);
    const now = Date.now() / 1000;
    return decoded.exp > now;
  } catch (error) {
    console.error('Token validation failed:', error);
    return false;
  }
};

export const getTokenClaims = (token: string): DecodedToken => {
  return jwtDecode<DecodedToken>(token);
};

export const getUserGroups = (token: string): string[] => {
  const claims = getTokenClaims(token);
  return claims.groups || [];
};

export const hasRole = (token: string, role: string): boolean => {
  const claims = getTokenClaims(token);
  return claims.realm_access?.roles?.includes(role) || false;
};
```

## Error Handling

```javascript
// auth/errorHandler.ts
export enum AuthError {
  INVALID_TOKEN = 'INVALID_TOKEN',
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  UNAUTHORIZED = 'UNAUTHORIZED',
  FORBIDDEN = 'FORBIDDEN',
  NETWORK_ERROR = 'NETWORK_ERROR',
}

export const handleAuthError = (error: any): AuthError => {
  if (error.response?.status === 401) {
    return AuthError.UNAUTHORIZED;
  }
  if (error.response?.status === 403) {
    return AuthError.FORBIDDEN;
  }
  if (error.message === 'Token expired') {
    return AuthError.TOKEN_EXPIRED;
  }
  if (!error.response) {
    return AuthError.NETWORK_ERROR;
  }
  return AuthError.INVALID_TOKEN;
};

export const retryWithRefresh = async (operation: () => Promise<any>) => {
  try {
    return await operation();
  } catch (error: any) {
    if (error.response?.status === 401) {
      // Try to refresh token
      await keycloak.refreshToken(30);
      return operation();
    }
    throw error;
  }
};
```

## Logout Functionality

```javascript
// auth/logout.ts
export const performLogout = async (keycloak: Keycloak) => {
  try {
    // Clear local storage
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_preferences');
    
    // Call backend logout endpoint (optional)
    await fetch('/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${keycloak.token}`
      }
    });
    
    // Redirect to Keycloak logout
    window.location.href = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/logout?redirect_uri=${window.location.origin}`;
  } catch (error) {
    console.error('Logout failed:', error);
  }
};
```

## Testing Authentication

```typescript
// __tests__/auth.test.ts
describe('Authentication', () => {
  it('should successfully authenticate user', async () => {
    const keycloak = await initKeycloak();
    expect(keycloak.authenticated).toBe(true);
  });

  it('should refresh token before expiry', async () => {
    const token = keycloak.token;
    await keycloak.refreshToken(30);
    expect(keycloak.token).not.toBe(token); // Token updated
  });

  it('should handle logout', async () => {
    await performLogout(keycloak);
    expect(localStorage.getItem('auth_token')).toBeNull();
  });
});
```
