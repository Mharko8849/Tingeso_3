package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name="loanXtools")
@NoArgsConstructor
@AllArgsConstructor

public class LoanXToolsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idLoan", referencedColumnName = "id", nullable = false)
    private LoanEntity idLoan;

    @ManyToOne
    @JoinColumn(name = "idTool", referencedColumnName = "id",  nullable = false)
    private ToolEntity idTool;

    @ManyToOne
    @JoinColumn(name = "idEmployeeDel", referencedColumnName = "id", nullable = true)
    private UserEntity idEmployeeDel;

    @ManyToOne
    @JoinColumn(name = "idEmployeeRec", referencedColumnName = "id", nullable = true)
    private UserEntity idEmployeeRec;

    private String toolActivity;

    private int debt;

    private int fine;

    private Boolean needRepair;

}
