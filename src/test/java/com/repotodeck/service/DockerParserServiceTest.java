package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerParserServiceTest {

    private final DockerParserService dockerParserService = new DockerParserService();

    @Test
    void parse_shouldExtractServicesImagesLinksAndTypes() {
        String yaml = """
                version: '3.8'
                services:
                  web:
                    image: nginx:latest
                    depends_on:
                      - db
                      - cache
                  db:
                    image: postgres:14
                  cache:
                    image: redis:7
                    links:
                      - web
                """;

        List<ServiceNode> nodes = dockerParserService.parse(yaml);

        assertNotNull(nodes);
        assertEquals(3, nodes.size(), "Should parse three services");

        ServiceNode web = findById(nodes, "web");
        ServiceNode db = findById(nodes, "db");
        ServiceNode cache = findById(nodes, "cache");

        assertEquals("nginx:latest", web.getImage());
        assertEquals("SERVICE", web.getType());
        assertTrue(web.getLinks().contains("db"));
        assertTrue(web.getLinks().contains("cache"));

        assertEquals("postgres:14", db.getImage());
        assertEquals("DATABASE", db.getType(), "Postgres image should be classified as DATABASE");

        assertEquals("redis:7", cache.getImage());
        assertEquals("SERVICE", cache.getType());
        assertTrue(cache.getLinks().contains("web"));
    }

    private ServiceNode findById(List<ServiceNode> nodes, String id) {
        Optional<ServiceNode> node = nodes.stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst();
        assertTrue(node.isPresent(), "Service with id '" + id + "' should exist");
        return node.get();
    }
}

