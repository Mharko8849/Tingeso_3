package com.example.demo.Repositories;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;


@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    List<LoanEntity> findByIdUser(UserEntity idUser);
    
    Page<LoanEntity> findByIdUser(UserEntity idUser, Pageable pageable);

    List<LoanEntity> findByStatus(String status);
    
    Page<LoanEntity> findByStatus(String status, Pageable pageable);
    
    Page<LoanEntity> findAllByOrderByIdDesc(Pageable pageable);

    List<LoanEntity> findByInitDateIsGreaterThanEqualAndReturnDateIsLessThanEqual(Date initDateIsGreaterThan, Date returnDateIsLessThan);

    List<LoanEntity> findByInitDateIsGreaterThanEqual (Date initDate);

    List<LoanEntity> findByReturnDateGreaterThanEqual (Date finishDate);

    List<LoanEntity> findByReturnDateGreaterThan(Date returnDateIsGreaterThan);

    List<LoanEntity> findByRealReturnDateLessThanEqual(Date finishDate);


}
