package com.example.demo.ServiceTest;

import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.InventoryRepository;
import com.example.demo.Services.InventoryService;
import com.example.demo.Services.KardexService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ToolService toolService;

    @Mock
    private KardexService kardexService;

    @Mock
    private UserService userService;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryEntity inventory;
    private ToolEntity tool;
    private UserEntity user;
    private ToolStateEntity disponibleState;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
        tool.setPriceRent(10);

        disponibleState = new ToolStateEntity();
        disponibleState.setId(1L);
        disponibleState.setState("DISPONIBLE");

        inventory = new InventoryEntity();
        inventory.setId(1L);
        inventory.setIdTool(tool);
        inventory.setToolState(disponibleState);
        inventory.setStockTool(10);

        user = new UserEntity();
        user.setId(1L);
        user.setRol("ADMIN");
    }

    @Test
    void testSaveInventoryEntity() {
        when(inventoryRepository.save(any(InventoryEntity.class))).thenReturn(inventory);
        InventoryEntity result = inventoryService.saveInventoryEntity(inventory);
        assertNotNull(result);
    }

    @Test
    void testGetInventoryByTool() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findByIdTool(tool)).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getInventoryByTool(tool);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAvailableTools() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getAvailableTools(tool);
        assertNotNull(result);
    }

    @Test
    void testGetAvailableTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getAvailableTools(tool));
    }

    @Test
    void testGetLoanTools() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "PRESTADA")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getLoanTools(tool);
        assertNotNull(result);
    }

    @Test
    void testGetLoanTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "PRESTADA")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getLoanTools(tool));
    }

    @Test
    void testGetReparationTools() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "EN REPARACION")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getReparationTools(tool);
        assertNotNull(result);
    }

    @Test
    void testGetReparationTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "EN REPARACION")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getReparationTools(tool));
    }

    @Test
    void testGetRemovedTools() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DADA DE BAJA")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getRemovedTools(tool);
        assertNotNull(result);
    }

    @Test
    void testGetRemovedTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DADA DE BAJA")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getRemovedTools(tool));
    }

    @Test
    void testGetInventoryAscPrice() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentAsc()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getInventoryAscPrice();
        assertEquals(1, result.size());
    }

    @Test
    void testGetInventoryDescPrice() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentDesc()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getInventoryDescPrice();
        assertEquals(1, result.size());
    }

    @Test
    void testGetMoreRecents() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAll(any(Sort.class))).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getMoreRecents();
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllInventory() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAll()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getAllInventory();
        assertEquals(1, result.size());
    }

    @Test
    void testGetToolStock() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findByIdTool(tool)).thenReturn(list);
        int result = inventoryService.getToolStock(1L);
        assertEquals(10, result);
    }

    @Test
    void testIsAvailableTool() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);
        boolean result = inventoryService.isAvailableTool(tool);
        assertTrue(result);
    }

    @Test
    void testIsAvailableTool_NoStock() {
        InventoryEntity emptyInventory = new InventoryEntity();
        emptyInventory.setStockTool(0);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(emptyInventory);
        boolean result = inventoryService.isAvailableTool(tool);
        assertFalse(result);
    }

    @Test
    void testLoanTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);
        InventoryEntity loanInv = new InventoryEntity();
        loanInv.setStockTool(0);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "PRESTADA")).thenReturn(loanInv);

        inventoryService.loanTool(1L);

        assertEquals(9, inventory.getStockTool());
        assertEquals(1, loanInv.getStockTool());
        verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));
    }

    @Test
    void testReceiveTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        InventoryEntity loanInv = new InventoryEntity();
        loanInv.setStockTool(1);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "PRESTADA")).thenReturn(loanInv);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);

        inventoryService.receiveTool(1L, "DISPONIBLE");

        assertEquals(0, loanInv.getStockTool());
        assertEquals(11, inventory.getStockTool());
        verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));
    }

    @Test
    void testRepairTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);
        InventoryEntity repairInv = new InventoryEntity();
        repairInv.setStockTool(1);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "EN REPARACION")).thenReturn(repairInv);

        inventoryService.repairTool(1L);

        assertEquals(11, inventory.getStockTool());
        assertEquals(0, repairInv.getStockTool());
        verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));
    }

    @Test
    void testAddStockToTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);

        inventoryService.addStockToTool(1L, 5, user);

        assertEquals(15, inventory.getStockTool());
        verify(userService, times(1)).isAdmin(user);
        verify(inventoryRepository, times(1)).save(inventory);
        verify(kardexService, times(1)).createKardexEntity(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void testGetInventoryByIdToolAndToolState_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "UNKNOWN")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getInventoryByIdToolAndToolState(tool, "UNKNOWN"));
    }

    @Test
    void testAddStockToTool_InvalidQuantity() {
        assertThrows(RuntimeException.class, () -> inventoryService.addStockToTool(1L, 0, user));
        assertThrows(RuntimeException.class, () -> inventoryService.addStockToTool(1L, -1, user));
    }

    @Test
    void testFilterInventory_InvalidPriceRange() {
        assertThrows(RuntimeException.class, () ->
            inventoryService.filterInventory(null, null, null, 100, 50, null, null, null, null));
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, false",
        "true, false, true",
        "false, true, true"
    })
    void testFilterInventory_SortLogic_ConflictingFlags(boolean asc, boolean desc, boolean recent) {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAll()).thenReturn(list);
        // Two sort flags true => both reset to false => normal findAll
        List<InventoryEntity> result = inventoryService.filterInventory(null, null, null, null, null, asc, desc, recent, null);
        assertNotNull(result);
    }

    @Test
    void testFilterInventory_SortLogic_RecentOnly() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAll(any(Sort.class))).thenReturn(list);
        List<InventoryEntity> result = inventoryService.filterInventory(null, null, null, null, null, false, false, true, null);
        assertEquals(1, result.size());
    }

    @Test
    void testFilterInventory_SortLogic_AscOnly() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentAsc()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.filterInventory(null, null, null, null, null, true, false, false, null);
        assertEquals(1, result.size());
    }

    @Test
    void testFilterInventory_SortLogic_DescOnly() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentDesc()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.filterInventory(null, null, null, null, null, false, true, false, null);
        assertEquals(1, result.size());
    }

    @Test
    void testFilterInventory_Filters() {
        CategoryEntity cat1 = new CategoryEntity();
        cat1.setId(1L);
        cat1.setName("CAT1");
        CategoryEntity cat2 = new CategoryEntity();
        cat2.setId(2L);
        cat2.setName("CAT2");

        ToolStateEntity stateDisponible = new ToolStateEntity();
        stateDisponible.setId(1L);
        stateDisponible.setState("DISPONIBLE");
        ToolStateEntity statePrestada = new ToolStateEntity();
        statePrestada.setId(2L);
        statePrestada.setState("PRESTADA");

        InventoryEntity inv1 = new InventoryEntity();
        inv1.setToolState(stateDisponible);
        ToolEntity t1 = new ToolEntity();
        t1.setId(1L);
        t1.setCategory(cat1);
        t1.setPriceRent(100);
        t1.setToolName("Hammer");
        inv1.setIdTool(t1);

        InventoryEntity inv2 = new InventoryEntity();
        inv2.setToolState(statePrestada);
        ToolEntity t2 = new ToolEntity();
        t2.setId(2L);
        t2.setCategory(cat2);
        t2.setPriceRent(200);
        t2.setToolName("Drill");
        inv2.setIdTool(t2);

        List<InventoryEntity> list = List.of(inv1, inv2);
        when(inventoryRepository.findAll()).thenReturn(list);

        // Filter by State
        assertEquals(1, inventoryService.filterInventory("DISPONIBLE", null, null, null, null, null, null, null, null).size());
        // Filter by Category
        assertEquals(1, inventoryService.filterInventory(null, "CAT1", null, null, null, null, null, null, null).size());
        // Filter by ID
        assertEquals(1, inventoryService.filterInventory(null, null, 1L, null, null, null, null, null, null).size());
        // Filter by Min Price
        assertEquals(1, inventoryService.filterInventory(null, null, null, 150, null, null, null, null, null).size());
        // Filter by Max Price
        assertEquals(1, inventoryService.filterInventory(null, null, null, null, 150, null, null, null, null).size());
        // Filter by search
        assertEquals(1, inventoryService.filterInventory(null, null, null, null, null, null, null, null, "Hamm").size());
    }

    @Test
    void testCheckStockAvailable_WithStock() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);
        assertTrue(inventoryService.checkStockAvailable(1L));
    }

    @Test
    void testCheckStockAvailable_NoStock() {
        inventory.setStockTool(0);
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(inventory);
        assertFalse(inventoryService.checkStockAvailable(1L));
    }

    @Test
    void testCheckStockAvailable_NullInventory() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE")).thenReturn(null);
        assertFalse(inventoryService.checkStockAvailable(1L));
    }

    @Test
    void testCheckStockAvailable_Exception() {
        when(toolService.getToolById(1L)).thenThrow(new IllegalStateException("Tool not found"));
        assertFalse(inventoryService.checkStockAvailable(1L));
    }
}
