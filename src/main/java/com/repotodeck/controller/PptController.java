package com.repotodeck.controller;

import com.repotodeck.model.ServiceNode;
import com.repotodeck.service.DockerParserService;
import com.repotodeck.service.PptGeneratorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PptController {

    private final DockerParserService dockerParserService;
    private final PptGeneratorService pptGeneratorService;

    public PptController(DockerParserService dockerParserService, PptGeneratorService pptGeneratorService) {
        this.dockerParserService = dockerParserService;
        this.pptGeneratorService = pptGeneratorService;
    }

    /**
     * Generate a PowerPoint presentation from Docker Compose YAML.
     * Expects JSON payload: { "yaml": "version: '3.8'..." }
     */
    @PostMapping(value = "/generate-slide", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generateSlide(@RequestBody Map<String, String> payload) {
        try {
            // 1. Extract YAML string from JSON wrapper
            String yamlContent = payload.get("yaml");
            
            if (yamlContent == null || yamlContent.trim().isEmpty()) {
                throw new IllegalArgumentException("YAML content cannot be empty");
            }

            // 2. Parse YAML to ServiceNode list
            List<ServiceNode> nodes = dockerParserService.parse(yamlContent);
            
            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("No services found in YAML. Check formatting.");
            }

            // 3. Generate PowerPoint slide
            byte[] pptxBytes = pptGeneratorService.generateSlide(nodes);

            // 4. Set headers for file download
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=architecture.pptx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                    .body(pptxBytes);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("Error generating PowerPoint: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(("Error processing YAML: " + e.getMessage()).getBytes());
        }
    }
}