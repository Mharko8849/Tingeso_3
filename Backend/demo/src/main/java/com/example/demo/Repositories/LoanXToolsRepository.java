package com.example.demo.Repositories;

import com.example.demo.Entities.LoanEntity;
import com.example.demo.Entities.LoanXToolsEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanXToolsRepository extends JpaRepository<LoanXToolsEntity, Long> {

    List<LoanXToolsEntity> findByIdLoan(LoanEntity idLoan);

    List<LoanXToolsEntity> findByIdEmployeeDel(UserEntity idEmployee);

    List<LoanXToolsEntity> findByIdEmployeeRec(UserEntity idEmployee);

    List<LoanXToolsEntity> findByIdTool_CategoryAndIdLoan_IdUserAndIdLoan_RealReturnDateIsNull(String category, UserEntity user);

    List<LoanXToolsEntity> findByIdLoan_IdUserAndIdToolAndIdLoan_RealReturnDateIsNull(UserEntity user, ToolEntity tool);

    @Query("SELECT COUNT(lxt) > 0 " +
            "FROM LoanXToolsEntity lxt " +
            "WHERE lxt.idLoan.idUser = :idUser " +
            "AND lxt.idTool = :idTool " +
            "AND lxt.idLoan.realReturnDate IS NULL")
    Boolean existActiveLoanWithTool(@Param("idUser") UserEntity idUser,
                                     @Param("idTool") ToolEntity idTool);

}
