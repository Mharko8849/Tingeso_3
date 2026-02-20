package com.example.demo.Repositories;

import com.example.demo.Entities.ToolEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity,Long> {

    ToolEntity findByToolName(String toolName);

    List<ToolEntity> findByCategory_Name(String category);
    
    Page<ToolEntity> findByCategory_Name(String category, Pageable pageable);

    List<ToolEntity> findByPriceRentGreaterThanEqual(int priceRentIsGreaterThan);

    List<ToolEntity> findByPriceRentLessThanEqual(int priceRentIsLessThan);

    List<ToolEntity> findAllByCategory_Name(String category);

    List<ToolEntity> findByPriceRentGreaterThanEqualAndCategory_Name(int priceRentIsGreaterThan, String category);

    List<ToolEntity> findByPriceRentLessThanEqualAndCategory_Name(int priceRentIsLessThan, String category);

    List<ToolEntity> findAllByOrderByPriceRentDesc();
    
    Page<ToolEntity> findAllByOrderByPriceRentDesc(Pageable pageable);


}
