package com.example.demo.ControllerTest;

import com.example.demo.Controllers.ToolStateController;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Services.ToolStateService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToolStateController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
class ToolStateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToolStateService toolStateService;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private ToolStateEntity state;

    @BeforeEach
    void setUp() {
        state = new ToolStateEntity();
        state.setId(1L);
        state.setState("DISPONIBLE");
    }

    @Test
    void testGetAllStates() throws Exception {
        List<ToolStateEntity> list = Arrays.asList(state);
        when(toolStateService.getAllStates()).thenReturn(list);
        mockMvc.perform(get("/tool-states/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("DISPONIBLE"));
    }

    @Test
    void testCreateState() throws Exception {
        when(toolStateService.createState(any(ToolStateEntity.class))).thenReturn(state);
        mockMvc.perform(post("/tool-states/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(state)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DISPONIBLE"));
    }

    @Test
    void testUpdateState() throws Exception {
        when(toolStateService.updateState(eq(1L), any(ToolStateEntity.class))).thenReturn(state);
        mockMvc.perform(put("/tool-states/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(state)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testDeleteState() throws Exception {
        when(toolStateService.deleteState(1L)).thenReturn(true);
        mockMvc.perform(delete("/tool-states/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testDeleteState_NotFound() throws Exception {
        when(toolStateService.deleteState(99L)).thenReturn(false);
        mockMvc.perform(delete("/tool-states/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}
