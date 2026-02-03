package com.example.demo.RepositoryTest;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.LoanXToolsEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.LoanXToolsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.sql.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class LoanXToolsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoanXToolsRepository loanXToolsRepository;

    @Test
    public void testFindByIdLoan() {
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

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);

        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository.findByIdLoan(loan);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdEmployeeDel() {
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

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);

        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setIdEmployeeDel(employee);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository.findByIdEmployeeDel(employee);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdEmployeeRec() {
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

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);

        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setIdEmployeeRec(employee);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository.findByIdEmployeeRec(employee);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdTool_CategoryAndIdLoan_IdUserAndIdLoan_RealReturnDateIsNull() {
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

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);

        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository.findByIdTool_CategoryAndIdLoan_IdUserAndIdLoan_RealReturnDateIsNull("Construction", user);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull() {
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

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);

        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository.findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(user, tool);

        assertThat(found).hasSize(1);
    }

    @Test
    public void testExistActiveLoanWithTool() {
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

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);

        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory("Construction");
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        entityManager.persist(tool);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        Boolean exists = loanXToolsRepository.existActiveLoanWithTool(user, tool);

        assertThat(exists).isTrue();
    }
}
