package com.example.demo.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@Entity
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor

public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    private String username;

    private String name;

    private String lastName;

    private String rut;

    private String phone;

    private String email;

    @Transient
    private String password;

    private String stateClient;

    private String rol;

    private int loans;
    
    private String keycloakId;
}
