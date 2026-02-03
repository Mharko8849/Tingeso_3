package com.example.demo.RepositoryTest;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Repositories.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class InventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    public void testFindByIdTool() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState("AVAILABLE");
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByIdTool(tool);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getIdTool()).isEqualTo(tool);
    }

    @Test
    public void testFindByIdToolAndToolState() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState("AVAILABLE");
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        InventoryEntity found = inventoryRepository.findByIdToolAndToolState(tool, "AVAILABLE");

        assertThat(found).isNotNull();
        assertThat(found.getToolState()).isEqualTo("AVAILABLE");
    }

    @Test
    public void testFindByStockToolGreaterThan() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState("AVAILABLE");
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByStockToolGreaterThan(5);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByToolState() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState("BROKEN");
        inventory.setStockTool(0);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByToolState("BROKEN");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdTool_Category() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState("AVAILABLE");
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByIdTool_Category("Construction");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByToolStateAndIdTool_Category() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setIdTool(tool);
        inventory.setToolState("AVAILABLE");
        inventory.setStockTool(10);
        entityManager.persist(inventory);
        entityManager.flush();

        List<InventoryEntity> found = inventoryRepository.findByToolStateAndIdTool_Category("AVAILABLE", "Construction");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindAllByOrderByIdTool_PriceRentAsc() {
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
    public void testFindAllByOrderByIdTool_PriceRentDesc() {
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
