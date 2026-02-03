package com.example.demo.ServiceTest;

import com.example.demo.Services.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest {

@TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    public void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    public void testSaveFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
        String fileName = fileStorageService.saveFile(file);
        assertTrue(fileName.endsWith("_test.jpg"));
        assertTrue(Files.exists(tempDir.resolve(fileName)));
    }

    @Test
    public void testSaveFile_NullOrEmpty() {
        assertNull(fileStorageService.saveFile(null));
        
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        assertNull(fileStorageService.saveFile(emptyFile));
    }

    @Test
    public void testSaveFile_Exception() {
        // Create a service with a read-only directory or invalid path to trigger IOException
        // However, since we pass the path in constructor and it creates directories, it's tricky.
        // Instead, we can try to pass a file as the directory path if it exists, or use a non-writable path.
        // But simpler is to mock the file to throw exception on getBytes(), but getBytes() is called inside.
        
        MockMultipartFile badFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes()) {
            @Override
            public byte[] getBytes() throws java.io.IOException {
                throw new java.io.IOException("Read error");
            }
        };

        assertThrows(RuntimeException.class, () -> fileStorageService.saveFile(badFile));
    }
}
