package com.example.demo.Entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Date;

@Data
@Entity
@Table(name="loan")
@NoArgsConstructor
@AllArgsConstructor

public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idUser", referencedColumnName = "id",  nullable = false)
    private UserEntity idUser;

    private Date initDate;

    private Date returnDate;

    private Date realReturnDate;

    private String status;
}
