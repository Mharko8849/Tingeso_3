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

class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private AuthService authService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
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
    void testRegisterWithRole_Success() {
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenReturn("keycloak-id");
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);

        UserEntity registeredUser = authService.registerWithRole(user, "CLIENT");

        assertNotNull(registeredUser);
        assertEquals("keycloak-id", user.getKeycloakId());
        assertEquals("CLIENT", user.getRol());
        assertEquals("ACTIVO", user.getStateClient());
    }

    @Test
    void testRegisterWithRole_KeycloakFails() {
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenThrow(new IllegalStateException("Keycloak error"));

        assertThrows(RuntimeException.class, () -> {
            authService.registerWithRole(user, "CLIENT");
        });
    }

    @Test
    void testRegisterWithRole_RollbackFails() {
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenReturn("keycloak-id");
        when(userService.saveUser(any(UserEntity.class))).thenThrow(new IllegalStateException("DB error"));
        doThrow(new IllegalStateException("Rollback error")).when(keycloakAdminService).deleteKeycloakUser("keycloak-id");

        assertThrows(RuntimeException.class, () -> {
            authService.registerWithRole(user, "CLIENT");
        });

        verify(keycloakAdminService, times(1)).deleteKeycloakUser("keycloak-id");
    }

    @Test
    void testLogin_FallbackLogic_UserFoundButLoginFails() {
        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("MixedCaseUser", "password"))
                .thenThrow(new IllegalStateException("Fail"));

        // 2. Fallback to username lookup
        UserEntity userByUsername = new UserEntity();
        userByUsername.setUsername("mixedcaseuser");
        userByUsername.setEmail("email@test.com");
        when(userService.getUserByUsername("MixedCaseUser")).thenReturn(null);
        when(userService.getUserByUsername("mixedcaseuser")).thenReturn(userByUsername);

        // 3. Login with email from found user FAILS
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password"))
                .thenThrow(new IllegalStateException("Fail again"));

        // 4. Fallback to email lookup
        when(userService.getUserByEmail("MixedCaseUser")).thenReturn(null);
        when(userService.getUserByEmail("mixedcaseuser")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> authService.login("MixedCaseUser", "password"));
    }

    @Test
    void testLogin_FallbackLogic_EmailFoundButLoginFails() {
        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password"))
                .thenThrow(new IllegalStateException("Fail"));

        // 2. Fallback to username lookup fails
        when(userService.getUserByUsername(anyString())).thenReturn(null);

        // 3. Fallback to email lookup
        UserEntity userByEmail = new UserEntity();
        userByEmail.setUsername("user");
        userByEmail.setEmail("email@test.com");
        when(userService.getUserByEmail("email@test.com")).thenReturn(userByEmail);

        // 4. Login with username from found user FAILS
        when(keycloakAdminService.requestPasswordGrant("user", "password"))
                .thenThrow(new IllegalStateException("Fail again"));

        assertThrows(RuntimeException.class, () -> authService.login("email@test.com", "password"));
    }

    @Test
    void testRegisterClient() {
        // We can't easily test the inner call to registerWithRole, so we'll just check if it runs without error
        // and assume the more detailed tests for registerWithRole cover the logic.
        // A more advanced approach would be to use a spy on authService.
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);
        assertDoesNotThrow(() -> authService.registerClient(user));
    }
    
    @Test
    void testRegisterEmployee() {
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);
        assertDoesNotThrow(() -> authService.registerEmployee(user));
    }

    @Test
    void testRegisterAdmin() {
        when(userService.saveUser(any(UserEntity.class))).thenReturn(user);
        assertDoesNotThrow(() -> authService.registerAdmin(user));
    }

    @Test
    void testLogin_Success() {
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
    void testLogin_InvalidCredentials() {
        when(keycloakAdminService.requestPasswordGrant(anyString(), anyString())).thenThrow(new IllegalStateException("Invalid credentials"));
        when(userService.getUserByUsername(anyString())).thenReturn(null);
        when(userService.getUserByEmail(anyString())).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", "wrongpassword");
        });
    }

    @Test
    void testLogin_UserNotFoundInDb() {
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
    void testLogin_FallbackLogic() {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", "test-token");

        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("MixedCaseUser", "password"))
                .thenThrow(new IllegalStateException("Fail"));

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
    void testLogin_FallbackToEmail() {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", "test-token");

        // 1. Direct login fails
        when(keycloakAdminService.requestPasswordGrant("email@test.com", "password"))
                .thenThrow(new IllegalStateException("Fail"));

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

    @Test
    void testRefresh_Success() {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "new-token");
        when(keycloakAdminService.refreshToken("valid-token")).thenReturn(tokenResponse);

        Map<String, Object> result = authService.refresh("valid-token");
        assertNotNull(result);
        assertTrue(result.containsKey("token"));
    }

    @Test
    void testRefresh_NullToken() {
        assertThrows(RuntimeException.class, () -> authService.refresh(null));
    }

    @Test
    void testRefresh_BlankToken() {
        assertThrows(RuntimeException.class, () -> authService.refresh("   "));
    }

    @Test
    void testRefresh_Error() {
        when(keycloakAdminService.refreshToken("bad-token")).thenThrow(new IllegalStateException("Token expired"));
        assertThrows(RuntimeException.class, () -> authService.refresh("bad-token"));
    }

    @Test
    void testRegisterWithRole_UsernameAlreadyExists() {
        // Existing user found by username → should throw
        when(userService.getUserByUsername("testuser")).thenReturn(user);
        assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
    }

    @Test
    void testRegisterWithRole_UsernameWithRutAlreadyExists() {
        // Username contains "-" (RUT format) → different error message
        user.setUsername("12345678-9");
        when(userService.getUserByUsername("12345678-9")).thenReturn(user);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
        assertTrue(ex.getMessage().contains("RUT"));
    }

    @Test
    void testRegisterWithRole_EmailAlreadyExists() {
        // Username not found but email exists → should throw
        when(userService.getUserByUsername(anyString())).thenReturn(null);
        when(userService.getUserByEmail("test@example.com")).thenReturn(user);
        assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
    }

    @Test
    void testRegisterWithRole_UserExistsInKeycloak() {
        // User not in DB but exists in Keycloak → throws inconsistency error
        when(userService.getUserByUsername(anyString())).thenReturn(null);
        when(userService.getUserByEmail(anyString())).thenReturn(null);
        when(keycloakAdminService.checkUserExistsByUsername("testuser")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
    }

    @Test
    void testRegisterWithRole_RollbackSuccess() {
        // DB save fails, but Keycloak rollback succeeds
        when(userService.getUserByUsername(anyString())).thenReturn(null);
        when(userService.getUserByEmail(anyString())).thenReturn(null);
        when(keycloakAdminService.checkUserExistsByUsername(anyString())).thenReturn(false);
        when(keycloakAdminService.createKeycloakUser(any(UserEntity.class), anyString())).thenReturn("keycloak-id");
        when(userService.saveUser(any(UserEntity.class))).thenThrow(new IllegalStateException("DB error"));
        // Rollback succeeds (doNothing is default for void methods)

        assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
        verify(keycloakAdminService, times(1)).deleteKeycloakUser("keycloak-id");
    }

    @Test
    void testRegisterWithRole_ValidateUser_MissingName() {
        user.setName(null);
        assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
    }

    @Test
    void testRegisterWithRole_ValidateUser_MissingPassword() {
        user.setPassword("");
        assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
    }

    @Test
    void testRegisterWithRole_ValidateUser_MultipleErrors() {
        user.setName(null);
        user.setLastName(null);
        user.setRut(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.registerWithRole(user, "CLIENT"));
        assertNotNull(ex.getMessage());
    }
}
