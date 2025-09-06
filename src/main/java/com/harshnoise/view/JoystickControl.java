package com.harshnoise.view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * JoystickControl - Interactive joystick widget for real-time parameter control
 * Features smooth interpolation, visual feedback, and precise mouse interaction
 */
public class JoystickControl extends Canvas {
    
    // ===== STYLE CONSTANTS =====
    private static final Color COLOR_BACKGROUND = Color.rgb(248, 249, 250);
    private static final Color COLOR_BORDER = Color.rgb(222, 226, 230);
    private static final Color COLOR_CROSSHAIR = Color.rgb(200, 200, 200);
    private static final Color COLOR_KNOB = Color.rgb(40, 167, 69);
    private static final Color COLOR_KNOB_BORDER = Color.rgb(30, 120, 50);
    private static final Color COLOR_SHADOW = Color.rgb(0, 0, 0, 0.1);
    
    // ===== GEOMETRY CONSTANTS =====
    private static final double KNOB_RADIUS = 16.0;
    private static final double BORDER_WIDTH = 3.0;
    private static final double CROSSHAIR_LENGTH = 12.0;
    private static final double SMOOTHING_ALPHA = 0.20; // Smoothing factor for interpolation
    
    // ===== READONLY PROPERTIES =====
    private float normX = 0.0f;  // Normalized X position [-1, 1]
    private float normY = 0.0f;  // Normalized Y position [-1, 1]
    private float radius = 0.0f; // Distance from center [0, 1]
    private float angle = 0.0f;  // Angle in radians [0, 2π)
    
    // ===== INTERNAL STATE =====
    private double centerX, centerY;
    private double maxRadius;
    private boolean isDragging = false;
    
    // Smoothing state
    private double targetX = 0.0;
    private double targetY = 0.0;
    private double currentX = 0.0;
    private double currentY = 0.0;
    
    // Animation timer for smooth return to center
    private AnimationTimer returnTimer;
    
    // Permanent animation timer for continuous updates
    private AnimationTimer pulse;
    
    public JoystickControl(double size) {
        super(size, size);
        
        // Calculate geometry
        centerX = size / 2.0;
        centerY = size / 2.0;
        maxRadius = (size / 2.0) - KNOB_RADIUS - BORDER_WIDTH;
        
        // Initialize return timer for smooth return to center
        returnTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateSmoothing();
                
                // Stop timer when close to center
                if (Math.abs(currentX) < 0.5 && Math.abs(currentY) < 0.5) {
                    currentX = 0.0;
                    currentY = 0.0;
                    stop();
                }
            }
        };
        
        // Initialize permanent pulse timer for continuous updates
        pulse = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Recalculate geometry on every frame
                double width = getWidth();
                double height = getHeight();
                centerX = width / 2.0;
                centerY = height / 2.0;
                maxRadius = Math.min(width, height) / 2.0 - KNOB_RADIUS - BORDER_WIDTH;
                
                // Always update smoothing and properties for real-time updates
                updateSmoothing();
                
                // Always redraw
                redraw();
            }
        };
        
        // Setup mouse interaction
        setupMouseHandlers();
        
        // Setup resize listeners
        setupResizeListeners();
        
        // Start pulse timer
        pulse.start();
        
        // Initial draw
        redraw();
    }
    
    /**
     * Setup mouse event handlers for joystick interaction
     */
    private void setupMouseHandlers() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
    }
    
    /**
     * Setup resize listeners for dynamic geometry updates
     */
    private void setupResizeListeners() {
        widthProperty().addListener((obs, oldVal, newVal) -> {
            // Geometry will be recalculated in pulse timer
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            // Geometry will be recalculated in pulse timer
        });
    }
    
    /**
     * Handle mouse press - start dragging if within joystick area
     */
    private void handleMousePressed(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        
        // Check if click is within joystick area
        double distance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
        if (distance <= maxRadius + KNOB_RADIUS) {
            isDragging = true;
            returnTimer.stop();
            
            // Update positions immediately
            updateJoystickPosition(mouseX, mouseY);
            currentX = targetX;
            currentY = targetY;
        }
        
        event.consume();
    }
    
    /**
     * Handle mouse drag - update joystick position
     */
    private void handleMouseDragged(MouseEvent event) {
        if (isDragging) {
            // Update positions directly per mouse movement
            updateJoystickPosition(event.getX(), event.getY());
            currentX = targetX;
            currentY = targetY;
        }
        
        event.consume();
    }
    
    /**
     * Handle mouse release - start smooth return to center
     */
    private void handleMouseReleased(MouseEvent event) {
        if (isDragging) {
            isDragging = false;
            targetX = 0.0;
            targetY = 0.0;
            returnTimer.start();
        }
        
        event.consume();
    }
    
    /**
     * Update joystick position based on mouse coordinates
     */
    private void updateJoystickPosition(double mouseX, double mouseY) {
        // Calculate offset from center
        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;
        
        // Calculate distance and constrain to max radius
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (distance > maxRadius) {
            // Constrain to circle boundary
            double scale = maxRadius / distance;
            deltaX *= scale;
            deltaY *= scale;
            distance = maxRadius;
        }
        
        // Update target position
        targetX = deltaX;
        targetY = deltaY;
    }
    
    /**
     * Update smoothing interpolation
     */
    private void updateSmoothing() {
        // When dragging, don't interpolate - use target values directly
        if (isDragging) {
            currentX = targetX;
            currentY = targetY;
        } else {
            // Linear interpolation towards target when not dragging
            currentX = lerp(currentX, targetX, SMOOTHING_ALPHA);
            currentY = lerp(currentY, targetY, SMOOTHING_ALPHA);
        }
        
        // Always update readonly properties
        updateProperties();
    }
    
    /**
     * Update readonly properties based on current position
     */
    private void updateProperties() {
        // Normalize to [-1, 1] range
        normX = (float) (currentX / maxRadius);
        normY = (float) (currentY / maxRadius);
        
        // Calculate radius [0, 1]
        radius = (float) Math.sqrt(normX * normX + normY * normY);
        
        // Calculate angle [0, 2π)
        angle = (float) Math.atan2(normY, normX);
        if (angle < 0) {
            angle += 2.0 * Math.PI;
        }
    }
    
    /**
     * Linear interpolation helper
     */
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
    
    /**
     * Redraw the joystick
     */
    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        // Clear canvas
        gc.clearRect(0, 0, width, height);
        
        // Enable antialiasing
        gc.setLineCap(StrokeLineCap.ROUND);
        
        // Draw background circle
        gc.setFill(COLOR_BACKGROUND);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(BORDER_WIDTH);
        gc.fillOval(BORDER_WIDTH, BORDER_WIDTH, width - 2 * BORDER_WIDTH, height - 2 * BORDER_WIDTH);
        gc.strokeOval(BORDER_WIDTH, BORDER_WIDTH, width - 2 * BORDER_WIDTH, height - 2 * BORDER_WIDTH);
        
        // Draw crosshairs
        gc.setStroke(COLOR_CROSSHAIR);
        gc.setLineWidth(1.0);
        
        // Horizontal crosshair
        gc.strokeLine(centerX - CROSSHAIR_LENGTH, centerY, centerX + CROSSHAIR_LENGTH, centerY);
        // Vertical crosshair
        gc.strokeLine(centerX, centerY - CROSSHAIR_LENGTH, centerX, centerY + CROSSHAIR_LENGTH);
        
        // Draw knob
        double knobX = centerX + currentX;
        double knobY = centerY + currentY;
        
        // Draw shadow
        gc.setFill(COLOR_SHADOW);
        gc.fillOval(knobX - KNOB_RADIUS + 1, knobY - KNOB_RADIUS + 1, 
                   KNOB_RADIUS * 2, KNOB_RADIUS * 2);
        
        // Draw knob
        gc.setFill(COLOR_KNOB);
        gc.setStroke(COLOR_KNOB_BORDER);
        gc.setLineWidth(1.5);
        gc.fillOval(knobX - KNOB_RADIUS, knobY - KNOB_RADIUS, 
                   KNOB_RADIUS * 2, KNOB_RADIUS * 2);
        gc.strokeOval(knobX - KNOB_RADIUS, knobY - KNOB_RADIUS, 
                     KNOB_RADIUS * 2, KNOB_RADIUS * 2);
    }
    
    // ===== READONLY PROPERTY ACCESSORS =====
    
    /**
     * Get normalized X position [-1, 1]
     */
    public float getNormX() {
        return normX;
    }
    
    /**
     * Get normalized Y position [-1, 1]
     */
    public float getNormY() {
        return normY;
    }
    
    /**
     * Get radius from center [0, 1]
     */
    public float getRadius() {
        return radius;
    }
    
    /**
     * Get angle in radians [0, 2π)
     */
    public float getAngle() {
        return angle;
    }
}
