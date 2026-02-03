package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name="inventory")
@NoArgsConstructor
@AllArgsConstructor

public class InventoryEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(unique=true, nullable=false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idTool", referencedColumnName = "id",  nullable = false)
    private ToolEntity idTool;

    private String toolState;

    private int stockTool;

}
