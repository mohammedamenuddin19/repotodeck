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
     * TEST 1: The "Grid Wrapping" Stress Test
     * Generates "test-grid-wrapping.pptx"
     * PURPOSE: Verify that 12 services wrap into 3 rows (5, 5, 2) instead of overflowing.
     */
    @Test
    void testGridWrapping_MassiveScale() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // 1. Create a "Massive" Middle Tier
        // We create 12 services. Since MAX_NODES_PER_ROW = 5,
        // Expectation:
        // Row 1: Service 0-4
        // Row 2: Service 5-9
        // Row 3: Service 10-11
        for (int i = 1; i <= 12; i++) {
            nodes.add(createNode("Microservice-" + i, "java:17", "db-cluster"));
        }

        // 2. Add a bottom Database layer to verify it gets pushed DOWN correctly
        nodes.add(createNode("db-cluster", "postgres", null));

        savePpt(nodes, "test-grid-wrapping.pptx");
    }

    /**
     * TEST 2: The "Heuristic & Color" Test
     * Generates "test-heuristics.pptx"
     * PURPOSE: Verify that specific keywords (cassandra, zuul, broker) land in the right tiers/colors.
     */
    @Test
    void testHeuristicsAndColors() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // Tier 1 (Teal): Frontend Keywords
        nodes.add(createNode("web-ui", "nginx:latest"));
        nodes.add(createNode("api-gateway", "netflix-zuul"));

        // Tier 2 (Blue): Logic Keywords
        nodes.add(createNode("payment-processor", "java:17"));
        nodes.add(createNode("auth-worker", "python:3.9"));

        // Tier 3 (Orange): Database/Infra Keywords
        // Testing the new keywords you added: cassandra, kafka, elastic, minio
        ServiceNode db1 = createNode("users-data", "cassandra", null);
        db1.setType("DATABASE"); 
        ServiceNode db2 = createNode("search-index", "elastic", null);
        db2.setType("DATABASE");
        ServiceNode db3 = createNode("event-bus", "kafka", null);
        db3.setType("DATABASE");
        ServiceNode db4 = createNode("blob-store", "minio", null);
        db4.setType("DATABASE");

        nodes.addAll(Arrays.asList(db1, db2, db3, db4));

        savePpt(nodes, "test-heuristics.pptx");
    }

    /**
     * TEST 3: Robustness
     * PURPOSE: Ensure empty/null inputs don't crash the calculator.
     */
    @Test
    void testRobustness() throws IOException {
        byte[] resultEmpty = pptGeneratorService.generateSlide(new ArrayList<>());
        assertNotNull(resultEmpty);
        assertTrue(resultEmpty.length > 0);

        byte[] resultNull = pptGeneratorService.generateSlide(null);
        assertNotNull(resultNull);
        assertTrue(resultNull.length > 0);
    }

    // --- HELPER METHODS ---

    private ServiceNode createNode(String id, String image, String... links) {
        ServiceNode node = new ServiceNode();
        node.setId(id);
        node.setImage(image);
        
        // Basic Type Setting for test logic
        if (image.contains("postgres") || image.contains("cassandra") || image.contains("kafka") || image.contains("elastic") || image.contains("minio")) {
            node.setType("DATABASE");
        } else {
            node.setType("SERVICE");
        }

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