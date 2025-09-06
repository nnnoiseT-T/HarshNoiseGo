package com.harshnoise.view;

import com.harshnoise.controller.AudioController;
import com.harshnoise.model.AudioModel;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * JoystickCard - Square card container for joystick control
 * Implements Preset #1 "Live Playable" mapping for real-time audio parameter control
 */
public class JoystickCard extends VBox {
    
    // ===== STYLE CONSTANTS =====
    private static final String STYLE_MAIN_CONTAINER = 
        "-fx-background-color: white;" +
        "-fx-background-radius: 10;" +
        "-fx-border-color: #e0e0e0;" +
        "-fx-border-radius: 10;" +
        "-fx-border-width: 1;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);";
    
    private static final String STYLE_TITLE = "-fx-text-fill: #2c3e50;";
    
    // ===== TEXT CONSTANTS =====
    private static final String TITLE_TEXT = "Joystick";
    
    // ===== LAYOUT CONSTANTS =====
    private static final int SPACING_MAIN = 8;
    private static final int PADDING_MAIN = 12;
    private static final int JOYSTICK_SIZE = 160;
    
    // ===== PRESET #1 MAPPING CONSTANTS =====
    private static final double DISTORTION_POWER = 1.6;  // X-axis power curve
    private static final double FILTER_POWER = 1.4;      // Y-axis power curve (reversed)
    private static final int GRAIN_SIZE_MIN = 8;          // Minimum grain size
    private static final int GRAIN_SIZE_MAX = 128;       // Maximum grain size
    
    private final AudioModel model;
    @SuppressWarnings("unused")
    private final AudioController controller;
    private final JoystickControl joystickControl;
    private final Label titleLabel;
    
    // ===== ANIMATION TIMER =====
    private AnimationTimer emitter;
    
    public JoystickCard(AudioModel model, AudioController controller) {
        this.model = model;
        this.controller = controller;
        
        // Setup layout
        setSpacing(SPACING_MAIN);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(PADDING_MAIN));
        setStyle(STYLE_MAIN_CONTAINER);
        
        // Create title
        titleLabel = createTitleLabel();
        
        // Create joystick control
        joystickControl = new JoystickControl(JOYSTICK_SIZE);
        
        // Setup joystick value change listener
        setupJoystickListener();
        
        // Add components
        getChildren().addAll(titleLabel, joystickControl);
        
        // Set dimensions to square shape
        setPrefSize(240, 240);  // Square joystick card
        setMinSize(240, 240);
        setMaxSize(240, 240);
    }
    
    /**
     * Create title label
     */
    private Label createTitleLabel() {
        Label label = new Label(TITLE_TEXT);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setStyle(STYLE_TITLE);
        label.setAlignment(Pos.CENTER);
        return label;
    }
    
    /**
     * Setup joystick value change listener with smooth updates
     */
    private void setupJoystickListener() {
        // Single AnimationTimer for smooth parameter updates (~50 FPS)
        emitter = new AnimationTimer() {
            private float lastNormX = Float.MAX_VALUE;
            private float lastNormY = Float.MAX_VALUE;
            private float lastRadius = Float.MAX_VALUE;
            private float lastAngle = Float.MAX_VALUE;
            
            @Override
            public void handle(long now) {
                float currentNormX = joystickControl.getNormX();
                float currentNormY = joystickControl.getNormY();
                float currentRadius = joystickControl.getRadius();
                float currentAngle = joystickControl.getAngle();
                
                // Check if values have changed significantly (epsilon = 0.01)
                if (Math.abs(currentNormX - lastNormX) > 0.01f ||
                    Math.abs(currentNormY - lastNormY) > 0.01f ||
                    Math.abs(currentRadius - lastRadius) > 0.01f ||
                    Math.abs(currentAngle - lastAngle) > 0.01f) {
                    
                    updateAudioParameters(currentNormX, currentNormY, currentRadius, currentAngle);
                    
                    lastNormX = currentNormX;
                    lastNormY = currentNormY;
                    lastRadius = currentRadius;
                    lastAngle = currentAngle;
                }
            }
        };
        
        emitter.start();
    }
    
    /**
     * Update audio parameters based on joystick values using Preset #1 mapping
     */
    private void updateAudioParameters(float normX, float normY, float radius, float angle) {
        // Preset #1 "Live Playable" mapping
        double distortion = calculateDistortion(normX);
        double filter = calculateFilter(normY);
        double tone = Math.max(0.0, Math.min(1.0, radius)); // radius → Tone
        int grainSize = calculateGrainSize(angle);
        
        // Use batch update to prevent multiple notifications
        model.setParamsBatch(() -> {
            model.setDistortionLevel(distortion);
            model.setFilterLevel(filter);
            model.setTone(tone);
            model.setGrainSize(grainSize);
        });
        
        // Update audio engine to apply changes immediately
        controller.updateAudioEngine();
    }
    
    /**
     * Calculate distortion level from X-axis position
     * X → Distortion: dist = ((normX + 1)/2)^1.6
     */
    private double calculateDistortion(float normX) {
        // Normalize to [0, 1] range
        double normalized = (normX + 1.0) / 2.0;
        // Apply power curve for more control in lower range
        return Math.pow(normalized, DISTORTION_POWER);
    }
    
    /**
     * Calculate filter level from Y-axis position (reversed)
     * Y → Filter: reversed axis, f = ((1 - (normY+1)/2))^1.4
     */
    private double calculateFilter(float normY) {
        // Normalize to [0, 1] range and reverse
        double normalized = 1.0 - (normY + 1.0) / 2.0;
        // Apply power curve for more control in lower range
        double filterValue = Math.pow(normalized, FILTER_POWER);
        // Limit filter to 0.0-0.8 range to avoid silence
        return filterValue * 0.8;
    }
    
    /**
     * Calculate grain size from angle
     * angle → GrainSize: map to [8,128], round to nearest int
     */
    private int calculateGrainSize(float angle) {
        // Map angle [0, 2π] to [0, 1]
        double normalized = angle / (2.0 * Math.PI);
        // Map to grain size range
        double grainSize = GRAIN_SIZE_MIN + normalized * (GRAIN_SIZE_MAX - GRAIN_SIZE_MIN);
        return (int) Math.round(grainSize);
    }
}
