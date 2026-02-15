package com.example.demo.Services;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Repositories.InventoryRepository;
import com.example.demo.Repositories.ToolRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ToolStateService toolStateService;

    @Autowired
    private CategoryService categoryService;

    public ArrayList<ToolEntity> getAllTools() {
        return (ArrayList<ToolEntity>) toolRepository.findAll();
    }

    public ToolEntity getToolById(Long id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la herramienta."));
    }

    @Transactional
    public ToolEntity createTool(UserEntity user, ToolEntity toolEntity, MultipartFile image) {
        userService.isAdmin(user);

        ArrayList<String> errors = new ArrayList<>();
        if (toolEntity.getToolName() == null || toolEntity.getToolName().isBlank()) {
            errors.add("Debe ingresar un nombre para la herramienta.");
        }
        if (toolEntity.getCategory() == null) {
            errors.add("Debe ingresar una categoría para la herramienta.");
        }
        if (toolEntity.getRepoCost() <= 0) {
            errors.add("Debe ingresar un costo para la herramienta. (Mayor que 0)");
        }
        if (toolEntity.getPriceRent() <= 0) {
            errors.add("Debe ingresar un precio para la renta de la herramienta. (Mayor a 0)");
        }
        if (toolEntity.getPriceFineAtDate() <= 0) {
            errors.add("Debe ingresar el precio de multa para la herramienta. (Mayor a 0)");
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join(" ", errors));
        }

        if (toolEntity.getCategory() != null) {
            toolEntity.setCategory(categoryService.createCategory(toolEntity.getCategory()));
        }

        String imageName = fileStorageService.saveFile(image);
        toolEntity.setImageUrl(imageName);

        ToolEntity savedTool = toolRepository.save(toolEntity);

        List<ToolStateEntity> allStates = toolStateService.getAllStates();
        for (ToolStateEntity state : allStates) {
            InventoryEntity inv = new InventoryEntity();
            inv.setIdTool(savedTool);
            inv.setToolState(state);
            inv.setStockTool(0);
            inventoryRepository.save(inv);
        }

        return savedTool;
    }


    public ToolEntity updateTool(Long idUser, Long idTool, ToolEntity toolUpdate, MultipartFile image) {
        UserEntity user = userService.findUserById(idUser);
        userService.isAdmin(user);
        ToolEntity tool = getToolById(idTool);

        if (toolUpdate.getToolName() != null && !toolUpdate.getToolName().isBlank()) {
            tool.setToolName(toolUpdate.getToolName());
        }
        if (toolUpdate.getCategory() != null) {
            tool.setCategory(categoryService.createCategory(toolUpdate.getCategory()));
        }
        if (toolUpdate.getRepoCost() > 0) {
            tool.setRepoCost(toolUpdate.getRepoCost());
        }
        if (toolUpdate.getPriceRent() > 0) {
            tool.setPriceRent(toolUpdate.getPriceRent());
        }
        if (toolUpdate.getPriceFineAtDate() > 0) {
            tool.setPriceFineAtDate(toolUpdate.getPriceFineAtDate());
        }

        if (image != null && !image.isEmpty()) {

            // 1. borrar imagen anterior si existe
            if (tool.getImageUrl() != null) {
                try {
                    Files.deleteIfExists(Paths.get("images", tool.getImageUrl()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            String fileName = fileStorageService.saveFile(image);
            tool.setImageUrl(fileName);
        }

        return toolRepository.save(tool);
    }

    public boolean deleteToolById(Long id) {
        try {
            toolRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
