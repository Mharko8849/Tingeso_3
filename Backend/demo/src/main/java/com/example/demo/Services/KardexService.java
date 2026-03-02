package com.example.demo.Services;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.KardexRepository;
import com.example.demo.Repositories.ToolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.DTO.EntityMapper;
import com.example.demo.DTO.PageResponseDTO;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class KardexService {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TOTAL_LOANS_KEY = "totalLoans";
    private static final String TOOL_KEY = "tool";

    private final KardexRepository kardexRepository;
    private final ToolRepository toolRepository;

    public KardexEntity saveKardexEntity(KardexEntity kardexEntity) {
        return kardexRepository.save(kardexEntity);
    }

    public KardexEntity createKardexEntity(ToolEntity idTool, String type, Date actualDate, int cant, Integer cost, UserEntity idUser, UserEntity idEmployee) {
        KardexEntity kardex = new KardexEntity();

        if (cost != null) {
            kardex.setCost(cost);
        }

        if (idTool == null) {
            throw new RuntimeException("Herramienta no encontrada");
        }
        kardex.setIdTool(idTool);

        if (type == null || type.isBlank()) {
            throw new RuntimeException("Debe especificar el motivo del movimiento");
        }
        kardex.setType(type);

        if (actualDate == null) {
            throw new RuntimeException("Debe contar con una fecha de movimiento");
        }
        kardex.setDate(actualDate);

        kardex.setCant(cant);

        kardex.setIdUser(idUser);

        if (idEmployee == null) {
            throw new RuntimeException("Usuario no encontrado");
        }
        kardex.setIdEmployee(idEmployee);

        return kardexRepository.save(kardex);
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> getAllKardex() {
        return kardexRepository.findAll();
    }

    /**
     * Devuelve todos los movimientos de kardex paginados, ordenados por ID descendente.
     * Este método era llamado desde el controlador pero faltaba en el servicio.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<KardexEntity> getAllKardexPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<KardexEntity> kardexPage = kardexRepository.findAll(pageable);
        return EntityMapper.toRawPageResponseDTO(kardexPage);
    }

    /**
     * Filtra movimientos de kardex con parámetros opcionales y devuelve una página.
     * Usa la query nativa del repositorio (SQL push-down) en lugar de filtrar en Java.
     * Este método era llamado desde el controlador pero faltaba en el servicio.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<KardexEntity> filterKardexPaginated(
            Long idTool, String type, String initDate, String finalDate,
            Long idUser, Long idEmployee, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<KardexEntity> kardexPage = kardexRepository.filterKardexPaginated(
                (type != null && type.isBlank()) ? null : type,
                idTool, idUser, idEmployee,
                (initDate != null && initDate.isBlank()) ? null : initDate,
                (finalDate != null && finalDate.isBlank()) ? null : finalDate,
                pageable);
        return EntityMapper.toRawPageResponseDTO(kardexPage);
    }

    @Transactional(readOnly = true)
    public KardexEntity getKardexById(Long id) {
        return kardexRepository.findById(id).orElseThrow(() -> new RuntimeException("Movimiento de kardex no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> getKardexByDateBetween(Date initDate, Date finalDate) {
        if (initDate == null) {
            throw new RuntimeException("No se ha proporcionado una fecha de movimiento inicial");
        }
        if (finalDate == null) {
            throw new RuntimeException("No se ha proporcionado una fecha de movimiento final");
        }
        return kardexRepository.findByDateBetween(initDate, finalDate);
    }

    @Transactional(readOnly = true)
    public List<KardexEntity> filterKardex(Long idTool, String type, Date initDate, Date finalDate, Long idUser, Long idEmployee) {
        List<KardexEntity> kardexList = kardexRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        if (type != null && !type.isBlank()) {
            kardexList = kardexList.stream()
                    .filter(kardex -> kardex.getType() != null && kardex.getType().equalsIgnoreCase(type))
                    .toList();
        }

        if (idTool != null) {
            kardexList = kardexList.stream()
                    .filter(kardex -> kardex.getIdTool() != null && kardex.getIdTool().getId().equals(idTool))
                    .toList();
        }

        if (idUser != null) {
            kardexList = kardexList.stream()
                    .filter(kardex -> kardex.getIdUser() != null && kardex.getIdUser().getId().equals(idUser))
                    .toList();
        }

        if (idEmployee != null) {
            kardexList = kardexList.stream()
                    .filter(kardex -> kardex.getIdEmployee() != null && kardex.getIdEmployee().getId().equals(idEmployee))
                    .toList();
        }

        if (initDate != null) {
            kardexList = kardexList.stream()
                    .filter(kardex -> isOnOrAfterDate(kardex, initDate))
                    .toList();
        }

        if (finalDate != null) {
            kardexList = kardexList.stream()
                    .filter(kardex -> isOnOrBeforeDate(kardex, finalDate))
                    .toList();
        }

        return kardexList;
    }

    private boolean isOnOrAfterDate(KardexEntity kardex, Date initDate) {
        if (kardex.getDate() == null) return false;
        String kardexDateStr = new java.text.SimpleDateFormat(DATE_FORMAT).format(kardex.getDate());
        String initDateStr = new java.text.SimpleDateFormat(DATE_FORMAT).format(initDate);
        return kardexDateStr.compareTo(initDateStr) >= 0;
    }

    private boolean isOnOrBeforeDate(KardexEntity kardex, Date finalDate) {
        if (kardex.getDate() == null) return false;
        String kardexDateStr = new java.text.SimpleDateFormat(DATE_FORMAT).format(kardex.getDate());
        String finalDateStr = new java.text.SimpleDateFormat(DATE_FORMAT).format(finalDate);
        return kardexDateStr.compareTo(finalDateStr) <= 0;
    }

    /**
     * Ranking de herramientas más prestadas en un rango arbitrario de fechas.
     * Delega el GROUP BY + SUM + ORDER BY a la base de datos en lugar de hacerlo en Java.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRankingToolsByDateRange(Date initDate, Date finalDate) {
        if (initDate == null || finalDate == null) {
            // Fallback: use this month's date range instead of calling another transactional method
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            initDate = new Date(calendar.getTimeInMillis());
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            finalDate = new Date(calendar.getTimeInMillis());
        }

        List<Object[]> rows = kardexRepository.getRankingByDateRangeQuery(initDate, finalDate);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put(TOOL_KEY, row[0]);
            map.put(TOTAL_LOANS_KEY, row[1]);
            result.add(map);
        }
        return result;
    }

    /**
     * Ranking paginado del mes actual.
     * Devuelve PageResponseDTO en lugar de List para no exponer toda la tabla.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<Map<String, Object>> getRankingToolsPaginated(int page, int size) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date initDate = new Date(calendar.getTimeInMillis());
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date finalDate = new Date(calendar.getTimeInMillis());

        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> rankingPage = kardexRepository.getRankingPaginated(initDate, finalDate, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] row : rankingPage.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put(TOOL_KEY, row[0]);
            map.put(TOTAL_LOANS_KEY, row[1]);
            content.add(map);
        }

        // Fallback: si no hay suficientes resultados con préstamos este mes,
        // rellenar con herramientas con totalLoans = 0 para que el carrusel siempre tenga contenido.
        if (content.size() < size) {
            List<Long> existingIds = content.stream()
                    .map(m -> ((ToolEntity) m.get(TOOL_KEY)).getId())
                    .collect(Collectors.toList());
            List<ToolEntity> allTools = toolRepository.findAll();
            for (ToolEntity tool : allTools) {
                if (content.size() >= size) break;
                if (!existingIds.contains(tool.getId())) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(TOOL_KEY, tool);
                    map.put(TOTAL_LOANS_KEY, 0);
                    content.add(map);
                }
            }
        }

        return new PageResponseDTO<>(
                content,
                rankingPage.getNumber(),
                rankingPage.getSize(),
                (long) content.size(),
                1,
                true,
                true
        );
    }

    /**
     * Ranking paginado por rango de fechas arbitrario.
     * Delega el GROUP BY + ORDER BY a la BD y aplica LIMIT/OFFSET via Pageable.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<Map<String, Object>> getRankingToolsByDateRangePaginated(
            Date initDate, Date finalDate, int page, int size) {
        if (initDate == null || finalDate == null) {
            // Fallback: use this month's date range instead of calling another transactional method
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            initDate = new Date(calendar.getTimeInMillis());
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            finalDate = new Date(calendar.getTimeInMillis());
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> rankingPage = kardexRepository.getRankingPaginated(initDate, finalDate, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] row : rankingPage.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put(TOOL_KEY, row[0]);
            map.put(TOTAL_LOANS_KEY, row[1]);
            content.add(map);
        }
        return new PageResponseDTO<>(
                content,
                rankingPage.getNumber(),
                rankingPage.getSize(),
                rankingPage.getTotalElements(),
                rankingPage.getTotalPages(),
                rankingPage.isLast(),
                rankingPage.isFirst()
        );
    }

    /**
     * Ranking del mes actual — top 10 herramientas más prestadas.
     * Usa la query nativa getRankingTop10 para hacer el GROUP BY en la BD (evita
     * traer toda la tabla de kardex a memoria para procesarla en Java).
     * Si hay menos de 10 herramientas con préstamos en el mes, rellena con el
     * resto de herramientas existentes con totalLoans = 0.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRankingTools() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date initDate = new Date(calendar.getTimeInMillis());
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date finalDate = new Date(calendar.getTimeInMillis());

        // La query ordena y limita en la BD — sin carga en memoria de la tabla completa.
        List<Object[]> rows = kardexRepository.getRankingTop10(initDate, finalDate);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put(TOOL_KEY, row[0]);
            map.put(TOTAL_LOANS_KEY, row[1]);
            result.add(map);
        }

        // Rellenar hasta 10 con herramientas sin préstamos en el mes.
        if (result.size() < 10) {
            List<ToolEntity> allTools = toolRepository.findAll();
            int targetSize = Math.min(10, allTools.size());

            List<Long> existingIds = result.stream()
                    .map(m -> ((ToolEntity) m.get(TOOL_KEY)).getId())
                    .collect(Collectors.toList());

            for (ToolEntity tool : allTools) {
                if (result.size() >= targetSize) break;
                if (!existingIds.contains(tool.getId())) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(TOOL_KEY, tool);
                    map.put(TOTAL_LOANS_KEY, 0);
                    result.add(map);
                }
            }
        }

        return result;
    }
}
