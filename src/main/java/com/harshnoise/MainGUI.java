package com.harshnoise;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * MainGUI class - Main user interface for HarshNoiseGo!
 * Contains all UI elements and event handling
 */
public class MainGUI extends VBox {
    
    // Core components
    private final AudioEngine audioEngine;
    private final Recorder recorder;
    
    // UI Controls
    private CheckBox whiteNoiseCheckBox;
    private CheckBox pinkNoiseCheckBox;
    private CheckBox granularNoiseCheckBox;
    private CheckBox brownNoiseCheckBox;      // New: Brown noise
    private CheckBox blueNoiseCheckBox;       // New: Blue noise
    private CheckBox violetNoiseCheckBox;     // New: Violet noise
    private CheckBox impulseNoiseCheckBox;    // New: Impulse noise
    private CheckBox modulatedNoiseCheckBox;  // New: Modulated noise
    private Slider volumeSlider;
    private Slider distortionSlider;
    private Slider filterSlider;
    private Slider grainSizeSlider;
    private Button randomizeButton;
    private Button chaosModeButton;
    private Button recordButton;              // New: Record button
    private Canvas waveformCanvas;
    private Label chaosIndicator;             // New: Chaos mode indicator
    
    // Chaos mode and visualization
    private Timer chaosTimer;
    private boolean chaosModeActive = false;
    private Timer visualizationTimer;
    private final Random random = new Random();
    
    // Recording state
    private boolean isRecording = false;
    private byte[] recordedAudioData = null;
    
    public MainGUI() {
        this.audioEngine = new AudioEngine();
        this.recorder = new Recorder();
        
        initializeUI();
        setupEventHandlers();
        setupVisualization();
        
        // Start audio engine immediately
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000); // Wait for UI to be ready
                initializeAudio();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Initialize the user interface
     */
    private void initializeUI() {
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #333;");
        
        // Simple clean title
        Label titleLabel = new Label("HarshNoiseGo!");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        titleLabel.setAlignment(Pos.CENTER);
        
        // Main content area in a scroll pane for better layout
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        
        // Top row with three panels - fixed height for perfect alignment
        HBox topRow = new HBox(20);
        topRow.setAlignment(Pos.TOP_CENTER);
        topRow.setPrefHeight(450); // Fixed height to match all panels
        
        // Left panel - Noise type controls
        VBox leftPanel = createNoiseTypePanel();
        
        // Center panel - Waveform visualization
        VBox centerPanel = createWaveformPanel();
        
        // Right panel - Parameter controls
        VBox rightPanel = createParameterPanel();
        
        topRow.getChildren().addAll(leftPanel, centerPanel, rightPanel);
        
        // Bottom panel - Control buttons (without recording)
        VBox bottomPanel = createBottomPanel();
        bottomPanel.setPrefHeight(120);
        
        // Add all components (status label removed)
        mainContainer.getChildren().addAll(topRow, bottomPanel);
        
        getChildren().addAll(titleLabel, mainContainer);
    }
    
    /**
     * Create noise type selection panel
     */
    private VBox createNoiseTypePanel() {
        VBox panel = new VBox(10);  // Reduced spacing to fit all 8 buttons properly
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(18));  // Adjusted padding for better fit
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        panel.setPrefWidth(220);  // Keep original width to match other panels
        panel.setPrefHeight(450);  // Increased height for better spacing
        panel.setMinWidth(220);
        panel.setMinHeight(450);   // Consistent minimum height
        panel.setMaxWidth(220);
        panel.setMaxHeight(450);   // Consistent maximum height
        
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
        
        // Add event listeners
        whiteNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        pinkNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        granularNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        brownNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        blueNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        violetNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        impulseNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        modulatedNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        
        panel.getChildren().addAll(title, whiteNoiseCheckBox, pinkNoiseCheckBox, granularNoiseCheckBox,
                                 brownNoiseCheckBox, blueNoiseCheckBox, violetNoiseCheckBox,
                                 impulseNoiseCheckBox, modulatedNoiseCheckBox);
        return panel;
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
            "-fx-padding: 8 16;" +  // Adjusted padding for better fit
            "-fx-background-color: #f8f9fa;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #dee2e6;" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 2;" +
            "-fx-cursor: hand;"
        );
        
        // Set fixed size - optimized for 450px panel height
        checkbox.setPrefWidth(180);  // Fit within 220px panel width
        checkbox.setPrefHeight(38);  // Adjusted height for better fit in panel
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
                "-fx-padding: 8 16;" +  // Adjusted padding for better fit
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
                    "-fx-padding: 8 16;" +  // Adjusted padding for better fit
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
     * Create a simple styled checkbox
     */
    private CheckBox createSimpleCheckBox(String text) {
        CheckBox checkbox = new CheckBox(text);
        checkbox.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px;");
        return checkbox;
    }
    
    /**
     * Create a styled checkbox with icon
     */
    private CheckBox createStyledCheckBox(String text, String icon) {
        CheckBox checkbox = new CheckBox(icon + " " + text);
        checkbox.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 8; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8; -fx-border-width: 1;");
        checkbox.setMaxWidth(Double.MAX_VALUE);
        checkbox.setAlignment(Pos.CENTER_LEFT);
        
        // Hover effect
        checkbox.setOnMouseEntered(e -> {
            checkbox.setStyle("-fx-text-fill: #ffd93d; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: rgba(255, 215, 61, 0.2); -fx-background-radius: 8; -fx-border-color: rgba(255, 215, 61, 0.5); -fx-border-radius: 8; -fx-border-width: 2;");
        });
        
        checkbox.setOnMouseExited(e -> {
            checkbox.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 8; -fx-border-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 8; -fx-border-width: 1;");
        });
        
        return checkbox;
    }
    
    /**
     * Create waveform visualization panel with recording button in top-right corner
     */
    private VBox createWaveformPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        panel.setPrefWidth(380);  // Keep original width for center panel
        panel.setPrefHeight(450);  // Increased height for better spacing
        panel.setMinHeight(450);   // Consistent minimum height
        panel.setMaxHeight(450);   // Consistent maximum height
        
        // Title bar with recording buttons in top-right corner
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setSpacing(0);
        titleBar.setPadding(new Insets(0, 0, 10, 0));
        
        Label title = new Label("Live Waveform");
        title.setStyle("-fx-text-fill: #495057; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Record button in top right corner
        recordButton = createRecordButton();
        
        titleBar.getChildren().addAll(title, spacer, recordButton);
        
        // Chaos mode indicator below record button
        Label chaosIndicator = new Label("CHAOS MODE");
        chaosIndicator.setStyle("-fx-text-fill: #ff6b35; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #2d1b3d; -fx-background-radius: 12; -fx-border-color: #ff6b35; -fx-border-width: 1; -fx-border-radius: 12;");
        chaosIndicator.setAlignment(Pos.CENTER_RIGHT);
        chaosIndicator.setVisible(false);  // Initially hidden
        
        // Create a container to right-align the chaos indicator
        HBox chaosContainer = new HBox();
        chaosContainer.setAlignment(Pos.CENTER_RIGHT);
        chaosContainer.setPadding(new Insets(0, 0, 0, 0));
        chaosContainer.getChildren().add(chaosIndicator);
        
        // Store reference to chaos indicator for later updates
        this.chaosIndicator = chaosIndicator;
        
        // Canvas with clean styling - positioned lower in the panel
        waveformCanvas = new Canvas(340, 200);  // Restored width for 380px panel
        waveformCanvas.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-border-width: 1;");
        
        // Add some spacing above the waveform canvas to move it down
        Region topSpacer = new Region();
        topSpacer.setPrefHeight(20);  // Add 20px spacing above waveform
        
        panel.getChildren().addAll(titleBar, chaosContainer, topSpacer, waveformCanvas);
        return panel;
    }
    
    /**
     * Create record button with recording functionality
     */
    private Button createRecordButton() {
        Button button = new Button("⏺️ Record");
        button.setStyle(
            "-fx-background-color: #dc3545;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 1, 1);"
        );
        
        // Set fixed size
        button.setPrefWidth(80);
        button.setPrefHeight(30);
        button.setMinWidth(80);
        button.setMinHeight(30);
        button.setMaxWidth(80);
        button.setMaxHeight(30);
        
        // Add recording functionality
        button.setOnAction(e -> toggleRecording());
        
        return button;
    }
    
    /**
     * Create parameter control panel
     */
    private VBox createParameterPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        panel.setPrefWidth(220);
        panel.setPrefHeight(450);  // Increased height for better spacing
        panel.setMinHeight(450);   // Consistent minimum height
        panel.setMaxHeight(450);   // Consistent maximum height
        
        // Simple title
        Label title = new Label("Parameters");
        title.setStyle("-fx-text-fill: #495057; -fx-font-weight: bold; -fx-font-size: 16px;");
        title.setAlignment(Pos.CENTER);
        
        // Volume control
        VBox volumeBox = createSliderControl("Volume", 0.0, 1.0, 0.7);
        volumeSlider = (Slider) volumeBox.getChildren().get(1);
        
        // Distortion control
        VBox distortionBox = createSliderControl("Distortion", 0.0, 1.0, 0.0);
        distortionSlider = (Slider) distortionBox.getChildren().get(1);
        
        // Filter control
        VBox filterBox = createSliderControl("Filter", 0.0, 1.0, 0.0);
        filterSlider = (Slider) filterBox.getChildren().get(1);
        
        // Grain size control
        VBox grainBox = createSliderControl("Grain Size", 1, 128, 64);
        grainSizeSlider = (Slider) grainBox.getChildren().get(1);
        
        panel.getChildren().addAll(title, volumeBox, distortionBox, filterBox, grainBox);
        return panel;
    }
    
    /**
     * Create slider control with label
     */
    private VBox createSliderControl(String label, double min, double max, double defaultValue) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        
        Label labelControl = new Label(label);
        labelControl.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-font-weight: bold;");
        labelControl.setAlignment(Pos.CENTER);
        
        Slider slider = new Slider(min, max, defaultValue);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setPrefWidth(180);
        slider.setPrefHeight(20);
        
        // Enhanced slider styling
        slider.setStyle(
            "-fx-control-inner-background: #e9ecef;" +
            "-fx-control-inner-background-alt: #dee2e6;" +
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;"
        );
        
        // Value display with better styling
        Label valueLabel = new Label(String.format("%.2f", defaultValue));
        valueLabel.setStyle(
            "-fx-text-fill: #6c757d;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-color: #f8f9fa;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 2 6;"
        );
        valueLabel.setAlignment(Pos.CENTER);
        
        // Update value label when slider changes
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });
        
        box.getChildren().addAll(labelControl, slider, valueLabel);
        return box;
    }
    
    /**
     * Create bottom control panel
     */
    private VBox createBottomPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        
        // Simple title
        Label title = new Label("Control Panel");
        title.setStyle("-fx-text-fill: #495057; -fx-font-weight: bold; -fx-font-size: 16px;");
        title.setAlignment(Pos.CENTER);
        
        // Button rows
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER);
        
        // Fun controls
        randomizeButton = createSimpleButton("Randomize", "#ffc107");
        chaosModeButton = createSimpleButton("Chaos Mode", "#6f42c1");
        
        // Audio control
        Button restartAudioButton = createSimpleButton("Restart Audio", "#fd7e14");
        restartAudioButton.setOnAction(e -> restartAudio());
        
        // Organize buttons in one row
        row1.getChildren().addAll(randomizeButton, chaosModeButton, restartAudioButton);
        
        panel.getChildren().addAll(title, row1);
        return panel;
    }
    
    /**
     * Create a simple styled button with color
     */
    private Button createSimpleButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 20;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 3, 0, 0, 1);"
        );
        
        // Set consistent size - larger to show full text
        button.setPrefWidth(140);
        button.setPrefHeight(45);
        button.setMaxWidth(140);
        button.setMaxHeight(45);
        
        // Enhanced hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: derive(" + color + ", 15%);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 5, 0, 0, 2);"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 20;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 3, 0, 0, 1);"
            );
        });
        
        return button;
    }
    
    /**
     * Setup event handlers for all controls
     */
    private void setupEventHandlers() {
        // Noise type checkboxes
        whiteNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        pinkNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        granularNoiseCheckBox.setOnAction(e -> updateNoiseLevels());
        
        // Parameter sliders
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioEngine.setVolume(newVal.doubleValue());
        });
        
        distortionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioEngine.setDistortionLevel(newVal.doubleValue());
        });
        
        filterSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioEngine.setFilterLevel(newVal.doubleValue());
        });
        
        grainSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioEngine.setGrainSize(newVal.intValue());
        });
        
        // Fun buttons
        randomizeButton.setOnAction(e -> randomizeParameters());
        chaosModeButton.setOnAction(e -> toggleChaosMode());
    }
    
    /**
     * Update noise levels based on checkbox states
     */
    private void updateNoiseLevels() {
        // Set noise levels based on checkbox states
        double whiteLevel = whiteNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double pinkLevel = pinkNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double granularLevel = granularNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double brownLevel = brownNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double blueLevel = blueNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double violetLevel = violetNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double impulseLevel = impulseNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        double modulatedLevel = modulatedNoiseCheckBox.isSelected() ? 1.0 : 0.0;
        
        // Allow complete silence - no forced noise type selection
        audioEngine.setWhiteNoiseLevel(whiteLevel);
        audioEngine.setPinkNoiseLevel(pinkLevel);
        audioEngine.setGranularNoiseLevel(granularLevel);
        audioEngine.setBrownNoiseLevel(brownLevel);
        audioEngine.setBlueNoiseLevel(blueLevel);
        audioEngine.setVioletNoiseLevel(violetLevel);
        audioEngine.setImpulseNoiseLevel(impulseLevel);
        audioEngine.setModulatedNoiseLevel(modulatedLevel);
        
        // Set volume
        double currentVolume = volumeSlider.getValue();
        audioEngine.setVolume(currentVolume);
        
        // Log current audio state for debugging
        String noiseTypes = "";
        if (whiteNoiseCheckBox.isSelected()) noiseTypes += "White ";
        if (pinkNoiseCheckBox.isSelected()) noiseTypes += "Pink ";
        if (granularNoiseCheckBox.isSelected()) noiseTypes += "Granular ";
        if (brownNoiseCheckBox.isSelected()) noiseTypes += "Brown ";
        if (blueNoiseCheckBox.isSelected()) noiseTypes += "Blue ";
        if (violetNoiseCheckBox.isSelected()) noiseTypes += "Violet ";
        if (impulseNoiseCheckBox.isSelected()) noiseTypes += "Impulse ";
        if (modulatedNoiseCheckBox.isSelected()) noiseTypes += "Modulated ";
        if (noiseTypes.isEmpty()) noiseTypes = "None (Silent)";
        
        System.out.println("DEBUG: Setting noise levels - White: " + whiteLevel + 
                          ", Pink: " + pinkLevel + ", Granular: " + granularLevel + 
                          ", Brown: " + brownLevel + ", Blue: " + blueLevel + 
                          ", Violet: " + violetLevel + ", Impulse: " + impulseLevel + 
                          ", Modulated: " + modulatedLevel + ", Volume: " + currentVolume);
    }
    
    // Recording functionality temporarily removed to focus on basic audio playback
    
    /**
     * Randomize all parameters
     */
    private void randomizeParameters() {
        // Ensure volume is never 0
        volumeSlider.setValue(Math.max(0.1, random.nextDouble()));
        distortionSlider.setValue(random.nextDouble());
        filterSlider.setValue(random.nextDouble());
        grainSizeSlider.setValue(random.nextInt(128) + 1);
        
        // Ensure at least one noise type is selected
        boolean hasNoiseType = false;
        whiteNoiseCheckBox.setSelected(random.nextBoolean());
        if (whiteNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        pinkNoiseCheckBox.setSelected(random.nextBoolean());
        if (pinkNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        granularNoiseCheckBox.setSelected(random.nextBoolean());
        if (granularNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        brownNoiseCheckBox.setSelected(random.nextBoolean());
        if (brownNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        blueNoiseCheckBox.setSelected(random.nextBoolean());
        if (blueNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        violetNoiseCheckBox.setSelected(random.nextBoolean());
        if (violetNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        impulseNoiseCheckBox.setSelected(random.nextBoolean());
        if (impulseNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        modulatedNoiseCheckBox.setSelected(random.nextBoolean());
        if (modulatedNoiseCheckBox.isSelected()) hasNoiseType = true;
        
        // If no noise type selected, force white noise
        if (!hasNoiseType) {
            whiteNoiseCheckBox.setSelected(true);
        }
        
        updateNoiseLevels();
    }
    
    /**
     * Toggle chaos mode
     */
    private void toggleChaosMode() {
        if (chaosModeActive) {
            stopChaosMode();
        } else {
            startChaosMode();
        }
    }
    
    /**
     * Start chaos mode
     */
    private void startChaosMode() {
        chaosModeActive = true;
        chaosModeButton.setText("Stop Chaos");
        chaosModeButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 5;");
        
        // Show chaos indicator
        if (chaosIndicator != null) {
            chaosIndicator.setVisible(true);
        }
        
        chaosTimer = new Timer();
        chaosTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (chaosModeActive) {
                        randomizeParameters();
                    }
                });
            }
        }, 0, random.nextInt(2000) + 500); // Random interval between 500ms and 2.5s
        

    }
    
    /**
     * Stop chaos mode
     */
    private void stopChaosMode() {
        chaosModeActive = false;
        chaosModeButton.setText("Chaos Mode");
        chaosModeButton.setStyle("-fx-background-color: #4ecdc4; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 5;");
        
        // Hide chaos indicator
        if (chaosIndicator != null) {
            chaosIndicator.setVisible(false);
        }
        
        if (chaosTimer != null) {
            chaosTimer.cancel();
            chaosTimer = null;
        }
        

    }
    
    /**
     * Setup real-time visualization
     */
    private void setupVisualization() {
        visualizationTimer = new Timer();
        visualizationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateWaveform());
            }
        }, 0, 50); // Update every 50ms
    }
    
    /**
     * Update waveform visualization
     */
    private void updateWaveform() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        double width = waveformCanvas.getWidth();
        double height = waveformCanvas.getHeight();
        
        // Clear canvas with light background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Draw simple center line
        gc.setStroke(Color.rgb(220, 220, 220));
        gc.setLineWidth(1.0);
        gc.strokeLine(0, height / 2, width, height / 2);
        
        // Draw simple waveform
        gc.setStroke(Color.rgb(40, 167, 69));
        gc.setLineWidth(2.0);
        gc.beginPath();
        
        for (int x = 0; x < width; x++) {
            // Generate sample value based on current audio parameters
            double sample = (random.nextDouble() * 2.0 - 1.0) * 
                          (whiteNoiseCheckBox.isSelected() ? 1.0 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (pinkNoiseCheckBox.isSelected() ? 0.7 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (granularNoiseCheckBox.isSelected() ? 0.5 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (brownNoiseCheckBox.isSelected() ? 0.8 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (blueNoiseCheckBox.isSelected() ? 0.9 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (violetNoiseCheckBox.isSelected() ? 1.0 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (impulseNoiseCheckBox.isSelected() ? 0.6 : 0.0) +
                          (random.nextDouble() * 2.0 - 1.0) * 
                          (modulatedNoiseCheckBox.isSelected() ? 0.7 : 0.0);
            
            // Apply effects
            sample *= volumeSlider.getValue();
            sample = Math.tanh(sample * (1.0 + distortionSlider.getValue()));
            
            // Convert to y coordinate
            double y = height / 2 + (sample * height / 2);
            
            if (x == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        
        gc.stroke();
        
        // Chaos mode indicator removed from canvas - now displayed below record button
    }
    

    
    /**
     * Initialize audio engine when GUI is shown
     */
    public void initializeAudio() {
        System.out.println("DEBUG: Initializing audio engine with silent startup...");
        
        // Start audio engine first
        audioEngine.start();
        
        // Wait a moment for audio engine to be ready
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Update noise levels (all should be 0.0 initially for silent startup)
        updateNoiseLevels();
        
        // Audio engine started successfully
        
        System.out.println("DEBUG: Audio initialization completed - Silent startup, waiting for user selection");
    }
    
    /**
     * Toggle recording state
     */
    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }
    
    /**
     * Start recording audio
     */
    private void startRecording() {
        isRecording = true;
        recordButton.setText("⏹️ Stop");
        recordButton.setStyle(
            "-fx-background-color: #6c757d;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 1, 1);" +
            "-fx-border-color: #495057;" +
            "-fx-border-width: 2;"
        );
        
        System.out.println("🎙️ Recording started...");
        
        // Start recording in AudioEngine
        audioEngine.startRecording();
    }
    
    /**
     * Stop recording and export audio
     */
    private void stopRecording() {
        isRecording = false;
        recordButton.setText("⏺️ Record");
        recordButton.setStyle(
            "-fx-background-color: #dc3545;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 6 12;" +
            "-fx-background-radius: 6;" +
            "-fx-border-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 1, 1);" +
            "-fx-border-color: transparent;" +
            "-fx-border-width: 0;"
        );
        
        System.out.println("⏹️ Recording stopped");
        
        // Stop recording in AudioEngine and get the recorded data
        audioEngine.stopRecording();
        byte[] audioData = audioEngine.getRecordedAudioData();
        
        if (audioData != null && audioData.length > 0) {
            try {
                String filename = recorder.generateUniqueFilename();
                String filepath = recorder.getDefaultExportDirectory() + "/" + filename;
                recorder.exportToWav(audioData, filepath);
                System.out.println("💾 Audio exported to: " + filepath);
                System.out.println("📊 Recorded " + (audioData.length / 2) + " samples (" + (audioData.length / 44100.0) + " seconds)");
                
                // Show completion dialog
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Recording Complete");
                    alert.setHeaderText("Recording Successfully Exported!");
                    alert.setContentText("Your audio has been saved to:\n" + filepath);
                    
                    // Customize dialog appearance with modern styling
                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-text-fill: #333333;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
                    );
                    
                    // Set custom button text and style
                    Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
                    okButton.setText("OK");
                    okButton.setStyle(
                        "-fx-background-color: #3498db;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;"
                    );
                    
                    // Show the dialog
                    alert.showAndWait();
                });
                

            } catch (Exception e) {
                System.err.println("❌ Error exporting audio: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ No audio data recorded");
        }
    }
    
    /**
     * Restart audio engine and reset to silent state
     */
    private void restartAudio() {
        System.out.println("Restarting audio engine...");
        
        // Stop current audio
        audioEngine.stop();
        
        // Reset all noise type selections to unchecked (silent startup state)
        whiteNoiseCheckBox.setSelected(false);
        pinkNoiseCheckBox.setSelected(false);
        granularNoiseCheckBox.setSelected(false);
        brownNoiseCheckBox.setSelected(false);
        blueNoiseCheckBox.setSelected(false);
        violetNoiseCheckBox.setSelected(false);
        impulseNoiseCheckBox.setSelected(false);
        modulatedNoiseCheckBox.setSelected(false);
        
        // Wait a bit
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Restart audio
        audioEngine.start();
        updateNoiseLevels();
        
        System.out.println("Audio engine restarted - Select a noise type to begin playing!");
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (chaosTimer != null) {
            chaosTimer.cancel();
        }
        if (visualizationTimer != null) {
            visualizationTimer.cancel();
        }
        audioEngine.stop();
    }
} 