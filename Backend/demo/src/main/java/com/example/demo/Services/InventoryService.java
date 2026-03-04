package com.example.demo.Services;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ToolService toolService;
    private final KardexService kardexService;
    private final UserService userService;

    public InventoryEntity saveInventoryEntity(InventoryEntity inventoryEntity) {
        return inventoryRepository.save(inventoryEntity);
    }

    public List<InventoryEntity> getInventoryByTool(ToolEntity toolEntity) {
        return inventoryRepository.findByIdTool(toolEntity);
    }

    public InventoryEntity getAvailableTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "DISPONIBLE");
        if (inv == null) {
            throw new IllegalStateException("Inventario DISPONIBLE no encontrado para la herramienta");
        }
        return inv;
    }

    public InventoryEntity getLoanTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "PRESTADA");
        if (inv == null) {
            throw new IllegalStateException("Inventario PRESTADA no encontrado para la herramienta");
        }
        return inv;
    }

    public InventoryEntity getReparationTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "EN REPARACION");
        if (inv == null) {
            throw new IllegalStateException("Inventario EN REPARACION no encontrado para la herramienta");
        }
        return inv;
    }

    public InventoryEntity getRemovedTools(ToolEntity toolEntity) {
        InventoryEntity inv = inventoryRepository.findByIdToolAndToolState_State(toolEntity, "DADA DE BAJA");
        if (inv == null) {
            throw new IllegalStateException("Inventario DADA DE BAJA no encontrado para la herramienta");
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
            throw new IllegalStateException("Inventario no encontrado para la herramienta con estado: " + toolState);
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
            throw new IllegalStateException("La cantidad debe ser mayor que cero.");
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

    @SuppressWarnings("java:S107")
    public List<InventoryEntity> filterInventory(String state, String category, Long idTool,
                                                 Integer minPrice, Integer maxPrice,
                                                 Boolean asc, Boolean desc, Boolean recent, String search) {

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalStateException("El precio mínimo no puede ser mayor que el precio máximo.");
        }

        boolean sortAsc = Boolean.TRUE.equals(asc);
        boolean sortDesc = Boolean.TRUE.equals(desc);
        boolean sortRecent = Boolean.TRUE.equals(recent);

        List<InventoryEntity> inventoryList = getSortedInventory(sortAsc, sortDesc, sortRecent);
        return applyInventoryFilters(inventoryList, state, category, idTool, minPrice, maxPrice, search);
    }

    private List<InventoryEntity> getSortedInventory(boolean sortAsc, boolean sortDesc, boolean sortRecent) {
        if (sortAsc && sortDesc) { sortAsc = false; sortDesc = false; }
        if (sortAsc && sortRecent) { sortAsc = false; sortRecent = false; }
        if (sortDesc && sortRecent) { sortDesc = false; sortRecent = false; }

        if (sortRecent) return getMoreRecents();
        if (sortAsc) return getInventoryAscPrice();
        if (sortDesc) return getInventoryDescPrice();
        return inventoryRepository.findAll();
    }

    private List<InventoryEntity> applyInventoryFilters(List<InventoryEntity> list, String state,
            String category, Long idTool, Integer minPrice, Integer maxPrice, String search) {
        if (state != null && !state.isBlank()) {
            list = list.stream()
                    .filter(inv -> inv.getToolState() != null &&
                            inv.getToolState().getState().equalsIgnoreCase(state))
                    .toList();
        }
        if (category != null && !category.isBlank()) {
            list = list.stream()
                    .filter(inv -> inv.getIdTool() != null &&
                            inv.getIdTool().getCategory() != null &&
                            inv.getIdTool().getCategory().getName().equalsIgnoreCase(category))
                    .toList();
        }
        if (idTool != null) {
            list = list.stream()
                    .filter(inv -> inv.getIdTool() != null && inv.getIdTool().getId().equals(idTool))
                    .toList();
        }
        if (minPrice != null) {
            list = list.stream()
                    .filter(inv -> inv.getIdTool() != null && inv.getIdTool().getPriceRent() >= minPrice)
                    .toList();
        }
        if (maxPrice != null) {
            list = list.stream()
                    .filter(inv -> inv.getIdTool() != null && inv.getIdTool().getPriceRent() <= maxPrice)
                    .toList();
        }
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            list = list.stream()
                    .filter(inv -> inv.getIdTool() != null &&
                            inv.getIdTool().getToolName() != null &&
                            inv.getIdTool().getToolName().toLowerCase().contains(searchLower))
                    .toList();
        }
        return list;
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
