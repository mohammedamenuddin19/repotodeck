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
     * TEST 1: THE "LOOK & FEEL" TEST
     * Generates "test-output-style.pptx"
     * CHECK FOR:
     * 1. Blue boxes for Services, Orange for DBs.
     * 2. Grey shadows behind every box.
     * 3. Lines appearing BEHIND the boxes (not crossing text).
     */
    @Test
    void testVisual_StandardStack() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // Tier 1: Frontend
        ServiceNode web = createNode("webapp-ui", "react-frontend:18", "api-gateway");
        
        // Tier 2: Backend
        ServiceNode api = createNode("api-gateway", "spring-boot:3.0", "auth-service", "payment-service");
        ServiceNode auth = createNode("auth-service", "node:18", "users-db");
        ServiceNode payment = createNode("payment-service", "go:1.20", "payments-db");

        // Tier 3: Database (Should be ORANGE)
        ServiceNode userDb = createNode("users-db", "postgres:15", null);
        userDb.setType("DATABASE");
        
        ServiceNode payDb = createNode("payments-db", "mysql:8.0", null);
        payDb.setType("DATABASE");

        nodes.addAll(Arrays.asList(web, api, auth, payment, userDb, payDb));

        savePpt(nodes, "test-output-style.pptx");
    }

    /**
     * TEST 2: THE "SMART ANCHOR" TEST
     * Generates "test-output-anchors.pptx"
     * CHECK FOR:
     * 1. Vertical connection (Tier 1 -> Tier 2) should look like a Waterfall.
     * 2. Side-by-Side connection (Tier 2 -> Tier 2) should go Center-to-Center.
     */
    @Test
    void testVisual_ComplexAnchors() throws IOException {
        List<ServiceNode> nodes = new ArrayList<>();

        // 1. Vertical Logic (Frontend -> Backend)
        ServiceNode top = createNode("Top-Service", "nginx", "Bottom-Service");
        ServiceNode bottom = createNode("Bottom-Service", "java", null);

        // 2. Side-by-Side Logic (Service A -> Service B in same tier)
        ServiceNode left = createNode("Left-Service", "node", "Right-Service");
        ServiceNode right = createNode("Right-Service", "node", null);

        // Force them into Tier 1 (Middle) via logic or naming if needed, 
        // but for now, the heuristic places them based on name/image.
        // Let's rely on the heuristic: "nginx" goes to Tier 0, "java" to Tier 1.
        
        nodes.add(top);
        nodes.add(bottom);
        nodes.add(left);
        nodes.add(right);

        savePpt(nodes, "test-output-anchors.pptx");
    }

    /**
     * TEST 3: THE ROBUSTNESS TEST
     * Ensures the code doesn't crash with garbage input.
     */
    @Test
    void testRobustness() throws IOException {
        // Empty List
        byte[] resultEmpty = pptGeneratorService.generateSlide(new ArrayList<>());
        assertNotNull(resultEmpty);
        assertTrue(resultEmpty.length > 0);

        // Null List
        byte[] resultNull = pptGeneratorService.generateSlide(null);
        assertNotNull(resultNull);
        assertTrue(resultNull.length > 0);
    }

    // --- HELPER METHODS ---

    private ServiceNode createNode(String id, String image, String... links) {
        ServiceNode node = new ServiceNode();
        node.setId(id);
        node.setImage(image);
        // Basic type detection for test convenience
        if (image.contains("postgres") || image.contains("mysql")) {
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
        System.out.println("✅ Generated: " + outputFile.getAbsolutePath());
    }
}