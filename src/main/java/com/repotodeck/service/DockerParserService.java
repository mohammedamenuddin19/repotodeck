package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

@Service
public class DockerParserService {

    @SuppressWarnings("unchecked")
    public List<ServiceNode> parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Yaml yaml = new Yaml();
            Object rootObject = yaml.load(yamlContent);
            if (!(rootObject instanceof Map<?, ?> rootMap)) return Collections.emptyList();

            Object servicesObj = rootMap.get("services");
            if (!(servicesObj instanceof Map<?, ?> servicesMapRaw)) return Collections.emptyList();

            Map<String, Object> servicesMap = (Map<String, Object>) servicesMapRaw;
            List<ServiceNode> result = new ArrayList<>();

            for (Map.Entry<String, Object> entry : servicesMap.entrySet()) {
                try {
                    String serviceName = entry.getKey();
                    Object value = entry.getValue();

                    if (!(value instanceof Map<?, ?> serviceDefRaw)) continue;
                    Map<String, Object> serviceDef = (Map<String, Object>) serviceDefRaw;

                    ServiceNode node = new ServiceNode();
                    node.setId(serviceName);

                    String image = serviceDef.getOrDefault("image", "unknown").toString();
                    node.setImage(image);

                    // Database Heuristics
                    String lowerImg = image.toLowerCase();
                    if (lowerImg.contains("postgres") || lowerImg.contains("mysql") || 
                        lowerImg.contains("mongo") || lowerImg.contains("redis") || 
                        lowerImg.contains("mariadb")) {
                        node.setType("DATABASE");
                    }

                    // Links
                    Set<String> links = new TreeSet<>();
                    extractLinksFromField(serviceDef.get("depends_on"), links);
                    extractLinksFromField(serviceDef.get("links"), links);

                    node.setLinks(new ArrayList<>(links));
                    result.add(node);
                } catch (Exception e) {
                    System.err.println("Skipping malformed service: " + entry.getKey());
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid YAML format: " + e.getMessage());
        }
    }

    private void extractLinksFromField(Object field, Set<String> links) {
        if (field == null) return;

        if (field instanceof List<?> list) {
            for (Object item : list) links.add(item.toString());
        } else if (field instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) links.add(key.toString());
        } else if (field instanceof String str) {
            links.add(str);
        }
    }
}