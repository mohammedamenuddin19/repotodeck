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

    private static final int NODE_WIDTH = 180;
    private static final int NODE_HEIGHT = 90;
    private static final int NODE_SPACING_X = 60;  // Space between nodes horizontally
    private static final int LAYER_SPACING_Y = 180; // Space between layers vertically
    private static final int START_Y = 120; // Top margin

    public byte[] generateSlide(List<ServiceNode> nodes) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            pptx.setPageSize(new java.awt.Dimension(1280, 720)); // HD Aspect Ratio
            XSLFSlide slide = pptx.createSlide();

            if (nodes == null || nodes.isEmpty()) {
                createEmptyState(slide);
                return writeToByteArray(pptx);
            }

            // 1. Organize Nodes into Layers (Tiers)
            Map<Integer, List<ServiceNode>> layers = organizeIntoLayers(nodes);
            Map<String, Rectangle2D.Double> nodePositions = new HashMap<>();

            // 2. Draw Nodes Layer by Layer
            for (int layerIdx = 0; layerIdx <= 2; layerIdx++) {
                List<ServiceNode> layerNodes = layers.getOrDefault(layerIdx, new ArrayList<>());
                if (layerNodes.isEmpty()) continue;

                // Calculate center alignment
                double totalLayerWidth = layerNodes.size() * (NODE_WIDTH + NODE_SPACING_X) - NODE_SPACING_X;
                double startX = (1280 - totalLayerWidth) / 2;
                double currentY = START_Y + (layerIdx * LAYER_SPACING_Y);

                for (int i = 0; i < layerNodes.size(); i++) {
                    ServiceNode node = layerNodes.get(i);
                    double currentX = startX + (i * (NODE_WIDTH + NODE_SPACING_X));

                    createNodeShape(slide, node, currentX, currentY);
                    
                    // Save position for connectors
                    nodePositions.put(node.getId(), new Rectangle2D.Double(currentX, currentY, NODE_WIDTH, NODE_HEIGHT));
                }
            }

            // 3. Draw Connectors (After nodes so lines are clean)
            drawConnectors(slide, nodes, nodePositions);

            return writeToByteArray(pptx);
        }
    }

    private Map<Integer, List<ServiceNode>> organizeIntoLayers(List<ServiceNode> nodes) {
        Map<Integer, List<ServiceNode>> layers = new HashMap<>();
        layers.put(0, new ArrayList<>()); // Frontend/Web
        layers.put(1, new ArrayList<>()); // Backend/API
        layers.put(2, new ArrayList<>()); // Database/Infra

        for (ServiceNode node : nodes) {
            String id = node.getId().toLowerCase();
            String image = node.getImage() != null ? node.getImage().toLowerCase() : "";

            if (node.getType().equals("DATABASE") || image.contains("redis") || image.contains("kafka")) {
                layers.get(2).add(node);
            } else if (image.contains("nginx") || image.contains("react") || image.contains("web") || image.contains("front")) {
                layers.get(0).add(node);
            } else {
                layers.get(1).add(node); // Default to middle tier (API/Services)
            }
        }
        return layers;
    }

    private void createNodeShape(XSLFSlide slide, ServiceNode node, double x, double y) {
        XSLFAutoShape shape = slide.createAutoShape();
        
        if ("DATABASE".equals(node.getType())) {
            shape.setShapeType(ShapeType.FLOW_CHART_MAGNETIC_DISK);
            shape.setFillColor(new Color(255, 140, 0)); // Darker Orange
        } else {
            shape.setShapeType(ShapeType.ROUND_RECT);
            shape.setFillColor(new Color(0, 114, 198)); // Professional Blue
        }

        shape.setAnchor(new Rectangle2D.Double(x, y, NODE_WIDTH, NODE_HEIGHT));
        shape.setLineColor(Color.DARK_GRAY);
        shape.setLineWidth(1.5);

        shape.clearText();
        XSLFTextParagraph p = shape.addNewTextParagraph();
        p.setTextAlign(TextParagraph.TextAlign.CENTER);
        
        XSLFTextRun r1 = p.addNewTextRun();
        r1.setText(node.getId());
        r1.setFontSize(14.0);
        r1.setBold(true);
        r1.setFontColor(Color.WHITE);
        r1.setFontFamily("DejaVu Sans");

        if (node.getImage() != null && !node.getImage().isEmpty()) {
            XSLFTextRun r2 = p.addNewTextRun();
            r2.setText("\n(" + node.getImage() + ")");
            r2.setFontSize(10.0);
            r2.setFontColor(new Color(230, 230, 230));
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

                // --- FIX: Use XSLFConnectorShape instead of AutoShape ---
                // This is the "Native" way to draw lines in PPT.
                // It avoids the "Corrupt Shape" errors caused by manual geometry.
                XSLFConnectorShape connector = slide.createConnector();
                
                double startX = start.getX() + start.getWidth() / 2;
                double startY = start.getY() + start.getHeight() / 2;
                double endX = end.getX() + end.getWidth() / 2;
                double endY = end.getY() + end.getHeight() / 2;

                double width = Math.abs(endX - startX);
                double height = Math.abs(endY - startY);

                // Prevent Zero-Dimension crash (PowerPoint hates 0x0 objects)
                if (width < 1.0) width = 1.0;
                if (height < 1.0) height = 1.0;

                connector.setAnchor(new Rectangle2D.Double(
                    Math.min(startX, endX), 
                    Math.min(startY, endY),
                    width, 
                    height
                ));

                // Handle Direction Flipping (Crucial for diagonals)
                if (startX > endX) {
                    connector.setFlipHorizontal(true);
                }
                if (startY > endY) {
                    connector.setFlipVertical(true);
                }

                connector.setLineColor(Color.GRAY);
                connector.setLineWidth(1.5);
            }
        }
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