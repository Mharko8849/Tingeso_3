package com.example.demo.Controllers;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.LoanXToolsEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.LoanService;
import com.example.demo.Services.LoanXToolsService;
import com.example.demo.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/loantool", "/loantool"})
@CrossOrigin("*")
public class LoanXToolsController {

    @Autowired
    private LoanXToolsService loanXToolsService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private com.example.demo.Services.ToolService toolService;

    @Autowired
    private UserService userService;


    /*
    GET
     */

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<LoanXToolsEntity>> getAllLoanTools() {
        List<LoanXToolsEntity> loanTools = loanXToolsService.getAllLoanXTools();
        return ResponseEntity.ok(loanTools);
    }

    @GetMapping("/loan/{idLoan}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<LoanXToolsEntity>> getByLoan(@PathVariable Long idLoan) {
        LoanEntity loan = loanService.getLoanById(idLoan);
        List<LoanXToolsEntity> loanTools = loanXToolsService.getAllLoanXToolsByIdLoan(loan);
        return ResponseEntity.ok(loanTools);
    }

    @GetMapping("/validate/{idUser}/{idTool}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SUPERADMIN')")
    public ResponseEntity<Boolean> isToolLoaned(@PathVariable Long idUser, @PathVariable Long idTool) {
        UserEntity user = userService.findUserById(idUser);
        ToolEntity tool = toolService.getToolById(idTool);
        boolean result = loanXToolsService.isToolLoanedToUser(tool, user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fine/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'SUPERADMIN')")
    public ResponseEntity<Integer> fineById(@PathVariable Long id, @RequestParam String state) {
        LoanXToolsEntity lxt = loanXToolsService.findLoanXToolsEntityById(id);
        int fine = loanXToolsService.calculateFine(lxt,state);
        return ResponseEntity.ok(fine);
    }

    @GetMapping("/total/{idLoan}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<Integer> getTotal(@PathVariable Long idLoan) {
        LoanEntity loan = loanService.getLoanById(idLoan);
        Integer lxt =  loanXToolsService.getTotalDebt(loan);
        return ResponseEntity.ok(lxt);
    }

    @GetMapping("/total/fine/{idLoan}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<Integer> getTotalFine(@PathVariable Long idLoan) {
        LoanEntity loan = loanService.getLoanById(idLoan);
        Integer lxt = loanXToolsService.getTotalFine(loan);
        return ResponseEntity.ok(lxt);
    }

    @GetMapping("/all/loans/{idUser}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<List<LoanXToolsEntity>> getAllLoanToolsByUser(@PathVariable Long idUser) {
        UserEntity user = userService.findUserById(idUser);
        List<LoanXToolsEntity> txl = loanXToolsService.getAllLoanXToolsByIdUser(user);
        return ResponseEntity.ok(txl);
    }

    @GetMapping("/repair/{loanId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<List<LoanXToolsEntity>> getAllRepairTools(@PathVariable Long loanId){
        List<LoanXToolsEntity> lxt = loanXToolsService.needRepairTools(loanId);
        return ResponseEntity.ok(lxt);
    }

    /*
    POST
     */

    @PostMapping("/give/{id}/user/{idUser}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<LoanXToolsEntity> giveLoanTool(@PathVariable Long id, @PathVariable Long idUser) {
        UserEntity user = userService.findUserById(idUser);
        LoanXToolsEntity loanTool = loanXToolsService.giveLoanTool(user, id);
        return ResponseEntity.ok(loanTool);
    }

    @PostMapping("/create/{loanId}/{toolId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<LoanXToolsEntity> createLoanXTool(@PathVariable Long loanId, @PathVariable Long toolId) {
        LoanXToolsEntity loanTool = loanXToolsService.createLoanXTool(loanId, toolId);
        return ResponseEntity.ok(loanTool);
    }

    @PostMapping("/give/all/user/{idUser}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<LoanXToolsEntity>> giveLoanTools(@PathVariable Long idUser, @RequestBody List<Long> ids) {
        UserEntity user = userService.findUserById(idUser);
        List<LoanXToolsEntity> updated = loanXToolsService.giveAllLoanTools(user, ids);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/receive/{id}/user/{idUser}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<LoanXToolsEntity> receiveLoanTool(@PathVariable Long id, @PathVariable Long idUser, @RequestParam String stateTool) {
        LoanXToolsEntity loanTool = loanXToolsService.receiveLoanTool(idUser, id, stateTool);
        return ResponseEntity.ok(loanTool);
    }

    @PostMapping({"/receive/all/{loanId}/{userId}", "/receive/all/loan/{loanId}/user/{userId}"})
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<?> receiveAllTools(
            @PathVariable Long loanId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> state) {

        var response = loanXToolsService.mapAndReceiveAllTools(loanId, userId, state);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/paydebt/{idLoan}/user/{idUser}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<Boolean> payDebt(@PathVariable Long idLoan, @PathVariable Long idUser) {
        Boolean paydebt = loanXToolsService.payDebt(idLoan,idUser);
        return ResponseEntity.ok(paydebt);
    }

    @PostMapping("/repair/{loanId}/pay")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<Boolean> payRepair(
            @PathVariable Long loanId,
            @RequestBody Map<String, Object> body) {

        Boolean response = loanXToolsService.mapAndPayRepairTool(loanId, body);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/close/loan/{idLoan}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<?> closeStrangeLoan(@PathVariable Long idLoan) {
        LoanEntity loan =  loanXToolsService.closeStrangeLoan(idLoan);
        return ResponseEntity.ok(loan);
    }

    /*
    DELETE
     */
    @DeleteMapping("/{id}/user/{idUser}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<Boolean> deleteLoanXTool(@PathVariable Long id, @PathVariable Long idUser) {
        loanXToolsService.deleteLoanXToolsById(idUser, id);
        return ResponseEntity.ok(true);
    }
}
