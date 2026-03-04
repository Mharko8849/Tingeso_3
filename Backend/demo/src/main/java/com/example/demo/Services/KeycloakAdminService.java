package com.example.demo.Services;

import com.example.demo.Entities.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);
    @SuppressWarnings("java:S1075")
    private static final String USERS_PATH = "/users/";
    private static final String CLIENT_SECRET_PARAM = "&client_secret=";

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate rest;

    @Autowired
    public KeycloakAdminService() {
        this.rest = new RestTemplate();
    }

    public KeycloakAdminService(RestTemplate rest) {
        this.rest = rest;
    }

    /** URL del endpoint donde se pide el token admin */
    private String tokenEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
    }

    /** Crea URLs para los endpoints de administración */
    private String adminEndpoint(String path) {
        return String.format("%s/admin/realms/%s%s", keycloakUrl, realm, path);
    }

    /** Pide un token de admin a Keycloak usando client_credentials */
    @SuppressWarnings("unchecked")
    public String obtainAdminAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=client_credentials&client_id=" + clientId +
                        CLIENT_SECRET_PARAM + clientSecret;

        try {
            ResponseEntity<Map<String, Object>> resp =
                    rest.exchange(tokenEndpoint(), HttpMethod.POST, new HttpEntity<>(body, headers),
                            (Class<Map<String, Object>>) (Class<?>) Map.class);

            Map<String, Object> map = resp.getBody();
            if (map != null && map.containsKey("access_token")) {
                return map.get("access_token").toString();
            }

            throw new IllegalStateException("Keycloak no entregó access_token");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo obtener token admin", ex);
        }
    }

    public String createKeycloakUser(UserEntity user, String roleName) {
        String resolvedRole = (roleName == null || roleName.isBlank()) ? "CLIENT" : roleName;

        String adminToken = obtainAdminAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> roleMap = findRoleByName(headers, resolvedRole);
        Map<String, Object> payload = buildUserPayload(user);
        ResponseEntity<Void> createResp = postNewUser(headers, payload, user.getUsername());
        String userId = extractUserId(createResp, headers, user.getUsername());
        assignRole(headers, userId, resolvedRole, roleMap);

        return userId;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findRoleByName(HttpHeaders headers, String roleName) {
        try {
            ResponseEntity<List<Map<String, Object>>> rolesResp = rest.exchange(
                    adminEndpoint("/roles"), HttpMethod.GET, new HttpEntity<>(headers),
                    (Class<List<Map<String, Object>>>) (Class<?>) List.class);

            List<Map<String, Object>> allRoles = rolesResp.getBody();
            if (allRoles != null) {
                for (Map<String, Object> role : allRoles) {
                    if (roleName.equals(role.get("name"))) {
                        return role;
                    }
                }
            }
            throw new IllegalStateException("El rol '" + roleName + "' no está configurado en el sistema. Contacta al administrador.");
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo verificar los roles del sistema. Intenta nuevamente.", ex);
        }
    }

    private Map<String, Object> buildUserPayload(UserEntity user) {
        Map<String, Object> password = new HashMap<>();
        password.put("type", "password");
        password.put("value", user.getPassword());
        password.put("temporary", false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());
        payload.put("firstName", user.getName());
        payload.put("lastName", user.getLastName());
        payload.put("emailVerified", true);
        payload.put("enabled", true);
        payload.put("credentials", Collections.singletonList(password));
        return payload;
    }

    private ResponseEntity<Void> postNewUser(HttpHeaders headers, Map<String, Object> payload, String username) {
        try {
            return rest.postForEntity(adminEndpoint("/users"), new HttpEntity<>(payload, headers), Void.class);
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
            String errorMsg = "El usuario ya está registrado. ";
            errorMsg += username.contains("-")
                    ? "El RUT " + username + " ya tiene una cuenta."
                    : "El nombre de usuario '" + username + "' ya está en uso.";
            throw new IllegalStateException(errorMsg);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            String body = ex.getResponseBodyAsString();
            String detail;
            if (body.contains("email")) {
                detail = "El correo electrónico ya está registrado.";
            } else if (body.contains("username")) {
                detail = "El nombre de usuario ya está en uso.";
            } else {
                detail = "Verifica que los datos sean correctos.";
            }
            throw new IllegalStateException("No se pudo crear la cuenta: " + detail);
        } catch (Exception ex) {
            throw new IllegalStateException("Error de conexión con el sistema de autenticación. Intenta nuevamente.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractUserId(ResponseEntity<Void> createResp, HttpHeaders headers, String username) {
        try {
            java.net.URI location = createResp.getHeaders().getLocation();
            if (location != null) {
                String path = location.getPath();
                return path.substring(path.lastIndexOf('/') + 1);
            }
            String encoded = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            ResponseEntity<List<Map<String, Object>>> searchResp = rest.exchange(
                    adminEndpoint("/users?username=") + encoded, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    (Class<List<Map<String, Object>>>) (Class<?>) List.class);
            List<Map<String, Object>> body = searchResp.getBody();
            if (body == null || body.isEmpty()) {
                throw new IllegalStateException("No se encontró el usuario creado en Keycloak");
            }
            Map<String, Object> userData = body.get(0);
            return userData.get("id").toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Error crítico: Usuario creado pero ID no disponible. Contacta al administrador.", ex);
        }
    }

    private void assignRole(HttpHeaders headers, String userId, String roleName, Map<String, Object> roleMap) {
        try {
            rest.postForEntity(
                adminEndpoint(USERS_PATH + userId + "/role-mappings/realm"),
                new HttpEntity<>(Collections.singletonList(roleMap), headers),
                Void.class);
        } catch (Exception ex) {
            rollbackUser(headers, userId);
            throw new IllegalStateException("No se pudo asignar el rol '" + roleName + "' al usuario. Usuario no creado.", ex);
        }
    }

    private void rollbackUser(HttpHeaders headers, String userId) {
        try {
            rest.exchange(adminEndpoint(USERS_PATH + userId), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception delEx) {
            logger.error("ROLLBACK FALLÓ: Usuario {} queda huérfano en Keycloak sin rol: {}", userId, delEx.getMessage());
        }
    }


    /**
     * Verifica si un usuario existe en Keycloak por username
     * @return true si existe, false si no existe
     */
    public boolean checkUserExistsByUsername(String username) {
        try {
            String adminToken = obtainAdminAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            // Buscar usuario por username exacto
            String url = adminEndpoint("/users?username=" + username + "&exact=true");
            
            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response = (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                java.util.List.class
            );
            
            List<?> users = response.getBody();
            boolean exists = users != null && !users.isEmpty();
            
            if (exists) {
                logger.warn("Usuario '{}' ya existe en Keycloak", username);
            }
            
            return exists;
            
        } catch (Exception ex) {
            logger.error("Error verificando existencia de usuario en Keycloak: {}", ex.getMessage());
            // En caso de error al verificar, asumimos que NO existe para intentar crearlo
            // El error real se capturará en createKeycloakUser
            return false;
        }
    }

    /** Elimina un usuario de Keycloak (se usa cuando falló la BD local) */
    @Transactional
    public void deleteKeycloakUser(String userId) {
        String adminToken = obtainAdminAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        try {
            rest.exchange(adminEndpoint(USERS_PATH + userId), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Rollback falló: Usuario queda inconsistente entre sistemas", ex);
        }
    }

    /** Pide un token usando username + password del usuario */
    @SuppressWarnings("unchecked")
    public Map<String, Object> requestPasswordGrant(String username, String password) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=password&client_id=" + clientId +
                        CLIENT_SECRET_PARAM + clientSecret +
                        "&username=" + username +
                        "&password=" + password;

        ResponseEntity<Map<String, Object>> resp =
                rest.exchange(tokenEndpoint(), HttpMethod.POST, new HttpEntity<>(body, headers),
                        (Class<Map<String, Object>>) (Class<?>) Map.class);

        return resp.getBody();
    }

    /** Refresca un token usando refresh_token */
    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=refresh_token&client_id=" + clientId +
                        CLIENT_SECRET_PARAM + clientSecret +
                        "&refresh_token=" + refreshToken;

        try {
            ResponseEntity<Map<String, Object>> resp =
                    rest.exchange(tokenEndpoint(), HttpMethod.POST, new HttpEntity<>(body, headers),
                            (Class<Map<String, Object>>) (Class<?>) Map.class);
            return resp.getBody();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo refrescar el token", ex);
        }
    }
}
