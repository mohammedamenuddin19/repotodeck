package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PptGeneratorService {

    // --- CONFIGURATION ---
    private static final int SLIDE_WIDTH = 1920;
    // We increase Height to 2000 to accommodate "Wrapping" rows. 
    // PPT scales this automatically, so it's safe.
    private static final int SLIDE_HEIGHT = 2000; 
    
    private static final int NODE_WIDTH = 220;
    private static final int NODE_HEIGHT = 100;
    private static final int NODE_SPACING_X = 60; // Horizontal gap
    private static final int ROW_SPACING_Y = 150; // Vertical gap WITHIN a tier (wrapping)
    private static final int TIER_SPACING_Y = 100; // Gap BETWEEN tiers
    private static final int START_Y = 100;
    private static final int MAX_NODES_PER_ROW = 5; // Force wrapping after 5 boxes

    // --- PALETTE ---
    private static final Color COLOR_BG = new Color(248, 249, 250); 
    private static final Color COLOR_FRONTEND = new Color(13, 148, 136); 
    private static final Color COLOR_SERVICE = new Color(37, 99, 235);   
    private static final Color COLOR_DB = new Color(234, 88, 12);        
    private static final Color COLOR_LINE = new Color(156, 163, 175); 
    private static final Color COLOR_SHADOW = new Color(200, 200, 200); 

    public byte[] generateSlide(List<ServiceNode> nodes) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            pptx.setPageSize(new java.awt.Dimension(SLIDE_WIDTH, SLIDE_HEIGHT));
            XSLFSlide slide = pptx.createSlide();
            slide.getBackground().setFillColor(COLOR_BG);

            if (nodes == null || nodes.isEmpty()) {
                createEmptyState(slide);
                return writeToByteArray(pptx);
            }

            Map<Integer, List<ServiceNode>> layers = organizeIntoLayers(nodes);
            
            // 1. DYNAMIC LAYOUT CALCULATION (The Grid Fix)
            Map<String, Rectangle2D.Double> nodePositions = calculateGridPositions(layers);

            drawConnectors(slide, nodes, nodePositions);
            drawShadows(slide, layers, nodePositions);
            drawNodes(slide, layers, nodePositions);

            return writeToByteArray(pptx);
        }
    }

    /**
     * THE NEW GRID ENGINE
     * Handles wrapping so boxes never go off-screen.
     */
    private Map<String, Rectangle2D.Double> calculateGridPositions(Map<Integer, List<ServiceNode>> layers) {
        Map<String, Rectangle2D.Double> positions = new HashMap<>();
        double currentY = START_Y;

        for (int layerIdx = 0; layerIdx <= 2; layerIdx++) {
            List<ServiceNode> layerNodes = layers.getOrDefault(layerIdx, new ArrayList<>());
            if (layerNodes.isEmpty()) continue;

            // Calculate rows needed for this layer
            int totalNodes = layerNodes.size();
            int rowsNeeded = (int) Math.ceil((double) totalNodes / MAX_NODES_PER_ROW);

            for (int r = 0; r < rowsNeeded; r++) {
                // Determine nodes in this specific row
                int startIdx = r * MAX_NODES_PER_ROW;
                int endIdx = Math.min(startIdx + MAX_NODES_PER_ROW, totalNodes);
                int nodesInThisRow = endIdx - startIdx;

                // Center this row horizontally
                double rowWidth = nodesInThisRow * NODE_WIDTH + (nodesInThisRow - 1) * NODE_SPACING_X;
                double startX = (SLIDE_WIDTH - rowWidth) / 2;

                for (int i = 0; i < nodesInThisRow; i++) {
                    ServiceNode node = layerNodes.get(startIdx + i);
                    double x = startX + i * (NODE_WIDTH + NODE_SPACING_X);
                    double y = currentY;
                    positions.put(node.getId(), new Rectangle2D.Double(x, y, NODE_WIDTH, NODE_HEIGHT));
                }
                
                // Move Y down for the next row within the same tier
                currentY += ROW_SPACING_Y;
            }

            // Add extra spacing before the NEXT tier begins
            currentY += TIER_SPACING_Y;
        }
        return positions;
    }

    private Map<Integer, List<ServiceNode>> organizeIntoLayers(List<ServiceNode> nodes) {
        Map<Integer, List<ServiceNode>> layers = new HashMap<>();
        layers.put(0, new ArrayList<>());
        layers.put(1, new ArrayList<>());
        layers.put(2, new ArrayList<>());

        for (ServiceNode node : nodes) {
            String id = node.getId().toLowerCase();
            String image = node.getImage() != null ? node.getImage().toLowerCase() : "";

            // --- IMPROVED DETECTION HEURISTICS ---
            // Added: cassandra, elastic, minio, bucket, mariadb, search, kafka, broker
            boolean isDb = image.contains("redis") || image.contains("mysql") || image.contains("mongo") || 
                           image.contains("postgres") || image.contains("cassandra") || image.contains("elastic") || 
                           image.contains("mariadb") || image.contains("kafka") || image.contains("minio") || 
                           id.contains("db") || id.contains("database") || id.contains("store") || 
                           id.contains("bucket") || id.contains("broker");

            boolean isFrontend = image.contains("nginx") || image.contains("react") || image.contains("web") || 
                                 image.contains("gateway") || image.contains("balancer") || image.contains("zuul") || 
                                 image.contains("frontend") || image.contains("ui");

            if (isDb) {
                layers.get(2).add(node);
            } else if (isFrontend) {
                layers.get(0).add(node);
            } else {
                layers.get(1).add(node);
            }
        }
        return layers;
    }

    // --- DRAWING (Unchanged but included for safety) ---
    
    private void drawNodes(XSLFSlide slide, Map<Integer, List<ServiceNode>> layers, Map<String, Rectangle2D.Double> positions) {
        for (int layerIdx : layers.keySet()) {
            List<ServiceNode> layerNodes = layers.get(layerIdx);
            for (ServiceNode node : layerNodes) {
                Rectangle2D.Double pos = positions.get(node.getId());
                if (pos != null) {
                    XSLFAutoShape shape = slide.createAutoShape();
                    if ("DATABASE".equals(node.getType()) || layers.get(2).contains(node)) {
                         shape.setShapeType(ShapeType.FLOW_CHART_MAGNETIC_DISK);
                         shape.setFillColor(COLOR_DB);
                    } else if (layerIdx == 0) {
                        shape.setShapeType(ShapeType.ROUND_RECT);
                        shape.setFillColor(COLOR_FRONTEND);
                    } else {
                        shape.setShapeType(ShapeType.ROUND_RECT);
                        shape.setFillColor(COLOR_SERVICE);
                    }
                    shape.setAnchor(pos);
                    shape.setLineColor(new Color(255, 255, 255, 100));
                    shape.setLineWidth(1.0);

                    XSLFTextParagraph p = shape.addNewTextParagraph();
                    p.setTextAlign(TextParagraph.TextAlign.CENTER);
                    XSLFTextRun r1 = p.addNewTextRun();
                    r1.setText(node.getId());
                    r1.setFontSize(16.0);
                    r1.setBold(true);
                    r1.setFontColor(Color.WHITE);
                    
                    if (node.getImage() != null && !node.getImage().isEmpty()) {
                        XSLFTextRun r2 = p.addNewTextRun();
                        r2.setText("\n" + node.getImage());
                        r2.setFontSize(11.0);
                        r2.setItalic(true);
                        r2.setFontColor(new Color(240, 240, 240));
                    }
                }
            }
        }
    }

    private void drawShadows(XSLFSlide slide, Map<Integer, List<ServiceNode>> layers, Map<String, Rectangle2D.Double> positions) {
        for (int layerIdx : layers.keySet()) {
            List<ServiceNode> layerNodes = layers.get(layerIdx);
            for (ServiceNode node : layerNodes) {
                Rectangle2D.Double pos = positions.get(node.getId());
                if (pos != null) {
                    XSLFAutoShape shadow = slide.createAutoShape();
                    if (layers.get(2).contains(node)) { 
                        shadow.setShapeType(ShapeType.FLOW_CHART_MAGNETIC_DISK);
                    } else {
                        shadow.setShapeType(ShapeType.ROUND_RECT);
                    }
                    shadow.setFillColor(COLOR_SHADOW);
                    shadow.setLineColor(COLOR_SHADOW);
                    shadow.setAnchor(new Rectangle2D.Double(pos.getX() + 6, pos.getY() + 6, NODE_WIDTH, NODE_HEIGHT));
                }
            }
        }
    }

    private void drawConnectors(XSLFSlide slide, List<ServiceNode> nodes, Map<String, Rectangle2D.Double> positions) {
        for (ServiceNode node : nodes) {
            if (node.getLinks() == null) continue;
            Rectangle2D.Double start = positions.get(node.getId());
            if (start == null) continue;
            for (String target : node.getLinks()) {
                Rectangle2D.Double end = positions.get(target);
                if (end == null) continue;
                
                double startX = start.getX() + start.getWidth() / 2;
                double startY;
                double endX = end.getX() + end.getWidth() / 2;
                double endY;

                if (end.getY() >= start.getY() + start.getHeight()) { // Target Below
                    startY = start.getY() + start.getHeight();
                    endY = end.getY();
                } else if (end.getY() <= start.getY() - start.getHeight()) { // Target Above
                    startY = start.getY();
                    endY = end.getY() + end.getHeight();
                } else { // Side-by-Side
                    startY = start.getY() + start.getHeight() / 2;
                    endY = end.getY() + end.getHeight() / 2;
                }

                double deltaX = endX - startX;
                double deltaY = endY - startY;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

                XSLFAutoShape line = slide.createAutoShape();
                line.setShapeType(ShapeType.RECT);
                line.setFillColor(COLOR_LINE);
                line.setLineColor(COLOR_LINE);
                line.setAnchor(new Rectangle2D.Double((startX + endX) / 2 - length / 2, (startY + endY) / 2 - 1, length, 2));
                line.setRotation(angle);
            }
        }
    }

    private void createEmptyState(XSLFSlide slide) {
        XSLFTextBox tb = slide.createTextBox();
        tb.setText("No services found");
        tb.setAnchor(new Rectangle2D.Double(100, 100, 500, 50));
    }

    private byte[] writeToByteArray(XMLSlideShow pptx) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pptx.write(out);
            return out.toByteArray();
        }
    }
}