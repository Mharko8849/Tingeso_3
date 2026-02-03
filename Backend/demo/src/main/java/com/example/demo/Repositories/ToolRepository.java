package com.example.demo.Repositories;

import com.example.demo.Entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity,Long> {

    ToolEntity findByToolName(String toolName);

    List<ToolEntity> findByCategory(String category);

    List<ToolEntity> findByPriceRentGreaterThanEqual(int priceRentIsGreaterThan);

    List<ToolEntity> findByPriceRentLessThanEqual(int priceRentIsLessThan);

    List<ToolEntity> findAllByCategory(String category);

    List<ToolEntity> findByPriceRentGreaterThanEqualAndCategory(int priceRentIsGreaterThan, String category);

    List<ToolEntity> findByPriceRentLessThanEqualAndCategory(int priceRentIsLessThan, String category);

    List<ToolEntity> findAllByOrderByPriceRentDesc();


}
