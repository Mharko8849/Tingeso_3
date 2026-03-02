package com.example.demo.Repositories;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    List<KardexEntity> findByDate(Date date);

    List<KardexEntity> findByIdUser(UserEntity userId);

    List<KardexEntity> findByType(String type);

    List<KardexEntity> findByIdTool(ToolEntity idTool);

    List<KardexEntity> findByDateGreaterThan(Date date);

    List<KardexEntity> findByDateLessThan(Date date);

    List<KardexEntity> findByDateBetween(Date date1, Date date2);

    // Paginado sin filtros: todos los registros
    Page<KardexEntity> findAll(Pageable pageable);

    /**
     * Filtra kardex con parámetros opcionales y devuelve una página.
     * Usa SQL nativo con PostgreSQL para evitar problemas de inferencia de tipos
     * de Hibernate 6 con parámetros null en JPQL.
     * Cuando un parámetro es NULL, la condición se ignora (NULL IS NULL → TRUE).
     */
    @Query(
        value = """
            SELECT * FROM kardex k
            WHERE (:type       IS NULL OR UPPER(k.type)      = UPPER(:type))
              AND (:idTool     IS NULL OR k.id_tool          = CAST(:idTool AS bigint))
              AND (:idUser     IS NULL OR k.id_user          = CAST(:idUser AS bigint))
              AND (:idEmployee IS NULL OR k.id_employee      = CAST(:idEmployee AS bigint))
              AND (:initDate   IS NULL OR k.date             >= TO_DATE(:initDate, 'YYYY-MM-DD'))
              AND (:finalDate  IS NULL OR k.date             <= TO_DATE(:finalDate, 'YYYY-MM-DD'))
            ORDER BY k.id DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM kardex k
            WHERE (:type       IS NULL OR UPPER(k.type)      = UPPER(:type))
              AND (:idTool     IS NULL OR k.id_tool          = CAST(:idTool AS bigint))
              AND (:idUser     IS NULL OR k.id_user          = CAST(:idUser AS bigint))
              AND (:idEmployee IS NULL OR k.id_employee      = CAST(:idEmployee AS bigint))
              AND (:initDate   IS NULL OR k.date             >= TO_DATE(:initDate, 'YYYY-MM-DD'))
              AND (:finalDate  IS NULL OR k.date             <= TO_DATE(:finalDate, 'YYYY-MM-DD'))
            """,
        nativeQuery = true)
    Page<KardexEntity> filterKardexPaginated(
            @Param("type")       String type,
            @Param("idTool")     Long   idTool,
            @Param("idUser")     Long   idUser,
            @Param("idEmployee") Long   idEmployee,
            @Param("initDate")   String initDate,
            @Param("finalDate")  String finalDate,
            Pageable pageable);

    /**
     * Ranking de herramientas más prestadas del mes — GROUP BY + SUM + LIMIT en la BD.
     */
    @Query("""
            SELECT k.idTool, SUM(k.cant)
            FROM KardexEntity k
            WHERE UPPER(k.type) = 'PRESTAMO'
              AND k.date BETWEEN :initDate AND :finalDate
            GROUP BY k.idTool
            ORDER BY SUM(k.cant) DESC
            LIMIT 10
            """)
    List<Object[]> getRankingTop10(
            @Param("initDate") Date initDate,
            @Param("finalDate") Date finalDate);

    /**
     * Ranking por rango de fechas arbitrario (sin límite de 10).
     */
    @Query("""
            SELECT k.idTool, SUM(k.cant)
            FROM KardexEntity k
            WHERE UPPER(k.type) = 'PRESTAMO'
              AND k.date BETWEEN :initDate AND :finalDate
            GROUP BY k.idTool
            ORDER BY SUM(k.cant) DESC
            """)
    List<Object[]> getRankingByDateRangeQuery(
            @Param("initDate") Date initDate,
            @Param("finalDate") Date finalDate);

    /**
     * Ranking paginado de herramientas más prestadas en un rango de fechas.
     * Usado por los endpoints /ranking/paginated y /ranking/range/paginated.
     * El countQuery cuenta herramientas distintas con préstamos en el periodo.
     */
    @Query(value = """
            SELECT k.idTool, SUM(k.cant)
            FROM KardexEntity k
            WHERE UPPER(k.type) = 'PRESTAMO'
              AND k.date BETWEEN :initDate AND :finalDate
            GROUP BY k.idTool
            ORDER BY SUM(k.cant) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT k.idTool)
            FROM KardexEntity k
            WHERE UPPER(k.type) = 'PRESTAMO'
              AND k.date BETWEEN :initDate AND :finalDate
            """)
    Page<Object[]> getRankingPaginated(
            @Param("initDate") Date initDate,
            @Param("finalDate") Date finalDate,
            Pageable pageable);

}
