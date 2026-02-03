package com.example.demo.ControllerTest;

import com.example.demo.Controllers.KardexController;
import com.example.demo.Entities.KardexEntity;
import com.example.demo.Services.KardexService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KardexController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
public class KardexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KardexService kardexService;

    @MockBean
    private ToolService toolService;

    @MockBean
    private UserService userService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private KardexEntity kardex;

    @BeforeEach
    public void setUp() {
        kardex = new KardexEntity();
        kardex.setId(1L);
        kardex.setType("IN");
    }

    @Test
    public void testGetAllKardex() throws Exception {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexService.getAllKardex()).thenReturn(list);

        mockMvc.perform(get("/api/kardex/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetKardexById() throws Exception {
        when(kardexService.getKardexById(1L)).thenReturn(kardex);

        mockMvc.perform(get("/api/kardex/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testFilterKardex() throws Exception {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexService.filterKardex(any(), any(), any(), any(), any(), any())).thenReturn(list);

        mockMvc.perform(get("/api/kardex/filter")
                .param("type", "IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetRankingByDateRange() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("tool", "Hammer");
        list.add(map);
        when(kardexService.getRankingToolsByDateRange(any(), any())).thenReturn(list);

        mockMvc.perform(get("/api/kardex/ranking/range")
                .param("initDate", "2023-01-01")
                .param("finalDate", "2023-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tool").value("Hammer"));
    }

    @Test
    public void testGetRanking() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("tool", "Hammer");
        list.add(map);
        when(kardexService.getRankingTools()).thenReturn(list);

        mockMvc.perform(get("/api/kardex/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tool").value("Hammer"));
    }
}
