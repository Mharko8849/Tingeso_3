package com.example.demo.ServiceTest;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.InventoryRepository;
import com.example.demo.Services.InventoryService;
import com.example.demo.Services.KardexService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InventoryServiceTest {

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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
        tool.setPriceRent(10);

        inventory = new InventoryEntity();
        inventory.setId(1L);
        inventory.setIdTool(tool);
        inventory.setToolState("DISPONIBLE");
        inventory.setStockTool(10);

        user = new UserEntity();
        user.setId(1L);
        user.setRol("ADMIN");
    }

    @Test
    public void testSaveInventoryEntity() {
        when(inventoryRepository.save(any(InventoryEntity.class))).thenReturn(inventory);
        InventoryEntity result = inventoryService.saveInventoryEntity(inventory);
        assertNotNull(result);
    }

    @Test
    public void testGetInventoryByTool() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findByIdTool(tool)).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getInventoryByTool(tool);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetAvailableTools() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getAvailableTools(tool);
        assertNotNull(result);
    }

    @Test
    public void testGetAvailableTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> {
            inventoryService.getAvailableTools(tool);
        });
    }

    @Test
    public void testGetLoanTools() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "PRESTADA")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getLoanTools(tool);
        assertNotNull(result);
    }

    @Test
    public void testGetReparationTools() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "EN REPARACION")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getReparationTools(tool);
        assertNotNull(result);
    }

    @Test
    public void testGetRemovedTools() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "DADA DE BAJA")).thenReturn(inventory);
        InventoryEntity result = inventoryService.getRemovedTools(tool);
        assertNotNull(result);
    }

    @Test
    public void testGetInventoryAscPrice() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentAsc()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getInventoryAscPrice();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetInventoryDescPrice() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentDesc()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getInventoryDescPrice();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetMoreRecents() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAll(any(Sort.class))).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getMoreRecents();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetAllInventory() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findAll()).thenReturn(list);
        List<InventoryEntity> result = inventoryService.getAllInventory();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetToolStock() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        when(inventoryRepository.findByIdTool(tool)).thenReturn(list);
        int result = inventoryService.getToolStock(1L);
        assertEquals(10, result);
    }

    @Test
    public void testIsAvailableTool() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(inventory);
        boolean result = inventoryService.isAvailableTool(tool);
        assertTrue(result);
    }

    @Test
    public void testLoanTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(inventory);
        InventoryEntity loanInv = new InventoryEntity();
        loanInv.setStockTool(0);
        when(inventoryRepository.findByIdToolAndToolState(tool, "PRESTADA")).thenReturn(loanInv);

        inventoryService.loanTool(1L);

        assertEquals(9, inventory.getStockTool());
        assertEquals(1, loanInv.getStockTool());
        verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));
    }

    @Test
    public void testReceiveTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        InventoryEntity loanInv = new InventoryEntity();
        loanInv.setStockTool(1);
        when(inventoryRepository.findByIdToolAndToolState(tool, "PRESTADA")).thenReturn(loanInv);
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(inventory);

        inventoryService.receiveTool(1L, "DISPONIBLE");

        assertEquals(0, loanInv.getStockTool());
        assertEquals(11, inventory.getStockTool());
        verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));
    }

    @Test
    public void testRepairTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(inventory);
        InventoryEntity repairInv = new InventoryEntity();
        repairInv.setStockTool(1);
        when(inventoryRepository.findByIdToolAndToolState(tool, "EN REPARACION")).thenReturn(repairInv);

        inventoryService.repairTool(1L);

        assertEquals(11, inventory.getStockTool());
        assertEquals(0, repairInv.getStockTool());
        verify(inventoryRepository, times(2)).save(any(InventoryEntity.class));
    }

    @Test
    public void testAddStockToTool() {
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryRepository.findByIdToolAndToolState(tool, "DISPONIBLE")).thenReturn(inventory);

        inventoryService.addStockToTool(1L, 5, user);

        assertEquals(15, inventory.getStockTool());
        verify(userService, times(1)).isAdmin(user);
        verify(inventoryRepository, times(1)).save(inventory);
        verify(kardexService, times(1)).createKardexEntity(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    public void testGetLoanTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "PRESTADA")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getLoanTools(tool));
    }

    @Test
    public void testGetReparationTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "EN REPARACION")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getReparationTools(tool));
    }

    @Test
    public void testGetRemovedTools_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "DADA DE BAJA")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getRemovedTools(tool));
    }

    @Test
    public void testGetInventoryByIdToolAndToolState_NotFound() {
        when(inventoryRepository.findByIdToolAndToolState(tool, "UNKNOWN")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> inventoryService.getInventoryByIdToolAndToolState(tool, "UNKNOWN"));
    }

    @Test
    public void testAddStockToTool_InvalidQuantity() {
        assertThrows(RuntimeException.class, () -> inventoryService.addStockToTool(1L, 0, user));
        assertThrows(RuntimeException.class, () -> inventoryService.addStockToTool(1L, -1, user));
    }

    @Test
    public void testFilterInventory_InvalidPriceRange() {
        assertThrows(RuntimeException.class, () -> 
            inventoryService.filterInventory(null, null, null, 100, 50, null, null, null));
    }

    @Test
    public void testFilterInventory_SortLogic() {
        List<InventoryEntity> list = new ArrayList<>();
        list.add(inventory);
        
        // Case 1: Recent=true overrides others
        when(inventoryRepository.findAll(any(Sort.class))).thenReturn(list);
        inventoryService.filterInventory(null, null, null, null, null, true, true, true);
        verify(inventoryRepository, times(1)).findAll(any(Sort.class)); // getMoreRecents

        // Case 2: Asc=true
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentAsc()).thenReturn(list);
        inventoryService.filterInventory(null, null, null, null, null, true, false, false);
        verify(inventoryRepository, times(1)).findAllByOrderByIdTool_PriceRentAsc();

        // Case 3: Desc=true
        when(inventoryRepository.findAllByOrderByIdTool_PriceRentDesc()).thenReturn(list);
        inventoryService.filterInventory(null, null, null, null, null, false, true, false);
        verify(inventoryRepository, times(1)).findAllByOrderByIdTool_PriceRentDesc();
    }

    @Test
    public void testFilterInventory_Filters() {
        InventoryEntity inv1 = new InventoryEntity();
        inv1.setToolState("DISPONIBLE");
        ToolEntity t1 = new ToolEntity();
        t1.setId(1L);
        t1.setCategory("CAT1");
        t1.setPriceRent(100);
        inv1.setIdTool(t1);

        InventoryEntity inv2 = new InventoryEntity();
        inv2.setToolState("PRESTADA");
        ToolEntity t2 = new ToolEntity();
        t2.setId(2L);
        t2.setCategory("CAT2");
        t2.setPriceRent(200);
        inv2.setIdTool(t2);

        List<InventoryEntity> list = List.of(inv1, inv2);
        when(inventoryRepository.findAll()).thenReturn(list);

        // Filter by State
        assertEquals(1, inventoryService.filterInventory("DISPONIBLE", null, null, null, null, null, null, null).size());

        // Filter by Category
        assertEquals(1, inventoryService.filterInventory(null, "CAT1", null, null, null, null, null, null).size());

        // Filter by ID
        assertEquals(1, inventoryService.filterInventory(null, null, 1L, null, null, null, null, null).size());

        // Filter by Min Price
        assertEquals(1, inventoryService.filterInventory(null, null, null, 150, null, null, null, null).size());

        // Filter by Max Price
        assertEquals(1, inventoryService.filterInventory(null, null, null, null, 150, null, null, null).size());
    }
}
