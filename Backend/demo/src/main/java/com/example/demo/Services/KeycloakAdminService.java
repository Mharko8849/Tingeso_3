package com.example.demo.Services;

import com.example.demo.Entities.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate rest;

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
    public String obtainAdminAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=client_credentials&client_id=" + clientId +
                        "&client_secret=" + clientSecret;

        try {
            ResponseEntity<Map> resp =
                    rest.exchange(tokenEndpoint(), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            Map<?, ?> map = resp.getBody();
            if (map != null && map.containsKey("access_token")) {
                return map.get("access_token").toString();
            }

            throw new RuntimeException("Keycloak no entregó access_token");
        } catch (Exception ex) {
            logger.error("Error pidiendo token admin a Keycloak: {}", ex.getMessage());
            throw new RuntimeException("No se pudo obtener token admin", ex);
        }
    }

    public String createKeycloakUser(UserEntity user, String roleName) {

        // Si no viene rol, por defecto será CLIENT
        if (roleName == null || roleName.isBlank()) {
            roleName = "CLIENT";
        }

        String adminToken;
        try {
            adminToken = obtainAdminAccessToken();
        } catch (Exception ex) {
            logger.error("No se pudo obtener token admin: {}", ex.getMessage());
            throw new RuntimeException("No se pudo obtener token admin", ex);
        }

        // Headers base
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Datos del usuario
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", user.getUsername());
        payload.put("email", user.getEmail());
        payload.put("firstName", user.getName());
        payload.put("lastName", user.getLastName());
        payload.put("emailVerified", true);
        payload.put("enabled", true);

        // Contraseña
        Map<String, Object> password = new HashMap<>();
        password.put("type", "password");
        password.put("value", user.getPassword());
        password.put("temporary", false);

        payload.put("credentials", Collections.singletonList(password));

        // Crear usuario base
        ResponseEntity<Void> createResp;

        try {
            createResp = rest.postForEntity(
                    adminEndpoint("/users"),
                    new HttpEntity<>(payload, headers),
                    Void.class
            );
        } catch (Exception ex) {
            logger.error("Error creando usuario en Keycloak: {}", ex.getMessage());
            throw new RuntimeException("Keycloak no pudo crear el usuario", ex);
        }

        // Obtener ID del usuario creado
        String userId = null;

        if (createResp.getHeaders().getLocation() != null) {
            String path = createResp.getHeaders().getLocation().getPath();
            userId = path.substring(path.lastIndexOf('/') + 1);
        }

        if (userId == null) {
            // Buscar por username si no aparece por header
            try {
                String encoded = java.net.URLEncoder.encode(user.getUsername(), java.nio.charset.StandardCharsets.UTF_8);
                ResponseEntity<List> searchResp =
                        rest.exchange(adminEndpoint("/users?username=") + encoded, HttpMethod.GET, new HttpEntity<>(headers), List.class);

                Map userData = (Map) searchResp.getBody().get(0);
                userId = userData.get("id").toString();
            } catch (Exception ex) {
                logger.error("No se encontró el usuario recién creado");
                throw new RuntimeException("No se encontró usuario creado", ex);
            }
        }

        // Obtener representación del rol solicitado
        Map roleMap;
        try {
            ResponseEntity<Map> roleResp = rest.exchange(
                    adminEndpoint("/roles/" + roleName),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            roleMap = roleResp.getBody();
            if (roleMap == null) {
                throw new RuntimeException("El rol no existe en Keycloak: " + roleName);
            }

        } catch (Exception ex) {
            logger.error("No se pudo obtener el rol {} en Keycloak", roleName);
            throw new RuntimeException("No se pudo obtener el rol " + roleName, ex);
        }

        // Asignar rol al usuario
        try {
            HttpEntity<List> assignReq = new HttpEntity<>(Collections.singletonList(roleMap), headers);
            rest.postForEntity(adminEndpoint("/users/" + userId + "/role-mappings/realm"), assignReq, Void.class);
        } catch (Exception ex) {
            logger.error("Error asignando rol {} al usuario {}", roleName, userId);
            throw new RuntimeException("No se pudo asignar rol " + roleName, ex);
        }

        return userId;
    }

    /** Elimina un usuario de Keycloak (se usa cuando falló la BD local) */
    @Transactional
    public void deleteKeycloakUser(String userId) {
        String adminToken = obtainAdminAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        try {
            rest.exchange(adminEndpoint("/users/" + userId), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception ex) {
            logger.error("Error eliminando usuario {}: {}", userId, ex.getMessage());
        }
    }

    /** Pide un token usando username + password del usuario */
    public Map requestPasswordGrant(String username, String password) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=password&client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&username=" + username +
                        "&password=" + password;

        ResponseEntity<Map> resp =
                rest.exchange(tokenEndpoint(), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        return resp.getBody();
    }
}
