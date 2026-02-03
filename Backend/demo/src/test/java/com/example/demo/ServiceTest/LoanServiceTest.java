package com.example.demo.ServiceTest;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.LoanRepository;
import com.example.demo.Services.LoanService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private LoanService loanService;

    private LoanEntity loan;
    private UserEntity user;
    private UserEntity client;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new UserEntity();
        user.setId(1L);
        user.setRol("ADMIN");

        client = new UserEntity();
        client.setId(2L);
        client.setRol("CLIENT");
        client.setStateClient("ACTIVO");
        client.setLoans(0);

        loan = new LoanEntity();
        loan.setId(1L);
        loan.setIdUser(client);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVO");
    }

    @Test
    public void testSaveLoan() {
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);
        LoanEntity result = loanService.saveLoan(loan);
        assertNotNull(result);
    }

    @Test
    public void testGetAllLoansByIdUser() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findByIdUser(client)).thenReturn(list);
        List<LoanEntity> result = loanService.getAllLoansByIdUser(client);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetLoanById() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        LoanEntity result = loanService.getLoanById(1L);
        assertNotNull(result);
    }

    @Test
    public void testGetLoanById_NotFound() {
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            loanService.getLoanById(1L);
        });
    }

    @Test
    public void testGetAllLoansByState() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findByStatus("ACTIVO")).thenReturn(list);
        List<LoanEntity> result = loanService.getAllLoansByState("ACTIVO");
        assertEquals(1, result.size());
    }

    @Test
    public void testGetOverdueLoans() {
        loan.setReturnDate(Date.valueOf("2020-01-01")); // Past date
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        List<LoanEntity> result = loanService.getOverdueLoans();
        assertEquals(1, result.size());
    }

    @Test
    public void testGetAllLoans() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        List<LoanEntity> result = loanService.getAllLoans();
        assertEquals(1, result.size());
    }

    @Test
    public void testCreateLoan() {
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        LoanEntity result = loanService.createLoan(client, user, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));

        assertNotNull(result);
        verify(userService, times(1)).validateAdminOrEmployee(user);
        verify(userService, times(1)).saveUser(client);
    }

    @Test
    public void testCreateLoan_RestrictedUser() {
        client.setStateClient("RESTRINGIDO");
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(client, user, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));
        });
    }

    @Test
    public void testCreateLoan_MaxLoans() {
        when(userService.canDoAnotherLoan(client)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(client, user, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));
        });
    }

    @Test
    public void testCreateLoan_InvalidDates() {
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(client, user, Date.valueOf("2023-01-10"), Date.valueOf("2023-01-01"));
        });
    }

    @Test
    public void testFilter() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findByStatus("ACTIVO")).thenReturn(list);
        List<LoanEntity> result = loanService.filter("ACTIVO");
        assertEquals(1, result.size());
    }

    @Test
    public void testFilter_NullOrBlank() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        
        assertEquals(1, loanService.filter(null).size());
        assertEquals(1, loanService.filter("").size());
    }

    @Test
    public void testFilter_Overdue() {
        loan.setReturnDate(Date.valueOf("2020-01-01")); // Past date
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        
        List<LoanEntity> result = loanService.filter("ATRASADO");
        assertEquals(1, result.size());
    }

    @Test
    public void testDeleteLoan_Exception() {
        when(loanRepository.findById(1L)).thenThrow(new RuntimeException("Not found"));
        boolean result = loanService.deleteLoan(1L);
        assertFalse(result);
    }

    @Test
    public void testIsValidDate_Nulls() {
        assertFalse(loanService.isValidDate(null, Date.valueOf("2023-01-01")));
        assertFalse(loanService.isValidDate(Date.valueOf("2023-01-01"), null));
        assertFalse(loanService.isValidDate(null, null));
    }

    @Test
    public void testDeleteLoan() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        boolean result = loanService.deleteLoan(1L);
        assertTrue(result);
        verify(loanRepository, times(1)).deleteById(1L);
        verify(userService, times(1)).saveUser(client);
    }
}
