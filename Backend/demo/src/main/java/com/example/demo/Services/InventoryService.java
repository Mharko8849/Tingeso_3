package com.example.demo.Services;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.List;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ToolService toolService;

    @Autowired
    private KardexService kardexService;

    @Autowired
    private UserService userService;

    public InventoryEntity saveInventoryEntity(InventoryEntity inventoryEntity) {
        return inventoryRepository.save(inventoryEntity);
    }

    public List<InventoryEntity> getInventoryByTool(ToolEntity toolEntity) {
        return inventoryRepository.findByIdTool(toolEntity);
    }

    public InventoryEntity getAvailableTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "DISPONIBLE");
        if (inv == null) {
            throw new RuntimeException("Inventario DISPONIBLE no encontrado para la herramienta");
        }
        return inv;
    }

    public InventoryEntity getLoanTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "PRESTADA");
        if (inv == null) {
            throw new RuntimeException("Inventario PRESTADA no encontrado para la herramienta");
        }
        return inv;
    }

    public InventoryEntity getReparationTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "EN REPARACION");
        if (inv == null) {
            throw new RuntimeException("Inventario EN REPARACION no encontrado para la herramienta");
        }
        return inv;
    }

    public InventoryEntity getRemovedTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "DADA DE BAJA");
        if (inv == null) {
            throw new RuntimeException("Inventario DADA DE BAJA no encontrado para la herramienta");
        }
        return inv;
    }

    public List<InventoryEntity> getInventoryAscPrice() {
        return inventoryRepository.findAllByOrderByIdTool_PriceRentAsc();
    }

    public List<InventoryEntity> getInventoryDescPrice() {
        return inventoryRepository.findAllByOrderByIdTool_PriceRentDesc();
    }

    public List<InventoryEntity> getMoreRecents(){
        return inventoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<InventoryEntity> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public InventoryEntity getInventoryByIdToolAndToolState(ToolEntity idTool, String toolState) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(idTool, toolState);
        if (inv == null) {
            throw new RuntimeException("Inventario no encontrado para la herramienta con estado: " + toolState);
        }
        return inv;
    }

    public int getToolStock(Long idTool) {
        ToolEntity tool = toolService.getToolById(idTool);
        List<InventoryEntity> inventories = inventoryRepository.findByIdTool(tool);
        return inventories.stream().mapToInt(InventoryEntity::getStockTool).sum();
    }

    public boolean isAvailableTool(ToolEntity toolEntity) {
        return getAvailableTools(toolEntity).getStockTool() >= 1;
    }

    @Transactional
    public void loanTool(Long idTool) {
        ToolEntity tool = toolService.getToolById(idTool);

        InventoryEntity availableTools = getAvailableTools(tool);
        InventoryEntity loanTools = getLoanTools(tool);

        availableTools.setStockTool(availableTools.getStockTool() - 1);
        loanTools.setStockTool(loanTools.getStockTool() + 1);

        inventoryRepository.save(availableTools);
        inventoryRepository.save(loanTools);
    }

    @Transactional
    public void receiveTool(Long idTool, String stateTool) {
        ToolEntity tool = toolService.getToolById(idTool);

        InventoryEntity loanTools = getLoanTools(tool);
        InventoryEntity toolsRecieve = getInventoryByIdToolAndToolState(tool, stateTool);

        loanTools.setStockTool(loanTools.getStockTool() - 1);
        toolsRecieve.setStockTool(toolsRecieve.getStockTool() + 1);

        inventoryRepository.save(toolsRecieve);
        inventoryRepository.save(loanTools);
    }

    @Transactional
    public void repairTool(Long idTool) {
        ToolEntity tool = toolService.getToolById(idTool);

        InventoryEntity availableTools = getAvailableTools(tool);
        InventoryEntity repairsTools = getReparationTools(tool);

        availableTools.setStockTool(availableTools.getStockTool() + 1);
        repairsTools.setStockTool(repairsTools.getStockTool() - 1);

        inventoryRepository.save(availableTools);
        inventoryRepository.save(repairsTools);
    }

    @Transactional
    public InventoryEntity addStockToTool(Long idTool, int quantity, UserEntity employee) {
        userService.isAdmin(employee);

        if (quantity <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor que cero.");
        }

        ToolEntity tool = toolService.getToolById(idTool);
        InventoryEntity available = getAvailableTools(tool);

        available.setStockTool(available.getStockTool() + quantity);
        inventoryRepository.save(available);

    Date actualDate = new Date(System.currentTimeMillis());
    // cost is null for inventory movements; cost is only set for debt payments
    kardexService.createKardexEntity(tool, "INGRESO", actualDate, quantity, null,null,employee);

        return available;
    }

    public List<InventoryEntity> filterInventory(String state, String category, Long idTool,
                                                 Integer minPrice, Integer maxPrice,
                                                 Boolean asc, Boolean desc, Boolean recent, String search) {

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new RuntimeException("El precio mínimo no puede ser mayor que el precio máximo.");
        }



        if (asc != null && asc && desc != null && desc) {
            asc = false;
            desc = false;
        }
        if (asc != null && asc && recent != null && recent) {
            asc = false;
            recent = false;
        }
        if (desc != null && desc && recent != null && recent){
            desc = false;
            recent = false;
        }

        List<InventoryEntity> inventoryList;

        if (recent != null && recent) {
            inventoryList = getMoreRecents();
        }
        else if (asc != null && asc) {
            inventoryList = getInventoryAscPrice();
        } else if (desc != null && desc) {
            inventoryList = getInventoryDescPrice();
        } else {
            inventoryList = inventoryRepository.findAll();
        }

        if (state != null && !state.isBlank()) {
            inventoryList = inventoryList.stream()
                    .filter(inventory -> inventory.getToolState() != null &&
                            inventory.getToolState().getState().equalsIgnoreCase(state))
                    .toList();
        }

        if (category != null && !category.isBlank()) {
            inventoryList = inventoryList.stream()
                    .filter(inventory -> inventory.getIdTool() != null &&
                            inventory.getIdTool().getCategory() != null &&
                            inventory.getIdTool().getCategory().getName().equalsIgnoreCase(category))
                    .toList();
        }

        if (idTool != null) {
            inventoryList = inventoryList.stream()
                    .filter(inventory -> inventory.getIdTool() != null &&
                            inventory.getIdTool().getId().equals(idTool))
                    .toList();
        }

        if (minPrice != null) {
            inventoryList = inventoryList.stream()
                    .filter(inventory -> inventory.getIdTool() != null &&
                            inventory.getIdTool().getPriceRent() >= minPrice)
                    .toList();
        }

        if (maxPrice != null) {
            inventoryList = inventoryList.stream()
                    .filter(inventory -> inventory.getIdTool() != null &&
                            inventory.getIdTool().getPriceRent() <= maxPrice)
                    .toList();
        }

        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            inventoryList = inventoryList.stream()
                    .filter(inventory -> inventory.getIdTool() != null &&
                            inventory.getIdTool().getToolName() != null &&
                            inventory.getIdTool().getToolName().toLowerCase().contains(searchLower))
                    .toList();
        }

        return inventoryList;
    }

    // Check if tool has available stock for loan
    public boolean checkStockAvailable(Long idTool) {
        try {
            ToolEntity tool = toolService.getToolById(idTool);
            InventoryEntity availableInventory = inventoryRepository.findByIdToolAndToolState_State(tool, "DISPONIBLE");
            return availableInventory != null && availableInventory.getStockTool() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
