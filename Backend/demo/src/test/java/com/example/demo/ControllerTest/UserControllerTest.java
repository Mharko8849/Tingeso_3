package com.example.demo.ControllerTest;

import com.example.demo.Controllers.UserController;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
    }

    @Test
    void testGetAllUsers() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.getUsers()).thenReturn((ArrayList<UserEntity>) list);

        mockMvc.perform(get("/api/user/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetUserById() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetAllEmployees() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.getAllEmployees()).thenReturn(list);

        mockMvc.perform(get("/api/user/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetAllClients() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.getAllClients()).thenReturn(list);

        mockMvc.perform(get("/api/user/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testFilterClients() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.filterClient("ACTIVO")).thenReturn(list);

        mockMvc.perform(get("/api/user/filter")
                .param("state", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testFilterEmployees() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.filterEmployee("EMPLOYEE")).thenReturn(list);

        mockMvc.perform(get("/api/user/filter/employee")
                .param("state", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testUpdateUser() throws Exception {
        when(userService.updateUser(any(UserEntity.class))).thenReturn(user);

        mockMvc.perform(put("/api/user/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testDeleteUserById() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testDeleteUserById_WithJwt_NotSuperAdmin() throws Exception {
        // Requester is ADMIN trying to delete another ADMIN → 403
        UserEntity requester = new UserEntity();
        requester.setId(2L);
        requester.setUsername("admin");
        requester.setRol("ADMIN");

        UserEntity target = new UserEntity();
        target.setId(1L);
        target.setRol("ADMIN");

        when(userService.getUserFromJwt(any())).thenReturn(requester);
        when(userService.findUserById(1L)).thenReturn(target);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteUserById_SuperAdmin_CanDeleteAdmin() throws Exception {
        // SUPERADMIN can delete ADMIN
        UserEntity requester = new UserEntity();
        requester.setId(2L);
        requester.setUsername("superadmin");
        requester.setRol("SUPERADMIN");

        UserEntity target = new UserEntity();
        target.setId(1L);
        target.setRol("ADMIN");

        when(userService.getUserFromJwt(any())).thenReturn(requester);
        when(userService.findUserById(1L)).thenReturn(target);
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testGetMe() throws Exception {
        when(userService.getUserFromJwt(any())).thenReturn(user);

        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
