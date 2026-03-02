package com.example.demo.ServiceTest;

import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Repositories.CategoryRepository;
import com.example.demo.Services.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryEntity cat1;
    private CategoryEntity cat2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cat1 = new CategoryEntity();
        cat1.setId(1L);
        cat1.setName("Construction");
        cat2 = new CategoryEntity();
        cat2.setId(2L);
        cat2.setName("Gardening");
    }

    @Test
    void testGetAllCategories() {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(cat1, cat2));
        List<CategoryEntity> result = categoryService.getAllCategories();
        assertEquals(2, result.size());
        // Sorted by ID descending
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
    }

    @Test
    void testCreateCategory_New() {
        when(categoryRepository.findByName("Construction")).thenReturn(null);
        when(categoryRepository.save(any(CategoryEntity.class))).thenReturn(cat1);
        CategoryEntity result = categoryService.createCategory(cat1);
        assertEquals("Construction", result.getName());
        verify(categoryRepository, times(1)).save(cat1);
    }

    @Test
    void testCreateCategory_AlreadyExists() {
        when(categoryRepository.findByName("Construction")).thenReturn(cat1);
        CategoryEntity result = categoryService.createCategory(cat1);
        assertEquals(cat1, result);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testFindByName() {
        when(categoryRepository.findByName("Construction")).thenReturn(cat1);
        CategoryEntity result = categoryService.findByName("Construction");
        assertEquals(cat1, result);
    }

    @Test
    void testFindByName_NotFound() {
        when(categoryRepository.findByName("Unknown")).thenReturn(null);
        assertNull(categoryService.findByName("Unknown"));
    }

    @Test
    void testUpdateCategory() {
        CategoryEntity update = new CategoryEntity();
        update.setName("Updated");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
        when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(i -> i.getArgument(0));
        CategoryEntity result = categoryService.updateCategory(1L, update);
        assertEquals("Updated", result.getName());
    }

    @Test
    void testUpdateCategory_NotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        CategoryEntity result = categoryService.updateCategory(99L, cat1);
        assertNull(result);
    }

    @Test
    void testDeleteCategory_Success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(1L);
        boolean result = categoryService.deleteCategory(1L);
        assertTrue(result);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void testDeleteCategory_NotFound() {
        when(categoryRepository.existsById(99L)).thenReturn(false);
        boolean result = categoryService.deleteCategory(99L);
        assertFalse(result);
        verify(categoryRepository, never()).deleteById(any());
    }
}
