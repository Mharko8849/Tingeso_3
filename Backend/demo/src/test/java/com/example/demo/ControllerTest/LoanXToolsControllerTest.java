package com.example.demo.ControllerTest;

import com.example.demo.Controllers.LoanXToolsController;
import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.LoanXToolsEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.LoanService;
import com.example.demo.Services.LoanXToolsService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanXToolsController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
public class LoanXToolsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanXToolsService loanXToolsService;

    @MockBean
    private LoanService loanService;

    @MockBean
    private ToolService toolService;

    @MockBean
    private UserService userService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private LoanXToolsEntity lxt;
    private LoanEntity loan;
    private UserEntity user;
    private ToolEntity tool;

    @BeforeEach
    public void setUp() {
        user = new UserEntity();
        user.setId(1L);

        tool = new ToolEntity();
        tool.setId(1L);

        loan = new LoanEntity();
        loan.setId(1L);

        lxt = new LoanXToolsEntity();
        lxt.setId(1L);
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
    }

    @Test
    public void testGetAllLoanTools() throws Exception {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(loanXToolsService.getAllLoanXTools()).thenReturn(list);

        mockMvc.perform(get("/api/loantool/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetByLoan() throws Exception {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsService.getAllLoanXToolsByIdLoan(loan)).thenReturn(list);

        mockMvc.perform(get("/api/loantool/loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testIsToolLoaned() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(toolService.getToolById(1L)).thenReturn(tool);
        when(loanXToolsService.isToolLoanedToUser(tool, user)).thenReturn(true);

        mockMvc.perform(get("/api/loantool/validate/1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void testFineById() throws Exception {
        when(loanXToolsService.findLoanXToolsEntityById(1L)).thenReturn(lxt);
        when(loanXToolsService.calculateFine(lxt, "SIN DAÑO")).thenReturn(10);

        mockMvc.perform(get("/api/loantool/fine/1")
                .param("state", "SIN DAÑO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10));
    }

    @Test
    public void testGetTotal() throws Exception {
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsService.getTotalDebt(loan)).thenReturn(100);

        mockMvc.perform(get("/api/loantool/total/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100));
    }

    @Test
    public void testGetTotalFine() throws Exception {
        when(loanService.getLoanById(1L)).thenReturn(loan);
        when(loanXToolsService.getTotalFine(loan)).thenReturn(50);

        mockMvc.perform(get("/api/loantool/total/fine/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(50));
    }

    @Test
    public void testGetAllLoanToolsByUser() throws Exception {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanXToolsService.getAllLoanXToolsByIdUser(user)).thenReturn(list);

        mockMvc.perform(get("/api/loantool/all/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetAllRepairTools() throws Exception {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        when(loanXToolsService.needRepairTools(1L)).thenReturn(list);

        mockMvc.perform(get("/api/loantool/repair/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGiveLoanTool() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanXToolsService.giveLoanTool(user, 1L)).thenReturn(lxt);

        mockMvc.perform(post("/api/loantool/give/1/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testCreateLoanXTool() throws Exception {
        when(loanXToolsService.createLoanXTool(1L, 1L)).thenReturn(lxt);

        mockMvc.perform(post("/api/loantool/create/1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testGiveLoanTools() throws Exception {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanXToolsService.giveAllLoanTools(eq(user), anyList())).thenReturn(list);

        mockMvc.perform(post("/api/loantool/give/all/user/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testReceiveLoanTool() throws Exception {
        when(loanXToolsService.receiveLoanTool(1L, 1L, "SIN DAÑO")).thenReturn(lxt);

        mockMvc.perform(post("/api/loantool/receive/1/user/1")
                .param("stateTool", "SIN DAÑO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testReceiveAllTools() throws Exception {
        List<LoanXToolsEntity> list = new ArrayList<>();
        list.add(lxt);
        Map<String, String> state = new HashMap<>();
        state.put("1", "SIN DAÑO");
        when(loanXToolsService.mapAndReceiveAllTools(eq(1L), eq(1L), anyMap())).thenReturn(list);

        mockMvc.perform(post("/api/loantool/receive/all/1/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(state)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testPayDebt() throws Exception {
        when(loanXToolsService.payDebt(1L, 1L)).thenReturn(true);

        mockMvc.perform(post("/api/loantool/paydebt/1/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void testPayRepair() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("adminUser", 1);
        body.put("cost", 100);
        when(loanXToolsService.mapAndPayRepairTool(eq(1L), anyMap())).thenReturn(true);

        mockMvc.perform(post("/api/loantool/repair/1/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    public void testCloseStrangeLoan() throws Exception {
        when(loanXToolsService.closeStrangeLoan(1L)).thenReturn(loan);

        mockMvc.perform(post("/api/loantool/close/loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testDeleteLoanXTool() throws Exception {
        mockMvc.perform(delete("/api/loantool/1/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
