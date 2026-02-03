package com.example.demo.RepositoryTest;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.LoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.sql.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class LoanRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    public void testFindByIdUser() {
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
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByIdUser(user);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getIdUser()).isEqualTo(user);
    }

    @Test
    public void testFindByStatus() {
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
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByStatus("ACTIVE");

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByInitDateIsGreaterThanEqualAndReturnDateIsLessThanEqual() {
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
        loan.setInitDate(Date.valueOf("2023-01-05"));
        loan.setReturnDate(Date.valueOf("2023-01-08"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByInitDateIsGreaterThanEqualAndReturnDateIsLessThanEqual(
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByInitDateIsGreaterThanEqual() {
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
        loan.setInitDate(Date.valueOf("2023-01-05"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVE");
        entityManager.persist(loan);
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByInitDateIsGreaterThanEqual(Date.valueOf("2023-01-01"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByReturnDateGreaterThanEqual() {
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
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByReturnDateGreaterThanEqual(Date.valueOf("2023-01-05"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByReturnDateGreaterThan() {
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
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByReturnDateGreaterThan(Date.valueOf("2023-01-05"));

        assertThat(found).hasSize(1);
    }

    @Test
    public void testFindByRealReturnDateLessThanEqual() {
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
        loan.setRealReturnDate(Date.valueOf("2023-01-08"));
        loan.setStatus("FINISHED");
        entityManager.persist(loan);
        entityManager.flush();

        List<LoanEntity> found = loanRepository.findByRealReturnDateLessThanEqual(Date.valueOf("2023-01-09"));

        assertThat(found).hasSize(1);
    }
}
