import Keycloak, { type KeycloakProfile, type KeycloakTokenParsed } from "keycloak-js";

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
  const authenticated = await keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
  });

  return authenticated;
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
  profile?: KeycloakProfile | null
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
