package com.example.demo.RepositoryTest;

import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Repositories.ToolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ToolRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ToolRepository toolRepository;

    private CategoryEntity createAndPersistCategory(String name) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        entityManager.persist(category);
        return category;
    }

    @Test
    public void testFindByToolName() {
        CategoryEntity cat = createAndPersistCategory("Construction");
        
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory(cat);
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);
        entityManager.flush();

        ToolEntity found = toolRepository.findByToolName("Hammer");

        assertThat(found.getToolName()).isEqualTo(tool.getToolName());
    }

    @Test
    public void testFindByCategory() {
        CategoryEntity cat = createAndPersistCategory("Power Tools");
        
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Drill");
        tool.setCategory(cat);
        tool.setRepoCost(200);
        tool.setPriceRent(20);
        tool.setPriceFineAtDate(10);
        entityManager.persist(tool);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findByCategory_Name("Power Tools");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCategory().getName()).isEqualTo("Power Tools");
    }

    @Test
    public void testFindByPriceRentGreaterThanEqual() {
        CategoryEntity cat1 = createAndPersistCategory("Construction");
        CategoryEntity cat2 = createAndPersistCategory("Hand Tools");
        
        ToolEntity tool1 = new ToolEntity();
        tool1.setToolName("Saw");
        tool1.setCategory(cat1);
        tool1.setRepoCost(150);
        tool1.setPriceRent(15);
        tool1.setPriceFineAtDate(8);
        entityManager.persist(tool1);

        ToolEntity tool2 = new ToolEntity();
        tool2.setToolName("Screwdriver");
        tool2.setCategory(cat2);
        tool2.setRepoCost(50);
        tool2.setPriceRent(5);
        tool2.setPriceFineAtDate(2);
        entityManager.persist(tool2);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findByPriceRentGreaterThanEqual(10);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getToolName()).isEqualTo("Saw");
    }

    @Test
    public void testFindByPriceRentLessThanEqual() {
        CategoryEntity cat1 = createAndPersistCategory("Construction");
        CategoryEntity cat2 = createAndPersistCategory("Hand Tools");
        
        ToolEntity tool1 = new ToolEntity();
        tool1.setToolName("Saw");
        tool1.setCategory(cat1);
        tool1.setRepoCost(150);
        tool1.setPriceRent(15);
        tool1.setPriceFineAtDate(8);
        entityManager.persist(tool1);

        ToolEntity tool2 = new ToolEntity();
        tool2.setToolName("Screwdriver");
        tool2.setCategory(cat2);
        tool2.setRepoCost(50);
        tool2.setPriceRent(5);
        tool2.setPriceFineAtDate(2);
        entityManager.persist(tool2);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findByPriceRentLessThanEqual(10);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getToolName()).isEqualTo("Screwdriver");
    }

    @Test
    public void testFindAllByCategory() {
        CategoryEntity cat = createAndPersistCategory("Hand Tools");
        
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Wrench");
        tool.setCategory(cat);
        tool.setRepoCost(80);
        tool.setPriceRent(8);
        tool.setPriceFineAtDate(4);
        entityManager.persist(tool);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findAllByCategory_Name("Hand Tools");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCategory().getName()).isEqualTo("Hand Tools");
    }

    @Test
    public void testFindByPriceRentGreaterThanEqualAndCategory() {
        CategoryEntity cat = createAndPersistCategory("Power Tools");
        
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Heavy Drill");
        tool.setCategory(cat);
        tool.setPriceRent(50);
        tool.setRepoCost(500);
        tool.setPriceFineAtDate(25);
        entityManager.persist(tool);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findByPriceRentGreaterThanEqualAndCategory_Name(40, "Power Tools");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByPriceRentLessThanEqualAndCategory() {
        CategoryEntity cat = createAndPersistCategory("Power Tools");
        
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Light Drill");
        tool.setCategory(cat);
        tool.setPriceRent(30);
        tool.setRepoCost(300);
        tool.setPriceFineAtDate(15);
        entityManager.persist(tool);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findByPriceRentLessThanEqualAndCategory_Name(40, "Power Tools");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindAllByOrderByPriceRentDesc() {
        ToolEntity tool1 = new ToolEntity();
        tool1.setToolName("Cheap Tool");
        tool1.setPriceRent(10);
        tool1.setRepoCost(100);
        tool1.setPriceFineAtDate(5);
        entityManager.persist(tool1);

        ToolEntity tool2 = new ToolEntity();
        tool2.setToolName("Expensive Tool");
        tool2.setPriceRent(100);
        tool2.setRepoCost(1000);
        tool2.setPriceFineAtDate(50);
        entityManager.persist(tool2);
        entityManager.flush();

        List<ToolEntity> found = toolRepository.findAllByOrderByPriceRentDesc();

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getPriceRent()).isEqualTo(100);
        assertThat(found.get(1).getPriceRent()).isEqualTo(10);
    }
}
