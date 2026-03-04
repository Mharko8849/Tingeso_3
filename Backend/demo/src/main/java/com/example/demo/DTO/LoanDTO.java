package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

/**
 * DTO para Loan - evita exponer toda la entidad y previene lazy loading issues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private Long id;
    private Long userId;
    private String username;
    private String clientName;
    private String clientEmail;
    private String clientRut;
    private String clientStateClient;
    private Date initDate;
    private Date returnDate;
    private Date realReturnDate;
    private String status;
}
