package com.example.demo.ControllerTest;

import com.example.demo.Controllers.InventoryController;
import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.InventoryService;
import com.example.demo.Services.ToolService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private UserService userService;

    @MockBean
    private ToolService toolService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private InventoryEntity inventory;
    private UserEntity user;

    @BeforeEach
    public void setUp() {
        inventory = new InventoryEntity();
        inventory.setId(1L);
        inventory.setStockTool(10);

        user = new UserEntity();
        user.setId(1L);
    }

    @Test
    public void testGetAllInventory() throws Exception {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryService.getAllInventory()).thenReturn(list);

        mockMvc.perform(get("/api/inventory/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testFilterInventory() throws Exception {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryService.filterInventory(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(list);

        mockMvc.perform(get("/api/inventory/filter")
                .param("state", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testAddStock() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(inventoryService.addStockToTool(eq(1L), eq(5), any(UserEntity.class))).thenReturn(inventory);

        mockMvc.perform(post("/api/inventory/add-stock/1/1")
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTool").value(10));
    }

    @Test
    public void testAddStock_UserNotFound() throws Exception {
        when(userService.findUserById(1L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/inventory/add-stock/1/1")
                .param("quantity", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddStock_Error() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(inventoryService.addStockToTool(eq(1L), eq(5), any(UserEntity.class))).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/inventory/add-stock/1/1")
                .param("quantity", "5"))
                .andExpect(status().isBadRequest());
    }
}
