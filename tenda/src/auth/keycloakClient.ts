import Keycloak, {
  type KeycloakProfile,
  type KeycloakTokenParsed,
} from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

export type KeycloakUser = {
  id: string;
  username: string;
  email?: string;
};

export async function initKeycloak() {
  try {
    const authenticated = await keycloak.init({
      // Keep init passive: avoid auto check/login iframe flow that often fails
      // on localhost with strict browser 3rd-party cookie policies.
      pkceMethod: "S256",
      checkLoginIframe: false,
      enableLogging: import.meta.env.DEV,
    });

    return authenticated;
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    if (message.includes("3rd party check iframe")) {
      // Degrade gracefully: user can still authenticate via explicit login button.
      return false;
    }
    throw error;
  }
}

export async function refreshToken(minValidity = 30) {
  try {
    await keycloak.updateToken(minValidity);
  } catch (error) {
    console.error("Failed to refresh Keycloak token", error);
    await keycloak.logout({ redirectUri: window.location.origin });
  }
}

export function getUserFromToken(
  tokenParsed?: KeycloakTokenParsed,
  profile?: KeycloakProfile | null,
): KeycloakUser | null {
  if (!tokenParsed) return null;
  const username =
    (tokenParsed as any).preferred_username ||
    tokenParsed.name ||
    tokenParsed.given_name ||
    "user";

  return {
    id: tokenParsed.sub || "",
    username,
    email: tokenParsed.email || profile?.email,
  };
}

export { keycloak };
