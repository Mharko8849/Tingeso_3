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
import org.springframework.boot.test.mock.mockito.MockBean;
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
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private UserEntity user;

    @BeforeEach
    public void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
    }

    @Test
    public void testGetAllUsers() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.getUsers()).thenReturn((ArrayList<UserEntity>) list);

        mockMvc.perform(get("/api/user/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetUserById() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testGetAllEmployees() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.getAllEmployees()).thenReturn(list);

        mockMvc.perform(get("/api/user/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetAllClients() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.getAllClients()).thenReturn(list);

        mockMvc.perform(get("/api/user/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testFilterClients() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.filterClient("ACTIVO")).thenReturn(list);

        mockMvc.perform(get("/api/user/filter")
                .param("state", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testFilterEmployees() throws Exception {
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        when(userService.filterEmployee("EMPLOYEE")).thenReturn(list);

        mockMvc.perform(get("/api/user/filter/employee")
                .param("state", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testUpdateUser() throws Exception {
        when(userService.updateUser(any(UserEntity.class))).thenReturn(user);

        mockMvc.perform(put("/api/user/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testDeleteUserById() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
