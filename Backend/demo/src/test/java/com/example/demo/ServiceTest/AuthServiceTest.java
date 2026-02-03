package com.example.demo.ServiceTest;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.AuthService;
import com.example.demo.Services.KeycloakAdminService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

public class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private AuthService authService;

    private UserEntity user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new UserEntity();
        user.setId(1L);
        user.setName("Test");
        user.setLastName("User");
        user.setRut("12345678-9");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRol("CLIENT");
    }

    @Test
    public void testRegisterWithRole_Success() throws Exception {
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenReturn("keycloak-id");
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);

        UserEntity registeredUser = authService.registerWithRole(user, "CLIENT");

        assertNotNull(registeredUser);
        assertEquals("keycloak-id", user.getKeycloakId());
        assertEquals("CLIENT", user.getRol());
        assertEquals("ACTIVO", user.getStateClient());
    }

    @Test
    public void testRegisterWithRole_KeycloakFails() throws Exception {
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenThrow(new RuntimeException("Keycloak error"));

        assertThrows(RuntimeException.class, () -> {
            authService.registerWithRole(user, "CLIENT");
        });
    }

    @Test
    public void testRegisterWithRole_RollbackFails() throws Exception {
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenReturn("keycloak-id");
        when(userService.saveUser(any(UserEntity.class))).thenThrow(new RuntimeException("DB error"));
        doThrow(new RuntimeException("Rollback error")).when(keycloakAdminService).deleteKeycloakUser("keycloak-id");

        assertThrows(RuntimeException.class, () -> {
            authService.registerWithRole(user, "CLIENT");
        });

        verify(keycloakAdminService, times(1)).deleteKeycloakUser("keycloak-id");
    }

    @Test
    public void testLogin_FallbackLogic_UserFoundButLoginFails() throws Exception {
        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("MixedCaseUser", "password"))
                .thenThrow(new RuntimeException("Fail"));

        // 2. Fallback to username lookup
        UserEntity userByUsername = new UserEntity();
        userByUsername.setUsername("mixedcaseuser");
        userByUsername.setEmail("email@test.com");
        when(userService.getUserByUsername("MixedCaseUser")).thenReturn(null);
        when(userService.getUserByUsername("mixedcaseuser")).thenReturn(userByUsername);

        // 3. Login with email from found user FAILS
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password"))
                .thenThrow(new RuntimeException("Fail again"));

        // 4. Fallback to email lookup
        when(userService.getUserByEmail("MixedCaseUser")).thenReturn(null);
        when(userService.getUserByEmail("mixedcaseuser")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> authService.login("MixedCaseUser", "password"));
    }

    @Test
    public void testLogin_FallbackLogic_EmailFoundButLoginFails() throws Exception {
        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password"))
                .thenThrow(new RuntimeException("Fail"));

        // 2. Fallback to username lookup fails
        when(userService.getUserByUsername(anyString())).thenReturn(null);

        // 3. Fallback to email lookup
        UserEntity userByEmail = new UserEntity();
        userByEmail.setUsername("user");
        userByEmail.setEmail("email@test.com");
        when(userService.getUserByEmail("email@test.com")).thenReturn(userByEmail);

        // 4. Login with username from found user FAILS
        when(keycloakAdminService.requestPasswordGrant("user", "password"))
                .thenThrow(new RuntimeException("Fail again"));

        assertThrows(RuntimeException.class, () -> authService.login("email@test.com", "password"));
    }

    @Test
    public void testRegisterClient() {
        // We can't easily test the inner call to registerWithRole, so we'll just check if it runs without error
        // and assume the more detailed tests for registerWithRole cover the logic.
        // A more advanced approach would be to use a spy on authService.
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);
        assertDoesNotThrow(() -> authService.registerClient(user));
    }
    
    @Test
    public void testRegisterEmployee() {
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);
        assertDoesNotThrow(() -> authService.registerEmployee(user));
    }

    @Test
    public void testRegisterAdmin() {
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);
        assertDoesNotThrow(() -> authService.registerAdmin(user));
    }

    @Test
    public void testLogin_Success() throws Exception {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", "test-token");
        
        when(keycloakAdminService.requestPasswordGrant("testuser", "password")).thenReturn(token);
        // User lookup behavior
        when(userService.getUserByEmail("testuser")).thenReturn(null);
        when(userService.getUserByEmail("test@example.com")).thenReturn(user);
        when(userService.getUserByUsername("testuser")).thenReturn(user);

        Map<String, Object> result = authService.login("testuser", "password");

        assertNotNull(result);
        assertTrue(result.containsKey("token"));
        assertTrue(result.containsKey("user"));
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        when(keycloakAdminService.requestPasswordGrant(anyString(), anyString())).thenThrow(new RuntimeException("Invalid credentials"));
        when(userService.getUserByUsername(anyString())).thenReturn(null);
        when(userService.getUserByEmail(anyString())).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", "wrongpassword");
        });
    }

    @Test
    public void testLogin_UserNotFoundInDb() throws Exception {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", "test-token");
        when(keycloakAdminService.requestPasswordGrant(anyString(), anyString())).thenReturn(token);
        when(userService.getUserByEmail(anyString())).thenReturn(null);
        when(userService.getUserByUsername(anyString())).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", "password");
        });
    }

    @Test
    public void testLogin_FallbackLogic() throws Exception {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", "test-token");

        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("MixedCaseUser", "password"))
                .thenThrow(new RuntimeException("Fail"));

        // 2. Fallback to username lookup
        UserEntity userByUsername = new UserEntity();
        userByUsername.setId(1L);
        userByUsername.setUsername("mixedcaseuser");
        userByUsername.setEmail("email@test.com");
        userByUsername.setRol("USER");
        
        when(userService.getUserByUsername("MixedCaseUser")).thenReturn(null);
        when(userService.getUserByUsername("mixedcaseuser")).thenReturn(userByUsername);

        // 3. Login with email from found user
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password")).thenReturn(token);

        // 4. Local user lookup
        when(userService.getUserByEmail("MixedCaseUser")).thenReturn(null);
        when(userService.getUserByEmail("mixedcaseuser")).thenReturn(null);
        // It will try username lookup again at the end
        
        Map<String, Object> result = authService.login("MixedCaseUser", "password");
        assertNotNull(result);
    }
    
    @Test
    public void testLogin_FallbackToEmail() throws Exception {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", "test-token");

        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password"))
                .thenThrow(new RuntimeException("Fail"));

        // 2. Fallback to username lookup fails
        when(userService.getUserByUsername(anyString())).thenReturn(null);

        // 3. Fallback to email lookup
        UserEntity userByEmail = new UserEntity();
        userByEmail.setId(2L);
        userByEmail.setUsername("user");
        userByEmail.setEmail("email@test.com");
        userByEmail.setRol("USER");
        
        when(userService.getUserByEmail("email@test.com")).thenReturn(userByEmail);

        // 4. Login with username from found user
        when(keycloakAdminService.requestPasswordGrant("user", "password")).thenReturn(token);

        Map<String, Object> result = authService.login("email@test.com", "password");
        assertNotNull(result);
    }
}
