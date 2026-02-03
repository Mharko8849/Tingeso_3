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
        Map token = null;

        try {
            token = keycloakAdminService.requestPasswordGrant(identifier, password);
        } catch (Exception ex) {
            logger.warn("Login directo falló, revisando alternativas...");

            // Intento por username: probar directas y también comparando en minúsculas
            UserEntity byUser = userService.getUserByUsername(identifier);
            if (byUser == null && identifier != null) {
                try {
                    byUser = userService.getUserByUsername(identifier.toLowerCase());
                } catch (Exception ignored) {}
            }

            if (byUser != null) {
                try {
                    token = keycloakAdminService.requestPasswordGrant(byUser.getEmail(), password);
                } catch (Exception ignored) {}
            }

            // Si ya tenemos token, no seguimos buscando
            if (token == null) {
                // Intento por email: probar directas y también comparando en minúsculas
                UserEntity byEmail = userService.getUserByEmail(identifier);
                if (byEmail == null && identifier != null) {
                    try {
                        byEmail = userService.getUserByEmail(identifier.toLowerCase());
                    } catch (Exception ignored) {}
                }

                if (byEmail != null) {
                    try {
                        token = keycloakAdminService.requestPasswordGrant(byEmail.getUsername(), password);
                    } catch (Exception ignored) {}
                }
            }

            if (token == null) {
                throw new RuntimeException("Credenciales inválidas.");
            }
        }

        // Buscar usuario local: probar email/username directos y también en minúsculas
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

        if (localUser == null)
            throw new RuntimeException("Usuario no encontrado.");

        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", localUser.getId(),
                        "username", localUser.getUsername(),
                        "email", localUser.getEmail(),
                        "rol", localUser.getRol()
                )
        );
    }
}
