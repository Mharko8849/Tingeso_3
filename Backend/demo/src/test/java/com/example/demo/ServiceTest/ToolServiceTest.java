package com.example.demo.ServiceTest;

import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Repositories.InventoryRepository;
import com.example.demo.Repositories.ToolRepository;
import com.example.demo.Services.CategoryService;
import com.example.demo.Services.FileStorageService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.ToolStateService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private UserService userService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ToolStateService toolStateService;

    @InjectMocks
    private ToolService toolService;

    private ToolEntity tool;
    private UserEntity user;
    private CategoryEntity category;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        category = new CategoryEntity();
        category.setName("Construction");

        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
        tool.setCategory(category);
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);

        user = new UserEntity();
        user.setId(1L);
        user.setRol("ADMIN");
        
        when(toolStateService.getAllStates()).thenReturn(new ArrayList<>());
    }

    @Test
    public void testGetAllTools() {
        ArrayList<ToolEntity> tools = new ArrayList<>();
        tools.add(tool);
        when(toolRepository.findAll()).thenReturn(tools);
        ArrayList<ToolEntity> result = toolService.getAllTools();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetToolById() {
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        ToolEntity result = toolService.getToolById(1L);
        assertNotNull(result);
        assertEquals(tool.getId(), result.getId());
    }

    @Test
    public void testGetToolById_NotFound() {
        when(toolRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            toolService.getToolById(1L);
        });
    }

    @Test
    public void testCreateTool() {
        MultipartFile image = mock(MultipartFile.class);
        when(fileStorageService.saveFile(image)).thenReturn("image.jpg");
        when(categoryService.createCategory(any(CategoryEntity.class))).thenReturn(category);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(tool);
        when(toolStateService.getAllStates()).thenReturn(Collections.singletonList(new ToolStateEntity()));

        ToolEntity result = toolService.createTool(user, tool, image);

        assertNotNull(result);
        verify(userService, times(1)).isAdmin(user);
        verify(inventoryRepository, atLeastOnce()).save(any(InventoryEntity.class));
    }

    @Test
    public void testCreateTool_InvalidData() {
        MultipartFile image = mock(MultipartFile.class);
        
        ToolEntity invalidTool = new ToolEntity();
        // Missing name and other fields should throw
        assertThrows(RuntimeException.class, () -> toolService.createTool(user, invalidTool, image));

        invalidTool.setToolName("Name");
        // Missing category
        assertThrows(RuntimeException.class, () -> toolService.createTool(user, invalidTool, image));

        invalidTool.setCategory(category);
        // Missing costs
        invalidTool.setRepoCost(0);
        assertThrows(RuntimeException.class, () -> toolService.createTool(user, invalidTool, image));
    }

    @Test
    public void testUpdateTool() {
        MultipartFile image = mock(MultipartFile.class);
        when(userService.findUserById(1L)).thenReturn(user);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(fileStorageService.saveFile(image)).thenReturn("new_image.jpg");
        when(categoryService.createCategory(any(CategoryEntity.class))).thenReturn(category);
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(tool);

        ToolEntity updateTool = new ToolEntity();
        updateTool.setToolName("New Name");
        updateTool.setCategory(category);
        updateTool.setRepoCost(200);
        updateTool.setPriceRent(20);
        updateTool.setPriceFineAtDate(10);

        ToolEntity result = toolService.updateTool(1L, 1L, updateTool, image);

        assertNotNull(result);
        // verify setters called on 'tool' - effectively verifying logic
        // Since we return the mock save result (which is the original 'tool'), we can check if 'tool' was mutated
        assertEquals("New Name", tool.getToolName()); 
        verify(userService, times(1)).isAdmin(user);
    }

    @Test
    public void testUpdateTool_PartialUpdate() {
        when(userService.findUserById(1L)).thenReturn(user);
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(ToolEntity.class))).thenReturn(tool);

        ToolEntity updateTool = new ToolEntity();
        // No fields set

        ToolEntity result = toolService.updateTool(1L, 1L, updateTool, null);

        assertNotNull(result);
        assertEquals("Hammer", result.getToolName()); // Should remain unchanged
        verify(fileStorageService, never()).saveFile(any());
    }

    @Test
    public void testDeleteToolById_Exception() {
        doThrow(new RuntimeException()).when(toolRepository).deleteById(1L);
        boolean result = toolService.deleteToolById(1L);
        assertFalse(result);
    }
}
