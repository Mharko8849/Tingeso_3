package com.example.demo.ControllerTest;

import com.example.demo.Controllers.KardexController;
import com.example.demo.DTO.PageResponseDTO;
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
class KardexControllerTest {

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
    void setUp() {
        kardex = new KardexEntity();
        kardex.setId(1L);
        kardex.setType("IN");
    }

    @Test
    void testGetAllKardex() throws Exception {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexService.getAllKardex()).thenReturn(list);

        mockMvc.perform(get("/kardex/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetKardexById() throws Exception {
        when(kardexService.getKardexById(1L)).thenReturn(kardex);

        mockMvc.perform(get("/kardex/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testFilterKardex() throws Exception {
        List<KardexEntity> content = new ArrayList<>();
        content.add(kardex);
        PageResponseDTO<KardexEntity> page = new PageResponseDTO<>(content, 0, 20, 1L, 1, true, true);
        when(kardexService.filterKardexPaginated(any(), any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/kardex/filter")
                .param("type", "IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testFilterKardex_InvalidDate() throws Exception {
        mockMvc.perform(get("/kardex/filter")
                .param("initDate", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRankingByDateRange() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("tool", "Hammer");
        list.add(map);
        when(kardexService.getRankingToolsByDateRange(any(), any())).thenReturn(list);

        mockMvc.perform(get("/kardex/ranking/range")
                .param("initDate", "2023-01-01")
                .param("finalDate", "2023-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tool").value("Hammer"));
    }

    @Test
    void testGetRanking() throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("tool", "Hammer");
        list.add(map);
        when(kardexService.getRankingTools()).thenReturn(list);

        mockMvc.perform(get("/kardex/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tool").value("Hammer"));
    }

    @Test
    void testGetAllKardexPaginated() throws Exception {
        List<KardexEntity> content = new ArrayList<>();
        content.add(kardex);
        PageResponseDTO<KardexEntity> page = new PageResponseDTO<>(content, 0, 20, 1L, 1, true, true);
        when(kardexService.getAllKardexPaginated(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/kardex/paginated")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testGetRankingPaginated() throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("tool", "Hammer");
        map.put("totalLoans", 5);
        content.add(map);
        PageResponseDTO<Map<String, Object>> page = new PageResponseDTO<>(content, 0, 10, 1L, 1, true, true);
        when(kardexService.getRankingToolsPaginated(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/kardex/ranking/paginated")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tool").value("Hammer"));
    }

    @Test
    void testGetRankingByDateRangePaginated() throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("tool", "Saw");
        map.put("totalLoans", 3);
        content.add(map);
        PageResponseDTO<Map<String, Object>> page = new PageResponseDTO<>(content, 0, 10, 1L, 1, true, true);
        when(kardexService.getRankingToolsByDateRangePaginated(any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/kardex/ranking/range/paginated")
                .param("initDate", "2023-01-01")
                .param("finalDate", "2023-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tool").value("Saw"));
    }

    @Test
    void testGetRankingByDateRangePaginated_InvalidDate() throws Exception {
        mockMvc.perform(get("/kardex/ranking/range/paginated")
                .param("initDate", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRankingByDateRange_InvalidDate() throws Exception {
        mockMvc.perform(get("/kardex/ranking/range")
                .param("initDate", "not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
