package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PptGeneratorServiceTest {

    @Test
    void generateSlide_shouldCreatePptxFile() throws IOException {
        // Arrange
        PptGeneratorService service = new PptGeneratorService();
        List<ServiceNode> nodes = createTestNodes();

        // Act
        byte[] pptxBytes = service.generateSlide(nodes);

        // Assert
        assertNotNull(pptxBytes);
        assertTrue(pptxBytes.length > 0);

        // Save to project root as test-output.pptx
        // Use user.dir system property which typically points to project root when running tests
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        
        // If user.dir doesn't point to backend, try to find it
        if (!projectRoot.getFileName().toString().equals("backend")) {
            // Check if we're in a subdirectory and need to go up
            Path current = projectRoot;
            while (current != null && !current.getFileName().toString().equals("backend")) {
                Path parent = current.getParent();
                if (parent == null) break;
                current = parent;
            }
            if (current != null && current.getFileName().toString().equals("backend")) {
                projectRoot = current;
            }
        }

        Path outputFile = projectRoot.resolve("test-output.pptx");
        Files.write(outputFile, pptxBytes);

        assertTrue(Files.exists(outputFile), "PPTX file should be created at: " + outputFile);
        assertTrue(Files.size(outputFile) > 0, "PPTX file should not be empty");
    }

    @Test
    void generateSlide_withEmptyList_shouldCreateEmptyPresentation() throws IOException {
        // Arrange
        PptGeneratorService service = new PptGeneratorService();

        // Act
        byte[] pptxBytes = service.generateSlide(new ArrayList<>());

        // Assert
        assertNotNull(pptxBytes);
        assertTrue(pptxBytes.length > 0);
    }

    @Test
    void generateSlide_withNullList_shouldHandleGracefully() throws IOException {
        // Arrange
        PptGeneratorService service = new PptGeneratorService();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            byte[] pptxBytes = service.generateSlide(null);
            assertNotNull(pptxBytes);
        });
    }

    /**
     * Create test nodes with various types and links.
     */
    private List<ServiceNode> createTestNodes() {
        ServiceNode web = new ServiceNode();
        web.setId("web");
        web.setImage("nginx:latest");
        web.setType("SERVICE");
        web.setLinks(Arrays.asList("db", "cache"));

        ServiceNode db = new ServiceNode();
        db.setId("db");
        db.setImage("postgres:15");
        db.setType("DATABASE");
        db.setLinks(new ArrayList<>());

        ServiceNode cache = new ServiceNode();
        cache.setId("cache");
        cache.setImage("redis:7");
        cache.setType("SERVICE");
        cache.setLinks(new ArrayList<>());

        ServiceNode api = new ServiceNode();
        api.setId("api");
        api.setImage("node:18");
        api.setType("SERVICE");
        api.setLinks(Arrays.asList("db"));

        return Arrays.asList(web, db, cache, api);
    }
}
