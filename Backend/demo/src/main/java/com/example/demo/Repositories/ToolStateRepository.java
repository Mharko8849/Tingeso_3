package com.example.demo.Repositories;

import com.example.demo.Entities.ToolStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolStateRepository extends JpaRepository<ToolStateEntity, Long> {
    ToolStateEntity findByState(String state);
}
