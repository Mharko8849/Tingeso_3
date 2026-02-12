package com.example.demo.Controllers;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Services.KardexService;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/kardex")
@CrossOrigin("*")
public class KardexController {

    @Autowired
    private KardexService kardexService;

    @Autowired
    private ToolService toolService;

    @Autowired
    private UserService userService;

    /*
    GET
     */

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<KardexEntity>> getAllKardex() {
        List<KardexEntity> kardex = kardexService.getAllKardex();
        return ResponseEntity.ok(kardex);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<KardexEntity> getKardexById(@PathVariable Long id) {
        KardexEntity kardex = kardexService.getKardexById(id);
        return ResponseEntity.ok(kardex);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<KardexEntity>> filterKardex(
            @RequestParam(required = false) Long idTool,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Date initDate,
            @RequestParam(required = false) Date finalDate,
            @RequestParam(required = false) Long idUser,
            @RequestParam(required = false) Long idEmployee) {

        List<KardexEntity> kardexList = kardexService.filterKardex(idTool, type, initDate, finalDate, idUser, idEmployee);
        return ResponseEntity.ok(kardexList);
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<Map<String, Object>>> getRanking() {
        List<Map<String, Object>> kardexList = kardexService.getRankingTools();
        return ResponseEntity.ok(kardexList);
    }

    @GetMapping("/ranking/range")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE', 'SUPERADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getRankingByDateRange(
            @RequestParam(required = false) Date initDate,
            @RequestParam(required = false) Date finalDate) {
        List<Map<String, Object>> ranking = kardexService.getRankingToolsByDateRange(initDate, finalDate);
        return ResponseEntity.ok(ranking);
    }
}
