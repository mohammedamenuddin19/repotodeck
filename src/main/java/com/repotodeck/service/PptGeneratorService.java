package com.repotodeck.service;

import com.repotodeck.model.ServiceNode;
import org.apache.poi.sl.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PptGeneratorService {

    private static final int COLUMNS = 3;
    private static final int SHAPE_WIDTH = 200;
    private static final int SHAPE_HEIGHT = 100;
    private static final int HORIZONTAL_SPACING = 250;
    private static final int VERTICAL_SPACING = 150;
    private static final int START_X = 100;
    private static final int START_Y = 100;

    /**
     * Generate a PowerPoint slide from a list of ServiceNode objects.
     * Nodes are arranged in a 3-column grid with connectors showing links.
     *
     * @param nodes list of ServiceNode objects to visualize
     * @return byte array representing the PPTX file
     * @throws IOException if PowerPoint generation fails
     */
    public byte[] generateSlide(List<ServiceNode> nodes) throws IOException {
        // 1. Wrap creation in try-with-resources so it auto-closes
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            
            if (nodes == null || nodes.isEmpty()) {
                XSLFSlide slide = pptx.createSlide();
                XSLFTextBox textBox = slide.createTextBox();
                textBox.setText("No services to display");
                textBox.setAnchor(new Rectangle2D.Double(START_X, START_Y, 400, 50));
                return writeToByteArray(pptx);
            }
    
            // Set slide size (16:9 aspect ratio)
            pptx.setPageSize(new java.awt.Dimension(1920, 1080));
            XSLFSlide slide = pptx.createSlide();
    
            Map<String, Rectangle2D.Double> nodePositions = new HashMap<>();
    
            // Create shapes
            for (int i = 0; i < nodes.size(); i++) {
                ServiceNode node = nodes.get(i);
                int row = i / COLUMNS;
                int col = i % COLUMNS;
                double x = START_X + col * HORIZONTAL_SPACING;
                double y = START_Y + row * VERTICAL_SPACING;
    
                XSLFAutoShape shape = createNodeShape(slide, node, x, y);
                Rectangle2D.Double bounds = new Rectangle2D.Double(x, y, SHAPE_WIDTH, SHAPE_HEIGHT);
                nodePositions.put(node.getId(), bounds);
            }
    
            // Draw connectors
            drawConnectors(slide, nodes, nodePositions);
    
            return writeToByteArray(pptx);
        } // <--- pptx.close() happens automatically here!
    }

    /**
     * Create a shape for a ServiceNode based on its type.
     */
    private XSLFAutoShape createNodeShape(XSLFSlide slide, ServiceNode node, double x, double y) {
        XSLFAutoShape shape;

        if ("DATABASE".equals(node.getType())) {
            // FlowChart_Magnetic_Disk shape for databases (Orange)
            shape = slide.createAutoShape();
            shape.setShapeType(ShapeType.FLOW_CHART_MAGNETIC_DISK);
            shape.setFillColor(new Color(255, 165, 0)); // Orange
        } else {
            // Round Rectangle for services (Blue)
            shape = slide.createAutoShape();
            shape.setShapeType(ShapeType.ROUND_RECT);
            shape.setFillColor(new Color(0, 100, 200)); // Blue
        }

        shape.setAnchor(new Rectangle2D.Double(x, y, SHAPE_WIDTH, SHAPE_HEIGHT));
        shape.setLineColor(Color.BLACK);
        shape.setLineWidth(2.0);

        // Add text label (ID and image)
        // Clear existing text by removing all paragraphs and creating a new one
        shape.clearText();
        XSLFTextParagraph paragraph = shape.addNewTextParagraph();
        XSLFTextRun run1 = paragraph.addNewTextRun();
        run1.setText(node.getId());
        run1.setFontSize(14.0);
        run1.setBold(true);
        run1.setFontColor(Color.WHITE);

        if (node.getImage() != null && !node.getImage().isEmpty()) {
            XSLFTextRun run2 = paragraph.addNewTextRun();
            run2.setText("\n" + node.getImage());
            run2.setFontSize(10.0);
            run2.setFontColor(Color.WHITE);
        }

        paragraph.setTextAlign(TextParagraph.TextAlign.CENTER);

        return shape;
    }

    /**
     * Draw connectors (lines) between nodes based on their links.
     */
    private void drawConnectors(XSLFSlide slide, List<ServiceNode> nodes, Map<String, Rectangle2D.Double> positions) {
        for (ServiceNode node : nodes) {
            Rectangle2D.Double sourcePos = positions.get(node.getId());
            if (sourcePos == null || node.getLinks() == null) {
                continue;
            }

            for (String targetId : node.getLinks()) {
                Rectangle2D.Double targetPos = positions.get(targetId);
                if (targetPos == null) {
                    continue; // Target node not found
                }

                // Calculate connection points (center of shapes)
                double sourceX = sourcePos.getX() + sourcePos.getWidth() / 2;
                double sourceY = sourcePos.getY() + sourcePos.getHeight() / 2;
                double targetX = targetPos.getX() + targetPos.getWidth() / 2;
                double targetY = targetPos.getY() + targetPos.getHeight() / 2;

                // Create connector line
                XSLFAutoShape connector = slide.createAutoShape();
                connector.setShapeType(ShapeType.LINE);
                
                // Calculate bounding box for the line
                double minX = Math.min(sourceX, targetX);
                double minY = Math.min(sourceY, targetY);
                double width = Math.abs(targetX - sourceX);
                double height = Math.abs(targetY - sourceY);
                
                // Ensure minimum dimensions for visibility
                if (width < 1) width = 1;
                if (height < 1) height = 1;
                
                connector.setAnchor(new Rectangle2D.Double(minX, minY, width, height));
                connector.setLineColor(Color.GRAY);
                connector.setLineWidth(1.5);
                connector.setFillColor(null); // No fill for lines
            }
        }
    }

    /**
     * Write the PowerPoint presentation to a byte array.
     */
    private byte[] writeToByteArray(XMLSlideShow pptx) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pptx.write(out);
            return out.toByteArray();
        }
    }
}
