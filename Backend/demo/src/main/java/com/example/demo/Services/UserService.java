package com.example.demo.Services;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.UserRepository;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private KeycloakAdminService keycloakAdminService;
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
    public ArrayList<UserEntity> getUsers() {
        return (ArrayList<UserEntity>) userRepository.findAll();
    }

    public List<UserEntity> getUsersByRol(String rol) {
        return userRepository.findByRol(rol);
    }

    public List<UserEntity> getAllEmployees() {
        List<UserEntity> employees = getUsersByRol("EMPLOYEE");
        List<UserEntity> admins = getUsersByRol("ADMIN");

        List<UserEntity> users = new ArrayList<>();
        users.addAll(employees);
        users.addAll(admins);

        return users;
    }

    public List<UserEntity> getAllClients(){
        return getUsersByRol("CLIENT");
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

    public List<UserEntity> filterClient(String state){
        if(state==null || state.isBlank()){
            return getAllClients();
        }
        else{
            List<UserEntity> clients = getAllClients();
            return clients.stream().filter(userEntity -> userEntity.getStateClient().equals(state)).toList();
        }
    }

    public List<UserEntity> filterEmployee(String role){
        if(role==null || role.isBlank()){
            return getAllEmployees();
        }
        else{
            List<UserEntity> employees = getAllEmployees();
            return employees.stream().filter(userEntity -> userEntity.getRol().equals(role)).toList();
        }
    }

    public boolean deleteUser(Long id) {
        try {
            // Obtener usuario para leer su keycloakId
            UserEntity user = userRepository.findById(id)
                .orElse(null);

            // Primero borrar en Keycloak si tenemos el ID
            if (user != null && user.getKeycloakId() != null && !user.getKeycloakId().isBlank()) {
                try {
                    keycloakAdminService.deleteKeycloakUser(user.getKeycloakId());
                } catch (Exception ex) {
                    // Continuar incluso si falla el borrado en Keycloak
                }
            }

            // Luego borrar en la BD local
            userRepository.deleteById(id);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void isAdmin(UserEntity user) {
        if (!user.getRol().equals("ADMIN") && !user.getRol().equals("SUPERADMIN")) {
            throw new RuntimeException("Acceso denegado. Se requiere rol ADMIN");
        }
    }

    public void validateAdminOrEmployee(UserEntity user) {
        if (!user.getRol().equals("ADMIN") &&
                !user.getRol().equals("EMPLOYEE") && !user.getRol().equals("SUPERADMIN")) {
            throw new RuntimeException("No cuenta con los permisos suficientes.");
        }
    }

    public boolean canDoAnotherLoan(UserEntity user) {
        if (user.getLoans()<5 && user.getLoans()>=0) {
            return true;
        }
        return false;
    }

}
