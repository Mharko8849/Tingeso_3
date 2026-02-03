package com.example.demo.ServiceTest;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.LoanXToolsEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.LoanRepository;
import com.example.demo.Repositories.LoanXToolsRepository;
import com.example.demo.Services.*;
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

public class LoanXToolsServiceTest {

    @Mock
    private LoanXToolsRepository loanXToolsRepository;
    @Mock
    private UserService userService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private KardexService kardexService;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ToolService toolService;
    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanXToolsService loanXToolsService;

    private LoanXToolsEntity lxt;
    private LoanEntity loan;
    private ToolEntity tool;
    private UserEntity user;
    private UserEntity employee;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new UserEntity();
        user.setId(1L);
        user.setRol("CLIENT");
        user.setStateClient("ACTIVO");
        user.setLoans(0);

        employee = new UserEntity();
        employee.setId(2L);
        employee.setRol("EMPLOYEE");

        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
        tool.setPriceRent(10);
        tool.setPriceFineAtDate(5);
        tool.setRepoCost(100);

        loan = new LoanEntity();
        loan.setId(1L);
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2023-01-01"));
        loan.setReturnDate(Date.valueOf("2023-01-10"));
        loan.setStatus("ACTIVO");

        lxt = new LoanXToolsEntity();
        lxt.setId(1L);
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setDebt(10);
        lxt.setFine(0);
        lxt.setNeedRepair(false);
    }

    @Test
    public void testSaveLoanXToolsEntity() {
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(lxt);
        LoanXToolsEntity result = loanXToolsService.saveLoanXToolsEntity(lxt);
        assertNotNull(result);
    }

    @Test
    public void testFindLoanXToolsEntityById() {
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        LoanXToolsEntity result = loanXToolsService.findLoanXToolsEntityById(1L);
        assertNotNull(result);
    }

    @Test
    public void testGetTotalDebt() {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(list);
        int result = loanXToolsService.getTotalDebt(loan);
        assertEquals(10, result);
    }

    @Test
    public void testGetTotalFine() {
        lxt.setFine(5);
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(list);
        int result = loanXToolsService.getTotalFine(loan);
        assertEquals(5, result);
    }

    @Test
    public void testDeleteLoanXToolsById() {
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        loanXToolsService.deleteLoanXToolsById(2L, 1L);
        verify(loanXToolsRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testGiveLoanTool() {
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(inventoryService.isAvailableTool(tool)).thenReturn(true);
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(lxt);

        LoanXToolsEntity result = loanXToolsService.giveLoanTool(employee, 1L);

        assertNotNull(result);
        assertEquals("PRESTADA", result.getToolActivity());
        verify(inventoryService, times(1)).loanTool(tool.getId());
    }

    @Test
    public void testCreateLoanXTool() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(loanService.isUserRestringed(user)).thenReturn(false);
        when(inventoryService.isAvailableTool(tool)).thenReturn(true);
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(lxt);

        LoanXToolsEntity result = loanXToolsService.createLoanXTool(1L, 1L);

        assertNotNull(result);
    }

    @Test
    public void testCalculateFineByDate() {
        loan.setReturnDate(Date.valueOf("2020-01-01")); // Past date
        int fine = loanXToolsService.calculateFineByDate(lxt);
        assertTrue(fine > 0);
    }

    @Test
    public void testCalculateFineByStateToolReturn() {
        int fine = loanXToolsService.calculateFineByStateToolReturn(lxt, "IRREPARABLE");
        assertEquals(100, fine);
    }

    @Test
    public void testReceiveLoanTool() {
        lxt.setToolActivity("PRESTADA");
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(lxt);

        LoanXToolsEntity result = loanXToolsService.receiveLoanTool(2L, 1L, "SIN DAÑO");

        assertNotNull(result);
        assertEquals("DEVUELTA", result.getToolActivity());
        verify(inventoryService, times(1)).receiveTool(tool.getId(), "DISPONIBLE");
    }

    @Test
    public void testPayDebt() {
        lxt.setFine(10);
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(list);
        when(loanService.isUserRestringed(user)).thenReturn(true);

        boolean result = loanXToolsService.payDebt(1L, 2L);

        assertTrue(result);
        assertEquals(0, lxt.getFine());
    }

    @Test
    public void testDeleteLoanXToolsById_WithActivity() {
        lxt.setToolActivity("PRESTADA");
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        
        assertThrows(RuntimeException.class, () -> loanXToolsService.deleteLoanXToolsById(2L, 1L));
    }

    @Test
    public void testGiveLoanTool_Exceptions() {
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        when(loanService.getLoanById(1L)).thenReturn(loan);

        // Case: Loan not active
        loan.setStatus("FINALIZADO");
        assertThrows(RuntimeException.class, () -> loanXToolsService.giveLoanTool(employee, 1L));
        loan.setStatus("ACTIVO");

        // Case: Already has activity
        lxt.setToolActivity("PRESTADA");
        assertThrows(RuntimeException.class, () -> loanXToolsService.giveLoanTool(employee, 1L));
        lxt.setToolActivity(null);

        // Case: No stock
        when(inventoryService.isAvailableTool(tool)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> loanXToolsService.giveLoanTool(employee, 1L));
    }

    @Test
    public void testCreateLoanXTool_Exceptions() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(toolService.getToolById(1L)).thenReturn(tool);

        // Case: User restricted
        when(loanService.isUserRestringed(user)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> loanXToolsService.createLoanXTool(1L, 1L));
        when(loanService.isUserRestringed(user)).thenReturn(false);

        // Case: Invalid dates
        loan.setReturnDate(Date.valueOf("2022-01-01"));
        assertThrows(RuntimeException.class, () -> loanXToolsService.createLoanXTool(1L, 1L));
        loan.setReturnDate(Date.valueOf("2023-01-10"));

        // Case: Not available
        when(inventoryService.isAvailableTool(tool)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> loanXToolsService.createLoanXTool(1L, 1L));
        when(inventoryService.isAvailableTool(tool)).thenReturn(true);

        // Case: Already loaned
        List<LoanXToolsEntity> existing = List.of(new LoanXToolsEntity());
        when(loanXToolsRepository.findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(user, tool)).thenReturn(existing);
        assertThrows(RuntimeException.class, () -> loanXToolsService.createLoanXTool(1L, 1L));
    }

    @Test
    public void testCalculateFineByStateToolReturn_Invalid() {
        assertThrows(RuntimeException.class, () -> loanXToolsService.calculateFineByStateToolReturn(lxt, "INVALID"));
    }

    @Test
    public void testReceiveLoanTool_Exceptions() {
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        when(loanService.getLoanById(1L)).thenReturn(loan);

        // Case: No activity
        lxt.setToolActivity(null);
        assertThrows(RuntimeException.class, () -> loanXToolsService.receiveLoanTool(2L, 1L, "SIN DAÑO"));
        
        // Case: Invalid damage
        lxt.setToolActivity("PRESTADA");
        assertThrows(RuntimeException.class, () -> loanXToolsService.receiveLoanTool(2L, 1L, "INVALID"));
    }

    @Test
    public void testMapAndPayRepairTool() {
        // Mock payRepairTool logic via spy or just test the mapping logic if possible.
        // Since payRepairTool is transactional and in same class, mocking it is hard without spy.
        // We will test the full flow.
        
        lxt.setNeedRepair(true);
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(list);

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("adminUser", 2);
        body.put("cost", 50);

        Boolean result = loanXToolsService.mapAndPayRepairTool(1L, body);
        assertTrue(result);
    }

    @Test
    public void testMapAndPayRepairTool_InvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> loanXToolsService.mapAndPayRepairTool(1L, new java.util.HashMap<>()));
        
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("adminUser", "invalid");
        body.put("cost", 50);
        assertThrows(IllegalArgumentException.class, () -> loanXToolsService.mapAndPayRepairTool(1L, body));
    }

    @Test
    public void testCloseStrangeLoan() {
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(new ArrayList<>());
        when(loanService.saveLoan(loan)).thenReturn(loan);

        LoanEntity result = loanXToolsService.closeStrangeLoan(1L);
        assertEquals("FINALIZADO", result.getStatus());
    }

    @Test
    public void testGetAllLoanXToolsByIdUser() {
        List<LoanEntity> loans = List.of(loan);
        when(loanService.getAllLoansByIdUser(user)).thenReturn(loans);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(List.of(lxt));

        List<LoanXToolsEntity> result = loanXToolsService.getAllLoanXToolsByIdUser(user);
        assertEquals(1, result.size());
    }

    @Test
    public void testGiveAllLoanTools() {
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(inventoryService.isAvailableTool(tool)).thenReturn(true);
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(lxt);

        List<LoanXToolsEntity> result = loanXToolsService.giveAllLoanTools(employee, List.of(1L));
        assertEquals(1, result.size());
    }
    
    @Test
    public void testMapAndReceiveAllTools() {
        // Setup for receiveAllLoanTools
        lxt.setToolActivity("PRESTADA");
        List<LoanXToolsEntity> list = List.of(lxt);
        
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(list);
        
        // Setup for receiveLoanTool
        when(userService.findUserById(2L)).thenReturn(employee);
        when(loanXToolsRepository.findById(1L)).thenReturn(Optional.of(lxt));
        when(loanXToolsRepository.save(any(LoanXToolsEntity.class))).thenReturn(lxt);

        java.util.Map<String, String> stateMap = new java.util.HashMap<>();
        stateMap.put("1", "SIN DAÑO");

        List<LoanXToolsEntity> result = loanXToolsService.mapAndReceiveAllTools(1L, 2L, stateMap);
        assertEquals(1, result.size());
    }

    @Test
    public void testMapAndReceiveAllTools_InvalidKey() {
        java.util.Map<String, String> stateMap = new java.util.HashMap<>();
        stateMap.put("invalid", "SIN DAÑO");
        assertThrows(IllegalArgumentException.class, () -> loanXToolsService.mapAndReceiveAllTools(1L, 2L, stateMap));
    }

    @Test
    public void testReceiveAllLoanTools_MismatchSize() {
        List<LoanXToolsEntity> list = List.of(lxt);
        java.util.Map<Long, String> states = new java.util.HashMap<>();
        
        assertThrows(IllegalArgumentException.class, () -> loanXToolsService.receiveAllLoanTools(2L, list, states));
    }

    @Test
    public void testReceiveAllLoanTools_MissingId() {
        List<LoanXToolsEntity> list = List.of(lxt);
        java.util.Map<Long, String> states = new java.util.HashMap<>();
        states.put(999L, "SIN DAÑO");
        
        assertThrows(IllegalArgumentException.class, () -> loanXToolsService.receiveAllLoanTools(2L, list, states));
    }

    @Test
    public void testNeedRepairTools() {
        lxt.setNeedRepair(true);
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsRepository.findByIdLoan(loan)).thenReturn(List.of(lxt));
        
        List<LoanXToolsEntity> result = loanXToolsService.needRepairTools(1L);
        assertEquals(1, result.size());
    }
}
