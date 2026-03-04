package com.example.demo.Services;

import com.example.demo.DTO.EntityMapper;
import com.example.demo.DTO.LoanDTO;
import com.example.demo.DTO.PageResponseDTO;
import com.example.demo.Entities.*;
import com.example.demo.Repositories.LoanRepository;
import com.example.demo.Repositories.LoanXToolsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserService userService;
    private final LoanXToolsRepository loanXToolsRepository;
    private final ToolService toolService;
    private final InventoryService inventoryService;

    public LoanEntity saveLoan(LoanEntity loanEntity) {
        return loanRepository.save(loanEntity);
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> getAllLoansByIdUser(UserEntity idUser) {
        return loanRepository.findByIdUser(idUser);
    }

    @Transactional(readOnly = true)
    public LoanEntity getLoanById(Long idLoan) {
        return loanRepository.findById(idLoan).orElseThrow(() -> new IllegalStateException("No se encontró el pedido"));
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> getAllLoansByState(String state) {
        return loanRepository.findByStatus(state);
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> getOverdueLoans(){
        Date actualDate = new Date(System.currentTimeMillis());
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getReturnDate().before(actualDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }
    
    /**
     * Obtiene préstamos paginados ordenados por ID descendente (más recientes primero)
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<LoanDTO> getAllLoansPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<LoanEntity> loanPage = loanRepository.findAllByOrderByIdDesc(pageable);
        return EntityMapper.toPageResponseDTO(loanPage);
    }
    
    /**
     * Obtiene préstamos filtrados por estado con paginación
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<LoanDTO> getLoansByStatePaginated(String state, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<LoanEntity> loanPage = loanRepository.findByStatus(state, pageable);
        return EntityMapper.toPageResponseDTO(loanPage);
    }
    
    /**
     * Obtiene préstamos de un usuario con paginación
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<LoanDTO> getLoansByUserPaginated(UserEntity user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<LoanEntity> loanPage = loanRepository.findByIdUser(user, pageable);
        return EntityMapper.toPageResponseDTO(loanPage);
    }

    public LoanEntity createLoan(UserEntity idClient, UserEntity user, Date initDate, Date returnDate) {

        // Validar que el usuario que crea el préstamo sea admin o empleado
        userService.validateAdminOrEmployee(user);
        idClient.setLoans(idClient.getLoans() + 1);
        userService.saveUser(idClient);

        validateConditions(idClient,initDate,returnDate);

        LoanEntity loan = new LoanEntity();
        loan.setIdUser(idClient);
        loan.setInitDate(initDate);
        loan.setReturnDate(returnDate);
        loan.setStatus("ACTIVO");

        return loanRepository.save(loan);
    }

    /* Métodos auxiliares */

    public boolean isUserRestringed(UserEntity idUser) {
        if (idUser.getStateClient().equals("RESTRINGIDO")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isValidDate(Date initDate, Date returnDate) {
        if (initDate != null && returnDate != null) {
            java.time.LocalDate init = initDate.toLocalDate();
            java.time.LocalDate ret = returnDate.toLocalDate();
            if (ret.isAfter(init)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void validateConditions(UserEntity idUser, Date initDate, Date returnDate) {
        if (isUserRestringed(idUser)) {
            throw new IllegalStateException("El usuario se encuentra restringido.");
        }

        if (!userService.canDoAnotherLoan(idUser)) {
            throw new IllegalStateException("El usuario ya cuenta con 5 prestamos.");
        }

        if (!isValidDate(initDate, returnDate)) {
            throw new IllegalStateException("La fecha ingresada es inválida. La fecha de devolución debe ser al menos 1 día después de la fecha inicial.");
        }
    }

    @Transactional(readOnly = true)
    public List<LoanEntity> filter(String state){
        if (state == null || state.isBlank()) {
            return loanRepository.findAll();
        } else if (state.equals("ATRASADO")) {
            Date actualDate = new Date(System.currentTimeMillis());
            return loanRepository.findAll().stream()
                    .filter(loan -> loan.getReturnDate().before(actualDate))
                    .toList();
        } else {
            return loanRepository.findByStatus(state);
        }
    }

    @Transactional
    public boolean deleteLoan(Long loanId){
        try{
            LoanEntity loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new IllegalStateException("No se encontró el pedido"));
            UserEntity user = loan.getIdUser();

            user.setLoans(user.getLoans() - 1);
            userService.saveUser(user);

            loanRepository.deleteById(loanId);
            return true;
        } catch (Exception ex){
            return false;
        }
    }

    /**
     * Crea un Loan y sus LoanXTools asociados en una transacción atómica.
     * Si alguna validación o creación falla, todo se revierte automáticamente.
     * @param employee El empleado que crea el préstamo
     * @param clientId ID del cliente
     * @param initDate Fecha de inicio
     * @param returnDate Fecha de devolución
     * @param toolIds Lista de IDs de herramientas
     * @return El Loan creado con sus LoanXTools
     */
    @Transactional
    public LoanEntity createLoanWithTools(UserEntity employee, Long clientId, Date initDate, Date returnDate, List<Long> toolIds) {
        // Validar que el empleado tiene permisos
        userService.validateAdminOrEmployee(employee);

        // Buscar el cliente
        UserEntity client = userService.findUserById(clientId);
        if (client == null) {
            throw new IllegalStateException("Cliente no encontrado");
        }

        // Validar condiciones del préstamo
        validateConditions(client, initDate, returnDate);

        // Validar que se proporcionaron herramientas
        if (toolIds == null || toolIds.isEmpty()) {
            throw new IllegalStateException("Debe proporcionar al menos una herramienta");
        }

        // Incrementar contador de préstamos del cliente
        client.setLoans(client.getLoans() + 1);
        userService.saveUser(client);

        // Crear el préstamo
        LoanEntity loan = new LoanEntity();
        loan.setIdUser(client);
        loan.setInitDate(initDate);
        loan.setReturnDate(returnDate);
        loan.setStatus("ACTIVO");
        loan = loanRepository.save(loan);

        // Validar fechas para LoanXTools
        java.time.LocalDate init = initDate.toLocalDate();
        java.time.LocalDate ret = returnDate.toLocalDate();
        if (!ret.isAfter(init)) {
            throw new IllegalStateException("La fecha de devolución debe ser al menos 1 día después de la fecha inicial.");
        }

        // Crear LoanXTools para cada herramienta
        int i = 0;
        while (i < toolIds.size()) {
            Long toolId = toolIds.get(i);
            
            // Buscar la herramienta
            ToolEntity tool = toolService.getToolById(toolId);
            if (tool == null) {
                throw new IllegalStateException("Herramienta no encontrada: " + toolId);
            }

            // Validar disponibilidad
            if (!inventoryService.isAvailableTool(tool)) {
                throw new IllegalStateException("La herramienta " + tool.getToolName() + " no está disponible");
            }

            // Validar que el cliente no tenga ya esta herramienta prestada
            List<LoanXToolsEntity> existingLoans = loanXToolsRepository
                    .findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(client, tool);
            if (!existingLoans.isEmpty()) {
                throw new IllegalStateException("El cliente ya tiene un préstamo activo de la herramienta: " + tool.getToolName());
            }

            // Crear LoanXToolsEntity
            LoanXToolsEntity lxt = new LoanXToolsEntity();
            lxt.setIdLoan(loan);
            lxt.setIdTool(tool);
            lxt.setDebt((int) tool.getPriceRent());
            lxt.setFine(0);
            lxt.setNeedRepair(false);
            loanXToolsRepository.save(lxt);
            
            i += 1;
        }

        return loan;
    }
}
