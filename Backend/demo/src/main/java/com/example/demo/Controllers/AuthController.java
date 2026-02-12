package com.example.demo.Controllers;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Registro de Cliente (desde frontend)
     * Rol forzado: CLIENT
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerClient(@RequestBody UserEntity user) {
        try {
            UserEntity created = authService.registerClient(user);
            return ResponseEntity.ok(created);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", ex.getMessage())
            );
        }
    }

    /**
     * Registro de Empleado (solo Admin)
     */
    @PostMapping("/register/employee")
    public ResponseEntity<?> registerEmployee(@RequestBody UserEntity user) {
        try {
            UserEntity created = authService.registerEmployee(user);
            return ResponseEntity.ok(created);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", ex.getMessage())
            );
        }
    }

    /**
     * Registro de Admin (solo SuperAdmin)
     */
    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdmin(@RequestBody UserEntity user) {
        try {
            UserEntity created = authService.registerAdmin(user);
            return ResponseEntity.ok(created);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", ex.getMessage())
            );
        }
    }

    /**
     * Login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody java.util.Map<String,String> body) {
        String identifier = body.get("username").toLowerCase(Locale.ROOT);
        String password = body.get("password");

        if (identifier == null || password == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "username y password requeridos"));
        }

        try {
            java.util.Map<String,Object> result = authService.login(identifier, password);
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            if ("Usuario no registrado".equals(msg)) {
                return ResponseEntity.status(404).body(java.util.Map.of("error", msg));
            } else if ("Credenciales inválidas.".equals(msg)) {
                return ResponseEntity.status(401).body(java.util.Map.of("error", msg));
            }
            return ResponseEntity.status(401).body(java.util.Map.of("error", msg));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
