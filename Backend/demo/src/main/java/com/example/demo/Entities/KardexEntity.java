package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Date;

@Data
@Entity
@Table(name="kardex")
@NoArgsConstructor
@AllArgsConstructor

public class KardexEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(unique=true, nullable=false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idTool", referencedColumnName = "id", nullable = false)
    private ToolEntity idTool;

    private String type;

    private Date date;

    private int cant;

    private Integer cost;

    @ManyToOne
    @JoinColumn(name = "idUser", referencedColumnName = "id", nullable = true)
    private UserEntity idUser;

    @ManyToOne
    @JoinColumn(name = "idEmployee", referencedColumnName = "id", nullable = false)
    private UserEntity idEmployee;

}
