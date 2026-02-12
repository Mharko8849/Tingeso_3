package com.example.demo.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:images}") String uploadDir) {
        String envPath = System.getenv("IMAGES_PATH");
        String finalPath = (envPath != null && !envPath.isBlank()) 
                ? envPath.replace("file:", "") 
                : uploadDir;

        this.fileStorageLocation = Paths.get(finalPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo crear el directorio para subir archivos.", ex);
        }
    }

    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = this.fileStorageLocation.resolve(fileName);

            Files.write(filePath, file.getBytes());

            return fileName;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage());
        }
    }
}
