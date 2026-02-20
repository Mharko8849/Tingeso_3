package com.example.demo.Controllers;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.InventoryService;
import com.example.demo.Services.ToolService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@CrossOrigin("*")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private ToolService toolService;

    /*
     GET
     */

    @GetMapping("/")
    public ResponseEntity<List<InventoryEntity>> getAllInventory() {
        List<InventoryEntity> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<InventoryEntity>> filterInventory(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long idTool,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean asc,
            @RequestParam(required = false) Boolean desc,
            @RequestParam(required = false) Boolean recent,
            @RequestParam(required = false) String search) {

        List<InventoryEntity> inventory = inventoryService.filterInventory(
                state, category, idTool, minPrice, maxPrice, asc, desc, recent, search
        );

        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/check-stock/{idTool}")
    public ResponseEntity<Boolean> checkStockAvailable(@PathVariable Long idTool) {
        boolean hasStock = inventoryService.checkStockAvailable(idTool);
        return ResponseEntity.ok(hasStock);
    }

    /*
     POST
     */

    @PostMapping("/add-stock/{idUser}/{idTool}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<InventoryEntity> addStock(
            @PathVariable Long idUser,
            @PathVariable Long idTool,
            @RequestParam int quantity) {

        UserEntity user = userService.findUserById(idUser);
        InventoryEntity inventory = inventoryService.addStockToTool(idTool, quantity, user);
        return ResponseEntity.ok(inventory);
    }

}
