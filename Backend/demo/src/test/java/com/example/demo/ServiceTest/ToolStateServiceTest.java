package com.example.demo.ServiceTest;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Repositories.InventoryRepository;
import com.example.demo.Repositories.ToolRepository;
import com.example.demo.Repositories.ToolStateRepository;
import com.example.demo.Services.ToolStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ToolStateServiceTest {

    @Mock
    private ToolStateRepository toolStateRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ToolStateService toolStateService;

    private ToolStateEntity state1;
    private ToolStateEntity state2;
    private ToolEntity tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        state1 = new ToolStateEntity();
        state1.setId(1L);
        state1.setState("DISPONIBLE");
        state2 = new ToolStateEntity();
        state2.setId(2L);
        state2.setState("PRESTADO");
        tool = new ToolEntity();
        tool.setId(10L);
        tool.setToolName("Hammer");
    }

    @Test
    void testGetAllStates() {
        when(toolStateRepository.findAll()).thenReturn(Arrays.asList(state2, state1));
        List<ToolStateEntity> result = toolStateService.getAllStates();
        assertEquals(2, result.size());
        // Sorted by ID ascending
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void testCreateState_AlreadyExists() {
        when(toolStateRepository.findByState("DISPONIBLE")).thenReturn(state1);
        ToolStateEntity result = toolStateService.createState(state1);
        assertEquals(state1, result);
        verify(toolStateRepository, never()).save(any());
    }

    @Test
    void testCreateState_New_NoExistingTools() {
        when(toolStateRepository.findByState("NEW_STATE")).thenReturn(null);
        ToolStateEntity newState = new ToolStateEntity();
        newState.setId(3L);
        newState.setState("NEW_STATE");
        when(toolStateRepository.save(newState)).thenReturn(newState);
        when(toolRepository.findAll()).thenReturn(Collections.emptyList());
        when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());

        ToolStateEntity result = toolStateService.createState(newState);
        assertEquals("NEW_STATE", result.getState());
    }

    @Test
    void testCreateState_New_WithTools_CreatesInventory() {
        ToolStateEntity newState = new ToolStateEntity();
        newState.setId(3L);
        newState.setState("MANTENIMIENTO");
        when(toolStateRepository.findByState("MANTENIMIENTO")).thenReturn(null);
        when(toolStateRepository.save(newState)).thenReturn(newState);
        when(toolRepository.findAll()).thenReturn(Arrays.asList(tool));
        // No existing inventory for this combination
        when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(inventoryRepository.save(any(InventoryEntity.class))).thenAnswer(i -> i.getArgument(0));

        ToolStateEntity result = toolStateService.createState(newState);
        assertEquals("MANTENIMIENTO", result.getState());
        verify(inventoryRepository, times(1)).save(any(InventoryEntity.class));
    }

    @Test
    void testCreateState_New_WithTools_InventoryAlreadyExists() {
        ToolStateEntity newState = new ToolStateEntity();
        newState.setId(3L);
        newState.setState("MANTENIMIENTO");
        when(toolStateRepository.findByState("MANTENIMIENTO")).thenReturn(null);
        when(toolStateRepository.save(newState)).thenReturn(newState);
        when(toolRepository.findAll()).thenReturn(Arrays.asList(tool));
        // Existing inventory already for this tool+state
        InventoryEntity existingInv = new InventoryEntity();
        existingInv.setIdTool(tool);
        existingInv.setToolState(newState);
        existingInv.setStockTool(5);
        when(inventoryRepository.findAll()).thenReturn(Arrays.asList(existingInv));

        toolStateService.createState(newState);
        // Should NOT create another inventory record
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void testFindByState() {
        when(toolStateRepository.findByState("DISPONIBLE")).thenReturn(state1);
        assertEquals(state1, toolStateService.findByState("DISPONIBLE"));
    }

    @Test
    void testFindByState_NotFound() {
        when(toolStateRepository.findByState("XYZ")).thenReturn(null);
        assertNull(toolStateService.findByState("XYZ"));
    }

    @Test
    void testUpdateState() {
        ToolStateEntity update = new ToolStateEntity();
        update.setState("UPDATED");
        update.setColor("#FF0000");
        when(toolStateRepository.findById(1L)).thenReturn(Optional.of(state1));
        when(toolStateRepository.save(any(ToolStateEntity.class))).thenAnswer(i -> i.getArgument(0));
        ToolStateEntity result = toolStateService.updateState(1L, update);
        assertEquals("UPDATED", result.getState());
        assertEquals("#FF0000", result.getColor());
    }

    @Test
    void testUpdateState_NoColor() {
        ToolStateEntity update = new ToolStateEntity();
        update.setState("UPDATED");
        update.setColor(null);
        when(toolStateRepository.findById(1L)).thenReturn(Optional.of(state1));
        when(toolStateRepository.save(any(ToolStateEntity.class))).thenAnswer(i -> i.getArgument(0));
        ToolStateEntity result = toolStateService.updateState(1L, update);
        assertEquals("UPDATED", result.getState());
    }

    @Test
    void testUpdateState_NotFound() {
        when(toolStateRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> toolStateService.updateState(99L, state1));
    }

    @Test
    void testDeleteState_Success() {
        when(toolStateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(toolStateRepository).deleteById(1L);
        assertTrue(toolStateService.deleteState(1L));
    }

    @Test
    void testDeleteState_NotFound() {
        when(toolStateRepository.existsById(99L)).thenReturn(false);
        assertFalse(toolStateService.deleteState(99L));
    }
}
