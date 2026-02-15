import Keycloak from "keycloak-js";

const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || "https://toolrent.192.168.39.45.nip.io";

const keycloak = new Keycloak({
  url: keycloakUrl,
  realm: "ToolRent",
  clientId: "toolrent-frontend",
});

export default keycloak;
