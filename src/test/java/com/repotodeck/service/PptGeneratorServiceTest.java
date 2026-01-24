package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PptGeneratorServiceTest {

    private final PptGeneratorService pptGeneratorService = new PptGeneratorService();

    /**
     * TEST 1: The "Coming Out" Fix (Wide Layout)
     * Generates "test-wide-layout.pptx"
     * PURPOSE: Verify that 5 boxes side-by-side fit inside the 1920px canvas.
     */
    @Test
    void testVisual_WideLayout_1920px() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // Tier 1 (Teal): Single Gateway
        nodes.add(createNode("api-gateway", "spring-cloud-gateway", "auth-service", "product-service", "order-service", "notification-service", "analytics-service"));

        // Tier 2 (Blue): 5 Services Wide (The Stress Test)
        // 5 * 220px + 4 * 60px = ~1340px width.
        // Old Canvas (1280px) -> Overflow / Cut off.
        // New Canvas (1920px) -> Perfect fit.
        nodes.add(createNode("auth-service", "node:18", "users-db"));
        nodes.add(createNode("product-service", "java:17", "products-db"));
        nodes.add(createNode("order-service", "golang:1.20", "orders-db"));
        nodes.add(createNode("notification-service", "python:3.9", "kafka"));
        nodes.add(createNode("analytics-service", "python:3.9", "hadoop"));

        // Tier 3 (Orange): Data
        ServiceNode db1 = createNode("users-db", "postgres", null); db1.setType("DATABASE");
        ServiceNode db2 = createNode("products-db", "mongo", null); db2.setType("DATABASE");
        ServiceNode db3 = createNode("orders-db", "mysql", null);   db3.setType("DATABASE");
        ServiceNode db4 = createNode("kafka", "kafka", null);       db4.setType("DATABASE");
        ServiceNode db5 = createNode("hadoop", "hadoop", null);     db5.setType("DATABASE");
        
        nodes.addAll(Arrays.asList(db1, db2, db3, db4, db5));

        savePpt(nodes, "test-wide-layout.pptx");
    }

    /**
     * TEST 2: The "3-Color Palette" Test
     * Generates "test-colors.pptx"
     * PURPOSE: Verify the new Teal (Top) -> Blue (Middle) -> Orange (Bottom) logic.
     */
    @Test
    void testVisual_ColorPalette() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // 1. Frontend (Should be TEAL)
        // Heuristic: "nginx" or "react" puts it in Tier 0
        ServiceNode frontend = createNode("frontend-ui", "react-app", "backend-api");
        
        // 2. Backend (Should be ROYAL BLUE)
        // Heuristic: "node" or "java" puts it in Tier 1
        ServiceNode backend = createNode("backend-api", "node:18", "main-db");

        // 3. Database (Should be BURNT ORANGE)
        // Heuristic: "postgres" puts it in Tier 2
        ServiceNode db = createNode("main-db", "postgres:15", null);
        db.setType("DATABASE");

        nodes.addAll(Arrays.asList(frontend, backend, db));

        savePpt(nodes, "test-colors.pptx");
    }

    /**
     * TEST 3: The "Netflix" Stress Test (Complex Spiderweb)
     * Generates "test-netflix.pptx"
     * PURPOSE: Verify that lines don't cross text and the diagram doesn't crash with 15+ nodes.
     */
    @Test
    void testVisual_NetflixComplex() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // Edge (Teal)
        nodes.add(createNode("netflix-zuul", "gateway", "movie-service", "user-service", "search-service"));

        // Microservices (Blue)
        nodes.add(createNode("movie-service", "java", "cassandra-movies"));
        nodes.add(createNode("user-service", "node", "cassandra-users"));
        nodes.add(createNode("search-service", "python", "elastic-search"));
        nodes.add(createNode("recommendation-engine", "spark", "movie-service", "user-service")); // Complex cross-linking

        // Data (Orange)
        ServiceNode d1 = createNode("cassandra-movies", "cassandra", null); d1.setType("DATABASE");
        ServiceNode d2 = createNode("cassandra-users", "cassandra", null);  d2.setType("DATABASE");
        ServiceNode d3 = createNode("elastic-search", "elastic", null);     d3.setType("DATABASE");

        nodes.addAll(Arrays.asList(d1, d2, d3));

        savePpt(nodes, "test-netflix.pptx");
    }

    // --- HELPER METHODS ---

    private ServiceNode createNode(String id, String image, String... links) {
        ServiceNode node = new ServiceNode();
        node.setId(id);
        node.setImage(image);
        // Basic type fallback
        node.setType("SERVICE"); 
        
        if (links != null && links.length > 0 && links[0] != null) {
            node.setLinks(Arrays.asList(links));
        }
        return node;
    }

    private void savePpt(List<ServiceNode> nodes, String filename) throws IOException {
        byte[] pptBytes = pptGeneratorService.generateSlide(nodes);
        File outputFile = new File(filename);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(pptBytes);
        }
        System.out.println("✅ Generated Test File: " + outputFile.getAbsolutePath());
    }
}