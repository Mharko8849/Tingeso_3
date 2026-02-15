package com.example.demo.Services;

import com.example.demo.Entities.ToolStateEntity;
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

    public List<ToolStateEntity> getAllStates() {
        // Ordenar por ID descendente (más reciente primero)
        return toolStateRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(ToolStateEntity::getId).reversed())
                .collect(Collectors.toList());
    }

    public ToolStateEntity createState(ToolStateEntity toolState) {
        if (toolStateRepository.findByState(toolState.getState()) != null) {
            return toolStateRepository.findByState(toolState.getState());
        }
        return toolStateRepository.save(toolState);
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
