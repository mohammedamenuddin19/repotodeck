package com.repotodeck.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ServiceNode {

    /**
     * The service name from docker-compose (e.g. "web").
     */
    private String id;

    /**
     * The container image (e.g. "nginx:latest").
     */
    private String image;

    /**
     * The node type. Defaults to "SERVICE", but can be set to "DATABASE"
     * based on heuristics in the parser.
     */
    private String type = "SERVICE";

    /**
     * List of services this node connects to, derived from "depends_on" or "links".
     */
    private List<String> links = new ArrayList<>();
}

