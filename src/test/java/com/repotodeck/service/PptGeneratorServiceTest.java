package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PptGeneratorServiceTest {

    private final PptGeneratorService pptGeneratorService = new PptGeneratorService();

    @Test
    void testGenerateSlide_WithValidData_ShouldReturnBytes() throws IOException {
        // 1. Arrange (Create Dummy Data)
        List<ServiceNode> nodes = new ArrayList<>();
        
        // Frontend Node
        ServiceNode web = new ServiceNode();
        web.setId("frontend-web");
        web.setImage("nginx:latest");
        web.setType("SERVICE");
        web.setLinks(Arrays.asList("backend-api"));
        nodes.add(web);

        // Backend Node
        ServiceNode api = new ServiceNode();
        api.setId("backend-api");
        api.setImage("java:17");
        api.setType("SERVICE");
        api.setLinks(Arrays.asList("user-db"));
        nodes.add(api);

        // Database Node
        ServiceNode db = new ServiceNode();
        db.setId("user-db");
        db.setImage("postgres:15");
        db.setType("DATABASE");
        nodes.add(db);

        // 2. Act (Generate PPT)
        byte[] result = pptGeneratorService.generateSlide(nodes);

        // 3. Assert (Verify it worked)
        assertNotNull(result, "Result should not be null");
        assertTrue(result.length > 0, "Result should have content (bytes)");

        // 4. Validate it is a Real PPT (Try to load it back)
        assertDoesNotThrow(() -> {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(result);
                 XMLSlideShow ppt = new XMLSlideShow(bis)) {
                assertNotNull(ppt.getSlides().get(0), "Should have at least one slide");
            }
        });
    }

    @Test
    void testGenerateSlide_WithEmptyList_ShouldNotCrash() throws IOException {
        byte[] result = pptGeneratorService.generateSlide(new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    /**
     * MANUAL DEBUG TEST
     * This test writes a real file to your project root.
     * Run this, then go open "debug-output.pptx" in your folder.
     */
    @Test
    void generateLocalFileForVisualInspection() throws IOException {
        // Create complex data to test the "Tiered Layout"
        List<ServiceNode> nodes = new ArrayList<>();
        
        // Tier 1
        ServiceNode lb = createNode("load-balancer", "nginx", "web-app");
        ServiceNode web = createNode("web-app", "react", "api-gateway");
        
        // Tier 2
        ServiceNode gateway = createNode("api-gateway", "spring", "auth-service", "payment-service");
        ServiceNode auth = createNode("auth-service", "node", "postgres-db");
        ServiceNode payment = createNode("payment-service", "go", "mysql-db", "redis");
        
        // Tier 3
        ServiceNode pg = createNode("postgres-db", "postgres", null);
        pg.setType("DATABASE");
        ServiceNode mysql = createNode("mysql-db", "mysql", null);
        mysql.setType("DATABASE");
        ServiceNode redis = createNode("redis", "redis", null);
        redis.setType("DATABASE");

        nodes.addAll(Arrays.asList(lb, web, gateway, auth, payment, pg, mysql, redis));

        // Generate
        byte[] result = pptGeneratorService.generateSlide(nodes);

        // Write to file system
        File outputFile = new File("debug-output.pptx");
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(result);
        }

        System.out.println("---------------------------------------------------");
        System.out.println("✅ SUCCESS! File generated at: " + outputFile.getAbsolutePath());
        System.out.println("📂 Go open this file NOW to check the layout.");
        System.out.println("---------------------------------------------------");
        
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    // Helper to make test data easier
    private ServiceNode createNode(String id, String image, String... links) {
        ServiceNode node = new ServiceNode();
        node.setId(id);
        node.setImage(image);
        node.setType("SERVICE");
        if (links != null) {
            node.setLinks(Arrays.asList(links));
        }
        return node;
    }
}