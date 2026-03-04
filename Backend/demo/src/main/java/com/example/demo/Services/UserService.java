package com.example.demo.Services;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.UserRepository;
import org.apache.catalina.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    public UserEntity saveUser(UserEntity user) {
        return userRepository.save(user);
    }

    public UserEntity findUserById(Long idUser) {
    return userRepository.findById(idUser)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró al usuario."));
    }

    /**
     * Funcion que trae a todos los usuarios del sistema
     */
    @Transactional(readOnly = true)
    public ArrayList<UserEntity> getUsers() {
        return (ArrayList<UserEntity>) userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getUsersByRol(String rol) {
        return userRepository.findByRol(rol);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllEmployees() {
        List<UserEntity> users = new ArrayList<>();
        users.addAll(userRepository.findByRol(ROLE_EMPLOYEE));
        users.addAll(userRepository.findByRol(ROLE_ADMIN));
        return users;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllClients(){
        return userRepository.findByRol(ROLE_CLIENT);
    }

    public UserEntity updateUser(UserEntity user) {
        return userRepository.save(user);
    }

    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    public UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserEntity getUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Obtiene el usuario correspondiente al token JWT.
     * Valida el token y busca el usuario por su Keycloak ID (sub).
     */
    public UserEntity getUserFromJwt(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autorizado");
        }
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido: sub no presente");
        }
        UserEntity user = getUserByKeycloakId(sub);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> filterClient(String state){
        if(state==null || state.isBlank()){
            return userRepository.findByRol(ROLE_CLIENT);
        }
        else{
            return userRepository.findByRol(ROLE_CLIENT).stream()
                .filter(userEntity -> userEntity.getStateClient().equals(state)).toList();
        }
    }

    @Transactional(readOnly = true)
    public List<UserEntity> filterEmployee(String role){
        List<UserEntity> allEmployees = new ArrayList<>();
        allEmployees.addAll(userRepository.findByRol(ROLE_EMPLOYEE));
        allEmployees.addAll(userRepository.findByRol(ROLE_ADMIN));
        if(role==null || role.isBlank()){
            return allEmployees;
        }
        else{
            return allEmployees.stream()
                .filter(userEntity -> userEntity.getRol().equals(role)).toList();
        }
    }

    public boolean deleteUser(Long id) {
        try {
            // Obtener usuario para leer su keycloakId
            UserEntity user = userRepository.findById(id)
                .orElse(null);

            // Primero borrar en Keycloak si tenemos el ID
            if (user != null && user.getKeycloakId() != null && !user.getKeycloakId().isBlank()) {
                deleteKeycloakUserSafely(user.getKeycloakId());
            }

            // Luego borrar en la BD local
            userRepository.deleteById(id);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void deleteKeycloakUserSafely(String keycloakId) {
        try {
            keycloakAdminService.deleteKeycloakUser(keycloakId);
        } catch (Exception ignored) {
            // Continuar incluso si falla el borrado en Keycloak
        }
    }

    public void isAdmin(UserEntity user) {
        if (!user.getRol().equals(ROLE_ADMIN) && !user.getRol().equals("SUPERADMIN")) {
            throw new IllegalStateException("Acceso denegado. Se requiere rol ADMIN");
        }
    }

    public void validateAdminOrEmployee(UserEntity user) {
        if (!user.getRol().equals(ROLE_ADMIN) &&
                !user.getRol().equals(ROLE_EMPLOYEE) && !user.getRol().equals("SUPERADMIN")) {
            throw new IllegalStateException("No cuenta con los permisos suficientes.");
        }
    }

    public boolean canDoAnotherLoan(UserEntity user) {
        if (user.getLoans()<5 && user.getLoans()>=0) {
            return true;
        }
        return false;
    }

}
