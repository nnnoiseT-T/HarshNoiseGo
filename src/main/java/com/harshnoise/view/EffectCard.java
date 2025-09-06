package com.harshnoise.view;

import com.harshnoise.audio.effects.AudioEffect;
import com.harshnoise.audio.effects.EffectsController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashMap;
import java.util.Map;

/**
 * Individual effect card
 * Displays effect name, switch, delete button, Wet button group and parameter controls
 */
public class EffectCard extends VBox {
    
    private AudioEffect effect;
    private int effectIndex;
    private final EffectsController effectsController;
    private final EffectsRackBar parent;
    
    private final ToggleButton bypassToggle;
    private final Button removeButton;
    private ToggleGroup wetToggleGroup;
    private final Map<String, Slider> parameterSliders;
    
    public EffectCard(AudioEffect effect, int effectIndex, EffectsController effectsController, EffectsRackBar parent) {
        this.effect = effect;
        this.effectIndex = effectIndex;
        this.effectsController = effectsController;
        this.parent = parent;
        this.parameterSliders = new HashMap<>();
        
        setSpacing(6);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(8));
        setPrefSize(220, 140);
        setMaxSize(220, 140);
        setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;"
        );
        
        // Card header: title, bypass switch, delete button
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(effect.getName());
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // Bypass switch (On/Bypass)
        bypassToggle = new ToggleButton();
        bypassToggle.setSelected(effect.isEnabled());
        bypassToggle.setText(bypassToggle.isSelected() ? "On" : "Bypass");
        bypassToggle.setStyle(
            "-fx-background-color: " + (bypassToggle.isSelected() ? "#4CAF50" : "#e0e0e0") + ";" +
            "-fx-text-fill: " + (bypassToggle.isSelected() ? "white" : "#666") + ";" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 10px;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 4 8;" +
            "-fx-cursor: hand;" +
            "-fx-min-width: 60;" +
            "-fx-min-height: 24;"
        );
        
        bypassToggle.setOnAction(e -> {
            boolean enabled = bypassToggle.isSelected();
            effectsController.setEffectEnabled(effectIndex, enabled);
            bypassToggle.setText(enabled ? "On" : "Bypass");
            updateBypassStyle();
        });
        
        // Delete button
        removeButton = new Button("×");
        removeButton.setStyle(
            "-fx-background-color: #ff6b6b;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 2 6;" +
            "-fx-cursor: hand;" +
            "-fx-min-width: 20;" +
            "-fx-min-height: 20;"
        );
        
        removeButton.setOnAction(e -> {
            effectsController.removeEffect(effectIndex);
            parent.refreshEffects();
        });
        
        headerRow.getChildren().addAll(titleLabel, bypassToggle, removeButton);
        
        // Wet button group
        VBox wetControls = createWetControls();
        
        // Parameter controls
        VBox paramControls = createParameterControls();
        
        getChildren().addAll(headerRow, wetControls, paramControls);
    }
    
    /**
     * Update card from effect data
     */
    public void updateFrom(AudioEffect effect, int newIndex) {
        // Update effect reference and index
        this.effect = effect;
        this.effectIndex = newIndex;
        
        // Update title
        Label titleLabel = (Label) ((HBox) getChildren().get(0)).getChildren().get(0);
        titleLabel.setText(effect.getName());
        
        // Update bypass toggle
        bypassToggle.setSelected(effect.isEnabled());
        bypassToggle.setText(bypassToggle.isSelected() ? "On" : "Bypass");
        updateBypassStyle();
        
        // Update wet controls
        updateWetControls();
        
        // Update parameter controls
        updateParameterControls();
    }
    
    /**
     * Update bypass button style
     */
    private void updateBypassStyle() {
        boolean enabled = bypassToggle.isSelected();
        bypassToggle.setStyle(
            "-fx-background-color: " + (enabled ? "#4CAF50" : "#e0e0e0") + ";" +
            "-fx-text-fill: " + (enabled ? "white" : "#666") + ";" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 10px;" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 4 8;" +
            "-fx-cursor: hand;" +
            "-fx-min-width: 60;" +
            "-fx-min-height: 24;"
        );
    }
    
    /**
     * Update wet controls
     */
    private void updateWetControls() {
        VBox wetControls = (VBox) getChildren().get(1);
        HBox wetButtons = (HBox) wetControls.getChildren().get(1);
        
        float currentWetRatio = effect.getWetDryRatio();
        float[] wetRatios = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
        
        int currentIndex = 0;
        for (int i = 0; i < wetRatios.length; i++) {
            if (Math.abs(currentWetRatio - wetRatios[i]) < 0.01f) {
                currentIndex = i;
                break;
            }
        }
        
        // Update button selection and styles
        for (int i = 0; i < wetButtons.getChildren().size(); i++) {
            ToggleButton btn = (ToggleButton) wetButtons.getChildren().get(i);
            boolean isSelected = i == currentIndex;
            btn.setSelected(isSelected);
            btn.setStyle(
                "-fx-background-color: " + (isSelected ? "#3498db" : "#f8f9fa") + ";" +
                "-fx-text-fill: " + (isSelected ? "white" : "#666") + ";" +
                "-fx-font-size: 9px;" +
                "-fx-background-radius: 3;" +
                "-fx-padding: 2 4;" +
                "-fx-cursor: hand;" +
                "-fx-min-width: 35;" +
                "-fx-min-height: 20;"
            );
        }
    }
    
    /**
     * Update parameter controls
     */
    private void updateParameterControls() {
        VBox paramControls = (VBox) getChildren().get(2);
        paramControls.getChildren().clear();
        
        // Recreate parameter controls based on effect type
        if (effect.getId().equals("bypass")) {
            createParameterSlider(paramControls, "Gain", "gain", 0.0f, 2.0f, effect.getParameter("gain"));
        } else if (effect.getId().equals("delay")) {
            createParameterSlider(paramControls, "Time", "time", 20.0f, 1200.0f, effect.getParameter("time"));
            createParameterSlider(paramControls, "Feedback", "feedback", 0.0f, 0.95f, effect.getParameter("feedback"));
        } else if (effect.getId().equals("reverb")) {
            createParameterSlider(paramControls, "Room", "room", 0.0f, 1.0f, effect.getParameter("room"));
            createParameterSlider(paramControls, "Damp", "damp", 0.0f, 1.0f, effect.getParameter("damp"));
        } else if (effect.getId().equals("bitcrusher")) {
            createParameterSlider(paramControls, "BitDepth", "bitdepth", 6.0f, 12.0f, effect.getParameter("bitdepth"));
            createParameterSlider(paramControls, "Rate (Hz)", "rate", 8000.0f, 24000.0f, effect.getParameter("rate"));
        } else if (effect.getId().equals("overdrive")) {
            createParameterSlider(paramControls, "Drive", "drive", 1.0f, 20.0f, effect.getParameter("drive"));
            createParameterSlider(paramControls, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("distortion")) {
            createParameterSlider(paramControls, "Drive", "drive", 1.0f, 20.0f, effect.getParameter("drive"));
            createParameterSlider(paramControls, "Shape", "shape", 0.0f, 1.0f, effect.getParameter("shape"));
            createParameterSlider(paramControls, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("fuzz")) {
            createParameterSlider(paramControls, "Drive", "drive", 3.0f, 40.0f, effect.getParameter("drive"));
            createParameterSlider(paramControls, "Bias", "bias", -0.5f, 0.5f, effect.getParameter("bias"));
            createParameterSlider(paramControls, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("wavefolder")) {
            createParameterSlider(paramControls, "Fold", "fold", 0.0f, 10.0f, effect.getParameter("fold"));
            createParameterSlider(paramControls, "Symmetry", "symmetry", -1.0f, 1.0f, effect.getParameter("symmetry"));
            createParameterSlider(paramControls, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("reverseDelay")) {
            createParameterSlider(paramControls, "Time", "time", 200.0f, 2000.0f, effect.getParameter("time"));
            createParameterSlider(paramControls, "Feedback", "feedback", 0.0f, 0.95f, effect.getParameter("feedback"));
            createParameterSlider(paramControls, "Xfade", "crossfade", 0.0f, 20.0f, effect.getParameter("crossfade"));
        } else if (effect.getId().equals("granular")) {
            createParameterSlider(paramControls, "Grain Size", "grainSize", 20.0f, 200.0f, effect.getParameter("grainSize"));
            createParameterSlider(paramControls, "Density", "density", 1.0f, 50.0f, effect.getParameter("density"));
            createParameterSlider(paramControls, "Pitch", "pitch", 0.5f, 2.0f, effect.getParameter("pitch"));
            createParameterSlider(paramControls, "Jitter", "jitter", 0.0f, 1.0f, effect.getParameter("jitter"));
            createParameterSlider(paramControls, "Reverse", "reverseProb", 0.0f, 1.0f, effect.getParameter("reverseProb"));
        } else if (effect.getId().equals("looper")) {
            createParameterSlider(paramControls, "Loop Len", "loopLen", 200.0f, 8000.0f, effect.getParameter("loopLen"));
            createParameterSlider(paramControls, "Overdub", "overdub", 0.0f, 1.0f, effect.getParameter("overdub"));
            createParameterSlider(paramControls, "Feedback", "feedback", 0.0f, 0.99f, effect.getParameter("feedback"));
            createParameterSlider(paramControls, "Speed", "speed", 0.5f, 2.0f, effect.getParameter("speed"));
            createParameterSlider(paramControls, "Stutter Prob", "stutterProb", 0.0f, 1.0f, effect.getParameter("stutterProb"));
            createParameterSlider(paramControls, "Slice Len", "sliceLen", 10.0f, 200.0f, effect.getParameter("sliceLen"));
            createParameterSlider(paramControls, "Jitter", "jitter", 0.0f, 1.0f, effect.getParameter("jitter"));
        } else {
            createParameterSlider(paramControls, "Param 1", "param1", 0.0f, 1.0f, 0.5f);
            createParameterSlider(paramControls, "Param 2", "param2", 0.0f, 1.0f, 0.5f);
        }
    }
    
    /**
     * Create Wet control button group
     */
    private VBox createWetControls() {
        VBox container = new VBox(4);
        container.setAlignment(Pos.CENTER_LEFT);
        
        Label wetLabel = new Label("Wet:");
        wetLabel.setFont(Font.font("Arial", 10));
        wetLabel.setStyle("-fx-text-fill: #555;");
        
        HBox wetButtons = new HBox(2);
        wetButtons.setAlignment(Pos.CENTER_LEFT);
        
        wetToggleGroup = new ToggleGroup();
        String[] wetValues = {"Dry", "25%", "50%", "75%", "Wet"};
        float[] wetRatios = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
        
        float currentWetRatio = effect.getWetDryRatio();
        int currentIndex = 0;
        for (int i = 0; i < wetRatios.length; i++) {
            if (Math.abs(currentWetRatio - wetRatios[i]) < 0.01f) {
                currentIndex = i;
                break;
            }
        }
        
        for (int i = 0; i < wetValues.length; i++) {
            ToggleButton button = new ToggleButton(wetValues[i]);
            button.setToggleGroup(wetToggleGroup);
            button.setSelected(i == currentIndex);
            button.setStyle(
                "-fx-background-color: " + (i == currentIndex ? "#3498db" : "#f8f9fa") + ";" +
                "-fx-text-fill: " + (i == currentIndex ? "white" : "#666") + ";" +
                "-fx-font-size: 9px;" +
                "-fx-background-radius: 3;" +
                "-fx-padding: 2 4;" +
                "-fx-cursor: hand;" +
                "-fx-min-width: 35;" +
                "-fx-min-height: 20;"
            );
            
            final int index = i;
            button.setOnAction(e -> {
                float wetRatio = wetRatios[index];
                effectsController.setEffectWetDryRatio(effectIndex, wetRatio);
                
                // Update all button styles
                for (int j = 0; j < wetButtons.getChildren().size(); j++) {
                    ToggleButton btn = (ToggleButton) wetButtons.getChildren().get(j);
                    boolean isSelected = j == index;
                    btn.setStyle(
                        "-fx-background-color: " + (isSelected ? "#3498db" : "#f8f9fa") + ";" +
                        "-fx-text-fill: " + (isSelected ? "white" : "#666") + ";" +
                        "-fx-font-size: 9px;" +
                        "-fx-background-radius: 3;" +
                        "-fx-padding: 2 4;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 35;" +
                        "-fx-min-height: 20;"
                    );
                }
            });
            
            wetButtons.getChildren().add(button);
        }
        
        container.getChildren().addAll(wetLabel, wetButtons);
        return container;
    }
    
    /**
     * Create parameter controls
     */
    private VBox createParameterControls() {
        VBox container = new VBox(3);
        container.setAlignment(Pos.CENTER_LEFT);
        
        // Create different parameter controls based on effect type
        if (effect.getId().equals("bypass")) {
            // Bypass effect only has gain parameter
            createParameterSlider(container, "Gain", "gain", 0.0f, 2.0f, effect.getParameter("gain"));
        } else if (effect.getId().equals("delay")) {
            // Delay effect parameters
            createParameterSlider(container, "Time", "time", 20.0f, 1200.0f, effect.getParameter("time"));
            createParameterSlider(container, "Feedback", "feedback", 0.0f, 0.95f, effect.getParameter("feedback"));
        } else if (effect.getId().equals("reverb")) {
            // Reverb effect parameters
            createParameterSlider(container, "Room", "room", 0.0f, 1.0f, effect.getParameter("room"));
            createParameterSlider(container, "Damp", "damp", 0.0f, 1.0f, effect.getParameter("damp"));
        } else if (effect.getId().equals("bitcrusher")) {
            // Bitcrusher effect parameters
            createParameterSlider(container, "BitDepth", "bitdepth", 6.0f, 12.0f, effect.getParameter("bitdepth"));
            createParameterSlider(container, "Rate (Hz)", "rate", 8000.0f, 24000.0f, effect.getParameter("rate"));
        } else if (effect.getId().equals("overdrive")) {
            // Overdrive effect parameters
            createParameterSlider(container, "Drive", "drive", 1.0f, 20.0f, effect.getParameter("drive"));
            createParameterSlider(container, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("distortion")) {
            // Distortion effect parameters
            createParameterSlider(container, "Drive", "drive", 1.0f, 20.0f, effect.getParameter("drive"));
            createParameterSlider(container, "Shape", "shape", 0.0f, 1.0f, effect.getParameter("shape"));
            createParameterSlider(container, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("fuzz")) {
            // Fuzz effect parameters
            createParameterSlider(container, "Drive", "drive", 3.0f, 40.0f, effect.getParameter("drive"));
            createParameterSlider(container, "Bias", "bias", -0.5f, 0.5f, effect.getParameter("bias"));
            createParameterSlider(container, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("wavefolder")) {
            // Wavefolder effect parameters
            createParameterSlider(container, "Fold", "fold", 0.0f, 10.0f, effect.getParameter("fold"));
            createParameterSlider(container, "Symmetry", "symmetry", -1.0f, 1.0f, effect.getParameter("symmetry"));
            createParameterSlider(container, "Tone", "tone", 0.0f, 1.0f, effect.getParameter("tone"));
        } else if (effect.getId().equals("reverseDelay")) {
            // Reverse delay effect parameters
            createParameterSlider(container, "Time", "time", 200.0f, 2000.0f, effect.getParameter("time"));
            createParameterSlider(container, "Feedback", "feedback", 0.0f, 0.95f, effect.getParameter("feedback"));
            createParameterSlider(container, "Xfade", "crossfade", 0.0f, 20.0f, effect.getParameter("crossfade"));
        } else if (effect.getId().equals("granular")) {
            // Granular effect parameters
            createParameterSlider(container, "Grain Size", "grainSize", 20.0f, 200.0f, effect.getParameter("grainSize"));
            createParameterSlider(container, "Density", "density", 1.0f, 50.0f, effect.getParameter("density"));
            createParameterSlider(container, "Pitch", "pitch", 0.5f, 2.0f, effect.getParameter("pitch"));
            createParameterSlider(container, "Jitter", "jitter", 0.0f, 1.0f, effect.getParameter("jitter"));
            createParameterSlider(container, "Reverse", "reverseProb", 0.0f, 1.0f, effect.getParameter("reverseProb"));
        } else if (effect.getId().equals("looper")) {
            // Looper effect parameters
            createParameterSlider(container, "Loop Len", "loopLen", 200.0f, 8000.0f, effect.getParameter("loopLen"));
            createParameterSlider(container, "Overdub", "overdub", 0.0f, 1.0f, effect.getParameter("overdub"));
            createParameterSlider(container, "Feedback", "feedback", 0.0f, 0.99f, effect.getParameter("feedback"));
            createParameterSlider(container, "Speed", "speed", 0.5f, 2.0f, effect.getParameter("speed"));
            createParameterSlider(container, "Stutter Prob", "stutterProb", 0.0f, 1.0f, effect.getParameter("stutterProb"));
            createParameterSlider(container, "Slice Len", "sliceLen", 10.0f, 200.0f, effect.getParameter("sliceLen"));
            createParameterSlider(container, "Jitter", "jitter", 0.0f, 1.0f, effect.getParameter("jitter"));
        } else {
            // Other effects use default parameters
            createParameterSlider(container, "Param 1", "param1", 0.0f, 1.0f, 0.5f);
            createParameterSlider(container, "Param 2", "param2", 0.0f, 1.0f, 0.5f);
        }
        
        return container;
    }
    
    /**
     * Create parameter slider
     */
    private void createParameterSlider(VBox container, String label, String paramName, float min, float max, float defaultValue) {
        HBox paramRow = new HBox(6);
        paramRow.setAlignment(Pos.CENTER_LEFT);
        
        Label paramLabel = new Label(label + ":");
        paramLabel.setFont(Font.font("Arial", 9));
        paramLabel.setStyle("-fx-text-fill: #555;");
        paramLabel.setPrefWidth(50);
        
        Slider slider = new Slider(min, max, defaultValue);
        slider.setPrefWidth(120);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setStyle(
            "-fx-control-inner-background: #e0e0e0;" +
            "-fx-control-inner-background-alt: #3498db;"
        );
        
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            effectsController.setEffectParameter(effectIndex, paramName, newVal.floatValue());
        });
        
        parameterSliders.put(paramName, slider);
        
        paramRow.getChildren().addAll(paramLabel, slider);
        container.getChildren().add(paramRow);
    }
}
