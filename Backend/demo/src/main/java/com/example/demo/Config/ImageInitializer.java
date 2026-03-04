package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class ImageInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ImageInitializer.class);

    @Value("${IMAGES_PATH:file:images/}")
    private String imagesPathStr;

    @Override
    public void run(String... args) throws Exception {
        // Usa la variable de entorno si existe, sino usa la propiedad inyectada
        String envPath = System.getenv("IMAGES_PATH");
        String effectivePath = (envPath != null && !envPath.isBlank()) ? envPath : imagesPathStr;

        // Clean up the path string (remove "file:" prefix if present)
        String cleanPath = effectivePath.replace("file:", "");
        Path targetDir = Paths.get(cleanPath);

        // Create the target directory if it doesn't exist
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Copy images from classpath to the target directory
        copyInitialImages(targetDir);
    }

    private void copyInitialImages(Path targetDir) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:initial-images/*.*");

            for (Resource resource : resources) {
                if (resource.exists() && resource.isReadable()) {
                    copyResourceIfNeeded(resource, targetDir);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load initial images from classpath: {}", e.getMessage());
        }
    }

    private void copyResourceIfNeeded(Resource resource, Path targetDir) {
        String filename = resource.getFilename();
        if (filename == null) {
            return;
        }
        Path targetFile = targetDir.resolve(filename);
        if (!Files.exists(targetFile)) {
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Initialized image: {}", filename);
            } catch (IOException e) {
                logger.warn("Failed to copy image {}: {}", filename, e.getMessage());
            }
        }
        copyTimestampedVariant(resource, targetDir, filename);
    }

    private void copyTimestampedVariant(Resource resource, Path targetDir, String filename) {
        String timestampedName = resolveTimestampedName(filename);
        if (timestampedName != null) {
            copyWithTimestamp(resource, targetDir, timestampedName);
        }
    }

    private String resolveTimestampedName(String filename) {
        if ("Sierra.png".equals(filename)) {
            return "1771455824130_Sierra.png";
        }
        if ("Pala.png".equals(filename)) {
            return "1771531179609_Pala.png";
        }
        if ("Martillo.png".equals(filename)) {
            return "1764735846760_Martillo.png";
        }
        return null;
    }

    private void copyWithTimestamp(Resource resource, Path targetDir, String timestampedName) {
        Path targetFile = targetDir.resolve(timestampedName);
        if (!Files.exists(targetFile)) {
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Initialized timestamped image: {}", timestampedName);
            } catch (IOException e) {
                logger.warn("Failed to copy timestamped image {}: {}", timestampedName, e.getMessage());
            }
        }
    }
}
