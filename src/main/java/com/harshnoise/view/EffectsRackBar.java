package com.harshnoise.view;

import com.harshnoise.audio.effects.AudioEffect;
import com.harshnoise.audio.effects.EffectRegistry;
import com.harshnoise.audio.effects.EffectsController;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.*;

/**
 * Optimized dynamic effect chain area
 * Supports adding, removing, and controlling effects with improved performance
 */
public class EffectsRackBar extends VBox {
    
    // ===== STYLE CONSTANTS =====
    private static final String STYLE_MAIN_CONTAINER = 
        "-fx-background-color: white;" +
        "-fx-background-radius: 10;" +
        "-fx-border-color: #e0e0e0;" +
        "-fx-border-radius: 10;" +
        "-fx-border-width: 1;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);";
    
    private static final String STYLE_TITLE = "-fx-text-fill: #2c3e50;";
    
    private static final String STYLE_ADD_BUTTON = 
        "-fx-background-color: #28a745;" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: bold;" +
        "-fx-font-size: 12px;" +
        "-fx-background-radius: 6;" +
        "-fx-padding: 8 12;" +
        "-fx-cursor: hand;" +
        "-fx-min-width: 100;" +
        "-fx-min-height: 35;";
    
    private static final String STYLE_DIALOG_CONTENT = 
        "-fx-font-size: 14px;" +
        "-fx-font-weight: bold;";
    
    private static final String STYLE_COMBOBOX = 
        "-fx-font-size: 12px;" +
        "-fx-padding: 5 10;";
    
    private static final String STYLE_DIALOG_PANE = 
        "-fx-background-color: white;" +
        "-fx-text-fill: #333333;" +
        "-fx-border-color: #e0e0e0;" +
        "-fx-border-width: 1;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;";
    
    // ===== TEXT CONSTANTS =====
    private static final String TITLE_TEXT = "Effects Chain";
    private static final String ADD_BUTTON_TEXT = "+ Add Effect";
    private static final String DIALOG_TITLE = "Add Effect";
    private static final String DIALOG_HEADER = "Select an effect to add:";
    private static final String DIALOG_CONTENT = "Effect:";
    private static final String ADD_BUTTON_TYPE = "Add";
    private static final String CANCEL_BUTTON_TYPE = "Cancel";
    private static final String NO_EFFECTS_TITLE = "No Effects Available";
    private static final String NO_EFFECTS_CONTENT = "No effects are currently registered.";
    
    // ===== LAYOUT CONSTANTS =====
    private static final int SPACING_MAIN = 12;
    private static final int PADDING_MAIN = 15;
    private static final int CONTAINER_SPACING = 15;
    private static final int DIALOG_WIDTH = 350;
    private static final int DIALOG_HEIGHT = 200;
    private static final int DIALOG_CONTENT_PADDING = 20;
    private static final int DIALOG_CONTENT_SPACING = 15;
    private static final int COMBOBOX_WIDTH = 250;
    private static final int DEBOUNCE_MS = 16;
    
    private final EffectsController effectsController;
    private FlowPane effectsContainer;
    private final ScrollPane scrollPane;
    private final Button addEffectButton;
    
    // ===== CACHED COMPONENTS =====
    private Dialog<String> addDialog;
    private ComboBox<String> addCombo;
    
    // ===== CARD CACHE =====
    private final Map<String, EffectCard> cardByKey = new LinkedHashMap<>();
    
    // ===== DEBOUNCE =====
    private PauseTransition debounceTimer;
    
    public EffectsRackBar(EffectsController effectsController) {
        this.effectsController = effectsController;
        
        setSpacing(SPACING_MAIN);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(PADDING_MAIN));
        setStyle(STYLE_MAIN_CONTAINER);
        
        // Create add effect button first
        addEffectButton = createAddEffectButton();
        
        // Create title row
        HBox titleRow = createTitleRow();
        
        // Create scrollable container
        scrollPane = createScrollableContainer();
        
        getChildren().addAll(titleRow, scrollPane);
        
        // Initialize existing effects
        refreshEffects();
    }
    
    /**
     * Create title row component
     */
    private HBox createTitleRow() {
        HBox titleRow = new HBox(CONTAINER_SPACING);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(TITLE_TEXT);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setStyle(STYLE_TITLE);
        
        titleRow.getChildren().addAll(titleLabel, addEffectButton);
        return titleRow;
    }
    
    /**
     * Create scrollable container for effects
     */
    private ScrollPane createScrollableContainer() {
        // Create FlowPane for wrapping effects
        effectsContainer = new FlowPane(CONTAINER_SPACING, CONTAINER_SPACING);
        effectsContainer.setAlignment(Pos.TOP_LEFT);
        effectsContainer.setPadding(new Insets(0));
        
        // Set FlowPane to wrap horizontally with sufficient width
        effectsContainer.setPrefWrapLength(800); // Allow wide horizontal layout
        effectsContainer.setHgap(CONTAINER_SPACING);
        effectsContainer.setVgap(CONTAINER_SPACING);
        
        // Create ScrollPane
        ScrollPane scroll = new ScrollPane(effectsContainer);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefHeight(180);
        
        return scroll;
    }
    
    /**
     * Create add effect button
     */
    private Button createAddEffectButton() {
        Button button = new Button(ADD_BUTTON_TEXT);
        button.setStyle(STYLE_ADD_BUTTON);
        button.setOnAction(e -> showAddEffectDialog());
        return button;
    }
    
    /**
     * Create and cache add effect dialog
     */
    private void createAddDialog() {
        if (addDialog != null) return;
        
        addDialog = new Dialog<>();
        addDialog.setTitle(DIALOG_TITLE);
        addDialog.setHeaderText(DIALOG_HEADER);
        
        // Set dialog properties for fullscreen compatibility
        addDialog.initModality(Modality.APPLICATION_MODAL);
        addDialog.initStyle(StageStyle.UTILITY);
        
        // Set dialog size
        addDialog.setResizable(false);
        addDialog.setWidth(DIALOG_WIDTH);
        addDialog.setHeight(DIALOG_HEIGHT);
        
        // Create dialog content
        VBox content = new VBox(DIALOG_CONTENT_SPACING);
        content.setPadding(new Insets(DIALOG_CONTENT_PADDING));
        content.setAlignment(Pos.CENTER);
        
        Label label = new Label(DIALOG_CONTENT);
        label.setStyle(STYLE_DIALOG_CONTENT);
        
        addCombo = new ComboBox<>();
        addCombo.setPrefWidth(COMBOBOX_WIDTH);
        addCombo.setStyle(STYLE_COMBOBOX);
        
        content.getChildren().addAll(label, addCombo);
        addDialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType addButtonType = new ButtonType(ADD_BUTTON_TYPE, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(CANCEL_BUTTON_TYPE, ButtonBar.ButtonData.CANCEL_CLOSE);
        addDialog.getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);
        
        // Custom dialog styling
        DialogPane dialogPane = addDialog.getDialogPane();
        dialogPane.setStyle(STYLE_DIALOG_PANE);
        
        // Set result converter
        addDialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return addCombo.getValue();
            }
            return null;
        });
    }
    
    /**
     * Show add effect dialog with improved fullscreen compatibility
     */
    private void showAddEffectDialog() {
        List<String> availableEffects = new ArrayList<>(effectsController.getAvailableEffectIds());
        
        if (availableEffects.isEmpty()) {
            showAlert(NO_EFFECTS_TITLE, NO_EFFECTS_CONTENT);
            return;
        }
        
        // Lazy initialize dialog
        createAddDialog();
        
        // Refresh combo box items
        addCombo.getItems().clear();
        addCombo.getItems().addAll(availableEffects);
        addCombo.setValue(availableEffects.get(0));
        
        // Set owner if available
        try {
            if (getScene() != null && getScene().getWindow() != null) {
                Stage primaryStage = (Stage) getScene().getWindow();
                addDialog.initOwner(primaryStage);
            }
        } catch (Exception e) {
            // Skip owner setting if not available
        }
        
        // Show dialog and handle result
        Runnable showDialog = () -> {
            addDialog.showAndWait().ifPresent(effectId -> {
                if (effectId != null) {
                    AudioEffect effect = effectsController.addEffect(effectId);
                    if (effect != null) {
                        refreshEffects();
                    }
                }
            });
        };
        
        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            Platform.runLater(showDialog);
        }
    }
    
    /**
     * Refresh effects display with incremental updates
     */
    public void refreshEffects() {
        // Debounce multiple rapid calls
        if (debounceTimer != null) {
            debounceTimer.stop();
        }
        
        debounceTimer = new PauseTransition(Duration.millis(DEBOUNCE_MS));
        debounceTimer.setOnFinished(e -> performRefreshEffects());
        debounceTimer.play();
    }
    
    /**
     * Perform the actual effects refresh
     */
    private void performRefreshEffects() {
        List<AudioEffect> currentEffects = effectsController.getEffectsSnapshot();
        Set<String> currentKeys = new HashSet<>();
        
        // Update existing cards and add new ones
        for (int i = 0; i < currentEffects.size(); i++) {
            AudioEffect effect = currentEffects.get(i);
            String key = generateCardKey(effect, i);
            currentKeys.add(key);
            
            EffectCard card = cardByKey.get(key);
            if (card == null) {
                // Create new card
                card = new EffectCard(effect, i, effectsController, this);
                cardByKey.put(key, card);
                effectsContainer.getChildren().add(card);
            } else {
                // Update existing card
                card.updateFrom(effect, i);
            }
        }
        
        // Remove cards that no longer exist
        Iterator<Map.Entry<String, EffectCard>> iterator = cardByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, EffectCard> entry = iterator.next();
            if (!currentKeys.contains(entry.getKey())) {
                EffectCard card = entry.getValue();
                effectsContainer.getChildren().remove(card);
                iterator.remove();
            }
        }
    }
    
    /**
     * Generate unique key for effect card
     */
    private String generateCardKey(AudioEffect effect, int index) {
        return effect.getId() + "_" + index;
    }
    
    /**
     * Show warning dialog with improved fullscreen compatibility
     */
    private void showAlert(String title, String content) {
        Runnable showAlert = () -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            
            // Set alert properties for fullscreen compatibility
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initStyle(StageStyle.UTILITY);
            
            // Set owner if available
            try {
                if (getScene() != null && getScene().getWindow() != null) {
                    Stage primaryStage = (Stage) getScene().getWindow();
                    alert.initOwner(primaryStage);
                }
            } catch (Exception e) {
                // Skip owner setting if not available
            }
            
            alert.showAndWait();
        };
        
        if (Platform.isFxApplicationThread()) {
            showAlert.run();
        } else {
            Platform.runLater(showAlert);
        }
    }
}
