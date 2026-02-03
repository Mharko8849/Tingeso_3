package com.example.demo.Controllers;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.LoanService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/api/loan")
@CrossOrigin("*")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserService userService;

    @Autowired
    private ToolService toolService;

    /*
    GET
     */

    @GetMapping("/user/{idUser}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<LoanEntity>> getAllLoansByUser(@PathVariable Long idUser) {
        UserEntity user = userService.findUserById(idUser);
        List<LoanEntity> loans = loanService.getAllLoansByIdUser(user);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{idLoan}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<LoanEntity> getLoanById(@PathVariable Long idLoan) {
        LoanEntity loan = loanService.getLoanById(idLoan);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<LoanEntity>> getAllLoans() {
        List<LoanEntity> loans = loanService.getAllLoans();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','SUPERADMIN')")
    public ResponseEntity<List<LoanEntity>> filterLoans(@RequestParam(required = false) String state){
        List<LoanEntity> loans = loanService.filter(state);
        return ResponseEntity.ok(loans);
    }

    /*
     POST
     */

    @PostMapping("/create/{idUser}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<LoanEntity> createLoan(
            @RequestBody UserEntity client,
            @PathVariable Long idUser,
            @RequestParam Date initDate,
            @RequestParam Date returnDate) {

        UserEntity clientUser = userService.findUserById(client.getId());
        UserEntity user = userService.findUserById(idUser);

        LoanEntity loan = loanService.createLoan(clientUser, user,  initDate, returnDate);
        return ResponseEntity.ok(loan);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN','SUPERADMIN')")
    public ResponseEntity<Boolean> deleteLoanById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.deleteLoan(id));
    }
}
