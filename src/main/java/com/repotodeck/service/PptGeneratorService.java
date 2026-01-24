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
    // Canvas upgraded to Full HD (1920x1080) to fit more boxes
    private static final int SLIDE_WIDTH = 1920;
    private static final int SLIDE_HEIGHT = 1080;
    
    private static final int NODE_WIDTH = 220;
    private static final int NODE_HEIGHT = 100;
    private static final int NODE_SPACING_X = 60;
    private static final int LAYER_SPACING_Y = 250; // More vertical breathing room
    private static final int START_Y = 150;
    
    // --- 3-TIER PALETTE ---
    private static final Color COLOR_BG = new Color(248, 249, 250); 
    private static final Color COLOR_FRONTEND = new Color(13, 148, 136); // Teal (Entry Points)
    private static final Color COLOR_SERVICE = new Color(37, 99, 235);   // Royal Blue (Logic)
    private static final Color COLOR_DB = new Color(234, 88, 12);        // Burnt Orange (Data)
    private static final Color COLOR_LINE = new Color(156, 163, 175); 
    private static final Color COLOR_SHADOW = new Color(200, 200, 200); 

    public byte[] generateSlide(List<ServiceNode> nodes) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            pptx.setPageSize(new java.awt.Dimension(SLIDE_WIDTH, SLIDE_HEIGHT));
            XSLFSlide slide = pptx.createSlide();

            XSLFBackground bg = slide.getBackground();
            bg.setFillColor(COLOR_BG);

            if (nodes == null || nodes.isEmpty()) {
                createEmptyState(slide);
                return writeToByteArray(pptx);
            }

            // 1. Math
            Map<Integer, List<ServiceNode>> layers = organizeIntoLayers(nodes);
            Map<String, Rectangle2D.Double> nodePositions = calculatePositions(layers);

            // 2. Draw Layers (Order matters for Z-Index)
            drawConnectors(slide, nodes, nodePositions); // Bottom
            drawShadows(slide, layers, nodePositions);   // Middle
            drawNodes(slide, layers, nodePositions);     // Top

            return writeToByteArray(pptx);
        }
    }

    // --- MATH ---

    private Map<String, Rectangle2D.Double> calculatePositions(Map<Integer, List<ServiceNode>> layers) {
        Map<String, Rectangle2D.Double> positions = new HashMap<>();
        
        for (int layerIdx = 0; layerIdx <= 2; layerIdx++) {
            List<ServiceNode> layerNodes = layers.getOrDefault(layerIdx, new ArrayList<>());
            if (layerNodes.isEmpty()) continue;

            // Centering Logic
            double totalLayerWidth = layerNodes.size() * (NODE_WIDTH + NODE_SPACING_X) - NODE_SPACING_X;
            double startX = (SLIDE_WIDTH - totalLayerWidth) / 2;
            double currentY = START_Y + (layerIdx * LAYER_SPACING_Y);

            for (int i = 0; i < layerNodes.size(); i++) {
                ServiceNode node = layerNodes.get(i);
                double currentX = startX + (i * (NODE_WIDTH + NODE_SPACING_X));
                positions.put(node.getId(), new Rectangle2D.Double(currentX, currentY, NODE_WIDTH, NODE_HEIGHT));
            }
        }
        return positions;
    }

    // --- DRAWING ---

    private void drawConnectors(XSLFSlide slide, List<ServiceNode> nodes, Map<String, Rectangle2D.Double> positions) {
        for (ServiceNode node : nodes) {
            if (node.getLinks() == null) continue;
            Rectangle2D.Double start = positions.get(node.getId());
            if (start == null) continue;

            for (String target : node.getLinks()) {
                Rectangle2D.Double end = positions.get(target);
                if (end == null) continue;

                // Smart Anchor Logic (Waterfall)
                double startX = start.getX() + start.getWidth() / 2;
                double startY;
                double endX = end.getX() + end.getWidth() / 2;
                double endY;

                if (end.getY() > start.getY() + start.getHeight()) { // Below
                    startY = start.getY() + start.getHeight();
                    endY = end.getY();
                } else if (end.getY() < start.getY() - start.getHeight()) { // Above
                    startY = start.getY();
                    endY = end.getY() + end.getHeight();
                } else { // Side-by-Side
                    startY = start.getY() + start.getHeight() / 2;
                    endY = end.getY() + end.getHeight() / 2;
                }

                // Rotated Rectangle Line
                double deltaX = endX - startX;
                double deltaY = endY - startY;
                double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

                XSLFAutoShape line = slide.createAutoShape();
                line.setShapeType(ShapeType.RECT);
                line.setFillColor(COLOR_LINE);
                line.setLineColor(COLOR_LINE);
                
                double centerX = (startX + endX) / 2;
                double centerY = (startY + endY) / 2;
                
                line.setAnchor(new Rectangle2D.Double(centerX - (length / 2), centerY - 1, length, 2));
                line.setRotation(angle);
            }
        }
    }

    private void drawShadows(XSLFSlide slide, Map<Integer, List<ServiceNode>> layers, Map<String, Rectangle2D.Double> positions) {
        for (List<ServiceNode> layerNodes : layers.values()) {
            for (ServiceNode node : layerNodes) {
                Rectangle2D.Double pos = positions.get(node.getId());
                if (pos != null) {
                    XSLFAutoShape shadow = slide.createAutoShape();
                    shadow.setShapeType("DATABASE".equals(node.getType()) ? ShapeType.FLOW_CHART_MAGNETIC_DISK : ShapeType.ROUND_RECT);
                    shadow.setFillColor(COLOR_SHADOW);
                    shadow.setLineColor(COLOR_SHADOW);
                    shadow.setAnchor(new Rectangle2D.Double(pos.getX() + 6, pos.getY() + 6, NODE_WIDTH, NODE_HEIGHT));
                }
            }
        }
    }

    private void drawNodes(XSLFSlide slide, Map<Integer, List<ServiceNode>> layers, Map<String, Rectangle2D.Double> positions) {
        for (int layerIdx : layers.keySet()) {
            List<ServiceNode> layerNodes = layers.get(layerIdx);
            for (ServiceNode node : layerNodes) {
                Rectangle2D.Double pos = positions.get(node.getId());
                if (pos != null) {
                    XSLFAutoShape shape = slide.createAutoShape();
                    
                    // --- COLOR LOGIC ---
                    if ("DATABASE".equals(node.getType())) {
                        shape.setShapeType(ShapeType.FLOW_CHART_MAGNETIC_DISK);
                        shape.setFillColor(COLOR_DB); // Orange
                    } else if (layerIdx == 0) {
                        shape.setShapeType(ShapeType.ROUND_RECT);
                        shape.setFillColor(COLOR_FRONTEND); // Teal (Tier 1)
                    } else {
                        shape.setShapeType(ShapeType.ROUND_RECT);
                        shape.setFillColor(COLOR_SERVICE); // Blue (Tier 2)
                    }

                    shape.setAnchor(pos);
                    shape.setLineColor(new Color(255, 255, 255, 100)); 
                    shape.setLineWidth(1.0);

                    // Text
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

    // --- DATA ORGANIZATION ---

    private Map<Integer, List<ServiceNode>> organizeIntoLayers(List<ServiceNode> nodes) {
        Map<Integer, List<ServiceNode>> layers = new HashMap<>();
        layers.put(0, new ArrayList<>());
        layers.put(1, new ArrayList<>());
        layers.put(2, new ArrayList<>());

        for (ServiceNode node : nodes) {
            String id = node.getId().toLowerCase();
            String image = node.getImage() != null ? node.getImage().toLowerCase() : "";

            // Heuristics for Tiering
            if (node.getType().equals("DATABASE") || image.contains("redis") || image.contains("mysql") || image.contains("mongo") || image.contains("postgres")) {
                layers.get(2).add(node); // Bottom
            } else if (image.contains("nginx") || image.contains("react") || image.contains("web") || image.contains("gateway") || image.contains("balancer")) {
                layers.get(0).add(node); // Top
            } else {
                layers.get(1).add(node); // Middle
            }
        }
        return layers;
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