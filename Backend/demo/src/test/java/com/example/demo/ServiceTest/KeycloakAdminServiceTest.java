package com.example.demo.ServiceTest;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.KeycloakAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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

public class KeycloakAdminServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KeycloakAdminService keycloakAdminService;

    private UserEntity user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
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

    @Test
    public void testObtainAdminAccessToken_Success() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "admin-token");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String token = keycloakAdminService.obtainAdminAccessToken();
        assertEquals("admin-token", token);
    }

    @Test
    public void testObtainAdminAccessToken_Failure() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class, () -> keycloakAdminService.obtainAdminAccessToken());
    }

    @Test
    public void testCreateKeycloakUser_Success() throws Exception {
        // Mock admin token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(contains("/protocol/openid-connect/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Mock create user
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("http://localhost:8080/admin/realms/test-realm/users/user-id-123"));
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(headers, HttpStatus.CREATED));

        // Mock get role
        Map<String, Object> roleResponse = new HashMap<>();
        roleResponse.put("id", "role-id");
        roleResponse.put("name", "CLIENT");
        when(restTemplate.exchange(contains("/roles/CLIENT"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(roleResponse, HttpStatus.OK));

        // Mock assign role
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        String userId = keycloakAdminService.createKeycloakUser(user, "CLIENT");
        assertEquals("user-id-123", userId);
    }

    @Test
    public void testCreateKeycloakUser_DefaultRole() throws Exception {
        // Mock admin token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(contains("/protocol/openid-connect/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Mock create user
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("http://localhost:8080/admin/realms/test-realm/users/user-id-123"));
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(headers, HttpStatus.CREATED));

        // Mock get role CLIENT (default)
        Map<String, Object> roleResponse = new HashMap<>();
        roleResponse.put("id", "role-id");
        roleResponse.put("name", "CLIENT");
        when(restTemplate.exchange(contains("/roles/CLIENT"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(roleResponse, HttpStatus.OK));

        // Mock assign role
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        String userId = keycloakAdminService.createKeycloakUser(user, null);
        assertEquals("user-id-123", userId);
    }

    @Test
    public void testCreateKeycloakUser_SearchFallback() throws Exception {
        // Mock admin token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(contains("/protocol/openid-connect/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Mock create user - NO Location header
        HttpHeaders headers = new HttpHeaders();
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(headers, HttpStatus.CREATED));

        // Mock search user
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", "user-id-found");
        List<Map<String, Object>> searchResult = List.of(userMap);
        when(restTemplate.exchange(contains("/users?username="), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(new ResponseEntity<>(searchResult, HttpStatus.OK));

        // Mock get role
        Map<String, Object> roleResponse = new HashMap<>();
        roleResponse.put("id", "role-id");
        roleResponse.put("name", "CLIENT");
        when(restTemplate.exchange(contains("/roles/CLIENT"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(roleResponse, HttpStatus.OK));

        // Mock assign role
        when(restTemplate.postForEntity(contains("/role-mappings/realm"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        String userId = keycloakAdminService.createKeycloakUser(user, "CLIENT");
        assertEquals("user-id-found", userId);
    }

    @Test
    public void testCreateKeycloakUser_RoleNotFound() throws Exception {
        // Mock admin token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(contains("/protocol/openid-connect/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Mock create user
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("http://localhost:8080/admin/realms/test-realm/users/user-id-123"));
        when(restTemplate.postForEntity(contains("/users"), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(headers, HttpStatus.CREATED));

        // Mock get role - returns null body
        when(restTemplate.exchange(contains("/roles/CLIENT"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(RuntimeException.class, () -> keycloakAdminService.createKeycloakUser(user, "CLIENT"));
    }

    @Test
    public void testDeleteKeycloakUser_Exception() {
        // Mock admin token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(contains("/protocol/openid-connect/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        when(restTemplate.exchange(contains("/users/user-id"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Delete failed"));

        // Should catch exception and log it, not throw
        assertDoesNotThrow(() -> keycloakAdminService.deleteKeycloakUser("user-id"));
    }

    @Test
    public void testDeleteKeycloakUser() {
        // Mock admin token
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "admin-token");
        when(restTemplate.exchange(contains("/protocol/openid-connect/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        when(restTemplate.exchange(contains("/users/user-id"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> keycloakAdminService.deleteKeycloakUser("user-id"));
    }

    @Test
    public void testRequestPasswordGrant_Success() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", "user-token");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Map result = keycloakAdminService.requestPasswordGrant("user", "pass");
        assertEquals("user-token", result.get("access_token"));
    }
}
