package com.example.demo.Controllers;

import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Services.ToolStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tool-states")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ToolStateController {

    private final ToolStateService toolStateService;

    @GetMapping("/")
    public List<ToolStateEntity> getAllStates() {
        return toolStateService.getAllStates();
    }

    @PostMapping("/")
    public ToolStateEntity createState(@RequestBody ToolStateEntity toolState) {
        return toolStateService.createState(toolState);
    }

    @PutMapping("/{id}")
    public ToolStateEntity updateState(@PathVariable Long id, @RequestBody ToolStateEntity toolState) {
        return toolStateService.updateState(id, toolState);
    }

    @DeleteMapping("/{id}")
    public boolean deleteState(@PathVariable Long id) {
        return toolStateService.deleteState(id);
    }
}
