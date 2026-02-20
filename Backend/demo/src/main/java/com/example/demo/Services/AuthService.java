package com.example.demo.Services;

import com.example.demo.Entities.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private KeycloakAdminService keycloakAdminService;


    /**
     * Registro estándar:
     * 1. Valida datos del usuario
     * 2. Verifica que NO exista en DB local
     * 3. Verifica que NO exista en Keycloak (prevención)
     * 4. Crea usuario en Keycloak con el rol indicado
     * 5. Guarda en la DB local
     * 6. Si falla DB → ROLLBACK en Keycloak (elimina usuario)
     */
    public UserEntity registerWithRole(UserEntity user, String rol) {
        validateUser(user);
        
        // ========== VALIDACIÓN PREVENTIVA: Verificar existencia en DB local ==========
        UserEntity existingByUsername = userService.getUserByUsername(user.getUsername());
        if (existingByUsername != null) {
            logger.warn("❌ Intento de registro duplicado: username '{}' ya existe en DB local", user.getUsername());
            String msg = "El usuario ya está registrado en el sistema. ";
            if (user.getUsername().contains("-")) {
                msg += "El RUT " + user.getUsername() + " ya tiene una cuenta.";
            } else {
                msg += "El nombre de usuario '" + user.getUsername() + "' ya está en uso.";
            }
            throw new RuntimeException(msg);
        }
        
        UserEntity existingByEmail = userService.getUserByEmail(user.getEmail());
        if (existingByEmail != null) {
            logger.warn("❌ Intento de registro duplicado: email '{}' ya existe en DB local", user.getEmail());
            throw new RuntimeException("El correo electrónico '" + user.getEmail() + "' ya está registrado.");
        }
        
        // ========== VALIDACIÓN PREVENTIVA: Verificar existencia en Keycloak ==========
        boolean existsInKeycloak = keycloakAdminService.checkUserExistsByUsername(user.getUsername());
        if (existsInKeycloak) {
            logger.error("🚨 INCONSISTENCIA DETECTADA: Usuario '{}' existe en Keycloak pero NO en DB local. Estado huérfano.", user.getUsername());
            String msg = "El usuario existe en el sistema de autenticación pero no en la base de datos. ";
            msg += "Contacta al administrador para resolver esta inconsistencia.";
            throw new RuntimeException(msg);
        }
        
        // ========== CREAR EN KEYCLOAK ==========
        String kcId;
        try {
            logger.info("➡️ Creando usuario '{}' en Keycloak con rol '{}'", user.getUsername(), rol);
            kcId = keycloakAdminService.createKeycloakUser(user, rol);
            logger.info("✅ Usuario '{}' creado en Keycloak con ID: {}", user.getUsername(), kcId);
        } catch (Exception ex) {
            logger.error("❌ Keycloak: error creando usuario '{}': {}", user.getUsername(), ex.getMessage());
            throw new RuntimeException("Error creando usuario en sistema de autenticación: " + ex.getMessage());
        }

        user.setKeycloakId(kcId);
        user.setRol(rol);

        // Ensure new users have default active state in local DB
        if (rol != null) {
            user.setStateClient("ACTIVO");
        }

        // ========== GUARDAR EN DB LOCAL (CON ROLLBACK SI FALLA) ==========
        try {
            logger.info("➡️ Guardando usuario '{}' en DB local", user.getUsername());
            UserEntity savedUser = userService.saveUser(user);
            logger.info("✅ Usuario '{}' registrado exitosamente en DB local", user.getUsername());
            return savedUser;
            
        } catch (Exception ex) {
            logger.error("❌ DB ERROR: Falló guardado en DB local para usuario '{}': {}", user.getUsername(), ex.getMessage());
            logger.warn("🔄 INICIANDO ROLLBACK: Eliminando usuario {} de Keycloak", kcId);

            // ========== ROLLBACK: ELIMINAR DE KEYCLOAK ==========
            try {
                keycloakAdminService.deleteKeycloakUser(kcId);
                logger.info("✅ Rollback completado: Usuario eliminado de Keycloak");
            } catch (Exception delEx) {
                logger.error("❌❌❌ ROLLBACK FALLÓ: Usuario '{}' (ID: {}) queda HUÉRFANO en Keycloak", user.getUsername(), kcId);
                logger.error("❌ Error del rollback: {}", delEx.getMessage());
                // Este es un caso CRÍTICO que requiere intervención manual
                throw new RuntimeException(
                    "ERROR CRÍTICO: Usuario creado en Keycloak pero no en DB. " +
                    "Contacta al administrador (Usuario huérfano: " + kcId + ")"
                );
            }

            // Rollback exitoso, propagar el error original
            throw new RuntimeException("No se pudo guardar el usuario en la base de datos: " + ex.getMessage());
        }
    }

    private void validateUser(UserEntity user) {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();

        if (user.getName() == null || user.getName().isBlank()) {
            errors.add("Debe ingresar su nombre.");
        }

        if (user.getLastName() == null || user.getLastName().isBlank()) {
            errors.add("Debe ingresar su apellido.");
        }

        if (user.getRut() == null || user.getRut().isBlank()) {
            errors.add("Debe ingresar su Rut.");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            errors.add("El Nombre de usuario no puede ser nulo.");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            errors.add("El Email no puede ser nulo.");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            errors.add("La contraseña no puede ser nula.");
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join(" ", errors));
        }
    }


    // Registro especial para clientes (forzado desde frontend)
    public UserEntity registerClient(UserEntity user) {
        return registerWithRole(user, "CLIENT");
    }

    // Registro para empleados (creado por Admin)
    public UserEntity registerEmployee(UserEntity user) {
        return registerWithRole(user, "EMPLOYEE");
    }

    // Registro para Admins (solo un SuperAdmin puede hacer esto)
    public UserEntity registerAdmin(UserEntity user) {
        return registerWithRole(user, "ADMIN");
    }


    /**
     * Login: intenta autenticar en Keycloak usando email o username.
     * Devuelve el token + datos del usuario local.
     */
    public Map<String, Object> login(String identifier, String password) {

        // 1. Buscar usuario local primero: probar email/username directos y también en minúsculas
        UserEntity localUser = null;
        if (identifier != null) {
            localUser = userService.getUserByEmail(identifier);
            if (localUser == null) {
                localUser = userService.getUserByEmail(identifier.toLowerCase());
            }
            if (localUser == null) {
                localUser = userService.getUserByUsername(identifier);
            }
            if (localUser == null) {
                localUser = userService.getUserByUsername(identifier.toLowerCase());
            }
        }

        if (localUser == null) {
            // Este mensaje será detectado por el Controller para retornar 404
            throw new RuntimeException("Usuario no registrado");
        }

        // 2. Validar credenciales en Keycloak
        Map token = null;

        // Intentar loguear preferentemente con el email o username que encontró el sistema
        // Primero intentamos con lo que el usuario escribió
        try {
            token = keycloakAdminService.requestPasswordGrant(identifier, password);
        } catch (Exception ex) {
            // Si falla, intentamos con las propiedades del usuario encontrado (email o username)
            // Esto ayuda si Keycloak espera email pero el usuario puso username, o viceversa,
            // o si hay temas de case-sensitivity.
            try {
                if (localUser.getEmail() != null)
                   token = keycloakAdminService.requestPasswordGrant(localUser.getEmail(), password);
            } catch (Exception ignored) {}

            if (token == null) {
                try {
                    if (localUser.getUsername() != null)
                        token = keycloakAdminService.requestPasswordGrant(localUser.getUsername(), password);
                } catch (Exception ignored) {}
            }
        }

        if (token == null) {
            throw new RuntimeException("Credenciales inválidas.");
        }

        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", localUser.getId(),
                        "username", localUser.getUsername(),
                        "email", localUser.getEmail(),
                        "name", localUser.getName() != null ? localUser.getName() : "",
                        "rol", localUser.getRol()
                )
        );
    }

    /**
     * Refresh: usa el refresh_token para obtener un nuevo access_token.
     */
    public Map<String, Object> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token requerido");
        }

        try {
            Map token = keycloakAdminService.refreshToken(refreshToken);
            return Map.of("token", token);
        } catch (Exception ex) {
            logger.error("Error refrescando token: {}", ex.getMessage());
            throw new RuntimeException("No se pudo refrescar el token");
        }
    }
}
