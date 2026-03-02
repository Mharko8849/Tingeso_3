package com.example.demo.ServiceTest;

import com.example.demo.DTO.PageResponseDTO;
import com.example.demo.Entities.KardexEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.KardexRepository;
import com.example.demo.Repositories.ToolRepository;
import com.example.demo.Services.KardexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KardexServiceTest {

    @Mock
    private KardexRepository kardexRepository;

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private KardexService kardexService;

    private KardexEntity kardex;
    private ToolEntity tool;
    private UserEntity user;
    private UserEntity employee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");

        user = new UserEntity();
        user.setId(1L);
        user.setName("Client");

        employee = new UserEntity();
        employee.setId(2L);
        employee.setName("Employee");

        kardex = new KardexEntity();
        kardex.setId(1L);
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-01"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
    }

    @Test
    void testSaveKardexEntity() {
        when(kardexRepository.save(any(KardexEntity.class))).thenReturn(kardex);
        KardexEntity result = kardexService.saveKardexEntity(kardex);
        assertNotNull(result);
    }

    @Test
    void testCreateKardexEntity() {
        when(kardexRepository.save(any(KardexEntity.class))).thenReturn(kardex);
        KardexEntity result = kardexService.createKardexEntity(tool, "IN", Date.valueOf("2023-01-01"), 10, 100, user, employee);
        assertNotNull(result);
    }

    @Test
    void testCreateKardexEntity_MissingData() {
        assertThrows(RuntimeException.class, () -> {
            kardexService.createKardexEntity(null, "IN", Date.valueOf("2023-01-01"), 10, 100, user, employee);
        });
        assertThrows(RuntimeException.class, () -> {
            kardexService.createKardexEntity(tool, null, Date.valueOf("2023-01-01"), 10, 100, user, employee);
        });
        assertThrows(RuntimeException.class, () -> {
            kardexService.createKardexEntity(tool, "IN", null, 10, 100, user, employee);
        });
        assertThrows(RuntimeException.class, () -> {
            kardexService.createKardexEntity(tool, "IN", Date.valueOf("2023-01-01"), 10, 100, user, null);
        });
    }

    @Test
    void testGetKardexByDateBetween() {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findByDateBetween(any(Date.class), any(Date.class))).thenReturn(list);
        
        List<KardexEntity> result = kardexService.getKardexByDateBetween(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-31"));
        assertEquals(1, result.size());
    }

    @Test
    void testGetKardexByDateBetween_NullDates() {
        assertThrows(RuntimeException.class, () -> kardexService.getKardexByDateBetween(null, Date.valueOf("2023-01-31")));
        assertThrows(RuntimeException.class, () -> kardexService.getKardexByDateBetween(Date.valueOf("2023-01-01"), null));
    }

    @Test
    void testFilterKardex_AllFilters() {
        KardexEntity k1 = new KardexEntity();
        k1.setType("IN");
        k1.setIdTool(tool);
        k1.setIdUser(user);
        k1.setIdEmployee(employee);
        k1.setDate(Date.valueOf("2023-06-15"));

        List<KardexEntity> list = List.of(k1);
        when(kardexRepository.findAll(any(Sort.class))).thenReturn(list);

        // Filter by Type
        assertEquals(1, kardexService.filterKardex(null, "IN", null, null, null, null).size());
        
        // Filter by Tool
        assertEquals(1, kardexService.filterKardex(1L, null, null, null, null, null).size());

        // Filter by User
        assertEquals(1, kardexService.filterKardex(null, null, null, null, 1L, null).size());

        // Filter by Employee
        assertEquals(1, kardexService.filterKardex(null, null, null, null, null, 2L).size());
        
        // Filter by Date
        assertEquals(1, kardexService.filterKardex(null, null, Date.valueOf("2023-06-01"), Date.valueOf("2023-06-30"), null, null).size());
    }

    @Test
    void testGetRankingToolsByDateRange() {
        Object[] row = new Object[]{tool, 5L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(kardexRepository.getRankingByDateRangeQuery(any(Date.class), any(Date.class))).thenReturn(rows);

        List<Map<String, Object>> result = kardexService.getRankingToolsByDateRange(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-31"));
        assertEquals(1, result.size());
        assertEquals(tool, result.get(0).get("tool"));
    }

    @Test
    void testGetRankingToolsByDateRange_NullDates() {
        // Should fallback to current month range
        Object[] row = new Object[]{tool, 3L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(kardexRepository.getRankingByDateRangeQuery(any(Date.class), any(Date.class))).thenReturn(rows);

        List<Map<String, Object>> result = kardexService.getRankingToolsByDateRange(null, null);
        assertEquals(1, result.size());
        assertEquals(tool, result.get(0).get("tool"));
    }

    @Test
    void testGetAllKardex() {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findAll()).thenReturn(list);
        List<KardexEntity> result = kardexService.getAllKardex();
        assertEquals(1, result.size());
    }

    @Test
    void testGetKardexById() {
        when(kardexRepository.findById(1L)).thenReturn(Optional.of(kardex));
        KardexEntity result = kardexService.getKardexById(1L);
        assertNotNull(result);
    }

    @Test
    void testGetKardexById_NotFound() {
        when(kardexRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            kardexService.getKardexById(1L);
        });
    }

    @Test
    void testFilterKardex() {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findAll(any(Sort.class))).thenReturn(list);
        List<KardexEntity> result = kardexService.filterKardex(null, "IN", null, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    void testGetRankingTools() {
        Object[] row = new Object[]{tool, 5L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(kardexRepository.getRankingTop10(any(Date.class), any(Date.class))).thenReturn(rows);
        when(toolRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = kardexService.getRankingTools();
        assertEquals(1, result.size());
        assertEquals(tool, result.get(0).get("tool"));
    }

    @Test
    void testGetRankingTools_FillsWithEmpty() {
        // When fewer than 10 results, fills with remaining tools
        List<Object[]> rows = new ArrayList<>();
        when(kardexRepository.getRankingTop10(any(Date.class), any(Date.class))).thenReturn(rows);
        ToolEntity tool2 = new ToolEntity();
        tool2.setId(99L);
        tool2.setToolName("Saw");
        when(toolRepository.findAll()).thenReturn(List.of(tool2));

        List<Map<String, Object>> result = kardexService.getRankingTools();
        assertEquals(1, result.size());
        assertEquals(tool2, result.get(0).get("tool"));
        assertEquals(0, result.get(0).get("totalLoans"));
    }

    @Test
    void testGetAllKardexPaginated() {
        Page<KardexEntity> page = new PageImpl<>(List.of(kardex));
        when(kardexRepository.findAll(any(Pageable.class))).thenReturn(page);
        PageResponseDTO<KardexEntity> result = kardexService.getAllKardexPaginated(0, 10);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testFilterKardexPaginated() {
        Page<KardexEntity> page = new PageImpl<>(List.of(kardex));
        when(kardexRepository.filterKardexPaginated(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        PageResponseDTO<KardexEntity> result = kardexService.filterKardexPaginated(null, "IN", null, null, null, null, 0, 10);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testFilterKardexPaginated_BlankParams() {
        Page<KardexEntity> page = new PageImpl<>(List.of(kardex));
        when(kardexRepository.filterKardexPaginated(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        // Blank strings should be treated as null
        PageResponseDTO<KardexEntity> result = kardexService.filterKardexPaginated(null, "", "", "", null, null, 0, 10);
        assertNotNull(result);
    }

    @Test
    void testGetRankingToolsPaginated() {
        // Test when there are enough results
        Object[] row = new Object[]{tool, 5L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        Page<Object[]> page = new PageImpl<>(rows);
        when(kardexRepository.getRankingPaginated(any(Date.class), any(Date.class), any(Pageable.class))).thenReturn(page);
        when(toolRepository.findAll()).thenReturn(new ArrayList<>());

        PageResponseDTO<Map<String, Object>> result = kardexService.getRankingToolsPaginated(0, 10);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(tool, result.getContent().get(0).get("tool"));
        assertEquals(5L, result.getContent().get(0).get("totalLoans"));
    }

    @Test
    void testGetRankingToolsPaginated_FillsWithEmpty() {
        // Test fallback: fewer than size tools with loans, fill with others
        Page<Object[]> page = new PageImpl<>(new ArrayList<>());
        when(kardexRepository.getRankingPaginated(any(Date.class), any(Date.class), any(Pageable.class))).thenReturn(page);
        ToolEntity tool2 = new ToolEntity();
        tool2.setId(99L);
        tool2.setToolName("Saw");
        when(toolRepository.findAll()).thenReturn(List.of(tool2));

        PageResponseDTO<Map<String, Object>> result = kardexService.getRankingToolsPaginated(0, 5);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(tool2, result.getContent().get(0).get("tool"));
        assertEquals(0, result.getContent().get(0).get("totalLoans"));
    }

    @Test
    void testGetRankingToolsByDateRangePaginated() {
        Object[] row = new Object[]{tool, 3L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        Page<Object[]> page = new PageImpl<>(rows);
        when(kardexRepository.getRankingPaginated(any(Date.class), any(Date.class), any(Pageable.class))).thenReturn(page);

        PageResponseDTO<Map<String, Object>> result = kardexService.getRankingToolsByDateRangePaginated(
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-31"), 0, 10);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(tool, result.getContent().get(0).get("tool"));
    }

    @Test
    void testGetRankingToolsByDateRangePaginated_NullDates() {
        // Fallback to current month
        Object[] row = new Object[]{tool, 2L};
        List<Object[]> rows2 = new ArrayList<>();
        rows2.add(row);
        Page<Object[]> page = new PageImpl<>(rows2);
        when(kardexRepository.getRankingPaginated(any(Date.class), any(Date.class), any(Pageable.class))).thenReturn(page);

        PageResponseDTO<Map<String, Object>> result = kardexService.getRankingToolsByDateRangePaginated(null, null, 0, 10);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testFilterKardex_NullDate() {
        // isOnOrAfterDate with null date on kardex
        KardexEntity k1 = new KardexEntity();
        k1.setType("IN");
        k1.setIdTool(tool);
        k1.setDate(null); // null date

        List<KardexEntity> list = List.of(k1);
        when(kardexRepository.findAll(any(Sort.class))).thenReturn(list);

        // Should not include entries with null dates when filtering by initDate
        List<KardexEntity> result = kardexService.filterKardex(null, null, Date.valueOf("2023-01-01"), null, null, null);
        assertEquals(0, result.size());
    }
}