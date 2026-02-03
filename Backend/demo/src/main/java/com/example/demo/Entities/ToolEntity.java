package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name="tool")
@NoArgsConstructor
@AllArgsConstructor

public class ToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    private String toolName;

    private String category;

    private int repoCost;

    private int priceRent;

    private int priceFineAtDate;

    private String imageUrl;
}
