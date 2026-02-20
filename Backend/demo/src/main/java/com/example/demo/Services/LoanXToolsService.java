package com.example.demo.Services;

import com.example.demo.Entities.*;
import com.example.demo.Repositories.LoanRepository;
import com.example.demo.Repositories.LoanXToolsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class LoanXToolsService {

    @Autowired
    private LoanXToolsRepository loanXToolsRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private KardexService kardexService;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private ToolService toolService;
    @Autowired
    private LoanService loanService;

    public LoanXToolsEntity saveLoanXToolsEntity(LoanXToolsEntity loanXToolsEntity) {
        return loanXToolsRepository.save(loanXToolsEntity);
    }

    public LoanXToolsEntity findLoanXToolsEntityById(Long id) {
        return loanXToolsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relacion no encontrada."));
    }

    public int getTotalDebt(LoanEntity loan){
        List<LoanXToolsEntity> lxt = getAllLoanXToolsByIdLoan(loan);
        int totalDebt = 0, i = 0;
        while(i<lxt.size()){
            totalDebt += lxt.get(i).getDebt();
            i+=1;
        }
        return  totalDebt;
    }

    public int getTotalFine(LoanEntity loan){
        List<LoanXToolsEntity> lxt = getAllLoanXToolsByIdLoan(loan);
        int totalFine = 0, i = 0;
        while(i<lxt.size()){
            totalFine += lxt.get(i).getFine();
            i+=1;
        }
        return totalFine;
    }

    public UserEntity getUserEntityByIdLoanXTool(Long id){
        LoanXToolsEntity lxt = findLoanXToolsEntityById(id);
        LoanEntity loan = loanService.getLoanById(lxt.getIdLoan().getId());
        return loan.getIdUser();
    }

    @Transactional
    public void deleteLoanXToolsById(Long idUser, Long idLoanXTools) {
        UserEntity user = userService.findUserById(idUser);
        userService.validateAdminOrEmployee(user);

        LoanXToolsEntity lxt = findLoanXToolsEntityById(idLoanXTools);
        if (lxt.getToolActivity() != null && !lxt.getToolActivity().isBlank()) {
            throw new RuntimeException("No se puede eliminar una relation que ya tiene actividad.");
        }

        loanXToolsRepository.deleteById(idLoanXTools);
    }

    public List<LoanXToolsEntity> getAllLoanXTools(){
        return loanXToolsRepository.findAll();
    }

    public List<LoanXToolsEntity> getAllLoanXToolsByIdLoan(LoanEntity idLoan){
        return loanXToolsRepository.findByIdLoan(idLoan);
    }

    public List<LoanXToolsEntity> getAllLoanXToolsByIdUser(UserEntity idUser){
        List<LoanEntity> loans = loanService.getAllLoansByIdUser(idUser);
        List<LoanXToolsEntity> loanXTools = new ArrayList<>();
        int i = 0;
        while(i<loans.size()){
            // getAllLoanXToolsByIdLoan returns a List<LoanXToolsEntity>, addAll to flatten
            loanXTools.addAll(getAllLoanXToolsByIdLoan(loans.get(i)));
            i+=1;
        }
        return loanXTools;
    }

    public boolean isToolLoanedToUser(ToolEntity idTool, UserEntity idUser){
        List<LoanXToolsEntity> tools = loanXToolsRepository
                .findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(idUser, idTool);
        return tools.size() > 0;
    }

    @Transactional
    public LoanXToolsEntity giveLoanTool(UserEntity employee, Long idLoanXTool){
        userService.validateAdminOrEmployee(employee);

        LoanXToolsEntity loanXToolsEntity = findLoanXToolsEntityById(idLoanXTool);

        UserEntity user = getUserEntityByIdLoanXTool(idLoanXTool);

        if (!loanXToolsEntity.getIdLoan().getStatus().equals("ACTIVO")) {
            throw new RuntimeException("El pedido ya se encuentra finalizado.");
        }

        if (loanXToolsEntity.getToolActivity() != null && !loanXToolsEntity.getToolActivity().isBlank()) {
            throw new RuntimeException("El pedido ya cuenta con actividades previas, por lo tanto no puede ser entregado.");
        }

        if (loanXToolsEntity.getIdLoan() == null) {
            throw new RuntimeException("No se encuentra el pedido solicitado");
        }
        if (loanXToolsEntity.getIdTool() == null) {
            throw new RuntimeException("No se encuentra la herramienta del pedido");
        }
        if (!inventoryService.isAvailableTool(loanXToolsEntity.getIdTool())) {
            throw new RuntimeException("No se encuentra stock disponible para ese producto");
        }

        Date actualDate = new Date(System.currentTimeMillis());
        inventoryService.loanTool(loanXToolsEntity.getIdTool().getId());
        loanXToolsEntity.setToolActivity("PRESTADA");
        loanXToolsEntity.setIdEmployeeDel(employee);
        kardexService.createKardexEntity(loanXToolsEntity.getIdTool(), "PRESTAMO", actualDate, 1, null, user, employee);

        return loanXToolsRepository.save(loanXToolsEntity);
    }

    @Transactional
    public List<LoanXToolsEntity> giveAllLoanTools(UserEntity idUser, List<Long> ids){
        List<LoanXToolsEntity> results = new ArrayList<>();
        int i = 0;
        while (i < ids.size()) {
            Long id = ids.get(i);
            LoanXToolsEntity updated = giveLoanTool(idUser, id);
            results.add(updated);
            i+=1;
        }
        return results;
    }

    @Transactional
    public LoanXToolsEntity createLoanXTool(Long loanId, Long toolId) {
        // Fetch loan
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("No se encontró el pedido solicitado"));

        // Fetch tool
        ToolEntity tool = toolService.getToolById(toolId);

        // Validate user restriction
        UserEntity client = loan.getIdUser();
        if (loanService.isUserRestringed(client)) {
            throw new RuntimeException("El usuario se encuentra restringido y no puede solicitar préstamos.");
        }

        // Validate dates
        Date initDate = loan.getInitDate();
        Date returnDate = loan.getReturnDate();
        if (initDate == null || returnDate == null) {
            throw new RuntimeException("Las fechas del pedido son inválidas.");
        }
        java.time.LocalDate init = initDate.toLocalDate();
        java.time.LocalDate ret = returnDate.toLocalDate();
        if (!ret.isAfter(init)) {
            throw new RuntimeException("La fecha ingresada es inválida. La fecha de devolución debe ser al menos 1 día después de la fecha inicial.");
        }

        // Availability
        if (!inventoryService.isAvailableTool(tool)) {
            throw new RuntimeException("La herramienta solicitada no se encuentra disponible.");
        }

        // Duplicate tool for user
        if (isToolLoanedToUser(tool, client)) {
            throw new RuntimeException("El usuario ya cuenta con un préstamo de dicha herramienta activo.");
        }

        // Create LoanXToolsEntity
        LoanXToolsEntity lxt = new LoanXToolsEntity();
        lxt.setIdLoan(loan);
        lxt.setIdTool(tool);
        lxt.setDebt((int) tool.getPriceRent());
        lxt.setFine(0);
        lxt.setNeedRepair(false);

        return loanXToolsRepository.save(lxt);
    }

    public int calculateFineByDate(LoanXToolsEntity lxt){
        LoanEntity loan = lxt.getIdLoan();
        ToolEntity tool = lxt.getIdTool();

        Date returnDate = loan.getReturnDate();
        Date realReturnDate = new Date(System.currentTimeMillis());

        java.time.LocalDate expected = returnDate.toLocalDate();
        java.time.LocalDate real = realReturnDate.toLocalDate();

        if (!real.isAfter(expected)) {
            return 0;
        }

        long daysLate = java.time.temporal.ChronoUnit.DAYS.between(expected, real);
        long fine = daysLate * tool.getPriceFineAtDate();

        return (int) fine;
    }

    public int calculateFineByStateToolReturn(LoanXToolsEntity lxt, String state) {
        switch (state) {
            case "SIN DAÑO", "DAÑO" -> {
                return 0;
            }
            case "IRREPARABLE" -> {
                int fine = lxt.getIdTool().getRepoCost();
                return fine;
            }
            default -> throw new RuntimeException("Tipo de daño inválido");
        }
    }

    public int calculateFine(LoanXToolsEntity lxt, String state) {
        int daysFine = calculateFineByDate(lxt);
        int stateFine = calculateFineByStateToolReturn(lxt, state);
        return daysFine + stateFine;
    }

    public boolean allToolsReturned(LoanEntity loan) {
        List<LoanXToolsEntity> list = getAllLoanXToolsByIdLoan(loan);
        int i = 0;
        while (i < list.size()) {
            String act = list.get(i).getToolActivity();
            if (act == null || !act.equals("DEVUELTA")) {
                return false;
            }
            i += 1;
        }
        return true;
    }


    @Transactional
    public LoanXToolsEntity receiveLoanTool(Long idUser, Long idLoanXTool, String damageTool){
        UserEntity employee = userService.findUserById(idUser);
        userService.validateAdminOrEmployee(employee);

        LoanXToolsEntity loanXToolsEntity = findLoanXToolsEntityById(idLoanXTool);

        UserEntity user = getUserEntityByIdLoanXTool(idLoanXTool);

        if (loanXToolsEntity.getToolActivity() == null || loanXToolsEntity.getToolActivity().isBlank()) {
            throw new RuntimeException("Error en la actividad del pedido");
        }
        if (loanXToolsEntity.getIdLoan() == null || loanXToolsEntity.getIdTool() == null) {
            throw new RuntimeException("No se encuentra la herramienta del pedido");
        }

        String stateTool;

        switch (damageTool) {
            case "SIN DAÑO" -> {
                inventoryService.receiveTool(loanXToolsEntity.getIdTool().getId(), "DISPONIBLE");
                stateTool = "DEVOLUCION";
            }
            case "DAÑO" -> {
                loanXToolsEntity.setNeedRepair(true);
                inventoryService.receiveTool(loanXToolsEntity.getIdTool().getId(), "EN REPARACION");
                stateTool = "REPARACION";
            }
            case "IRREPARABLE" -> {
                inventoryService.receiveTool(loanXToolsEntity.getIdTool().getId(), "DADA DE BAJA");
                stateTool = "BAJA";
            }
            case null, default -> throw new RuntimeException("Tipo de daño inválido");
        }

        Date actualDate = new Date(System.currentTimeMillis());

        int fine = calculateFine(loanXToolsEntity,damageTool);

        if (fine!=0){
            loanXToolsEntity.setFine(fine);
        }

        loanXToolsEntity.setToolActivity("DEVUELTA");
        loanXToolsEntity.setIdEmployeeRec(employee);
        kardexService.createKardexEntity(loanXToolsEntity.getIdTool(), stateTool, actualDate, 1,null, user,employee);

        return loanXToolsRepository.save(loanXToolsEntity);
    }

    @Transactional
    public List<LoanXToolsEntity> mapAndReceiveAllTools(
            Long loanId,
            Long idEmployee,
            Map<String, String> stateToolRaw) {

        // Convertir Map<String, String> → Map<Long, String> usando WHILE
        Map<Long, String> stateTool = new HashMap<>();

        var iterator = stateToolRaw.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();

            try {
                Long key = Long.parseLong(entry.getKey());
                stateTool.put(key, entry.getValue());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Clave inválida: " + entry.getKey());
            }
        }

        // Obtener el loan
        LoanEntity loan = loanService.getLoanById(loanId);

        // Obtener herramientas asociadas al loan
        List<LoanXToolsEntity> lxtList = getAllLoanXToolsByIdLoan(loan);

        // Delegar a la verdadera lógica de negocio
        return receiveAllLoanTools(idEmployee, lxtList, stateTool);
    }


    @Transactional
    public List<LoanXToolsEntity> receiveAllLoanTools(Long idEmployee, List<LoanXToolsEntity> lxtList, Map<Long, String> states) {

        LoanEntity loan = lxtList.get(0).getIdLoan();

        UserEntity client = loan.getIdUser();

        List<LoanXToolsEntity> results = new ArrayList<>();
        int totalFine = 0;
        boolean anyNeedRepair = false;

        if (lxtList.size() != states.size()) {
            throw new IllegalArgumentException("Cantidad de estados no coincide con cantidad de herramientas");
        }

        int i = 0;
        while (i < lxtList.size()) {
            LoanXToolsEntity lxt = lxtList.get(i);

            if (!states.containsKey(lxt.getId())) {
                 throw new IllegalArgumentException("Falta el estado para la herramienta con id: " + lxt.getId());
             }

            String state = states.get(lxt.getId());

            LoanXToolsEntity updated = receiveLoanTool(idEmployee, lxt.getId(), state);
            results.add(updated);

            if (updated.getFine() != 0) {
                totalFine += updated.getFine();
            }
            if ("DAÑO".equals(state)) {
                anyNeedRepair = true;
            }

            i += 1;
        }

        if (allToolsReturned(loan)) {
            Date actualDate = new Date(System.currentTimeMillis());
            loan.setRealReturnDate(actualDate);

            client.setLoans(client.getLoans() - 1);

            // Un pedido solo está FINALIZADO cuando no hay multas pendientes NI reparaciones pendientes
            if (totalFine == 0 && !anyNeedRepair) {
                loan.setStatus("FINALIZADO");
                if(!userHaveDebt(client)){
                    client.setStateClient("ACTIVO");
                }
            }
            else {
                // Si hay multa O necesita reparación, el pedido está PENDIENTE hasta que se pague/repare
                loan.setStatus("PENDIENTE");
                client.setStateClient("RESTRINGIDO");
            }

            loanService.saveLoan(loan);
            userService.saveUser(client);
        }

        return results;
    }


    public boolean userHaveDebt(UserEntity user){
        List<LoanXToolsEntity> lxt = getAllLoanXToolsByIdUser(user);
        int i = 0;
        while (i<lxt.size()) {
            if(lxt.get(i).getFine()!=0){
                return true;
            }
            i+=1;
        }
        return false;
    }

    public boolean needRepairToolByLoan(Long loanId){
        LoanEntity loan = loanService.getLoanById(loanId);
        List<LoanXToolsEntity> lxt = getAllLoanXToolsByIdLoan(loan);
        int i = 0;
        while (i<lxt.size()) {
            if(lxt.get(i).getNeedRepair()){
                return true;
            }
            i+=1;
        }
        return false;
    }

    public List<LoanXToolsEntity> needRepairTools(Long loanId){
        LoanEntity loan = loanService.getLoanById(loanId);
        List<LoanXToolsEntity> results = getAllLoanXToolsByIdLoan(loan);
        results = results.stream().filter(lxt -> lxt.getNeedRepair().equals(Boolean.TRUE)).toList();
        return results;
    }

    @Transactional
    public Boolean mapAndPayRepairTool(Long loanId, Map<String, Object> body) {

        Object adminObj = body.get("adminUser");
        Object costObj = body.get("cost");

        if (adminObj == null || costObj == null) {
            throw new IllegalArgumentException("adminUser y cost son obligatorios");
        }

        if (!(adminObj instanceof Number) || !(costObj instanceof Number)) {
            throw new IllegalArgumentException("adminUser y cost deben ser numéricos");
        }

        Long adminUser = ((Number) adminObj).longValue();
        int cost = ((Number) costObj).intValue();

        // Delegar a la lógica real
        return payRepairTool(loanId, adminUser, cost);
    }


    @Transactional
    public boolean payDebt(Long loanId, Long adminUser) {
        UserEntity admin = userService.findUserById(adminUser);
        userService.validateAdminOrEmployee(admin);
        LoanEntity loan = loanService.getLoanById(loanId);
        UserEntity user = loanService.getLoanById(loanId).getIdUser();
        List<LoanXToolsEntity> lxt = getAllLoanXToolsByIdLoan(loan);
        if(loanService.isUserRestringed(user) || userHaveDebt(user)){
            int i = 0;
            while(i<lxt.size()){
                LoanXToolsEntity l = lxt.get(i);
                Date actualDate = new Date(System.currentTimeMillis());
                kardexService.createKardexEntity(l.getIdTool(),"PAGO DEUDA",actualDate,1, l.getFine(),user, admin);
                l.setFine(0);
                saveLoanXToolsEntity(l);
                i+=1;
            }
            if (!userHaveDebt(user) && !needRepairToolByLoan(loanId)) {
                user.setStateClient("ACTIVO");
            }

            userService.saveUser(user);

            if (!needRepairToolByLoan(loanId)) {
                loan.setStatus("FINALIZADO");
            }
            loanRepository.save(loan);
            return true;
        }else{
            return false;
        }
    }

    @Transactional
    public boolean payRepairTool(Long loanId, Long adminUser, int cost) {
        UserEntity admin = userService.findUserById(adminUser);
        userService.validateAdminOrEmployee(admin);

        LoanEntity loan = loanService.getLoanById(loanId);
        UserEntity user = loan.getIdUser();

        List<LoanXToolsEntity> lxtList = getAllLoanXToolsByIdLoan(loan);

        boolean hadRepairPending = false;

        int i = 0;
        while (i < lxtList.size()) {
            LoanXToolsEntity lxt = lxtList.get(i);

            if (lxt.getNeedRepair().equals(true)) {
                hadRepairPending = true;

                Date actualDate = new Date(System.currentTimeMillis());

                kardexService.createKardexEntity(lxt.getIdTool(),"PAGO REPARACION", actualDate,1, cost, user, admin);

                lxt.setNeedRepair(false);
                loanXToolsRepository.save(lxt);

                inventoryService.repairTool(lxt.getIdTool().getId());
            }
            i++;
        }

        if (hadRepairPending) {
            // Verificar si podemos liberar al usuario (solo si no tiene más deudas en ningún préstamo)
            if (!userHaveDebt(user)) {
                user.setStateClient("ACTIVO");
            }
            userService.saveUser(user);

            // Solo marcar el préstamo como FINALIZADO si no hay multas pendientes
            if (getTotalFine(loan) == 0) {
                loan.setStatus("FINALIZADO");
            }
            loanService.saveLoan(loan);

            return true;
        }

        return false;
    }

    public LoanEntity closeStrangeLoan(Long loanId) {
        LoanEntity loan = loanService.getLoanById(loanId);
        List<LoanXToolsEntity> lxt =  getAllLoanXToolsByIdLoan(loan);
        if (loan.getStatus().equals("ACTIVO") && lxt.isEmpty()) {
            loan.setStatus("FINALIZADO");
        }
        return loanService.saveLoan(loan);
    }

}
