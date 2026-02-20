package com.example.demo.Services;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Repositories.InventoryRepository;
import com.example.demo.Repositories.ToolRepository;
import com.example.demo.Repositories.ToolStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToolStateService {

    @Autowired
    ToolStateRepository toolStateRepository;

    @Autowired
    ToolRepository toolRepository;

    @Autowired
    InventoryRepository inventoryRepository;

    public List<ToolStateEntity> getAllStates() {
        // Ordenar por ID ascendente
        return toolStateRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(ToolStateEntity::getId))
                .collect(Collectors.toList());
    }

    public ToolStateEntity createState(ToolStateEntity toolState) {
        // Check if state already exists
        ToolStateEntity existingState = toolStateRepository.findByState(toolState.getState());
        if (existingState != null) {
            return existingState;
        }

        // Save new state
        ToolStateEntity savedState = toolStateRepository.save(toolState);

        // Create inventory records for ALL existing tools with stock=0
        List<ToolEntity> allTools = toolRepository.findAll();
        for (ToolEntity tool : allTools) {
            // Check if inventory record already exists for this tool-state combination
            boolean exists = inventoryRepository.findAll().stream()
                .anyMatch(inv -> 
                    inv.getIdTool() != null && inv.getIdTool().getId().equals(tool.getId()) &&
                    inv.getToolState() != null && inv.getToolState().getId().equals(savedState.getId())
                );
            
            if (!exists) {
                InventoryEntity inv = new InventoryEntity();
                inv.setIdTool(tool);
                inv.setToolState(savedState);
                inv.setStockTool(0);
                inventoryRepository.save(inv);
            }
        }

        return savedState;
    }
    
    public ToolStateEntity findByState(String state) {
        return toolStateRepository.findByState(state);
    }

    public ToolStateEntity updateState(Long id, ToolStateEntity updatedState) {
        ToolStateEntity existingState = toolStateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
        existingState.setState(updatedState.getState());
        if (updatedState.getColor() != null) {
            existingState.setColor(updatedState.getColor());
        }
        return toolStateRepository.save(existingState);
    }

    public boolean deleteState(Long id) {
        if (toolStateRepository.existsById(id)) {
            toolStateRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
