package com.example.demo.DTO;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.ToolEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entidades a DTOs
 */
@Component
public class EntityMapper {
    
    /**
     * Convierte LoanEntity a LoanDTO
     */
    public static LoanDTO toLoanDTO(LoanEntity loan) {
        if (loan == null) return null;
        
        LoanDTO dto = new LoanDTO();
        dto.setId(loan.getId());
        dto.setInitDate(loan.getInitDate());
        dto.setReturnDate(loan.getReturnDate());
        dto.setRealReturnDate(loan.getRealReturnDate());
        dto.setStatus(loan.getStatus());
        
        if (loan.getIdUser() != null) {
            dto.setUserId(loan.getIdUser().getId());
            dto.setUsername(loan.getIdUser().getUsername());
            dto.setClientName(loan.getIdUser().getName() + " " + 
                             (loan.getIdUser().getLastName() != null ? loan.getIdUser().getLastName() : ""));
            dto.setClientEmail(loan.getIdUser().getEmail());
            dto.setClientRut(loan.getIdUser().getRut());
            dto.setClientStateClient(loan.getIdUser().getStateClient());
        }
        
        return dto;
    }
    
    /**
     * Convierte ToolEntity a ToolDTO
     */
    public static ToolDTO toToolDTO(ToolEntity tool) {
        if (tool == null) return null;
        
        ToolDTO dto = new ToolDTO();
        dto.setId(tool.getId());
        dto.setName(tool.getToolName());
        dto.setDescription(""); // ToolEntity no tiene description
        dto.setCost(tool.getPriceRent());
        dto.setRepoCost(tool.getRepoCost());
        dto.setState(""); // Estado viene de InventoryEntity, no de ToolEntity
        dto.setImage(tool.getImageUrl());
        dto.setStock(0); // Stock viene de InventoryEntity, requiere consulta adicional
        
        if (tool.getCategory() != null) {
            dto.setCategoryId(tool.getCategory().getId());
            dto.setCategoryName(tool.getCategory().getName());
        }
        
        return dto;
    }
    
    /**
     * Convierte Page<LoanEntity> a PageResponseDTO<LoanDTO>
     */
    public static PageResponseDTO<LoanDTO> toPageResponseDTO(Page<LoanEntity> page) {
        PageResponseDTO<LoanDTO> response = new PageResponseDTO<>();
        response.setContent(page.getContent().stream()
                .map(EntityMapper::toLoanDTO)
                .toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        return response;
    }
    
    /**
     * Convierte Page<ToolEntity> a PageResponseDTO<ToolDTO>
     */
    public static PageResponseDTO<ToolDTO> toToolPageResponseDTO(Page<ToolEntity> page) {
        PageResponseDTO<ToolDTO> response = new PageResponseDTO<>();
        response.setContent(page.getContent().stream()
                .map(EntityMapper::toToolDTO)
                .toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        return response;
    }
}
