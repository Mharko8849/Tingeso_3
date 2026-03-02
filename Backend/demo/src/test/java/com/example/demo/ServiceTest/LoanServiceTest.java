package com.example.demo.ServiceTest;

import com.example.demo.DTO.PageResponseDTO;
import com.example.demo.DTO.LoanDTO;
import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.LoanXToolsEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.LoanRepository;
import com.example.demo.Repositories.LoanXToolsRepository;
import com.example.demo.Services.InventoryService;
import com.example.demo.Services.LoanService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserService userService;

    @Mock
    private LoanXToolsRepository loanXToolsRepository;

    @Mock
    private ToolService toolService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private LoanService loanService;

    private LoanEntity loan;
    private UserEntity user;
    private UserEntity client;

    @BeforeEach
    void setUp() {
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
    void testSaveLoan() {
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);
        LoanEntity result = loanService.saveLoan(loan);
        assertNotNull(result);
    }

    @Test
    void testGetAllLoansByIdUser() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findByIdUser(client)).thenReturn(list);
        List<LoanEntity> result = loanService.getAllLoansByIdUser(client);
        assertEquals(1, result.size());
    }

    @Test
    void testGetLoanById() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        LoanEntity result = loanService.getLoanById(1L);
        assertNotNull(result);
    }

    @Test
    void testGetLoanById_NotFound() {
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            loanService.getLoanById(1L);
        });
    }

    @Test
    void testGetAllLoansByState() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findByStatus("ACTIVO")).thenReturn(list);
        List<LoanEntity> result = loanService.getAllLoansByState("ACTIVO");
        assertEquals(1, result.size());
    }

    @Test
    void testGetOverdueLoans() {
        loan.setReturnDate(Date.valueOf("2020-01-01")); // Past date
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        List<LoanEntity> result = loanService.getOverdueLoans();
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllLoans() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        List<LoanEntity> result = loanService.getAllLoans();
        assertEquals(1, result.size());
    }

    @Test
    void testCreateLoan() {
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        LoanEntity result = loanService.createLoan(client, user, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));

        assertNotNull(result);
        verify(userService, times(1)).validateAdminOrEmployee(user);
        verify(userService, times(1)).saveUser(client);
    }

    @Test
    void testCreateLoan_RestrictedUser() {
        client.setStateClient("RESTRINGIDO");
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(client, user, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));
        });
    }

    @Test
    void testCreateLoan_MaxLoans() {
        when(userService.canDoAnotherLoan(client)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(client, user, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"));
        });
    }

    @Test
    void testCreateLoan_InvalidDates() {
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(client, user, Date.valueOf("2023-01-10"), Date.valueOf("2023-01-01"));
        });
    }

    @Test
    void testFilter() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findByStatus("ACTIVO")).thenReturn(list);
        List<LoanEntity> result = loanService.filter("ACTIVO");
        assertEquals(1, result.size());
    }

    @Test
    void testFilter_NullOrBlank() {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        
        assertEquals(1, loanService.filter(null).size());
        assertEquals(1, loanService.filter("").size());
    }

    @Test
    void testFilter_Overdue() {
        loan.setReturnDate(Date.valueOf("2020-01-01")); // Past date
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanRepository.findAll()).thenReturn(list);
        
        List<LoanEntity> result = loanService.filter("ATRASADO");
        assertEquals(1, result.size());
    }

    @Test
    void testDeleteLoan_Exception() {
        when(loanRepository.findById(1L)).thenThrow(new RuntimeException("Not found"));
        boolean result = loanService.deleteLoan(1L);
        assertFalse(result);
    }

    @Test
    void testIsValidDate_Nulls() {
        assertFalse(loanService.isValidDate(null, Date.valueOf("2023-01-01")));
        assertFalse(loanService.isValidDate(Date.valueOf("2023-01-01"), null));
        assertFalse(loanService.isValidDate(null, null));
    }

    @Test
    void testDeleteLoan() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        boolean result = loanService.deleteLoan(1L);
        assertTrue(result);
        verify(loanRepository, times(1)).deleteById(1L);
        verify(userService, times(1)).saveUser(client);
    }

    @Test
    void testGetAllLoansPaginated() {
        Page<LoanEntity> page = new PageImpl<>(List.of(loan));
        when(loanRepository.findAllByOrderByIdDesc(any(Pageable.class))).thenReturn(page);
        PageResponseDTO<LoanDTO> result = loanService.getAllLoansPaginated(0, 8);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetLoansByStatePaginated() {
        Page<LoanEntity> page = new PageImpl<>(List.of(loan));
        when(loanRepository.findByStatus(eq("ACTIVO"), any(Pageable.class))).thenReturn(page);
        PageResponseDTO<LoanDTO> result = loanService.getLoansByStatePaginated("ACTIVO", 0, 8);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetLoansByUserPaginated() {
        Page<LoanEntity> page = new PageImpl<>(List.of(loan));
        when(loanRepository.findByIdUser(eq(client), any(Pageable.class))).thenReturn(page);
        PageResponseDTO<LoanDTO> result = loanService.getLoansByUserPaginated(client, 0, 8);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testValidateConditions_RestrictedUser() {
        client.setStateClient("RESTRINGIDO");
        assertThrows(RuntimeException.class,
                () -> loanService.validateConditions(client, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10")));
    }

    @Test
    void testValidateConditions_MaxLoans() {
        when(userService.canDoAnotherLoan(client)).thenReturn(false);
        assertThrows(RuntimeException.class,
                () -> loanService.validateConditions(client, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10")));
    }

    @Test
    void testValidateConditions_InvalidDate() {
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        assertThrows(RuntimeException.class,
                () -> loanService.validateConditions(client, Date.valueOf("2023-01-10"), Date.valueOf("2023-01-01")));
    }

    @Test
    void testCreateLoanWithTools_Success() {
        ToolEntity tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
        tool.setPriceRent(100);

        when(userService.findUserById(2L)).thenReturn(client);
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryService.isAvailableTool(tool)).thenReturn(true);
        when(loanXToolsRepository.findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(client, tool))
                .thenReturn(Collections.emptyList());
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(new LoanXToolsEntity());

        LoanEntity result = loanService.createLoanWithTools(user, 2L, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"), List.of(1L));
        assertNotNull(result);
    }

    @Test
    void testCreateLoanWithTools_ClientNotFound() {
        when(userService.findUserById(99L)).thenReturn(null);
        assertThrows(RuntimeException.class,
                () -> loanService.createLoanWithTools(user, 99L, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"), List.of(1L)));
    }

    @Test
    void testCreateLoanWithTools_NoTools() {
        when(userService.findUserById(2L)).thenReturn(client);
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);
        assertThrows(RuntimeException.class,
                () -> loanService.createLoanWithTools(user, 2L, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"), Collections.emptyList()));
    }

    @Test
    void testCreateLoanWithTools_ToolNotAvailable() {
        ToolEntity tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
        tool.setPriceRent(100);

        when(userService.findUserById(2L)).thenReturn(client);
        when(userService.canDoAnotherLoan(client)).thenReturn(true);
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(inventoryService.isAvailableTool(tool)).thenReturn(false);
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loan);

        assertThrows(RuntimeException.class,
                () -> loanService.createLoanWithTools(user, 2L, Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10"), List.of(1L)));
    }

    @Test
    void testIsUserRestringed_Activo() {
        client.setStateClient("ACTIVO");
        assertFalse(loanService.isUserRestringed(client));
    }

    @Test
    void testIsUserRestringed_Restringido() {
        client.setStateClient("RESTRINGIDO");
        assertTrue(loanService.isUserRestringed(client));
    }

    @Test
    void testIsValidDate_EqualDates() {
        // Equal dates - not valid
        assertFalse(loanService.isValidDate(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-01")));
    }

    @Test
    void testIsValidDate_Valid() {
        assertTrue(loanService.isValidDate(Date.valueOf("2023-01-01"), Date.valueOf("2023-01-10")));
    }
}
