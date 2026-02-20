package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para Tool - evita exponer toda la entidad
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDTO {
    private Long id;
    private String name;
    private String description;
    private int cost;
    private int repoCost;
    private String state;
    private Long categoryId;
    private String categoryName;
    private String image;
    private int stock;
}
