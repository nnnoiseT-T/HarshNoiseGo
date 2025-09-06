package com.harshnoise.controller;

import com.harshnoise.AudioEngine;
import com.harshnoise.Recorder;
import com.harshnoise.model.AudioModel;
import com.harshnoise.audio.effects.EffectsController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.harshnoise.PerlinModulator;

/**
 * Optimized AudioController - Handles audio control logic and business operations
 * Part of MVC architecture for HarshNoiseGo with performance enhancements
 */
public class AudioController {
    
    private static final boolean DEBUG = false; // Debug logging flag
    private static final long LOG_THROTTLE_MS = 1000; // Log throttle interval
    private static final int CHAOS_BASE_PERIOD_MS = 800; // Fixed chaos period - slower interval
    private static final int CHAOS_JITTER_MS = 100; // Random jitter range
    private static final int RESTART_DELAY_MS = 500; // Restart delay
    
    private final AudioModel model;
    private final AudioEngine audioEngine;
    private final Recorder recorder;
    private final Random random = new Random();
    
    // Background executor for audio operations
    private final ExecutorService backgroundExecutor;
    
    // Effects controller
    private EffectsController effectsController;
    
    // Chaos mode scheduler
    private ScheduledExecutorService chaosScheduler;
    private final AtomicBoolean chaosActive = new AtomicBoolean(false);
    
    // Perlin noise modulator
    private PerlinModulator perlinModulator;
    
    // Log throttling
    private long lastLogTime = 0;
    
    public AudioController(AudioModel model) {
        this.model = model;
        this.audioEngine = new AudioEngine();
        this.recorder = new Recorder();
        this.perlinModulator = new PerlinModulator(model);
        
        // Initialize background executor
        this.backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AudioController-Background");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize effects controller
        this.effectsController = new EffectsController(audioEngine.getEffectChain());
    }
    
    /**
     * Initialize audio engine - non-blocking
     */
    public void initializeAudio() {
        if (DEBUG) System.out.println("DEBUG: Initializing audio engine with silent startup...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Start audio engine on background thread
                audioEngine.start();
                
                // Wait a moment for audio engine to be ready
                Thread.sleep(200);
                
                // Update noise levels on JavaFX thread
                Platform.runLater(() -> {
                    updateAudioEngine();
                    if (DEBUG) System.out.println("DEBUG: Audio initialization completed - Silent startup, waiting for user selection");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    if (DEBUG) System.err.println("DEBUG: Audio initialization interrupted");
                });
            }
        }, backgroundExecutor);
    }
    
    /**
     * Update audio engine with current model values - optimized with parameter snapshot
     */
    public void updateAudioEngine() {
        // Snapshot all model parameters to local final variables
        final double whiteLevel = model.getWhiteNoiseLevel();
        final double pinkLevel = model.getPinkNoiseLevel();
        final double granularLevel = model.getGranularNoiseLevel();
        final double brownLevel = model.getBrownNoiseLevel();
        final double blueLevel = model.getBlueNoiseLevel();
        final double violetLevel = model.getVioletNoiseLevel();
        final double impulseLevel = model.getImpulseNoiseLevel();
        final double modulatedLevel = model.getModulatedNoiseLevel();
        final double tone = model.getTone();
        final double distortionLevel = model.getDistortionLevel();
        final double filterLevel = model.getFilterLevel();
        final int grainSize = model.getGrainSize();
        
        // Batch update audio engine parameters
        audioEngine.setWhiteNoiseLevel(whiteLevel);
        audioEngine.setPinkNoiseLevel(pinkLevel);
        audioEngine.setGranularNoiseLevel(granularLevel);
        audioEngine.setBrownNoiseLevel(brownLevel);
        audioEngine.setBlueNoiseLevel(blueLevel);
        audioEngine.setVioletNoiseLevel(violetLevel);
        audioEngine.setImpulseNoiseLevel(impulseLevel);
        audioEngine.setModulatedNoiseLevel(modulatedLevel);
        audioEngine.setTone(tone);
        audioEngine.setDistortionLevel(distortionLevel);
        audioEngine.setFilterLevel(filterLevel);
        audioEngine.setGrainSize(grainSize);
        
        // Throttled logging
        long currentTime = System.currentTimeMillis();
        if (DEBUG && (currentTime - lastLogTime) > LOG_THROTTLE_MS) {
            System.out.println("DEBUG: Audio engine updated - Tone: " + tone + 
                              ", Distortion: " + distortionLevel + ", Filter: " + filterLevel);
            lastLogTime = currentTime;
        }
    }
    
    /**
     * Randomize all parameters
     */
    public void randomizeParameters() {
        // Only randomize volume if not locked
        if (!model.isVolumeLocked()) {
            model.setVolume(Math.max(0.1, random.nextDouble()));
        }
        model.setTone(random.nextDouble());
        model.setDistortionLevel(random.nextDouble());
        model.setFilterLevel(random.nextDouble() * 0.8); // Limit filter to 0.0-0.8 to avoid silence
        model.setGrainSize(random.nextInt(128) + 1);
        
        // Randomize noise types
        model.setWhiteNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setPinkNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setGranularNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setBrownNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setBlueNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setVioletNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setImpulseNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        model.setModulatedNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
        
        // Ensure at least one noise type is selected
        if (model.getWhiteNoiseLevel() == 0.0 && model.getPinkNoiseLevel() == 0.0 && 
            model.getGranularNoiseLevel() == 0.0 && model.getBrownNoiseLevel() == 0.0 &&
            model.getBlueNoiseLevel() == 0.0 && model.getVioletNoiseLevel() == 0.0 &&
            model.getImpulseNoiseLevel() == 0.0 && model.getModulatedNoiseLevel() == 0.0) {
            model.setWhiteNoiseLevel(1.0);
        }
        
        updateAudioEngine();
    }
    
    /**
     * Toggle chaos mode - thread-safe
     */
    public void toggleChaosMode() {
        if (chaosActive.get()) {
            stopChaosMode();
        } else {
            startChaosMode();
        }
    }
    
    /**
     * Start chaos mode - thread-safe with atomic flag
     */
    private void startChaosMode() {
        if (chaosActive.compareAndSet(false, true)) {
            model.setChaosModeActive(true);
            
            // Create new scheduler
            chaosScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ChaosMode-Scheduler");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule chaos task with fixed period and jitter
            chaosScheduler.scheduleAtFixedRate(() -> {
                if (chaosActive.get()) {
                    // Compute parameter changes off-thread
                    double newVolume = Math.max(0.1, random.nextDouble());
                    double newTone = random.nextDouble();
                    double newDistortion = random.nextDouble();
                    double newFilter = random.nextDouble() * 0.8; // Limit filter to 0.0-0.8 to avoid silence
                    int newGrainSize = random.nextInt(128) + 1;
                    
                    // Apply changes on JavaFX thread
                    Platform.runLater(() -> {
                        if (chaosActive.get()) {
                            // Only update volume if not locked
                            if (!model.isVolumeLocked()) {
                                model.setVolume(newVolume);
                            }
                            model.setTone(newTone);
                            model.setDistortionLevel(newDistortion);
                            model.setFilterLevel(newFilter);
                            model.setGrainSize(newGrainSize);
                            
                            // Randomize noise types
                            model.setWhiteNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setPinkNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setGranularNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setBrownNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setBlueNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setVioletNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setImpulseNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            model.setModulatedNoiseLevel(random.nextBoolean() ? 1.0 : 0.0);
                            
                            // Ensure at least one noise type is selected
                            if (model.getWhiteNoiseLevel() == 0.0 && model.getPinkNoiseLevel() == 0.0 && 
                                model.getGranularNoiseLevel() == 0.0 && model.getBrownNoiseLevel() == 0.0 &&
                                model.getBlueNoiseLevel() == 0.0 && model.getVioletNoiseLevel() == 0.0 &&
                                model.getImpulseNoiseLevel() == 0.0 && model.getModulatedNoiseLevel() == 0.0) {
                                model.setWhiteNoiseLevel(1.0);
                            }
                            
                            updateAudioEngine();
                        }
                    });
                }
            }, CHAOS_BASE_PERIOD_MS + random.nextInt(CHAOS_JITTER_MS), 
               CHAOS_BASE_PERIOD_MS + random.nextInt(CHAOS_JITTER_MS), TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Stop chaos mode - thread-safe with proper cleanup
     */
    private void stopChaosMode() {
        if (chaosActive.compareAndSet(true, false)) {
            model.setChaosModeActive(false);
            
            if (chaosScheduler != null) {
                chaosScheduler.shutdownNow();
                chaosScheduler = null;
            }
        }
    }
    
    /**
     * Toggle recording state
     */
    public void toggleRecording() {
        if (!model.isRecording()) {
            startRecording();
        } else {
            stopRecording();
        }
    }
    
    /**
     * Start recording audio
     */
    private void startRecording() {
        model.setRecording(true);
        System.out.println("🎙️ Recording started...");
        audioEngine.startRecording();
    }
    
    /**
     * Stop recording and export audio
     */
    private void stopRecording() {
        model.setRecording(false);
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
     * Restart audio engine - non-blocking
     */
    public void restartAudio() {
        System.out.println("Restarting audio engine...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Stop current audio
                audioEngine.stop();
                
                // Reset all noise type selections to unchecked (silent startup state)
                Platform.runLater(() -> model.resetNoiseLevels());
                
                // Wait a bit
                Thread.sleep(RESTART_DELAY_MS);
                
                // Restart audio
                audioEngine.start();
                
                // Update audio engine on JavaFX thread
                Platform.runLater(() -> {
                    updateAudioEngine();
                    System.out.println("Audio engine restarted - Select a noise type to begin playing!");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    if (DEBUG) System.err.println("DEBUG: Audio restart interrupted");
                });
            }
        }, backgroundExecutor);
    }
    
    /**
     * Toggle Perlin noise modulation - thread-safe
     */
    public void togglePerlinNoise() {
        if (perlinModulator != null) {
            perlinModulator.toggle();
        }
    }
    
    /**
     * Check if Perlin noise is active
     */
    public boolean isPerlinNoiseActive() {
        return perlinModulator != null && perlinModulator.isActive();
    }
    
    /**
     * Cleanup resources - thread-safe and idempotent
     */
    public void cleanup() {
        try {
            // Stop chaos mode
            if (chaosActive.compareAndSet(true, false)) {
                if (chaosScheduler != null) {
                    chaosScheduler.shutdownNow();
                    chaosScheduler = null;
                }
            }
            
            // Cleanup perlin modulator
            if (perlinModulator != null) {
                perlinModulator.cleanup();
            }
            
            // Stop audio engine
            audioEngine.stop();
            
            // Shutdown background executor
            if (backgroundExecutor != null) {
                backgroundExecutor.shutdownNow();
            }
            
        } catch (Exception e) {
            if (DEBUG) System.err.println("DEBUG: Error during cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Get AudioEngine instance for external access
     * @return AudioEngine instance
     */
    public AudioEngine getAudioEngine() {
        return audioEngine;
    }
    
    /**
     * Get effects controller
     * @return Effects controller instance
     */
    public EffectsController getEffectsController() {
        return effectsController;
    }
}
