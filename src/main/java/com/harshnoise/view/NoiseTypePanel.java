package com.harshnoise.view;

import com.harshnoise.model.AudioModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import com.harshnoise.controller.AudioController;

/**
 * NoiseTypePanel - UI component for noise type selection
 * Part of MVC architecture for HarshNoiseGo
 */
public class NoiseTypePanel extends VBox {
    
    private final AudioModel model;
    private final AudioController controller;
    
    // Noise type checkboxes
    private CheckBox whiteNoiseCheckBox;
    private CheckBox pinkNoiseCheckBox;
    private CheckBox granularNoiseCheckBox;
    private CheckBox brownNoiseCheckBox;
    private CheckBox blueNoiseCheckBox;
    private CheckBox violetNoiseCheckBox;
    private CheckBox impulseNoiseCheckBox;
    private CheckBox modulatedNoiseCheckBox;
    
    public NoiseTypePanel(AudioModel model, AudioController controller) {
        this.model = model;
        this.controller = controller;
        initializeUI();
        setupEventHandlers();
    }
    
    /**
     * Initialize the user interface
     */
    private void initializeUI() {
        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        setPrefWidth(220);
        setPrefHeight(450);
        setMinWidth(220);
        setMinHeight(450);
        setMaxWidth(220);
        setMaxHeight(450);
        
        // Title
        Label title = new Label("Noise Types");
        title.setStyle("-fx-text-fill: #495057; -fx-font-weight: bold; -fx-font-size: 16px;");
        title.setAlignment(Pos.CENTER);
        
        // Create styled noise type buttons
        whiteNoiseCheckBox = createNoiseTypeButton("White Noise", "", "#6c757d");
        pinkNoiseCheckBox = createNoiseTypeButton("Pink Noise", "", "#e83e8c");
        granularNoiseCheckBox = createNoiseTypeButton("Granular Noise", "", "#fd7e14");
        brownNoiseCheckBox = createNoiseTypeButton("Brown Noise", "", "#8B4513");
        blueNoiseCheckBox = createNoiseTypeButton("Blue Noise", "", "#0066CC");
        violetNoiseCheckBox = createNoiseTypeButton("Violet Noise", "", "#8A2BE2");
        impulseNoiseCheckBox = createNoiseTypeButton("Impulse Noise", "", "#FF4500");
        modulatedNoiseCheckBox = createNoiseTypeButton("Modulated Noise", "", "#32CD32");
        
        // Set default selection - no noise types selected initially (silent startup)
        whiteNoiseCheckBox.setSelected(false);
        pinkNoiseCheckBox.setSelected(false);
        granularNoiseCheckBox.setSelected(false);
        brownNoiseCheckBox.setSelected(false);
        blueNoiseCheckBox.setSelected(false);
        violetNoiseCheckBox.setSelected(false);
        impulseNoiseCheckBox.setSelected(false);
        modulatedNoiseCheckBox.setSelected(false);
        
        getChildren().addAll(title, whiteNoiseCheckBox, pinkNoiseCheckBox, granularNoiseCheckBox,
                           brownNoiseCheckBox, blueNoiseCheckBox, violetNoiseCheckBox,
                           impulseNoiseCheckBox, modulatedNoiseCheckBox);
    }
    
    /**
     * Create a styled noise type button with icon and color
     */
    private CheckBox createNoiseTypeButton(String text, String icon, String color) {
        CheckBox checkbox = new CheckBox(icon.isEmpty() ? text : icon + " " + text);
        checkbox.setStyle(
            "-fx-text-fill: " + color + ";" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 16;" +
            "-fx-background-color: #f8f9fa;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #dee2e6;" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 2;" +
            "-fx-cursor: hand;"
        );
        
        // Set fixed size - optimized for 450px panel height
        checkbox.setPrefWidth(180);
        checkbox.setPrefHeight(38);
        checkbox.setMinWidth(180);
        checkbox.setMinHeight(38);
        checkbox.setMaxWidth(180);
        checkbox.setMaxHeight(38);
        checkbox.setAlignment(Pos.CENTER_LEFT);
        
        // Hover effect
        checkbox.setOnMouseEntered(e -> {
            checkbox.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 16;" +
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: " + color + ";" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 2;" +
                "-fx-cursor: hand;"
            );
        });
        
        checkbox.setOnMouseExited(e -> {
            if (!checkbox.isSelected()) {
                checkbox.setStyle(
                    "-fx-text-fill: " + color + ";" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 8 16;" +
                    "-fx-background-color: #f8f9fa;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #dee2e6;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 2;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        // Selected state effect
        checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                checkbox.setStyle(
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 12 16;" +
                    "-fx-background-color: " + color + ";" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: " + color + ";" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 2;" +
                    "-fx-cursor: hand;"
                );
            } else {
                checkbox.setStyle(
                    "-fx-text-fill: " + color + ";" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 12 16;" +
                    "-fx-background-color: #f8f9fa;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #dee2e6;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-width: 2;" +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        return checkbox;
    }
    
    /**
     * Setup event handlers for all controls
     */
    private void setupEventHandlers() {
        // Noise type checkboxes
        whiteNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        pinkNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        granularNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        brownNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        blueNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        violetNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        impulseNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        modulatedNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
    }
    
    /**
     * Update noise levels based on checkbox states
     */
    private void updateNoiseLevels() {
        // Set noise levels based on checkbox states
        model.setWhiteNoiseLevel(whiteNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setPinkNoiseLevel(pinkNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setGranularNoiseLevel(granularNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setBrownNoiseLevel(brownNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setBlueNoiseLevel(blueNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setVioletNoiseLevel(violetNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setImpulseNoiseLevel(impulseNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        model.setModulatedNoiseLevel(modulatedNoiseCheckBox.isSelected() ? 1.0 : 0.0);
        
        // Notify controller to update audio engine
        controller.updateAudioEngine();
    }
    
    /**
     * Reset all checkboxes to unchecked state
     */
    public void resetCheckboxes() {
        whiteNoiseCheckBox.setSelected(false);
        pinkNoiseCheckBox.setSelected(false);
        granularNoiseCheckBox.setSelected(false);
        brownNoiseCheckBox.setSelected(false);
        blueNoiseCheckBox.setSelected(false);
        violetNoiseCheckBox.setSelected(false);
        impulseNoiseCheckBox.setSelected(false);
        modulatedNoiseCheckBox.setSelected(false);
    }
    
    /**
     * Set checkbox states based on model values
     */
    public void updateFromModel() {
        whiteNoiseCheckBox.setSelected(model.getWhiteNoiseLevel() > 0);
        pinkNoiseCheckBox.setSelected(model.getPinkNoiseLevel() > 0);
        granularNoiseCheckBox.setSelected(model.getGranularNoiseLevel() > 0);
        brownNoiseCheckBox.setSelected(model.getBrownNoiseLevel() > 0);
        blueNoiseCheckBox.setSelected(model.getBlueNoiseLevel() > 0);
        violetNoiseCheckBox.setSelected(model.getVioletNoiseLevel() > 0);
        impulseNoiseCheckBox.setSelected(model.getImpulseNoiseLevel() > 0);
        modulatedNoiseCheckBox.setSelected(model.getModulatedNoiseLevel() > 0);
    }
}
