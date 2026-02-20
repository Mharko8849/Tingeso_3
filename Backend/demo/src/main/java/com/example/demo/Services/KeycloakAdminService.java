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

        // ===== PASO 1: VERIFICAR QUE EL ROL EXISTE (ANTES DE CREAR USUARIO) =====
        Map roleMap;
        try {
            logger.info("🔍 Buscando rol '{}' en Keycloak...", roleName);
            
            // WORKAROUND: En lugar de GET /roles/{roleName} que falla con "unknown_error",
            // listamos TODOS los roles y buscamos el que necesitamos
            ResponseEntity<List> rolesResp = rest.exchange(
                    adminEndpoint("/roles"),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    List.class
            );

            List<Map> allRoles = rolesResp.getBody();
            roleMap = null;
            
            if (allRoles != null) {
                // Buscar el rol por nombre en la lista
                for (Map role : allRoles) {
                    if (roleName.equals(role.get("name"))) {
                        roleMap = role;
                        break;
                    }
                }
            }
            
            if (roleMap == null) {
                logger.error("❌ El rol '{}' NO EXISTE en Keycloak", roleName);
                throw new RuntimeException("El rol '" + roleName + "' no está configurado en el sistema. Contacta al administrador.");
            }
            
            logger.info("✅ Rol '{}' encontrado en Keycloak (ID: {})", roleName, roleMap.get("id"));

        } catch (RuntimeException ex) {
            // Re-lanzar excepciones de lógica de negocio
            throw ex;
        } catch (Exception ex) {
            logger.error("❌ Error buscando roles en Keycloak: {}", ex.getMessage());
            throw new RuntimeException("No se pudo verificar los roles del sistema. Intenta nuevamente.");
        }

        // ===== PASO 2: PREPARAR DATOS DEL USUARIO =====
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

        // ===== PASO 3: CREAR USUARIO BASE =====
        ResponseEntity<Void> createResp;
        String userId = null;

        try {
            createResp = rest.postForEntity(
                    adminEndpoint("/users"),
                    new HttpEntity<>(payload, headers),
                    Void.class
            );
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
            // Error 409: Usuario ya existe
            logger.error("Usuario ya existe en Keycloak: {}", user.getUsername());
            
            String errorMsg = "El usuario ya está registrado. ";
            if (user.getUsername().contains("-")) {
                errorMsg += "El RUT " + user.getUsername() + " ya tiene una cuenta.";
            } else {
                errorMsg += "El nombre de usuario '" + user.getUsername() + "' ya está en uso.";
            }
            
            throw new RuntimeException(errorMsg);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            // Otros errores 4xx
            logger.error("Error del cliente al crear usuario en Keycloak ({}): {}", 
                ex.getStatusCode(), ex.getMessage());
            
            String errorMsg = "No se pudo crear la cuenta: ";
            if (ex.getResponseBodyAsString().contains("email")) {
                errorMsg += "El correo electrónico ya está registrado.";
            } else if (ex.getResponseBodyAsString().contains("username")) {
                errorMsg += "El nombre de usuario ya está en uso.";
            } else {
                errorMsg += "Verifica que los datos sean correctos.";
            }
            
            throw new RuntimeException(errorMsg);
        } catch (Exception ex) {
            // Otros errores (red, timeout, etc.)
            logger.error("Error inesperado creando usuario en Keycloak: {}", ex.getMessage());
            throw new RuntimeException("Error de conexión con el sistema de autenticación. Intenta nuevamente.");
        }

        // ===== PASO 4: OBTENER ID DEL USUARIO CREADO =====
        try {
            if (createResp.getHeaders().getLocation() != null) {
                String path = createResp.getHeaders().getLocation().getPath();
                userId = path.substring(path.lastIndexOf('/') + 1);
                logger.info("✅ Usuario creado en Keycloak con ID: {}", userId);
            }

            if (userId == null) {
                // Buscar por username si no aparece por header
                logger.warn("⚠️ Location header no disponible, buscando usuario por username...");
                String encoded = java.net.URLEncoder.encode(user.getUsername(), java.nio.charset.StandardCharsets.UTF_8);
                ResponseEntity<List> searchResp =
                        rest.exchange(adminEndpoint("/users?username=") + encoded, HttpMethod.GET, new HttpEntity<>(headers), List.class);

                Map userData = (Map) searchResp.getBody().get(0);
                userId = userData.get("id").toString();
                logger.info("✅ Usuario encontrado con ID: {}", userId);
            }
        } catch (Exception ex) {
            logger.error("❌ No se pudo obtener ID del usuario recién creado");
            // Usuario creado pero no podemos obtener su ID → No podemos continuar ni hacer rollback
            throw new RuntimeException("Error crítico: Usuario creado pero ID no disponible. Contacta al administrador.");
        }

        // ===== PASO 5: ASIGNAR ROL AL USUARIO (CON ROLLBACK SI FALLA) =====
        try {
            logger.info("➡️ Asignando rol '{}' al usuario {}...", roleName, userId);
            
            HttpEntity<List> assignReq = new HttpEntity<>(Collections.singletonList(roleMap), headers);
            rest.postForEntity(
                adminEndpoint("/users/" + userId + "/role-mappings/realm"), 
                assignReq, 
                Void.class
            );
            
            logger.info("✅ Rol '{}' asignado correctamente al usuario {}", roleName, userId);
            
        } catch (Exception ex) {
            logger.error("❌ Error asignando rol '{}' al usuario {}: {}", roleName, userId, ex.getMessage());
            logger.warn("🔄 ROLLBACK: Eliminando usuario {} de Keycloak...", userId);
            
            // ROLLBACK INTERNO: Eliminar usuario de Keycloak
            try {
                rest.exchange(
                    adminEndpoint("/users/" + userId), 
                    HttpMethod.DELETE, 
                    new HttpEntity<>(headers), 
                    Void.class
                );
                logger.info("✅ Rollback completado: Usuario {} eliminado de Keycloak", userId);
            } catch (Exception delEx) {
                logger.error("❌❌❌ ROLLBACK FALLÓ: Usuario {} queda huérfano en Keycloak sin rol", userId);
                logger.error("Error del rollback: {}", delEx.getMessage());
            }
            
            throw new RuntimeException("No se pudo asignar el rol '" + roleName + "' al usuario. Usuario no creado.");
        }

        return userId;
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
            
            ResponseEntity<java.util.List> response = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                java.util.List.class
            );
            
            java.util.List users = response.getBody();
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
            rest.exchange(adminEndpoint("/users/" + userId), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            logger.info("✅ Rollback exitoso: Usuario {} eliminado de Keycloak", userId);
        } catch (Exception ex) {
            logger.error("❌ Error en rollback - No se pudo eliminar usuario {} de Keycloak: {}", userId, ex.getMessage());
            // Este es un error crítico que debería monitorearse
            throw new RuntimeException("Rollback falló: Usuario queda inconsistente entre sistemas");
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

    /** Refresca un token usando refresh_token */
    public Map refreshToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=refresh_token&client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&refresh_token=" + refreshToken;

        try {
            ResponseEntity<Map> resp =
                    rest.exchange(tokenEndpoint(), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            return resp.getBody();
        } catch (Exception ex) {
            logger.error("Error refrescando token: {}", ex.getMessage());
            throw new RuntimeException("No se pudo refrescar el token", ex);
        }
    }
}
