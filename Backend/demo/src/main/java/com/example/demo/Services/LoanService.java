package com.example.demo.Services;

import com.example.demo.Entities.*;
import com.example.demo.Repositories.LoanRepository;
import com.example.demo.Repositories.LoanXToolsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LoanXToolsRepository loanXToolsRepository;

    @Autowired
    private ToolService toolService;

    @Autowired
    private InventoryService inventoryService;

    public LoanEntity saveLoan(LoanEntity loanEntity) {
        return loanRepository.save(loanEntity);
    }

    public List<LoanEntity> getAllLoansByIdUser(UserEntity idUser) {
        return loanRepository.findByIdUser(idUser);
    }

    public LoanEntity getLoanById(Long idLoan) {
        return loanRepository.findById(idLoan).orElseThrow(() -> new RuntimeException("No se encontró el pedido"));
    }

    public List<LoanEntity> getAllLoansByState(String state) {
        return loanRepository.findByStatus(state);
    }

    public List<LoanEntity> getOverdueLoans(){
        Date actualDate = new Date(System.currentTimeMillis());
        List<LoanEntity> loans = getAllLoans();
        return loans.stream()
                .filter(loan -> loan.getReturnDate().before(actualDate))
                .toList();
    }

    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
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
            throw new RuntimeException("El usuario se encuentra restringido.");
        }

        if (!userService.canDoAnotherLoan(idUser)) {
            throw new RuntimeException("El usuario ya cuenta con 5 prestamos.");
        }

        if (!isValidDate(initDate, returnDate)) {
            throw new RuntimeException("La fecha ingresada es inválida. La fecha de devolución debe ser al menos 1 día después de la fecha inicial.");
        }
    }

    public List<LoanEntity> filter(String state){
        if(state==null || state.isBlank()){
            return loanRepository.findAll();
        }
        else if(state.equals("ATRASADO")){
            return getOverdueLoans();
        }
        else{
            return getAllLoansByState(state);
        }
    }

    public boolean deleteLoan(Long loanId){
        try{
            LoanEntity loan = getLoanById(loanId);
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
            throw new RuntimeException("Cliente no encontrado");
        }

        // Validar condiciones del préstamo
        validateConditions(client, initDate, returnDate);

        // Validar que se proporcionaron herramientas
        if (toolIds == null || toolIds.isEmpty()) {
            throw new RuntimeException("Debe proporcionar al menos una herramienta");
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
            throw new RuntimeException("La fecha de devolución debe ser al menos 1 día después de la fecha inicial.");
        }

        // Crear LoanXTools para cada herramienta
        int i = 0;
        while (i < toolIds.size()) {
            Long toolId = toolIds.get(i);
            
            // Buscar la herramienta
            ToolEntity tool = toolService.getToolById(toolId);
            if (tool == null) {
                throw new RuntimeException("Herramienta no encontrada: " + toolId);
            }

            // Validar disponibilidad
            if (!inventoryService.isAvailableTool(tool)) {
                throw new RuntimeException("La herramienta " + tool.getToolName() + " no está disponible");
            }

            // Validar que el cliente no tenga ya esta herramienta prestada
            List<LoanXToolsEntity> existingLoans = loanXToolsRepository
                    .findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(client, tool);
            if (!existingLoans.isEmpty()) {
                throw new RuntimeException("El cliente ya tiene un préstamo activo de la herramienta: " + tool.getToolName());
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
