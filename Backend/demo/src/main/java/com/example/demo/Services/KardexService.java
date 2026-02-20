package com.example.demo.Services;

import com.example.demo.Entities.KardexEntity;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Repositories.KardexRepository;
import com.example.demo.Repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
public class KardexService {

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private ToolRepository toolRepository;

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

    public List<KardexEntity> getAllKardex() {
        return kardexRepository.findAll();
    }

    public KardexEntity getKardexById(Long id) {
        return kardexRepository.findById(id).orElseThrow(() -> new RuntimeException("Movimiento de kardex no encontrado"));
    }

    public List<KardexEntity> getKardexByDateBetween(Date initDate, Date finalDate) {
        if (initDate == null) {
            throw new RuntimeException("No se ha proporcionado una fecha de movimiento inicial");
        }
        if (finalDate == null) {
            throw new RuntimeException("No se ha proporcionado una fecha de movimiento final");
        }
        return kardexRepository.findByDateBetween(initDate, finalDate);
    }

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
                    .filter(kardex -> {
                        if (kardex.getDate() == null) return false;
                        // Compare only the date part (ignore time)
                        String kardexDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(kardex.getDate());
                        String initDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(initDate);
                        return kardexDateStr.compareTo(initDateStr) >= 0;
                    })
                    .toList();
        }

        if (finalDate != null) {
            kardexList = kardexList.stream()
                    .filter(kardex -> {
                        if (kardex.getDate() == null) return false;
                        // Compare only the date part (ignore time)
                        String kardexDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(kardex.getDate());
                        String finalDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(finalDate);
                        return kardexDateStr.compareTo(finalDateStr) <= 0;
                    })
                    .toList();
        }

        return kardexList;
    }

    public List<Map<String, Object>> getRankingToolsByDateRange(Date initDate, Date finalDate) {
        if (initDate == null || finalDate == null) {
            return getRankingTools();
        }

        List<KardexEntity> kardexList = kardexRepository.findByDateBetween(initDate, finalDate);

        Map<ToolEntity, Integer> toolCountMap = kardexList.stream()
                .filter(k -> k.getType() != null && "PRESTAMO".equalsIgnoreCase(k.getType()))
                .collect(Collectors.groupingBy(
                        KardexEntity::getIdTool,
                        Collectors.summingInt(KardexEntity::getCant)
                ));

        List<Map.Entry<ToolEntity, Integer>> sortedList = new ArrayList<>(toolCountMap.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        while (i < sortedList.size()) {
            Map.Entry<ToolEntity, Integer> entry = sortedList.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("tool", entry.getKey());
            map.put("totalLoans", entry.getValue());
            result.add(map);
            i+=1;
        }

        return result;
    }

    public List<Map<String, Object>> getRankingTools() {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date initDate = new Date(calendar.getTimeInMillis());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date finalDate = new Date(calendar.getTimeInMillis());

        List<KardexEntity> kardexList = kardexRepository.findByDateBetween(initDate, finalDate);

        Map<ToolEntity, Integer> toolCountMap = kardexList.stream()
                .filter(k -> k.getType() != null && "PRESTAMO".equalsIgnoreCase(k.getType()))
                .collect(Collectors.groupingBy(
                        KardexEntity::getIdTool,
                        Collectors.summingInt(KardexEntity::getCant)
                ));

        List<Map.Entry<ToolEntity, Integer>> sortedList = new ArrayList<>(toolCountMap.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        while (i < sortedList.size() && i < 10) {
            Map.Entry<ToolEntity, Integer> entry = sortedList.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("tool", entry.getKey());
            map.put("totalLoans", entry.getValue());
            result.add(map);
            i+=1;
        }

        if (result.size() < 10) {
            List<ToolEntity> allTools = toolRepository.findAll();
            int targetSize = 10;
            if (allTools.size() < 10) {
                targetSize = allTools.size();
            }

            List<Long> existingIds = result.stream()
                    .map(m -> ((ToolEntity) m.get("tool")).getId())
                    .collect(Collectors.toList());

            i = 0;
            while (i < allTools.size()) {
                if (result.size() >= targetSize) {
                    i = allTools.size();
                } else {
                    ToolEntity tool = allTools.get(i);
                    if (!existingIds.contains(tool.getId())) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("tool", tool);
                        map.put("totalLoans", 0);
                        result.add(map);
                    }
                    i+=1;
                }
            }
        }

        return result;
    }
}
