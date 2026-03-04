package com.example.demo.ServiceTest;

import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Services.KeycloakAdminService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private UserService userService;

    private UserEntity user;
    private UserEntity admin;

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
        user.setStateClient("ACTIVO");
        user.setLoans(0);

        admin = new UserEntity();
        admin.setId(2L);
        admin.setRol("ADMIN");
    }

    @Test
    void testSaveUser() {
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        UserEntity savedUser = userService.saveUser(user);
        assertNotNull(savedUser);
        assertEquals(user.getUsername(), savedUser.getUsername());
    }

    @Test
    void testFindUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserEntity foundUser = userService.findUserById(1L);
        assertNotNull(foundUser);
        assertEquals(user.getId(), foundUser.getId());
    }

    @Test
    void testFindUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> {
            userService.findUserById(1L);
        });
    }

    @Test
    void testGetUsers() {
        ArrayList<UserEntity> users = new ArrayList<>();
        users.add(user);
        when(userRepository.findAll()).thenReturn(users);
        ArrayList<UserEntity> result = userService.getUsers();
        assertEquals(1, result.size());
    }

    @Test
    void testGetUsersByRol() {
        List<UserEntity> users = new ArrayList<>();
        users.add(user);
        when(userRepository.findByRol("CLIENT")).thenReturn(users);
        List<UserEntity> result = userService.getUsersByRol("CLIENT");
        assertEquals(1, result.size());
        assertEquals("CLIENT", result.get(0).getRol());
    }

    @Test
    void testGetAllEmployees() {
        UserEntity employee = new UserEntity();
        employee.setRol("EMPLOYEE");
        UserEntity adminUser = new UserEntity();
        adminUser.setRol("ADMIN");
        List<UserEntity> employees = List.of(employee);
        List<UserEntity> admins = List.of(adminUser);
        when(userRepository.findByRol("EMPLOYEE")).thenReturn(employees);
        when(userRepository.findByRol("ADMIN")).thenReturn(admins);

        List<UserEntity> result = userService.getAllEmployees();
        assertEquals(2, result.size());
    }

    @Test
    void testGetAllClients() {
        List<UserEntity> clients = List.of(user);
        when(userRepository.findByRol("CLIENT")).thenReturn(clients);
        List<UserEntity> result = userService.getAllClients();
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateUser() {
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);
        user.setName("Updated Name");
        UserEntity updatedUser = userService.updateUser(user);
        assertEquals("Updated Name", updatedUser.getName());
    }

    @Test
    void testDeleteUser() {
        user.setKeycloakId("keycloak-id");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        boolean result = userService.deleteUser(1L);
        assertTrue(result);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void testGetUserByUsername() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user);
        UserEntity result = userService.getUserByUsername("testuser");
        assertEquals(user, result);
    }

    @Test
    void testGetUserByEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        UserEntity result = userService.getUserByEmail("test@example.com");
        assertEquals(user, result);
    }

    @Test
    void testGetUserByKeycloakId() {
        when(userRepository.findByKeycloakId("kc-id")).thenReturn(user);
        UserEntity result = userService.getUserByKeycloakId("kc-id");
        assertEquals(user, result);
    }

    @Test
    void testFilterClient_NullOrBlank() {
        List<UserEntity> clients = List.of(user);
        when(userRepository.findByRol("CLIENT")).thenReturn(clients);
        
        assertEquals(1, userService.filterClient(null).size());
        assertEquals(1, userService.filterClient("").size());
    }

    @Test
    void testFilterEmployee_NullOrBlank() {
        UserEntity employee = new UserEntity();
        employee.setRol("EMPLOYEE");
        List<UserEntity> employees = List.of(employee);
        when(userRepository.findByRol("EMPLOYEE")).thenReturn(employees);
        when(userRepository.findByRol("ADMIN")).thenReturn(new ArrayList<>());
        
        assertEquals(1, userService.filterEmployee(null).size());
        assertEquals(1, userService.filterEmployee("").size());
    }

    @Test
    void testDeleteUser_DbException() {
        user.setKeycloakId("keycloak-id");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("DB Error")).when(userRepository).deleteById(1L);
        
        boolean result = userService.deleteUser(1L);
        assertFalse(result);
    }

    @Test
    void testDeleteUser_KeycloakException() {
        user.setKeycloakId("keycloak-id");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("KC Error")).when(keycloakAdminService).deleteKeycloakUser("keycloak-id");
        
        boolean result = userService.deleteUser(1L);
        assertTrue(result); // Should continue and delete from DB
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void testIsAdmin() {
        UserEntity adminUser = new UserEntity();
        adminUser.setRol("ADMIN");
        assertDoesNotThrow(() -> userService.isAdmin(adminUser));
        
        UserEntity superAdmin = new UserEntity();
        superAdmin.setRol("SUPERADMIN");
        assertDoesNotThrow(() -> userService.isAdmin(superAdmin));

        UserEntity client = new UserEntity();
        client.setRol("CLIENT");
        assertThrows(RuntimeException.class, () -> userService.isAdmin(client));
    }

    @Test
    void testValidateAdminOrEmployee() {
        UserEntity adminUser = new UserEntity();
        adminUser.setRol("ADMIN");
        assertDoesNotThrow(() -> userService.validateAdminOrEmployee(adminUser));
        
        UserEntity employee = new UserEntity();
        employee.setRol("EMPLOYEE");
        assertDoesNotThrow(() -> userService.validateAdminOrEmployee(employee));

        UserEntity superAdmin = new UserEntity();
        superAdmin.setRol("SUPERADMIN");
        assertDoesNotThrow(() -> userService.validateAdminOrEmployee(superAdmin));

        UserEntity client = new UserEntity();
        client.setRol("CLIENT");
        assertThrows(RuntimeException.class, () -> userService.validateAdminOrEmployee(client));
    }

    @Test
    void testFilterEmployee() {
        UserEntity employee = new UserEntity();
        employee.setRol("EMPLOYEE");
        List<UserEntity> employeesAndAdmins = List.of(employee);
        when(userRepository.findByRol("EMPLOYEE")).thenReturn(employeesAndAdmins);
        when(userRepository.findByRol("ADMIN")).thenReturn(new ArrayList<>());
        List<UserEntity> result = userService.filterEmployee("EMPLOYEE");
        assertEquals(1, result.size());
    }

    @Test
    void testCanDoAnotherLoan() {
        user.setLoans(4);
        assertTrue(userService.canDoAnotherLoan(user));
        user.setLoans(5);
        assertFalse(userService.canDoAnotherLoan(user));
    }

    @Test
    void testGetUserFromJwt_NullJwt() {
        assertThrows(ResponseStatusException.class, () -> userService.getUserFromJwt(null));
    }

    @Test
    void testGetUserFromJwt_Success() {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwt.getSubject()).thenReturn("keycloak-sub-id");
        when(userRepository.findByKeycloakId("keycloak-sub-id")).thenReturn(user);
        UserEntity result = userService.getUserFromJwt(jwt);
        assertEquals(user, result);
    }

    @Test
    void testGetUserFromJwt_NullSub() {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwt.getSubject()).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.getUserFromJwt(jwt));
    }

    @Test
    void testGetUserFromJwt_BlankSub() {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwt.getSubject()).thenReturn("   ");
        assertThrows(ResponseStatusException.class, () -> userService.getUserFromJwt(jwt));
    }

    @Test
    void testGetUserFromJwt_UserNotFound() {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwt.getSubject()).thenReturn("unknown-sub");
        when(userRepository.findByKeycloakId("unknown-sub")).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> userService.getUserFromJwt(jwt));
    }

    @Test
    void testFilterClient_WithState() {
        UserEntity activeClient = new UserEntity();
        activeClient.setRol("CLIENT");
        activeClient.setStateClient("ACTIVO");

        UserEntity inactiveClient = new UserEntity();
        inactiveClient.setRol("CLIENT");
        inactiveClient.setStateClient("INACTIVO");

        when(userRepository.findByRol("CLIENT")).thenReturn(List.of(activeClient, inactiveClient));
        List<UserEntity> result = userService.filterClient("ACTIVO");
        assertEquals(1, result.size());
        assertEquals("ACTIVO", result.get(0).getStateClient());
    }
}
