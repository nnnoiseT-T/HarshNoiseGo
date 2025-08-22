package com.harshnoise;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main class for HarshNoiseGo! application
 * Launches the JavaFX application with the main GUI
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create main GUI controller
        MainGUI mainGUI = new MainGUI();
        
        // Create scene with the main GUI
        Scene scene = new Scene(mainGUI, 1000, 700);
        
        // Configure primary stage
        primaryStage.setTitle("HarshNoiseGo! - Harsh Noise Generator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Show the stage
        primaryStage.show();
        
        // Initialize audio engine after a short delay to ensure JavaFX is ready
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000); // Wait for JavaFX to be fully ready
                mainGUI.initializeAudio();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
} 