package com.example.demo.Repositories;

import com.example.demo.Entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    UserEntity findByEmail(String email);

    UserEntity findByKeycloakId(String keycloakId);

    UserEntity findByUsernameIgnoreCase(String userName);

    List<UserEntity> findByRol(String rol);

}
