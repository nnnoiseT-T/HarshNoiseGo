package com.harshnoise.view;

import com.harshnoise.model.AudioModel;
import com.harshnoise.controller.AudioController;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * ParameterPanel - UI component for controlling audio parameters
 * Part of MVC architecture for HarshNoiseGo.
 *
 * Features:
 * - Smooth synchronization with AudioModel (updates from Joystick or other inputs won't trigger feedback loops)
 * - Lightweight throttling during slider drags to avoid excessive engine updates
 * - Sliders snap cleanly and update UI labels in real time
 */
public class ParameterPanel extends VBox {

    private final AudioModel model;
    private final AudioController controller;

    // Sliders for each parameter
    private Slider toneSlider;
    private Slider distortionSlider;
    private Slider filterSlider;
    private Slider grainSizeSlider;

    // Flag to prevent recursive updates (avoids feedback loops)
    private boolean updatingFromModel = false;

    // Throttle timer to avoid spamming engine updates while dragging
    private final PauseTransition throttle = new PauseTransition(Duration.millis(20));

    public ParameterPanel(AudioModel model, AudioController controller) {
        this.model = model;
        this.controller = controller;
        initializeUI();
        setupEventHandlers();
    }

    /**
     * Initialize UI layout and styling
     */
    private void initializeUI() {
        setSpacing(15);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dee2e6; "
               + "-fx-border-radius: 10; -fx-border-width: 1; "
               + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);");
        setPrefWidth(220);
        setPrefHeight(450);
        setMinHeight(450);
        setMaxHeight(450);

        // Title
        Label title = new Label("Parameters");
        title.setStyle("-fx-text-fill: #495057; -fx-font-weight: bold; -fx-font-size: 16px;");
        title.setAlignment(Pos.CENTER);

        // Tone slider
        VBox toneBox = createSliderControl("Tone", 0.0, 1.0, 0.0);
        toneSlider = (Slider) toneBox.getChildren().get(1);

        // Distortion slider
        VBox distortionBox = createSliderControl("Distortion", 0.0, 1.0, 0.0);
        distortionSlider = (Slider) distortionBox.getChildren().get(1);

        // Filter slider
        VBox filterBox = createSliderControl("Filter", 0.0, 1.0, 0.0);
        filterSlider = (Slider) filterBox.getChildren().get(1);

        // Grain Size slider
        VBox grainBox = createSliderControl("Grain Size", 1, 128, 1);
        grainSizeSlider = (Slider) grainBox.getChildren().get(1);
        grainSizeSlider.setBlockIncrement(1);
        grainSizeSlider.setMajorTickUnit(1);
        grainSizeSlider.setSnapToTicks(false); // manual rounding

        getChildren().addAll(title, toneBox, distortionBox, filterBox, grainBox);

        // Configure throttle: batch updates every 20ms instead of every pixel move
        throttle.setOnFinished(e -> controller.updateAudioEngine());
        
        // Sync sliders with model's initial values
        updateFromModel();
    }

    /**
     * Create a slider + label pair inside a VBox
     */
    private VBox createSliderControl(String label, double min, double max, double defaultValue) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));

        // Label
        Label labelControl = new Label(label);
        labelControl.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-font-weight: bold;");
        labelControl.setAlignment(Pos.CENTER);

        // Slider
        Slider slider = new Slider(min, max, defaultValue);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setPrefWidth(180);
        slider.setPrefHeight(20);

        // Slider styling
        slider.setStyle(
            "-fx-control-inner-background: #e9ecef;"
          + "-fx-control-inner-background-alt: #dee2e6;"
          + "-fx-background-color: transparent;"
          + "-fx-background-radius: 10;"
          + "-fx-border-radius: 10;"
        );

        // Value label
        Label valueLabel = new Label(String.format("%.2f", defaultValue));
        valueLabel.setStyle(
            "-fx-text-fill: #6c757d;"
          + "-fx-font-size: 11px;"
          + "-fx-font-weight: bold;"
          + "-fx-background-color: #f8f9fa;"
          + "-fx-background-radius: 4;"
          + "-fx-padding: 2 6;"
        );
        valueLabel.setAlignment(Pos.CENTER);

        // Sync value label text with slider
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
            valueLabel.setText(String.format("%.2f", newVal.doubleValue()))
        );

        box.getChildren().addAll(labelControl, slider, valueLabel);
        return box;
    }


    /**
     * Set up listeners for each slider.
     * Sliders update the AudioModel directly, but engine refreshes are throttled.
     * Dragging does NOT trigger feedback loops because of updatingFromModel guard.
     */
    private void setupEventHandlers() {
        // Tone
        toneSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingFromModel) return;
            model.setTone(newVal.doubleValue());
            throttle.playFromStart();
        });
        toneSlider.valueChangingProperty().addListener((o, was, now) -> {
            if (!updatingFromModel && !now) controller.updateAudioEngine();
        });

        // Distortion
        distortionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingFromModel) return;
            model.setDistortionLevel(newVal.doubleValue());
            throttle.playFromStart();
        });
        distortionSlider.valueChangingProperty().addListener((o, was, now) -> {
            if (!updatingFromModel && !now) controller.updateAudioEngine();
        });

        // Filter
        filterSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingFromModel) return;
            model.setFilterLevel(newVal.doubleValue());
            throttle.playFromStart();
        });
        filterSlider.valueChangingProperty().addListener((o, was, now) -> {
            if (!updatingFromModel && !now) controller.updateAudioEngine();
        });

        // Grain Size
        grainSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingFromModel) return;
            int gs = (int) Math.round(newVal.doubleValue());
            model.setGrainSize(gs);
            throttle.playFromStart();
        });
        grainSizeSlider.valueChangingProperty().addListener((o, was, now) -> {
            if (!updatingFromModel && !now) controller.updateAudioEngine();
        });
    }

    /**
     * Update slider values based on AudioModel state.
     * Guard flag prevents triggering listeners during programmatic updates.
     */
    public void updateFromModel() {
        updatingFromModel = true;
        try {
            toneSlider.setValue(model.getTone());
            distortionSlider.setValue(model.getDistortionLevel());
            filterSlider.setValue(model.getFilterLevel());
            grainSizeSlider.setValue(model.getGrainSize());
        } finally {
            updatingFromModel = false;
        }
    }

    /**
     * Explicitly set slider values (external call).
     * Guard flag ensures no recursive triggers.
     */
    public void setSliderValues(double tone, double distortion, double filter, int grainSize) {
        updatingFromModel = true;
        try {
            toneSlider.setValue(tone);
            distortionSlider.setValue(distortion);
            filterSlider.setValue(filter);
            grainSizeSlider.setValue(grainSize);
        } finally {
            updatingFromModel = false;
        }
    }
}