package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class DockerParserService {

    /**
     * Parse a docker-compose YAML string into a list of ServiceNode objects.
     *
     * @param yamlContent raw docker-compose YAML
     * @return list of parsed ServiceNode instances
     */
    @SuppressWarnings("unchecked")
    public List<ServiceNode> parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return Collections.emptyList();
        }

        Yaml yaml = new Yaml();
        Object rootObject = yaml.load(yamlContent);
        if (!(rootObject instanceof Map<?, ?> rootMap)) {
            return Collections.emptyList();
        }

        Object servicesObj = rootMap.get("services");
        if (!(servicesObj instanceof Map<?, ?> servicesMapRaw)) {
            return Collections.emptyList();
        }

        Map<String, Object> servicesMap = (Map<String, Object>) servicesMapRaw;
        List<ServiceNode> result = new ArrayList<>();

        for (Map.Entry<String, Object> entry : servicesMap.entrySet()) {
            String serviceName = entry.getKey();
            Object value = entry.getValue();

            if (!(value instanceof Map<?, ?> serviceDefRaw)) {
                continue;
            }

            Map<String, Object> serviceDef = (Map<String, Object>) serviceDefRaw;

            ServiceNode node = new ServiceNode();
            node.setId(serviceName);

            String image = serviceDef.getOrDefault("image", "").toString();
            node.setImage(image);

            // Smart logic to determine type
            String loweredImage = image.toLowerCase();
            if (loweredImage.contains("postgres")
                    || loweredImage.contains("mysql")
                    || loweredImage.contains("mongo")) {
                node.setType("DATABASE");
            }

            // Collect links from "depends_on" and "links"
            Set<String> links = new TreeSet<>();
            extractLinksFromField(serviceDef.get("depends_on"), links);
            extractLinksFromField(serviceDef.get("links"), links);

            node.setLinks(new ArrayList<>(links));
            result.add(node);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void extractLinksFromField(Object field, Set<String> links) {
        if (field == null) {
            return;
        }

        if (field instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    links.add(item.toString());
                }
            }
        } else if (field instanceof Map<?, ?> map) {
            // In compose v3, depends_on can be a map of serviceName -> condition
            for (Object key : map.keySet()) {
                if (key != null) {
                    links.add(key.toString());
                }
            }
        } else if (field instanceof String str) {
            links.add(str);
        }
    }
}

