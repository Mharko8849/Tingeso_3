package com.example.demo.ControllerTest;

import com.example.demo.Controllers.ToolController;
import com.example.demo.Entities.ToolEntity;
import com.example.demo.Entities.UserEntity;
import com.example.demo.Services.ToolService;
import com.example.demo.Services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.DTO.PageResponseDTO;
import com.example.demo.DTO.ToolDTO;

import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToolController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
class ToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToolService toolService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private ToolEntity tool;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);

        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
    }

    @Test
    void testGetAllTools() throws Exception {
        List<ToolEntity> list = new ArrayList<>();
        list.add(tool);
        when(toolService.getAllTools()).thenReturn((ArrayList<ToolEntity>) list);

        mockMvc.perform(get("/tool/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testAddTool() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(userService.findUserById(1L)).thenReturn(user);
        when(toolService.createTool(any(UserEntity.class), any(ToolEntity.class), any(MultipartFile.class))).thenReturn(tool);

        mockMvc.perform(multipart("/tool/user/1")
                .file(image)
                .file(toolPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testAddTool_UserNotFound() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(userService.findUserById(1L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(multipart("/tool/user/1")
                .file(image)
                .file(toolPart))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddTool_Error() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(userService.findUserById(1L)).thenReturn(user);
        when(toolService.createTool(any(UserEntity.class), any(ToolEntity.class), any(MultipartFile.class))).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(multipart("/tool/user/1")
                .file(image)
                .file(toolPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateTool() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(toolService.updateTool(eq(1L), eq(1L), any(ToolEntity.class), any(MultipartFile.class))).thenReturn(tool);

        mockMvc.perform(multipart("/tool/1/user/1")
                .file(image)
                .file(toolPart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testDeleteTool() throws Exception {
        when(toolService.deleteToolById(1L)).thenReturn(true);

        mockMvc.perform(delete("/tool/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testGetAllToolsPaginated() throws Exception {
        ToolDTO toolDTO = new ToolDTO(1L, "Hammer", null, 0, 0, null, null, null, null, 0);
        List<ToolDTO> content = List.of(toolDTO);
        PageResponseDTO<ToolDTO> page = new PageResponseDTO<>(content, 0, 8, 1L, 1, true, true);
        when(toolService.getAllToolsPaginated(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/tool/paginated")
                .param("page", "0")
                .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testGetToolsByCategoryPaginated() throws Exception {
        ToolDTO toolDTO = new ToolDTO(1L, "Hammer", null, 0, 0, null, null, "Construction", null, 0);
        List<ToolDTO> content = List.of(toolDTO);
        PageResponseDTO<ToolDTO> page = new PageResponseDTO<>(content, 0, 8, 1L, 1, true, true);
        when(toolService.getToolsByCategoryPaginated(eq("Construction"), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/tool/category/Construction/paginated")
                .param("page", "0")
                .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }
}
