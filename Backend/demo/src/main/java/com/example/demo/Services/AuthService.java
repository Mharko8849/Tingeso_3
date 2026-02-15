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
     * Crea usuario en Keycloak con el rol indicado,
     * luego lo guarda en la DB local.
     * Si falla en la DB → rollback en Keycloak.
     */
    public UserEntity registerWithRole(UserEntity user, String rol) {
        validateUser(user);
        String kcId;

        try {
            // Ahora usa createKeycloakUser(user, rol)
            kcId = keycloakAdminService.createKeycloakUser(user, rol);
        } catch (Exception ex) {
            logger.error("Keycloak: error creando usuario: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error creando usuario en sistema de autenticación.");
        }

        user.setKeycloakId(kcId);
        user.setRol(rol);

        // Ensure new users have default active state in local DB
        if (rol != null) {
            // Use the existing field `stateClient` for all roles
            user.setStateClient("ACTIVO");
        }

        try {
            return userService.saveUser(user);
        } catch (Exception ex) {

            logger.error("DB error: intentando borrar usuario Keycloak {}: {}", kcId, ex.getMessage());

            try {
                keycloakAdminService.deleteKeycloakUser(kcId);
            } catch (Exception delEx) {
                logger.error("Rollback Keycloak falló: {}", delEx.getMessage());
            }

            throw new RuntimeException(ex.getMessage());
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
}
