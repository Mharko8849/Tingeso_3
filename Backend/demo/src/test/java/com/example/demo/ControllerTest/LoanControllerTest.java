package com.example.demo.ControllerTest;

import com.example.demo.Controllers.LoanController;
import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.LoanService;
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

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
public class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private UserService userService;

    @MockBean
    private ToolService toolService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private LoanEntity loan;
    private UserEntity user;

    @BeforeEach
    public void setUp() {
        user = new UserEntity();
        user.setId(1L);

        loan = new LoanEntity();
        loan.setId(1L);
        loan.setIdUser(user);
    }

    @Test
    public void testGetAllLoansByUser() throws Exception {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.getAllLoansByIdUser(user)).thenReturn(list);

        mockMvc.perform(get("/api/loan/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetLoanById() throws Exception {
        when(loanService.getLoanById(1L)).thenReturn(loan);

        mockMvc.perform(get("/api/loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testGetAllLoans() throws Exception {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanService.getAllLoans()).thenReturn(list);

        mockMvc.perform(get("/api/loan/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testFilterLoans() throws Exception {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanService.filter("ACTIVO")).thenReturn(list);

        mockMvc.perform(get("/api/loan/filter")
                .param("state", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testCreateLoan() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.createLoan(any(UserEntity.class), any(UserEntity.class), any(Date.class), any(Date.class))).thenReturn(loan);

        mockMvc.perform(post("/api/loan/create/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .param("initDate", "2023-01-01")
                .param("returnDate", "2023-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testCreateLoan_UserNotFound() throws Exception {
        when(userService.findUserById(1L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/loan/create/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .param("initDate", "2023-01-01")
                .param("returnDate", "2023-01-10"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateLoan_Error() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.createLoan(any(UserEntity.class), any(UserEntity.class), any(Date.class), any(Date.class))).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/loan/create/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .param("initDate", "2023-01-01")
                .param("returnDate", "2023-01-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteLoanById() throws Exception {
        when(loanService.deleteLoan(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
