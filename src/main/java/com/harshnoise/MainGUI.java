package com.harshnoise;

import com.harshnoise.controller.AudioController;
import com.harshnoise.model.AudioModel;
import com.harshnoise.view.NoiseTypePanel;
import com.harshnoise.view.ParameterPanel;
import com.harshnoise.view.WaveformPanel;
import com.harshnoise.view.EffectsRackBar;
import com.harshnoise.view.JoystickCard;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Refactored MainGUI - Main user interface coordinator for HarshNoiseGo!
 * Follows MVC architecture pattern with improved code organization and maintainability
 */
public class MainGUI extends VBox {
    
    // ===== UI CONSTANTS =====
    private static final int SPACING_MAIN = 20;
    private static final int SPACING_BUTTONS = 20;
    private static final int PADDING_MAIN = 20;
    private static final int TOP_ROW_HEIGHT = 450;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 35;
    private static final int BUTTON_FONT_SIZE = 13;
    private static final int BUTTON_FONT_SIZE_ACTIVE = 14;
    private static final int TITLE_FONT_SIZE = 32;
    private static final int BUTTON_TRANSLATE_X = -80;
    
    // ===== STYLE CONSTANTS =====
    private static final String STYLE_MAIN_BACKGROUND = "-fx-background-color: #f8f9fa; -fx-text-fill: #333;";
    private static final String STYLE_TITLE = "-fx-text-fill: #2c3e50;";
    private static final String STYLE_BUTTON_BASE = 
        "-fx-text-fill: white;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 6;" +
        "-fx-padding: 8 16;" +
        "-fx-cursor: hand;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 0, 1);";
    private static final String STYLE_BUTTON_HOVER_EFFECT = 
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);";
    
    // ===== BUTTON COLORS =====
    private static final String COLOR_RANDOMIZE = "#ffc107";
    private static final String COLOR_CHAOS = "#6f42c1";
    private static final String COLOR_PERLIN = "#17a2b8";
    private static final String COLOR_RESTART = "#fd7e14";
    private static final String COLOR_ACTIVE = "#ff6b6b";
    
    // ===== MVC COMPONENTS =====
    private final AudioModel model;
    private final AudioController controller;
    
    // ===== VIEW COMPONENTS =====
    private NoiseTypePanel noiseTypePanel;
    private WaveformPanel waveformPanel;
    private ParameterPanel parameterPanel;
    private EffectsRackBar effectsRackBar;
    private JoystickCard joystickCard;
    
    // ===== HEADER CONTROL BUTTONS =====
    private Button randomizeButton;
    private Button chaosModeButton;
    private Button perlinNoiseButton;
    private Button restartAudioButton;
    
    public MainGUI() {
        // Initialize MVC components
        this.model = new AudioModel();
        this.controller = new AudioController(model);
        
        initializeUI();
        setupEventHandlers();
        setupModelObservers();
        
        // Start audio engine with clean scheduling
        Platform.runLater(() -> controller.initializeAudio());
    }
    
    /**
     * Initialize the complete user interface
     */
    private void initializeUI() {
        setupMainContainer();
        createHeader();
        createMainContent();
    }
    
    /**
     * Setup main container properties
     */
    private void setupMainContainer() {
        setSpacing(SPACING_MAIN);
        setPadding(new Insets(PADDING_MAIN));
        setStyle(STYLE_MAIN_BACKGROUND);
    }
    
    /**
     * Create header with title and control buttons
     */
    private void createHeader() {
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setSpacing(0);
        
        // Create title
        Label titleLabel = createTitleLabel();
        
        // Create control buttons
        createControlButtons();
        
        // Create button container for right alignment
        HBox buttonContainer = new HBox(SPACING_BUTTONS);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.getChildren().addAll(randomizeButton, chaosModeButton, perlinNoiseButton, restartAudioButton);
        buttonContainer.setTranslateX(BUTTON_TRANSLATE_X);
        
        // Add title and button container to header row
        headerRow.getChildren().addAll(titleLabel, buttonContainer);
        
        // Set HBox growth priority
        HBox.setHgrow(titleLabel, Priority.NEVER);
        HBox.setHgrow(buttonContainer, Priority.ALWAYS);
        
        getChildren().add(headerRow);
    }
    
    /**
     * Create title label
     */
    private Label createTitleLabel() {
        Label titleLabel = new Label("HarshNoiseGo!");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, TITLE_FONT_SIZE));
        titleLabel.setStyle(STYLE_TITLE);
        titleLabel.setAlignment(Pos.CENTER_LEFT);
        return titleLabel;
    }
    
    /**
     * Create all control buttons
     */
    private void createControlButtons() {
        randomizeButton = createControlButton("Randomize", COLOR_RANDOMIZE);
        chaosModeButton = createControlButton("Chaos Mode", COLOR_CHAOS);
        perlinNoiseButton = createControlButton("Perlin Noise", COLOR_PERLIN);
        restartAudioButton = createControlButton("Restart Audio", COLOR_RESTART);
    }
    
    /**
     * Create main content area with panels
     */
    private void createMainContent() {
        VBox mainContainer = new VBox(SPACING_MAIN);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        
        // Create top row with three panels
        HBox topRow = createTopRow();
        
        // Create effects rack bar
        effectsRackBar = new EffectsRackBar(controller.getEffectsController());
        
        // Create joystick card
        joystickCard = new JoystickCard(model, controller);
        
        // Create rack row with effects and joystick
        HBox rackRow = new HBox(12);
        rackRow.setAlignment(Pos.TOP_CENTER);
        rackRow.getChildren().addAll(effectsRackBar, joystickCard);
        
        // Set HBox growth priority - effects rack takes most space
        HBox.setHgrow(effectsRackBar, Priority.ALWAYS);
        
        // Add all components
        mainContainer.getChildren().addAll(topRow, rackRow);
        
        // Set VBox growth priority
        VBox.setVgrow(topRow, Priority.ALWAYS);
        
        getChildren().add(mainContainer);
    }
    
    /**
     * Create top row with three main panels
     */
    private HBox createTopRow() {
        HBox topRow = new HBox(SPACING_MAIN);
        topRow.setAlignment(Pos.TOP_CENTER);
        topRow.setPrefHeight(TOP_ROW_HEIGHT);
        
        // Create panels
        noiseTypePanel = new NoiseTypePanel(model, controller);
        waveformPanel = new WaveformPanel(model, controller.getAudioEngine());
        parameterPanel = new ParameterPanel(model, controller);
        
        // Ensure parameter panel is synchronized with model's initial values
        parameterPanel.updateFromModel();
        
        topRow.getChildren().addAll(noiseTypePanel, waveformPanel, parameterPanel);
        return topRow;
    }
    
    /**
     * Create a styled control button with consistent properties
     */
    private Button createControlButton(String text, String color) {
        Button button = new Button(text);
        applyButtonStyle(button, color, BUTTON_FONT_SIZE);
        setupButtonSize(button);
        setupButtonHoverEffects(button, color);
        return button;
    }
    
    /**
     * Apply base button style
     */
    private void applyButtonStyle(Button button, String color, int fontSize) {
        String style = String.format(
            "-fx-background-color: %s;" +
            "-fx-font-size: %dpx;" +
            "%s",
            color, fontSize, STYLE_BUTTON_BASE
        );
        button.setStyle(style);
    }
    
    /**
     * Setup button size constraints
     */
    private void setupButtonSize(Button button) {
        button.setPrefWidth(BUTTON_WIDTH);
        button.setPrefHeight(BUTTON_HEIGHT);
        button.setMaxWidth(BUTTON_WIDTH);
        button.setMaxHeight(BUTTON_HEIGHT);
    }
    
    /**
     * Setup button hover effects
     */
    private void setupButtonHoverEffects(Button button, String color) {
        button.setOnMouseEntered(e -> {
            String hoverStyle = String.format(
                "-fx-background-color: derive(%s, 15%%);" +
                "-fx-font-size: %dpx;" +
                "%s" +
                "%s",
                color, BUTTON_FONT_SIZE, STYLE_BUTTON_BASE, STYLE_BUTTON_HOVER_EFFECT
            );
            button.setStyle(hoverStyle);
        });
        
        button.setOnMouseExited(e -> {
            applyButtonStyle(button, color, BUTTON_FONT_SIZE);
        });
    }
    
    /**
     * Setup event handlers for all controls
     */
    private void setupEventHandlers() {
        // Control panel buttons
        randomizeButton.setOnAction(e -> controller.randomizeParameters());
        chaosModeButton.setOnAction(e -> controller.toggleChaosMode());
        perlinNoiseButton.setOnAction(e -> controller.togglePerlinNoise());
        restartAudioButton.setOnAction(e -> controller.restartAudio());
        
        // Waveform panel recording button
        waveformPanel.getRecordButton().setOnAction(e -> controller.toggleRecording());
    }
    
    /**
     * Setup model observers to update UI when model changes
     */
    private void setupModelObservers() {
        model.addObserver((obs, arg) -> {
            Platform.runLater(() -> {
                updateUIComponents();
            });
        });
    }
    
    /**
     * Update all UI components when model changes
     */
    private void updateUIComponents() {
        // Update waveform panel
        waveformPanel.updateChaosIndicator();
        waveformPanel.updateRecordingButton();
        
        // Update control buttons
        updateChaosModeButton(model.isChaosModeActive());
        updatePerlinNoiseButton(model.isPerlinNoiseActive());
        
        // Update parameter panel
        parameterPanel.updateFromModel();
        
        // Update noise type panel
        noiseTypePanel.updateFromModel();
    }
    
    /**
     * Update button state with consistent styling
     */
    private void updateButtonState(Button button, boolean isActive, String activeText, String inactiveText, String inactiveColor) {
        button.setText(isActive ? activeText : inactiveText);
        String color = isActive ? COLOR_ACTIVE : inactiveColor;
        int fontSize = isActive ? BUTTON_FONT_SIZE_ACTIVE : BUTTON_FONT_SIZE;
        applyButtonStyle(button, color, fontSize);
    }
    
    /**
     * Update chaos mode button state
     */
    public void updateChaosModeButton(boolean isActive) {
        updateButtonState(chaosModeButton, isActive, "Stop Chaos", "Chaos Mode", COLOR_CHAOS);
    }
    
    /**
     * Update Perlin noise button state
     */
    public void updatePerlinNoiseButton(boolean isActive) {
        updateButtonState(perlinNoiseButton, isActive, "Stop Perlin", "Perlin Noise", COLOR_PERLIN);
    }
    
    /**
     * Initialize audio engine when GUI is shown
     */
    public void initializeAudio() {
        controller.initializeAudio();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        controller.cleanup();
        waveformPanel.cleanup();
    }
} 