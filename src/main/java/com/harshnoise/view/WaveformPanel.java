package com.harshnoise.view;

import com.harshnoise.model.AudioModel;
import com.harshnoise.AudioEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * WaveformPanel - UI component for waveform visualization
 * Part of MVC architecture for HarshNoiseGo
 */
public class WaveformPanel extends VBox {
    
    private final AudioModel model;
    private final AudioEngine audioEngine;
    private final Random random = new Random();
    
    // UI Components
    private Button recordButton;
    private Label chaosIndicator;
    private Canvas waveformCanvas;
    
    // Visualization timer
    private Timer visualizationTimer;
    
    public WaveformPanel(AudioModel model, AudioEngine audioEngine) {
        this.model = model;
        this.audioEngine = audioEngine;
        initializeUI();
        setupVisualization();
    }
    
    /**
     * Initialize the user interface
     */
    private void initializeUI() {
        setSpacing(15);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        setPrefWidth(380);
        setPrefHeight(450);
        setMinHeight(450);
        setMaxHeight(450);
        
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
        chaosIndicator = new Label("CHAOS MODE");
        chaosIndicator.setStyle("-fx-text-fill: #ff6b35; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-color: #2d1b3d; -fx-background-radius: 12; -fx-border-color: #ff6b35; -fx-border-width: 1; -fx-border-radius: 12;");
        chaosIndicator.setAlignment(Pos.CENTER_RIGHT);
        chaosIndicator.setVisible(false);  // Initially hidden
        
        // Create a container to right-align the chaos indicator
        HBox chaosContainer = new HBox();
        chaosContainer.setAlignment(Pos.CENTER_RIGHT);
        chaosContainer.setPadding(new Insets(0, 0, 0, 0));
        chaosContainer.getChildren().add(chaosIndicator);
        
        // Canvas with clean styling - positioned lower in the panel
        waveformCanvas = new Canvas(340, 200);
        waveformCanvas.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-border-width: 1;");
        
        // Add some spacing above the waveform canvas to move it down
        Region topSpacer = new Region();
        topSpacer.setPrefHeight(20);  // Add 20px spacing above waveform
        
        getChildren().addAll(titleBar, chaosContainer, topSpacer, waveformCanvas);
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
        
        return button;
    }
    
    /**
     * Setup real-time visualization
     */
    private void setupVisualization() {
        visualizationTimer = new Timer();
        visualizationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> updateWaveform());
            }
        }, 0, 50); // Update every 50ms
    }
    
    /**
     * Update waveform visualization with real audio samples
     */
    private void updateWaveform() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        double width = waveformCanvas.getWidth();
        double height = waveformCanvas.getHeight();
        
        // Clear canvas with light background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Draw center line
        gc.setStroke(Color.rgb(220, 220, 220));
        gc.setLineWidth(1.0);
        gc.strokeLine(0, height / 2, width, height / 2);
        
        // Get real audio samples from AudioEngine
        float[] samples = audioEngine.getLatestWaveformSamples();
        
        if (samples != null && samples.length > 0) {
            // Draw real waveform
            gc.setStroke(Color.rgb(40, 167, 69));
            gc.setLineWidth(2.0);
            gc.beginPath();
            
            // Downsample samples to fit canvas width
            int samplesToShow = Math.min(samples.length, (int) width);
            double sampleStep = (double) samples.length / samplesToShow;
            
            for (int i = 0; i < samplesToShow; i++) {
                int sampleIndex = (int) (i * sampleStep);
                float sample = samples[sampleIndex];
                
                // Convert sample to y coordinate
                double x = (double) i / samplesToShow * width;
                double y = height / 2 + (sample * height / 2 * 0.8); // Scale down for better visibility
                
                if (i == 0) {
                    gc.moveTo(x, y);
                } else {
                    gc.lineTo(x, y);
                }
            }
            
            gc.stroke();
            
            // Draw amplitude indicators
            drawAmplitudeIndicators(gc, width, height, samples);
        } else {
            // No audio data available - draw placeholder
            drawPlaceholderWaveform(gc, width, height);
        }
    }
    
    /**
     * Draw amplitude indicators on the sides
     */
    private void drawAmplitudeIndicators(GraphicsContext gc, double width, double height, float[] samples) {
        if (samples == null || samples.length == 0) return;
        
        // Calculate peak amplitude
        float peak = 0.0f;
        for (float sample : samples) {
            peak = Math.max(peak, Math.abs(sample));
        }
        
        // Draw peak indicator on right side - moved closer to edge
        double peakHeight = peak * height * 0.8;
        gc.setStroke(Color.rgb(255, 107, 53));
        gc.setLineWidth(3.0);
        gc.strokeLine(width - 5, height / 2 - peakHeight / 2, width - 5, height / 2 + peakHeight / 2);
        
        // Draw peak value text - positioned better
        gc.setFill(Color.rgb(100, 100, 100));
        gc.setFont(javafx.scene.text.Font.font("Arial", 9));
        String peakText = String.format("%.2f", peak);
        gc.fillText(peakText, width - 30, height / 2 - peakHeight / 2 - 3);
    }
    
    /**
     * Draw placeholder waveform when no audio data is available
     */
    private void drawPlaceholderWaveform(GraphicsContext gc, double width, double height) {
        gc.setStroke(Color.rgb(200, 200, 200));
        gc.setLineWidth(1.0);
        
        // Draw a simple sine wave placeholder
        gc.beginPath();
        for (int x = 0; x < width; x++) {
            double y = height / 2 + Math.sin(x * 0.02) * height / 4;
            if (x == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();
        
        // Draw "No Audio" text
        gc.setFill(Color.rgb(150, 150, 150));
        gc.setFont(javafx.scene.text.Font.font("Arial", 12));
        gc.fillText("No Audio Data", width / 2 - 40, height / 2);
    }
    
    /**
     * Update chaos mode indicator
     */
    public void updateChaosIndicator() {
        if (chaosIndicator != null) {
            chaosIndicator.setVisible(model.isChaosModeActive());
        }
    }
    
    /**
     * Update recording button state
     */
    public void updateRecordingButton() {
        if (recordButton != null) {
            if (model.isRecording()) {
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
            } else {
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
            }
        }
    }
    
    /**
     * Get record button for external event handling
     */
    public Button getRecordButton() {
        return recordButton;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (visualizationTimer != null) {
            visualizationTimer.cancel();
        }
    }
}
