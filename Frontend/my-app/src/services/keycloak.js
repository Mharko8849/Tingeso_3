import Keycloak from "keycloak-js";

const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || "http://192.168.49.2:31000";

const keycloak = new Keycloak({
  url: keycloakUrl,
  realm: "ToolRent",
  clientId: "toolrent-frontend",
});

export default keycloak;
