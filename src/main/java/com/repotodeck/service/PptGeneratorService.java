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

    // --- CONFIGURATION FOR "COOL" LOOK ---
    private static final int NODE_WIDTH = 220;
    private static final int NODE_HEIGHT = 100;
    private static final int NODE_SPACING_X = 50;
    private static final int LAYER_SPACING_Y = 200;
    private static final int START_Y = 100;
    
    // Modern Palette (Material Design)
    private static final Color COLOR_BG = new Color(248, 249, 250); // Off-White Background
    private static final Color COLOR_SERVICE = new Color(37, 99, 235); // Royal Blue
    private static final Color COLOR_DB = new Color(234, 88, 12);     // Burnt Orange
    private static final Color COLOR_LINE = new Color(156, 163, 175); // Soft Gray
    private static final Color COLOR_SHADOW = new Color(200, 200, 200); // Light Shadow

    public byte[] generateSlide(List<ServiceNode> nodes) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            pptx.setPageSize(new java.awt.Dimension(1280, 720));
            XSLFSlide slide = pptx.createSlide();

            // 0. Set Background Color (Looks premium)
            XSLFBackground bg = slide.getBackground();
            bg.setFillColor(COLOR_BG);

            if (nodes == null || nodes.isEmpty()) {
                createEmptyState(slide);
                return writeToByteArray(pptx);
            }

            // 1. Math: Calculate All Positions First (Don't draw yet)
            Map<Integer, List<ServiceNode>> layers = organizeIntoLayers(nodes);
            Map<String, Rectangle2D.Double> nodePositions = calculatePositions(layers);

            // 2. Layer 1: Connectors (Background)
            // Lines must be drawn FIRST so they appear BEHIND the boxes
            drawConnectors(slide, nodes, nodePositions);

            // 3. Layer 2: Shadows (Middle Ground)
            // Gives depth to the diagram
            drawShadows(slide, layers, nodePositions);

            // 4. Layer 3: Nodes (Foreground)
            // Boxes drawn last so they cover lines and shadows
            drawNodes(slide, layers, nodePositions);

            return writeToByteArray(pptx);
        }
    }

    // --- MATH HELPERS ---

    private Map<String, Rectangle2D.Double> calculatePositions(Map<Integer, List<ServiceNode>> layers) {
        Map<String, Rectangle2D.Double> positions = new HashMap<>();
        
        for (int layerIdx = 0; layerIdx <= 2; layerIdx++) {
            List<ServiceNode> layerNodes = layers.getOrDefault(layerIdx, new ArrayList<>());
            if (layerNodes.isEmpty()) continue;

            double totalLayerWidth = layerNodes.size() * (NODE_WIDTH + NODE_SPACING_X) - NODE_SPACING_X;
            double startX = (1280 - totalLayerWidth) / 2;
            double currentY = START_Y + (layerIdx * LAYER_SPACING_Y);

            for (int i = 0; i < layerNodes.size(); i++) {
                ServiceNode node = layerNodes.get(i);
                double currentX = startX + (i * (NODE_WIDTH + NODE_SPACING_X));
                positions.put(node.getId(), new Rectangle2D.Double(currentX, currentY, NODE_WIDTH, NODE_HEIGHT));
            }
        }
        return positions;
    }

    // --- DRAWING HELPERS ---

    private void drawConnectors(XSLFSlide slide, List<ServiceNode> nodes, Map<String, Rectangle2D.Double> positions) {
        for (ServiceNode node : nodes) {
            if (node.getLinks() == null) continue;
            Rectangle2D.Double start = positions.get(node.getId());
            if (start == null) continue;

            for (String target : node.getLinks()) {
                Rectangle2D.Double end = positions.get(target);
                if (end == null) continue;

                // Smart Anchor Logic (Waterfall Style)
                double startX = start.getX() + start.getWidth() / 2;
                double startY;
                double endX = end.getX() + end.getWidth() / 2;
                double endY;

                if (end.getY() > start.getY() + start.getHeight()) { // Target is below
                    startY = start.getY() + start.getHeight();
                    endY = end.getY();
                } else if (end.getY() < start.getY() - start.getHeight()) { // Target is above
                    startY = start.getY();
                    endY = end.getY() + end.getHeight();
                } else { // Side by side
                    startY = start.getY() + start.getHeight() / 2;
                    endY = end.getY() + end.getHeight() / 2;
                }

                // Draw Rotated Rectangle Line
                double deltaX = endX - startX;
                double deltaY = endY - startY;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

                XSLFAutoShape line = slide.createAutoShape();
                line.setShapeType(ShapeType.RECT);
                line.setFillColor(COLOR_LINE);
                line.setLineColor(COLOR_LINE); // No border
                
                double centerX = (startX + endX) / 2;
                double centerY = (startY + endY) / 2;
                
                line.setAnchor(new Rectangle2D.Double(centerX - (length / 2), centerY - 1, length, 2));
                line.setRotation(angle);
            }
        }
    }

    private void drawShadows(XSLFSlide slide, Map<Integer, List<ServiceNode>> layers, Map<String, Rectangle2D.Double> positions) {
        // Simple trick: Draw a gray box 5 pixels offset from the real box
        for (List<ServiceNode> layerNodes : layers.values()) {
            for (ServiceNode node : layerNodes) {
                Rectangle2D.Double pos = positions.get(node.getId());
                if (pos != null) {
                    XSLFAutoShape shadow = slide.createAutoShape();
                    if ("DATABASE".equals(node.getType())) {
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

    private void drawNodes(XSLFSlide slide, Map<Integer, List<ServiceNode>> layers, Map<String, Rectangle2D.Double> positions) {
        for (List<ServiceNode> layerNodes : layers.values()) {
            for (ServiceNode node : layerNodes) {
                Rectangle2D.Double pos = positions.get(node.getId());
                if (pos != null) {
                    XSLFAutoShape shape = slide.createAutoShape();
                    
                    if ("DATABASE".equals(node.getType())) {
                        shape.setShapeType(ShapeType.FLOW_CHART_MAGNETIC_DISK);
                        shape.setFillColor(COLOR_DB);
                    } else {
                        shape.setShapeType(ShapeType.ROUND_RECT);
                        shape.setFillColor(COLOR_SERVICE);
                    }

                    shape.setAnchor(pos);
                    shape.setLineColor(new Color(255, 255, 255, 100)); // Subtle white border
                    shape.setLineWidth(1.0);

                    // Text
                    XSLFTextParagraph p = shape.addNewTextParagraph();
                    p.setTextAlign(TextParagraph.TextAlign.CENTER);
                    
                    XSLFTextRun r1 = p.addNewTextRun();
                    r1.setText(node.getId());
                    r1.setFontSize(16.0); // Bigger Text
                    r1.setBold(true);
                    r1.setFontColor(Color.WHITE);

                    if (node.getImage() != null && !node.getImage().isEmpty()) {
                        XSLFTextRun r2 = p.addNewTextRun();
                        r2.setText("\n" + node.getImage());
                        r2.setFontSize(11.0);
                        r2.setItalic(true); // Stylish
                        r2.setFontColor(new Color(240, 240, 240));
                    }
                }
            }
        }
    }

    // --- DATA ORGANIZATION ---

    private Map<Integer, List<ServiceNode>> organizeIntoLayers(List<ServiceNode> nodes) {
        Map<Integer, List<ServiceNode>> layers = new HashMap<>();
        layers.put(0, new ArrayList<>());
        layers.put(1, new ArrayList<>());
        layers.put(2, new ArrayList<>());

        for (ServiceNode node : nodes) {
            String id = node.getId().toLowerCase();
            String image = node.getImage() != null ? node.getImage().toLowerCase() : "";

            if (node.getType().equals("DATABASE") || image.contains("redis") || image.contains("db") || image.contains("mysql") || image.contains("mongo")) {
                layers.get(2).add(node);
            } else if (image.contains("nginx") || image.contains("react") || image.contains("web") || image.contains("front") || image.contains("ui")) {
                layers.get(0).add(node);
            } else {
                layers.get(1).add(node);
            }
        }
        return layers;
    }

    private void createEmptyState(XSLFSlide slide) {
        XSLFTextBox tb = slide.createTextBox();
        tb.setText("No services found in YAML");
        tb.setAnchor(new Rectangle2D.Double(100, 100, 500, 50));
    }

    private byte[] writeToByteArray(XMLSlideShow pptx) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pptx.write(out);
            return out.toByteArray();
        }
    }
}