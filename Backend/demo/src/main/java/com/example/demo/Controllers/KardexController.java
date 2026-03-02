package com.example.demo.Controllers;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Services.KardexService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import com.example.demo.DTO.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kardex")
@CrossOrigin("*")
@RequiredArgsConstructor
public class KardexController {

    private final KardexService kardexService;
    private final ToolService toolService;
    private final UserService userService;

    /*
    GET
     */

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<KardexEntity>> getAllKardex() {
        List<KardexEntity> kardex = kardexService.getAllKardex();
        return ResponseEntity.ok(kardex);
    }

    /**
     * Devuelve los movimientos de kardex paginados (más recientes primero).
     * Parámetros: page (default 0), size (default 20)
     */
    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<PageResponseDTO<KardexEntity>> getAllKardexPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponseDTO<KardexEntity> kardexPage = kardexService.getAllKardexPaginated(page, size);
        return ResponseEntity.ok(kardexPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<KardexEntity> getKardexById(@PathVariable Long id) {
        KardexEntity kardex = kardexService.getKardexById(id);
        return ResponseEntity.ok(kardex);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<PageResponseDTO<KardexEntity>> filterKardex(
            @RequestParam(required = false) Long idTool,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String initDate,
            @RequestParam(required = false) String finalDate,
            @RequestParam(required = false) Long idUser,
            @RequestParam(required = false) Long idEmployee,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Validar formato de fechas si se proporcionan (YYYY-MM-DD)
        if (initDate != null && !initDate.isBlank() && !initDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ResponseEntity.badRequest().build();
        }
        if (finalDate != null && !finalDate.isBlank() && !finalDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ResponseEntity.badRequest().build();
        }

        PageResponseDTO<KardexEntity> result = kardexService.filterKardexPaginated(
                idTool, type, initDate, finalDate, idUser, idEmployee, page, size);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/ranking")
    public ResponseEntity<List<Map<String, Object>>> getRanking() {
        List<Map<String, Object>> kardexList = kardexService.getRankingTools();
        return ResponseEntity.ok(kardexList);
    }

    /**
     * Ranking paginado del mes actual.
     * Úsalo en lugar de /ranking cuando se trabaje con grandes volúmenes de datos.
     */
    @GetMapping("/ranking/paginated")
    public ResponseEntity<PageResponseDTO<Map<String, Object>>> getRankingPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponseDTO<Map<String, Object>> result = kardexService.getRankingToolsPaginated(page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ranking/range")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getRankingByDateRange(
            @RequestParam(required = false) String initDate,
            @RequestParam(required = false) String finalDate) {
        
        Date parsedInitDate = null;
        Date parsedFinalDate = null;
        
        try {
            if (initDate != null && !initDate.isEmpty()) {
                parsedInitDate = Date.valueOf(initDate);
            }
            if (finalDate != null && !finalDate.isEmpty()) {
                parsedFinalDate = Date.valueOf(finalDate);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Map<String, Object>> ranking = kardexService.getRankingToolsByDateRange(parsedInitDate, parsedFinalDate);
        return ResponseEntity.ok(ranking);
    }

    /**
     * Ranking paginado por rango de fechas.
     * Úsalo en lugar de /ranking/range cuando se trabaje con grandes volúmenes de datos.
     */
    @GetMapping("/ranking/range/paginated")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<PageResponseDTO<Map<String, Object>>> getRankingByDateRangePaginated(
            @RequestParam(required = false) String initDate,
            @RequestParam(required = false) String finalDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Date parsedInitDate = null;
        Date parsedFinalDate = null;

        try {
            if (initDate != null && !initDate.isEmpty()) {
                parsedInitDate = Date.valueOf(initDate);
            }
            if (finalDate != null && !finalDate.isEmpty()) {
                parsedFinalDate = Date.valueOf(finalDate);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        PageResponseDTO<Map<String, Object>> result =
                kardexService.getRankingToolsByDateRangePaginated(parsedInitDate, parsedFinalDate, page, size);
        return ResponseEntity.ok(result);
    }
}
