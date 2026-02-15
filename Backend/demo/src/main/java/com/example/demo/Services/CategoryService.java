package com.example.demo.Services;

import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    CategoryRepository categoryRepository;

    public List<CategoryEntity> getAllCategories() {
        // Ordenar por ID descendente (más reciente primero)
        return categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(CategoryEntity::getId).reversed())
                .collect(Collectors.toList());
    }

    public CategoryEntity createCategory(CategoryEntity category) {
        if (categoryRepository.findByName(category.getName()) != null) {
            return categoryRepository.findByName(category.getName());
        }
        return categoryRepository.save(category);
    }
    
    public CategoryEntity findByName(String name) {
        return categoryRepository.findByName(name);
    }

    public CategoryEntity updateCategory(Long id, CategoryEntity category) {
        CategoryEntity existing = categoryRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setName(category.getName());
        return categoryRepository.save(existing);
    }

    public boolean deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            return false;
        }
        categoryRepository.deleteById(id);
        return true;
    }
}
