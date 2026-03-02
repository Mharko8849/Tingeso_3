package com.example.demo.ServiceTest;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.KeycloakAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KeycloakAdminServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private KeycloakAdminService keycloakAdminService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        keycloakAdminService = new KeycloakAdminService(restTemplate);
        ReflectionTestUtils.setField(keycloakAdminService, "keycloakUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakAdminService, "realm", "test-realm");
        ReflectionTestUtils.setField(keycloakAdminService, "clientId", "test-client");
        ReflectionTestUtils.setField(keycloakAdminService, "clientSecret", "test-secret");
        user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setName("Test");
        user.setLastName("User");
        user.setPassword("password");
    }

    private void mockAdminToken() {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(
                contains("/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
    }

    @SuppressWarnings("unchecked")
    private void mockRolesList(String roleName) {
        Map<String, Object> role = new HashMap<>();
        role.put("id", "role-id-" + roleName);
        role.put("name", roleName);
        List<Map> allRoles = List.of(role);
        when(restTemplate.exchange(
                contains("/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(new ResponseEntity<>(allRoles, HttpStatus.OK));
    }

    @Test
    void testObtainAdminAccessToken_Success() {
        mockAdminToken();
        String token = keycloakAdminService.obtainAdminAccessToken();
        assertEquals("admin-token", token);
    }

    @Test
    void testObtainAdminAccessToken_Failure() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Error"));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.obtainAdminAccessToken());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_Success() throws Exception {
        mockAdminToken();
        mockRolesList("CLIENT");
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setLocation(new URI("http://localhost:8080/admin/realms/test-realm/users/user-id-123"));
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(null, createHeaders, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
        String userId = keycloakAdminService.createKeycloakUser(user, "CLIENT");
        assertEquals("user-id-123", userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_DefaultRole() throws Exception {
        mockAdminToken();
        mockRolesList("CLIENT");
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setLocation(new URI("http://localhost:8080/admin/realms/test-realm/users/user-id-456"));
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(null, createHeaders, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
        String userId = keycloakAdminService.createKeycloakUser(user, null);
        assertEquals("user-id-456", userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_SearchFallback() throws Exception {
        mockAdminToken();
        mockRolesList("CLIENT");
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.CREATED));
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", "user-id-found");
        List<Map> searchResult = List.of(userMap);
        when(restTemplate.exchange(contains("/users?username="), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(new ResponseEntity<>(searchResult, HttpStatus.OK));
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
        String userId = keycloakAdminService.createKeycloakUser(user, "CLIENT");
        assertEquals("user-id-found", userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_RoleNotFound() {
        mockAdminToken();
        when(restTemplate.exchange(
                contains("/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "NONEXISTENT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_ConflictThrows() {
        mockAdminToken();
        mockRolesList("CLIENT");
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        HttpStatus.CONFLICT, "Conflict", "username conflict".getBytes(), null));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }

    @Test
    void testDeleteKeycloakUser_Success() {
        mockAdminToken();
        when(restTemplate.exchange(contains("/users/user-id"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
        assertDoesNotThrow(() -> keycloakAdminService.deleteKeycloakUser("user-id"));
    }

    @Test
    void testDeleteKeycloakUser_Exception() {
        mockAdminToken();
        when(restTemplate.exchange(contains("/users/user-id"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Delete failed"));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.deleteKeycloakUser("user-id"));
    }

    @Test
    void testRequestPasswordGrant_Success() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "user-token");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));
        Map result = keycloakAdminService.requestPasswordGrant("user", "pass");
        assertEquals("user-token", result.get("access_token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckUserExistsByUsername_Exists() {
        mockAdminToken();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", "some-id");
        when(restTemplate.exchange(contains("/users?username="), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(new ResponseEntity<>(List.of(userMap), HttpStatus.OK));
        assertTrue(keycloakAdminService.checkUserExistsByUsername("testuser"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckUserExistsByUsername_NotExists() {
        mockAdminToken();
        when(restTemplate.exchange(contains("/users?username="), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));
        assertFalse(keycloakAdminService.checkUserExistsByUsername("nonexistent"));
    }

    @Test
    void testCheckUserExistsByUsername_Error() {
        mockAdminToken();
        when(restTemplate.exchange(contains("/users?username="), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenThrow(new RuntimeException("Connection error"));
        assertFalse(keycloakAdminService.checkUserExistsByUsername("testuser"));
    }

    @Test
    void testRefreshToken_Success() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "new-access-token");
        responseBody.put("refresh_token", "new-refresh-token");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));
        Map result = keycloakAdminService.refreshToken("valid-refresh-token");
        assertNotNull(result);
        assertEquals("new-access-token", result.get("access_token"));
    }

    @Test
    void testRefreshToken_Failure() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Expired token"));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.refreshToken("bad-refresh-token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_OtherClientError() {
        mockAdminToken();
        mockRolesList("CLIENT");
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        HttpStatus.BAD_REQUEST, "Bad request", "email already taken".getBytes(), null));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_ConnectionError() {
        mockAdminToken();
        mockRolesList("CLIENT");
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_RoleAssignFails() throws Exception {
        mockAdminToken();
        mockRolesList("CLIENT");
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setLocation(new URI("http://localhost:8080/admin/realms/test-realm/users/user-id-789"));
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(null, createHeaders, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Role assign failed"));
        // rollback: delete user
        when(restTemplate.exchange(contains("/users/user-id-789"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_NullRoles() {
        mockAdminToken();
        // Return null body for roles
        when(restTemplate.exchange(
                contains("/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateKeycloakUser_ConflictWithUsername() {
        mockAdminToken();
        mockRolesList("CLIENT");
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        HttpStatus.CONFLICT, "Conflict", "username testuser conflict".getBytes(), null));
        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }
}
