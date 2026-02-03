package com.example.demo.ServiceTest;

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
import org.springframework.data.domain.Sort;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class KardexServiceTest {

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
    public void setUp() {
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
    public void testSaveKardexEntity() {
        when(kardexRepository.save(any(KardexEntity.class))).thenReturn(kardex);
        KardexEntity result = kardexService.saveKardexEntity(kardex);
        assertNotNull(result);
    }

    @Test
    public void testCreateKardexEntity() {
        when(kardexRepository.save(any(KardexEntity.class))).thenReturn(kardex);
        KardexEntity result = kardexService.createKardexEntity(tool, "IN", Date.valueOf("2023-01-01"), 10, 100, user, employee);
        assertNotNull(result);
    }

    @Test
    public void testCreateKardexEntity_MissingData() {
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
    public void testGetKardexByDateBetween() {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findByDateBetween(any(Date.class), any(Date.class))).thenReturn(list);
        
        List<KardexEntity> result = kardexService.getKardexByDateBetween(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-31"));
        assertEquals(1, result.size());
    }

    @Test
    public void testGetKardexByDateBetween_NullDates() {
        assertThrows(RuntimeException.class, () -> kardexService.getKardexByDateBetween(null, Date.valueOf("2023-01-31")));
        assertThrows(RuntimeException.class, () -> kardexService.getKardexByDateBetween(Date.valueOf("2023-01-01"), null));
    }

    @Test
    public void testFilterKardex_AllFilters() {
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
    public void testGetRankingToolsByDateRange() {
        kardex.setType("PRESTAMO");
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findByDateBetween(any(Date.class), any(Date.class))).thenReturn(list);

        List<Map<String, Object>> result = kardexService.getRankingToolsByDateRange(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-31"));
        assertEquals(1, result.size());
        assertEquals(tool, result.get(0).get("tool"));
    }

    @Test
    public void testGetRankingToolsByDateRange_NullDates() {
        // Should fallback to getRankingTools() which uses current month
        kardex.setType("PRESTAMO");
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findByDateBetween(any(Date.class), any(Date.class))).thenReturn(list);
        when(toolRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = kardexService.getRankingToolsByDateRange(null, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetAllKardex() {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findAll()).thenReturn(list);
        List<KardexEntity> result = kardexService.getAllKardex();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetKardexById() {
        when(kardexRepository.findById(1L)).thenReturn(Optional.of(kardex));
        KardexEntity result = kardexService.getKardexById(1L);
        assertNotNull(result);
    }

    @Test
    public void testGetKardexById_NotFound() {
        when(kardexRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            kardexService.getKardexById(1L);
        });
    }

    @Test
    public void testFilterKardex() {
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findAll(any(Sort.class))).thenReturn(list);
        List<KardexEntity> result = kardexService.filterKardex(null, "IN", null, null, null, null);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetRankingTools() {
        kardex.setType("PRESTAMO");
        List<KardexEntity> list = new ArrayList<>();
        list.add(kardex);
        when(kardexRepository.findByDateBetween(any(Date.class), any(Date.class))).thenReturn(list);
        when(toolRepository.findAll()).thenReturn(new ArrayList<>());

        List<Map<String, Object>> result = kardexService.getRankingTools();
        assertEquals(1, result.size());
        assertEquals(tool, result.get(0).get("tool"));
    }
}
