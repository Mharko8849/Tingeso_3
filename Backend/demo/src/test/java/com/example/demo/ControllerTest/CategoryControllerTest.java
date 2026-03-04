package com.example.demo.ControllerTest;

import com.example.demo.Controllers.CategoryController;
import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Services.CategoryService;
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

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private CategoryEntity cat;

    @BeforeEach
    void setUp() {
        cat = new CategoryEntity();
        cat.setId(1L);
        cat.setName("Construction");
    }

    @Test
    void testGetAllCategories() throws Exception {
        List<CategoryEntity> list = Arrays.asList(cat);
        when(categoryService.getAllCategories()).thenReturn(list);
        mockMvc.perform(get("/categories/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testCreateCategory() throws Exception {
        when(categoryService.createCategory(any(CategoryEntity.class))).thenReturn(cat);
        mockMvc.perform(post("/categories/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Construction"));
    }

    @Test
    void testUpdateCategory() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryEntity.class))).thenReturn(cat);
        mockMvc.perform(put("/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testUpdateCategory_NotFound() throws Exception {
        when(categoryService.updateCategory(eq(99L), any(CategoryEntity.class))).thenReturn(null);
        mockMvc.perform(put("/categories/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteCategory_Success() throws Exception {
        when(categoryService.deleteCategory(1L)).thenReturn(true);
        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCategory_NotFound() throws Exception {
        when(categoryService.deleteCategory(99L)).thenReturn(false);
        mockMvc.perform(delete("/categories/99"))
                .andExpect(status().isNotFound());
    }
}
