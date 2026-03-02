package com.example.demo.RepositoryTest;

import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.ToolStateEntity;
import com.example.demo.Repositories.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryRepository inventoryRepository;

    private CategoryEntity category;
    private ToolStateEntity availableState;
    private ToolStateEntity brokenState;

    @BeforeEach
    void setUp() {
        category = new CategoryEntity();
        category.setName("Construction");
        entityManager.persist(category);

        availableState = new ToolStateEntity();
        availableState.setState("AVAILABLE");
        entityManager.persist(availableState);

        brokenState = new ToolStateEntity();
        brokenState.setState("BROKEN");
        entityManager.persist(brokenState);
    }

    private ToolEntity createTool(String name) {
        ToolEntity tool = new ToolEntity();
        tool.setToolName(name);
        tool.setCategory(category);
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);
        return tool;
    }

    @Test
    void testFindByIdTool() {
        ToolEntity tool = createTool("Hammer");

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState(availableState);
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByIdTool(tool);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getIdTool()).isEqualTo(tool);
    }

    @Test
    void testFindByIdToolAndToolState() {
        ToolEntity tool = createTool("Hammer");

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState(availableState);
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        InventoryEntity found = inventoryRepository.findByIdToolAndToolState_State(tool, "AVAILABLE");

        assertThat(found).isNotNull();
        assertThat(found.getToolState().getState()).isEqualTo("AVAILABLE");
    }

    @Test
    void testFindByStockToolGreaterThan() {
        ToolEntity tool = createTool("Hammer");

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState(availableState);
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByStockToolGreaterThan(5);

        assertThat(found).hasSize(1);
    }

    @Test
    void testFindByToolState() {
        ToolEntity tool = createTool("Hammer");

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState(brokenState);
        inventory.setStockTool(0);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByToolState_State("BROKEN");

        assertThat(found).hasSize(1);
    }

    @Test
    void testFindByIdTool_Category() {
        ToolEntity tool = createTool("Hammer");

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState(availableState);
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByIdTool_Category_Name("Construction");

        assertThat(found).hasSize(1);
    }

    @Test
    void testFindByToolStateAndIdTool_Category() {
        ToolEntity tool = createTool("Hammer");

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState(availableState);
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByToolState_StateAndIdTool_Category_Name("AVAILABLE", "Construction");

        assertThat(found).hasSize(1);
    }

    @Test
    void testFindAllByOrderByIdTool_PriceRentAsc() {
        ToolEntity tool1 = new ToolEntity();
        tool1.setToolName("Cheap");
        tool1.setPriceRent(10);
        tool1.setRepoCost(100);
        tool1.setPriceFineAtDate(5);
        entityManager.persist(tool1);

        ToolEntity tool2 = new ToolEntity();
        tool2.setToolName("Expensive");
        tool2.setPriceRent(100);
        tool2.setRepoCost(1000);
        tool2.setPriceFineAtDate(50);
        entityManager.persist(tool2);

        InventoryEntity inv1 = new InventoryEntity();
        inv1.setIdTool(tool1);
        inv1.setStockTool(5);
        entityManager.persist(inv1);

        InventoryEntity inv2 = new InventoryEntity();
        inv2.setIdTool(tool2);
        inv2.setStockTool(5);
        entityManager.persist(inv2);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findAllByOrderByIdTool_PriceRentAsc();

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getIdTool().getPriceRent()).isEqualTo(10);
    }

    @Test
    void testFindAllByOrderByIdTool_PriceRentDesc() {
        ToolEntity tool1 = new ToolEntity();
        tool1.setToolName("Cheap");
        tool1.setPriceRent(10);
        tool1.setRepoCost(100);
        tool1.setPriceFineAtDate(5);
        entityManager.persist(tool1);

        ToolEntity tool2 = new ToolEntity();
        tool2.setToolName("Expensive");
        tool2.setPriceRent(100);
        tool2.setRepoCost(1000);
        tool2.setPriceFineAtDate(50);
        entityManager.persist(tool2);

        InventoryEntity inv1 = new InventoryEntity();
        inv1.setIdTool(tool1);
        inv1.setStockTool(5);
        entityManager.persist(inv1);

        InventoryEntity inv2 = new InventoryEntity();
        inv2.setIdTool(tool2);
        inv2.setStockTool(5);
        entityManager.persist(inv2);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findAllByOrderByIdTool_PriceRentDesc();

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getIdTool().getPriceRent()).isEqualTo(100);
    }
}
