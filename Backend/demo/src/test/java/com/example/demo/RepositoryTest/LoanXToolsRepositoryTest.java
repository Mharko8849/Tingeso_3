package com.example.demo.RepositoryTest;

import com.example.demo.Entities.CategoryEntity;
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
class LoanXToolsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoanXToolsRepository loanXToolsRepository;

    private CategoryEntity createCategory(String name) {
        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        return entityManager.persist(category);
    }

    private ToolEntity createTool(CategoryEntity category) {
        ToolEntity tool = new ToolEntity();
        tool.setToolName("Hammer");
        tool.setCategory(category);
        tool.setRepoCost(100);
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        return entityManager.persist(tool);
    }

    private UserEntity createUser(String rut, String username, String email, String rol) {
        UserEntity user = new UserEntity();
        user.setName("Test");
        user.setLastName("User");
        user.setRut(rut);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setRol(rol);
        user.setStateClient("ACTIVO");
        user.setLoans(0);
        return entityManager.persist(user);
    }

    private LoanEntity createLoan(UserEntity user) {
        LoanEntity loan = new LoanEntity();
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        return entityManager.persist(loan);
    }

    @Test
    void testFindByIdLoan() {
        UserEntity user = createUser("12345678-9", "testuser", "test@example.com", "CLIENT");
        LoanEntity loan = createLoan(user);
        CategoryEntity category = createCategory("Construction");
        ToolEntity tool = createTool(category);

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
    void testFindByIdEmployeeDel() {
        UserEntity user = createUser("12345678-9", "testuser", "test@example.com", "CLIENT");
        UserEntity employee = createUser("11111111-1", "employee", "emp@example.com", "EMPLOYEE");
        LoanEntity loan = createLoan(user);
        CategoryEntity category = createCategory("Construction2");
        ToolEntity tool = createTool(category);

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
    void testFindByIdEmployeeRec() {
        UserEntity user = createUser("12345678-9", "testuser2", "test2@example.com", "CLIENT");
        UserEntity employee = createUser("22222222-2", "employee2", "emp2@example.com", "EMPLOYEE");
        LoanEntity loan = createLoan(user);
        CategoryEntity category = createCategory("Construction3");
        ToolEntity tool = createTool(category);

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
    void testFindByIdTool_Category_NameAndIdLoan_IdUserAndIdLoan_RealReturnDateIsNull() {
        UserEntity user = createUser("33333333-3", "testuser3", "test3@example.com", "CLIENT");
        LoanEntity loan = createLoan(user);
        CategoryEntity category = createCategory("Construction4");
        ToolEntity tool = createTool(category);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository
                .findByIdTool_Category_NameAndIdLoan_IdUserAndIdLoan_RealReturnDateIsNull("Construction4", user);
        assertThat(found).hasSize(1);
    }

    @Test
    void testFindByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull() {
        UserEntity user = createUser("44444444-4", "testuser4", "test4@example.com", "CLIENT");
        LoanEntity loan = createLoan(user);
        CategoryEntity category = createCategory("Construction5");
        ToolEntity tool = createTool(category);

        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setToolActivity("ACTIVE");
        lxt.setDebt(0);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
        entityManager.persist(lxt);
        entityManager.flush();

        List<LoanXToolsEntity> found = loanXToolsRepository
                .findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(user, tool);
        assertThat(found).hasSize(1);
    }

    @Test
    void testExistActiveLoanWithTool() {
        UserEntity user = createUser("55555555-5", "testuser5", "test5@example.com", "CLIENT");
        LoanEntity loan = createLoan(user);
        CategoryEntity category = createCategory("Construction6");
        ToolEntity tool = createTool(category);

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
