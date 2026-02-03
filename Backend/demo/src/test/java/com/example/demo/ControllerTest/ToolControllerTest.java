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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToolController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "admin", roles = {"ADMIN", "SUPERADMIN"})
public class ToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolService toolService;

    @MockBean
    private UserService userService;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private ToolEntity tool;
    private UserEntity user;

    @BeforeEach
    public void setUp() {
        user = new UserEntity();
        user.setId(1L);

        tool = new ToolEntity();
        tool.setId(1L);
        tool.setToolName("Hammer");
    }

    @Test
    public void testGetAllTools() throws Exception {
        List<ToolEntity> list = new ArrayList<>();
        list.add(tool);
        when(toolService.getAllTools()).thenReturn((ArrayList<ToolEntity>) list);

        mockMvc.perform(get("/api/tool/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testAddTool() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(userService.findUserById(1L)).thenReturn(user);
        when(toolService.createTool(any(UserEntity.class), any(ToolEntity.class), any(MultipartFile.class))).thenReturn(tool);

        mockMvc.perform(multipart("/api/tool/user/1")
                .file(image)
                .file(toolPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testAddTool_UserNotFound() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(userService.findUserById(1L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(multipart("/api/tool/user/1")
                .file(image)
                .file(toolPart))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddTool_Error() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(userService.findUserById(1L)).thenReturn(user);
        when(toolService.createTool(any(UserEntity.class), any(ToolEntity.class), any(MultipartFile.class))).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        mockMvc.perform(multipart("/api/tool/user/1")
                .file(image)
                .file(toolPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateTool() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "some-image".getBytes());
        MockMultipartFile toolPart = new MockMultipartFile("tool", "", "application/json", objectMapper.writeValueAsString(tool).getBytes());

        when(toolService.updateTool(eq(1L), eq(1L), any(ToolEntity.class), any(MultipartFile.class))).thenReturn(tool);

        mockMvc.perform(multipart("/api/tool/1/user/1")
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
    public void testDeleteTool() throws Exception {
        when(toolService.deleteToolById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/tool/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
