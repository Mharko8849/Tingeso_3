package com.example.demo.Controllers;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.UserService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping({"/api/user", "/user"})
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserService userService;

    /*
     * GET endpoints
     */

    // Listar todos los usuarios (solo ADMIN o SUPERADMIN)
    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    // Buscar usuario por ID (solo ADMIN o SUPERADMIN)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    // Listar todos los empleados (EMPLOYEE y ADMIN)
    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<UserEntity>> getAllEmployees() {
        return ResponseEntity.ok(userService.getAllEmployees());
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UserEntity>> getAllClients() {
        return ResponseEntity.ok(userService.getAllClients());
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UserEntity>> filterClients(@RequestParam(required = false) String state) {
        List<UserEntity> clients = userService.filterClient(state);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/filter/employee")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UserEntity>> filterEmployees(@RequestParam(required = false) String state) {
        List<UserEntity> employees = userService.filterEmployee(state);
        return ResponseEntity.ok(employees);
    }

    /*
     * PUT endpoints
     */

    // Actualizar datos de usuario
    @PutMapping("/")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE', 'ADMIN','SUPERADMIN')")
    public ResponseEntity<UserEntity> updateUser(@RequestBody UserEntity user) {
        return ResponseEntity.ok(userService.updateUser(user));
    }

    /*
     * DELETE endpoints
     */

    // Eliminar usuario por ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        UserEntity requester = userService.getUserFromJwt(jwt);
        UserEntity target = userService.findUserById(id);
        
        // Jerarquía de eliminación:
        // - SUPERADMIN puede eliminar ADMIN o EMPLOYEE
        // - ADMIN solo puede eliminar EMPLOYEE
        if (target != null) {
            if (("ADMIN".equals(target.getRol()) || "SUPERADMIN".equals(target.getRol())) 
                && !"SUPERADMIN".equals(requester.getRol())) {
                return ResponseEntity.status(403).body("Acceso denegado: Solo un SUPERADMIN puede eliminar administradores.");
            }
        }
        
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    /**
     * Devuelve el usuario local correspondiente al token JWT del request.
     * La lógica de validación y búsqueda se delega al UserService.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        UserEntity user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(user);
    }
}
