package com.example.demo.Repositories;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity,Long> {

    List<KardexEntity> findByDate(Date date);

    List<KardexEntity> findByIdUser(UserEntity userId);

    List<KardexEntity> findByType(String type);

    List<KardexEntity> findByIdTool(ToolEntity idTool);

    List<KardexEntity> findByDateGreaterThan(Date date);

    List<KardexEntity> findByDateLessThan(Date date);

    List<KardexEntity> findByDateBetween(Date date1, Date date2);

}
