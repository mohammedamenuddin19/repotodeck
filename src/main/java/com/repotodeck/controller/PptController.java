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
     *
     * @param yamlContent the Docker Compose YAML content as a string
     * @return ResponseEntity with PPTX file as downloadable attachment
     */
    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> generateSlide(@RequestBody String yamlContent) {
        try {
            // Parse YAML to ServiceNode list
            List<ServiceNode> nodes = dockerParserService.parse(yamlContent);

            // Generate PowerPoint slide
            byte[] pptxBytes = pptGeneratorService.generateSlide(nodes);

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "architecture.pptx");
            headers.setContentLength(pptxBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pptxBytes);

        } catch (IOException e) {
            // Return error response
            return ResponseEntity.internalServerError()
                    .body(("Error generating PowerPoint: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            // Return error response for parsing or other errors
            return ResponseEntity.badRequest()
                    .body(("Error processing YAML: " + e.getMessage()).getBytes());
        }
    }
}
