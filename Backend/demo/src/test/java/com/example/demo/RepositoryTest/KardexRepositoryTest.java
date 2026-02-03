package com.example.demo.RepositoryTest;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.KardexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.sql.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class KardexRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private KardexRepository kardexRepository;

    @Test
    public void testFindByDate() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-01"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByDate(Date.valueOf("2023-01-01"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdUser() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        UserEntity user = new UserEntity();
        user.setName("Test");
        user.setLastName("User");
        user.setRut("12345678-9");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRol("CLIENT");
        user.setStateClient("ACTIVO");
        user.setLoans(0);
        entityManager.persist(user);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("OUT");
        kardex.setDate(Date.valueOf("2023-01-01"));
        kardex.setCant(1);
        kardex.setCost(10);
        kardex.setIdEmployee(employee);
        kardex.setIdUser(user);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByIdUser(user);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByType() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-01"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByType("IN");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdTool() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-01"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByIdTool(tool);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByDateGreaterThan() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-05"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByDateGreaterThan(Date.valueOf("2023-01-01"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByDateLessThan() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-01"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByDateLessThan(Date.valueOf("2023-01-05"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByDateBetween() {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        UserEntity employee = new UserEntity();
        employee.setName("Emp");
        employee.setLastName("Loyee");
        employee.setRut("11111111-1");
        employee.setUsername("employee");
        employee.setEmail("emp@example.com");
        employee.setPassword("password");
        employee.setRol("EMPLOYEE");
        employee.setStateClient("ACTIVO");
        employee.setLoans(0);
        entityManager.persist(employee);

        KardexEntity kardex = new KardexEntity();
        kardex.setIdTool(tool);
        kardex.setType("IN");
        kardex.setDate(Date.valueOf("2023-01-05"));
        kardex.setCant(10);
        kardex.setCost(100);
        kardex.setIdEmployee(employee);
        entityManager.persist(kardex);
        entityManager.flush();

        List<KardexEntity> found = kardexRepository.findByDateBetween(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));

        assertThat(found).hasSize(1);
    }
}
