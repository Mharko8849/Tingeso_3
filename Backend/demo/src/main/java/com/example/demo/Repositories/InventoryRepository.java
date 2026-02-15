package com.example.demo.Repositories;

import com.example.demo.Entities.InventoryEntity;
import com.example.demo.Entities.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    List<InventoryEntity> findByIdTool(ToolEntity idTool);

    InventoryEntity findByIdToolAndToolState_State(ToolEntity idTool, String toolState);

    List<InventoryEntity> findByStockToolGreaterThan(int stock);

    List<InventoryEntity> findByToolState_State(String toolState);

    List<InventoryEntity> findByIdTool_Category_Name(String category);

    List<InventoryEntity> findByToolState_StateAndIdTool_Category_Name(String toolState, String category);
    
    List<InventoryEntity> findAllByOrderByIdTool_PriceRentAsc();

    List<InventoryEntity> findAllByOrderByIdTool_PriceRentDesc();
}
