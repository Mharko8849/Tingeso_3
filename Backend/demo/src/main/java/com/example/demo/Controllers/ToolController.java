package com.example.demo.Controllers;

import com.example.demo.DTO.PageResponseDTO;
import com.example.demo.DTO.ToolDTO;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tool")
@CrossOrigin("*")
public class ToolController {

    @Autowired
    private ToolService toolService;
    @Autowired
    private UserService userService;

    /*
     * GET: Traer todas las herramientas
     */
    @GetMapping("/")
    public ResponseEntity<List<ToolEntity>> getAllTools() {
        List<ToolEntity> tools = toolService.getAllTools();
        return ResponseEntity.ok(tools);
    }
    
    /*
     * GET: Traer herramientas paginadas
     */
    @GetMapping("/paginated")
    public ResponseEntity<PageResponseDTO<ToolDTO>> getAllToolsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        PageResponseDTO<ToolDTO> tools = toolService.getAllToolsPaginated(page, size);
        return ResponseEntity.ok(tools);
    }
    
    /*
     * GET: Traer herramientas por categoría paginadas
     */
    @GetMapping("/category/{category}/paginated")
    public ResponseEntity<PageResponseDTO<ToolDTO>> getToolsByCategoryPaginated(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        PageResponseDTO<ToolDTO> tools = toolService.getToolsByCategoryPaginated(category, page, size);
        return ResponseEntity.ok(tools);
    }

    /*
     * POST: Agregar nueva herramienta
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @PostMapping(value = "/user/{idUser}", consumes = "multipart/form-data")
    public ResponseEntity<ToolEntity> addTool(@PathVariable Long idUser, @RequestPart ToolEntity tool,
                                              @RequestPart(required = false) MultipartFile image) {
        UserEntity user = userService.findUserById(idUser);
        ToolEntity createdTool = toolService.createTool(user, tool,image);
        return ResponseEntity.ok(createdTool);
    }

    /*
     * PUT: Actualizar herramienta
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @PutMapping(value = "/{idTool}/user/{idUser}",consumes = "multipart/form-data")
    public ResponseEntity<ToolEntity> updateTool(@PathVariable Long idUser, @PathVariable Long idTool,
                                                 @RequestPart ToolEntity tool, @RequestPart(required = false) MultipartFile image) {
        ToolEntity updatedTool = toolService.updateTool(idUser, idTool, tool, image);
        return ResponseEntity.ok(updatedTool);
    }

    /*
     * DELETE: Eliminar herramienta
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteTool(@PathVariable Long id) {
        boolean isDeleted = toolService.deleteToolById(id);
        return ResponseEntity.ok(isDeleted);
    }
}
