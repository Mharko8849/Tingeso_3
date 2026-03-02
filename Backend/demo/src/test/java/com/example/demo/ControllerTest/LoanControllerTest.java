package com.example.demo.ControllerTest;

import com.example.demo.Controllers.LoanController;
import com.example.demo.DTO.LoanDTO;
import com.example.demo.DTO.PageResponseDTO;
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
class LoanControllerTest {

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
    void setUp() {
        user = new UserEntity();
        user.setId(1L);

        loan = new LoanEntity();
        loan.setId(1L);
        loan.setIdUser(user);
    }

    @Test
    void testGetAllLoansByUser() throws Exception {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.getAllLoansByIdUser(user)).thenReturn(list);

        mockMvc.perform(get("/loan/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetLoanById() throws Exception {
        when(loanService.getLoanById(1L)).thenReturn(loan);

        mockMvc.perform(get("/loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetAllLoans() throws Exception {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanService.getAllLoans()).thenReturn(list);

        mockMvc.perform(get("/loan/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testFilterLoans() throws Exception {
        List<LoanEntity> list = new ArrayList<>();
        list.add(loan);
        when(loanService.filter("ACTIVO")).thenReturn(list);

        mockMvc.perform(get("/loan/filter")
                .param("state", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testCreateLoan() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.createLoan(any(UserEntity.class), any(UserEntity.class), any(Date.class), any(Date.class))).thenReturn(loan);

        mockMvc.perform(post("/loan/create/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .param("initDate", "2023-01-01")
                .param("returnDate", "2023-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testCreateLoan_UserNotFound() throws Exception {
        when(userService.findUserById(1L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/loan/create/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .param("initDate", "2023-01-01")
                .param("returnDate", "2023-01-10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateLoan_Error() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.createLoan(any(UserEntity.class), any(UserEntity.class), any(Date.class), any(Date.class))).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/loan/create/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .param("initDate", "2023-01-01")
                .param("returnDate", "2023-01-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteLoanById() throws Exception {
        when(loanService.deleteLoan(1L)).thenReturn(true);

        mockMvc.perform(delete("/loan/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testGetAllLoansPaginated() throws Exception {
        LoanDTO loanDTO = new LoanDTO();
        loanDTO.setId(1L);
        PageResponseDTO<LoanDTO> page = new PageResponseDTO<>(List.of(loanDTO), 0, 8, 1L, 1, true, true);
        when(loanService.getAllLoansPaginated(0, 8)).thenReturn(page);

        mockMvc.perform(get("/loan/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testGetLoansByUserPaginated() throws Exception {
        LoanDTO loanDTO = new LoanDTO();
        loanDTO.setId(1L);
        PageResponseDTO<LoanDTO> page = new PageResponseDTO<>(List.of(loanDTO), 0, 8, 1L, 1, true, true);
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.getLoansByUserPaginated(eq(user), eq(0), eq(8))).thenReturn(page);

        mockMvc.perform(get("/loan/user/1/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testFilterLoansPaginated_WithState() throws Exception {
        LoanDTO loanDTO = new LoanDTO();
        loanDTO.setId(1L);
        PageResponseDTO<LoanDTO> page = new PageResponseDTO<>(List.of(loanDTO), 0, 8, 1L, 1, true, true);
        when(loanService.getLoansByStatePaginated(eq("ACTIVO"), eq(0), eq(8))).thenReturn(page);

        mockMvc.perform(get("/loan/filter/paginated").param("state", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testFilterLoansPaginated_NoState() throws Exception {
        LoanDTO loanDTO = new LoanDTO();
        loanDTO.setId(1L);
        PageResponseDTO<LoanDTO> page = new PageResponseDTO<>(List.of(loanDTO), 0, 8, 1L, 1, true, true);
        when(loanService.getAllLoansPaginated(0, 8)).thenReturn(page);

        mockMvc.perform(get("/loan/filter/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testCreateLoanWithTools_Success() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.createLoanWithTools(any(), any(), any(), any(), any())).thenReturn(loan);

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("clientId", 2);
        body.put("initDate", "2024-01-01");
        body.put("returnDate", "2024-01-10");
        body.put("toolIds", List.of(1, 2));

        mockMvc.perform(post("/loan/create-with-tools/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateLoanWithTools_RuntimeException() throws Exception {
        when(userService.findUserById(1L)).thenReturn(user);
        when(loanService.createLoanWithTools(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Client not found"));

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("clientId", 2);
        body.put("initDate", "2024-01-01");
        body.put("returnDate", "2024-01-10");
        body.put("toolIds", List.of(1));

        mockMvc.perform(post("/loan/create-with-tools/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateLoanWithTools_EmployeeNotFound() throws Exception {
        when(userService.findUserById(1L)).thenReturn(null);

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("clientId", 2);
        body.put("initDate", "2024-01-01");
        body.put("returnDate", "2024-01-10");
        body.put("toolIds", List.of(1));

        mockMvc.perform(post("/loan/create-with-tools/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
