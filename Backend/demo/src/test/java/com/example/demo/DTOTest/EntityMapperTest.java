package com.example.demo.DTOTest;

import com.example.demo.DTO.EntityMapper;
import com.example.demo.DTO.LoanDTO;
import com.example.demo.DTO.PageResponseDTO;
import com.example.demo.DTO.ToolDTO;
import com.example.demo.Entities.CategoryEntity;
import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    private LoanEntity loan;
    private UserEntity user;
    private ToolEntity tool;
    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
        user.setName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setRut("12345678-9");
        user.setStateClient("ACTIVO");

        loan = new LoanEntity();
        loan.setId(10L);
        loan.setIdUser(user);
        loan.setInitDate(Date.valueOf("2024-01-01"));
        loan.setReturnDate(Date.valueOf("2024-01-10"));
        loan.setRealReturnDate(Date.valueOf("2024-01-09"));
        loan.setStatus("DEVUELTO");

        category = new CategoryEntity();
        category.setId(2L);
        category.setName("Construction");

        tool = new ToolEntity();
        tool.setId(5L);
        tool.setToolName("Hammer");
        tool.setPriceRent(100);
        tool.setRepoCost(50);
        tool.setImageUrl("hammer.jpg");
        tool.setCategory(category);
    }

    @Test
    void testToLoanDTO_WithUser() {
        LoanDTO dto = EntityMapper.toLoanDTO(loan);
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(1L, dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertNotNull(dto.getClientName());
        assertEquals("test@example.com", dto.getClientEmail());
        assertEquals("12345678-9", dto.getClientRut());
        assertEquals("ACTIVO", dto.getClientStateClient());
        assertEquals("DEVUELTO", dto.getStatus());
        assertEquals(Date.valueOf("2024-01-01"), dto.getInitDate());
    }

    @Test
    void testToLoanDTO_NullLoan() {
        assertNull(EntityMapper.toLoanDTO(null));
    }

    @Test
    void testToLoanDTO_NullUser() {
        loan.setIdUser(null);
        LoanDTO dto = EntityMapper.toLoanDTO(loan);
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertNull(dto.getUserId());
        assertNull(dto.getUsername());
    }

    @Test
    void testToLoanDTO_UserNullLastName() {
        user.setLastName(null);
        LoanDTO dto = EntityMapper.toLoanDTO(loan);
        assertNotNull(dto.getClientName());
    }

    @Test
    void testToToolDTO_WithCategory() {
        ToolDTO dto = EntityMapper.toToolDTO(tool);
        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Hammer", dto.getName());
        assertEquals(100, dto.getCost());
        assertEquals(50, dto.getRepoCost());
        assertEquals("hammer.jpg", dto.getImage());
        assertEquals(2L, dto.getCategoryId());
        assertEquals("Construction", dto.getCategoryName());
    }

    @Test
    void testToToolDTO_NullTool() {
        assertNull(EntityMapper.toToolDTO(null));
    }

    @Test
    void testToToolDTO_NullCategory() {
        tool.setCategory(null);
        ToolDTO dto = EntityMapper.toToolDTO(tool);
        assertNotNull(dto);
        assertNull(dto.getCategoryId());
        assertNull(dto.getCategoryName());
    }

    @Test
    void testToPageResponseDTO() {
        List<LoanEntity> loans = Arrays.asList(loan);
        Page<LoanEntity> page = new PageImpl<>(loans, PageRequest.of(0, 10), 1);
        PageResponseDTO<LoanDTO> result = EntityMapper.toPageResponseDTO(page);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getId());
        assertEquals(0, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    @Test
    void testToPageResponseDTO_Empty() {
        Page<LoanEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        PageResponseDTO<LoanDTO> result = EntityMapper.toPageResponseDTO(page);
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void testToToolPageResponseDTO() {
        List<ToolEntity> tools = Arrays.asList(tool);
        Page<ToolEntity> page = new PageImpl<>(tools, PageRequest.of(0, 5), 1);
        PageResponseDTO<ToolDTO> result = EntityMapper.toToolPageResponseDTO(page);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Hammer", result.getContent().get(0).getName());
    }

    @Test
    void testToRawPageResponseDTO() {
        List<LoanEntity> loans = Arrays.asList(loan);
        Page<LoanEntity> page = new PageImpl<>(loans, PageRequest.of(1, 5), 6);
        PageResponseDTO<LoanEntity> result = EntityMapper.toRawPageResponseDTO(page);
        assertNotNull(result);
        assertEquals(1, result.getPageNumber());
        assertEquals(5, result.getPageSize());
        assertEquals(6L, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void testLoanDTO_AllArgsConstructor() {
        LoanDTO dto = new LoanDTO(1L, 2L, "user", "Client Name", "email@test.com",
                "11.111.111-1", "ACTIVO", Date.valueOf("2024-01-01"),
                Date.valueOf("2024-01-10"), null, "ACTIVO");
        assertEquals(1L, dto.getId());
        assertEquals("user", dto.getUsername());
    }

    @Test
    void testLoanDTO_PartialConstructor() {
        LoanDTO dto = new LoanDTO();
        dto.setId(1L);
        dto.setUserId(2L);
        dto.setUsername("user");
        dto.setClientName("Client Name");
        dto.setInitDate(Date.valueOf("2024-01-01"));
        dto.setReturnDate(Date.valueOf("2024-01-10"));
        dto.setRealReturnDate(null);
        dto.setStatus("ACTIVO");
        assertEquals(1L, dto.getId());
        assertEquals("Client Name", dto.getClientName());
    }

    @Test
    void testLoanDTO_NoArgs() {
        LoanDTO dto = new LoanDTO();
        assertNull(dto.getId());
        dto.setId(5L);
        assertEquals(5L, dto.getId());
    }
}
