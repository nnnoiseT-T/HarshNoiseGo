package com.harshnoise;

import javafx.animation.AnimationTimer;
import com.harshnoise.model.AudioModel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PerlinModulator - Advanced fBM-based parameter modulation system
 * Features: time warping, slew limiting, jitter, edge behaviors, lock-free design
 */
public class PerlinModulator {
    
    private final PerlinNoise perlinNoise;
    private final AudioModel model;
    private AnimationTimer animationTimer;
    private boolean isActive = false;
    
    // Time tracking for smooth animation
    private long startTime;
    private double globalTimeScale = 0.004; // ~4x faster perceived motion
    
    // Time warping noise generator
    private final PerlinNoise warpNoise;
    
    // Parameter configurations
    private final Map<String, PerlinParameterConfig> parameterConfigs;
    
    // Lock-free parameter values using atomic references
    private final AtomicReference<Double> volumeValue = new AtomicReference<>(0.7);
    private final AtomicReference<Double> toneValue = new AtomicReference<>(0.5);
    private final AtomicReference<Double> distortionValue = new AtomicReference<>(0.0);
    private final AtomicReference<Double> filterValue = new AtomicReference<>(0.0);
    private final AtomicReference<Double> grainSizeValue = new AtomicReference<>(64.0);
    
    // Previous values for slew limiting
    private final Map<String, Double> previousValues = new HashMap<>();
    
    // Background computation service
    private ScheduledExecutorService computationService;
    
    // Computation frequency (Hz)
    private static final int COMPUTATION_FREQUENCY = 150; // 150 FPS for ultra-smooth updates
    
    // ===== Enhancement defaults =====
    private static final double DEPTH_VOLUME     = 0.0;  // volume is typically locked
    private static final double DEPTH_TONE       = 0.85;
    private static final double DEPTH_DIST       = 0.90;
    private static final double DEPTH_FILTER     = 0.85;
    private static final double DEPTH_GRAIN      = 0.85;

    private static final double GAMMA_TONE       = 1.6;  // >1 increases edge contrast
    private static final double GAMMA_DIST       = 1.8;
    private static final double GAMMA_FILTER     = 1.6;
    private static final double GAMMA_GRAIN      = 1.4;
    
    public PerlinModulator(AudioModel model) {
        this.perlinNoise = new PerlinNoise();
        this.warpNoise = new PerlinNoise(System.currentTimeMillis() + 1); // Different seed
        this.model = model;
        this.parameterConfigs = new HashMap<>();
        this.computationService = null; // Will be created when needed
        
        initializeParameterConfigs();
        setupAnimationTimer();
        initializePreviousValues();
    }
    
    /**
     * Initialize default parameter configurations with random offsets
     */
    private void initializeParameterConfigs() {
        // Create configurations
        PerlinParameterConfig volumeConfig = PerlinParameterConfig.createVolumePreset();
        PerlinParameterConfig toneConfig = PerlinParameterConfig.createTonePreset();
        PerlinParameterConfig distortionConfig = PerlinParameterConfig.createDistortionPreset();
        PerlinParameterConfig filterConfig = PerlinParameterConfig.createFilterPreset();
        PerlinParameterConfig grainSizeConfig = PerlinParameterConfig.createGrainSizePreset();
        
        // Randomize offsets for phase shifting
        volumeConfig.randomizeOffset();
        toneConfig.randomizeOffset();
        distortionConfig.randomizeOffset();
        filterConfig.randomizeOffset();
        grainSizeConfig.randomizeOffset();
        
        // Store configurations
        parameterConfigs.put("volume", volumeConfig);
        parameterConfigs.put("tone", toneConfig);
        parameterConfigs.put("distortion", distortionConfig);
        parameterConfigs.put("filter", filterConfig);
        parameterConfigs.put("grainSize", grainSizeConfig);
    }
    
    /**
     * Initialize previous values for slew limiting
     */
    private void initializePreviousValues() {
        previousValues.put("volume", 0.7);
        previousValues.put("tone", 0.0);
        previousValues.put("distortion", 0.0);
        previousValues.put("filter", 0.0);
        previousValues.put("grainSize", 1.0);
    }
    
    /**
     * Setup the animation timer for UI updates (non-blocking)
     */
    private void setupAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isActive) {
                    updateUIFromComputedValues();
                }
            }
        };
    }
    
    /**
     * Start Perlin noise modulation
     */
    public void start() {
        if (!isActive) {
            isActive = true;
            startTime = System.currentTimeMillis();
            
            // Start background computation
            startBackgroundComputation();
            
            // Start UI updates
            animationTimer.start();
            
            // Notify model of state change
            model.setPerlinNoiseActive(true);
            
            System.out.println("🎵 Advanced fBM Perlin Noise modulation started");
        }
    }
    
    /**
     * Stop Perlin noise modulation
     */
    public void stop() {
        if (isActive) {
            isActive = false;
            
            // Stop background computation
            stopBackgroundComputation();
            
            // Stop UI updates
            animationTimer.stop();
            
            // Notify model of state change
            model.setPerlinNoiseActive(false);
            
            System.out.println("⏹️ Advanced fBM Perlin Noise modulation stopped");
        }
    }
    
    /**
     * Toggle Perlin noise modulation
     */
    public void toggle() {
        if (isActive) {
            stop();
        } else {
            start();
        }
    }
    
    /**
     * Check if modulation is active
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Start background computation service
     */
    private void startBackgroundComputation() {
        // Create a new computation service each time to avoid issues with terminated pools
        if (computationService != null && !computationService.isShutdown()) {
            computationService.shutdown();
        }
        
        // Create new service
        ScheduledExecutorService newService = Executors.newScheduledThreadPool(1);
        
        newService.scheduleAtFixedRate(
            this::computeNewValues,
            0,
            1000 / COMPUTATION_FREQUENCY,
            TimeUnit.MILLISECONDS
        );
        
        // Update the reference
        this.computationService = newService;
    }
    
    /**
     * Stop background computation service
     */
    private void stopBackgroundComputation() {
        if (computationService != null && !computationService.isShutdown()) {
            computationService.shutdown();
            try {
                // Wait a bit for tasks to complete
                if (!computationService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    computationService.shutdownNow();
                }
            } catch (InterruptedException e) {
                computationService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Compute new parameter values using fBM with time warping
     */
    private void computeNewValues() {
        if (!isActive) return;
        
        double currentTime = (System.currentTimeMillis() - startTime) * globalTimeScale;
        
        // Generate time warping noise (slower layer) - extreme warping for full range
        double warpNoiseValue = warpNoise.fbm(currentTime * 0.5) * 2.0; // Increased from 0.4*1.5
        
        // Compute each parameter
        computeParameter("volume", currentTime, warpNoiseValue);
        computeParameter("tone", currentTime, warpNoiseValue);
        computeParameter("distortion", currentTime, warpNoiseValue);
        computeParameter("filter", currentTime, warpNoiseValue);
        computeParameter("grainSize", currentTime, warpNoiseValue);
    }
    
    /**
     * Compute a single parameter value using the complete pipeline
     */
    private void computeParameter(String paramName, double baseTime, double warpNoise) {
        PerlinParameterConfig config = parameterConfigs.get(paramName);
        if (config == null) return;
        
        // 1. Generate warped time
        double warpedTime = config.getEffectiveTime(baseTime, warpNoise);
        
        // 2. Generate fBM noise: raw = fBM(t*speed + slowNoise) ∈ [-1,1]
        double rawNoise = perlinNoise.fbm(warpedTime, config.getOctaves(), 
                                        config.getPersistence(), config.getLacunarity());
        
        // Optional: Add turbulence blend for tone/filter edge presence
        if ("filter".equals(paramName) || "tone".equals(paramName)) {
            double edge = perlinNoise.turbulence(warpedTime * 0.8,
                            Math.max(2, config.getOctaves() - 1),
                            Math.min(0.7, config.getPersistence() + 0.1),
                            config.getLacunarity());
            rawNoise = 0.7 * rawNoise + 0.3 * (edge * 2.0 - 1.0); // re-center after blend
        }
        
        // 3. Convert to [0,1]: x = (raw + 1) / 2
        double normalized = (rawNoise + 1.0) / 2.0;
        
        // 3.5) Enhanced range expansion - force wider movement
        double depth = 1.0;
        double gamma = 1.0;
        switch (paramName) {
            case "tone":       depth = DEPTH_TONE;   gamma = GAMMA_TONE;   break;
            case "distortion": depth = DEPTH_DIST;   gamma = GAMMA_DIST;   break;
            case "filter":     depth = DEPTH_FILTER; gamma = GAMMA_FILTER; break;
            case "grainSize":  depth = DEPTH_GRAIN;  gamma = GAMMA_GRAIN;  break;
            default: break; // volume/unknown stays default
        }
        
        // Compute centered original in [-1,1]
        double centered = (normalized - 0.5) * 2.0;
        
        // Sign-preserving power curve
        double shaped = Math.copySign(Math.pow(Math.abs(centered), gamma), centered);
        
        // Mix original with shaped by depth (0..1)
        double mixed = (1.0 - depth) * centered + depth * shaped;
        
        // CRITICAL: Force full range utilization with extreme expansion
        // Scale up the mixed signal to use the full [-1,1] range
        double expanded = mixed * 3.0; // Extreme expansion to force full range
        expanded = Math.max(-1.0, Math.min(1.0, expanded)); // Clamp to [-1,1]
        
        // Apply additional range stretching to ensure 0-1 coverage
        // Map [-1,1] to [0,1] with additional stretching
        double stretched = (expanded + 1.0) / 2.0; // Normal [0,1] mapping
        stretched = Math.pow(stretched, 0.7); // Power curve to push values toward extremes
        stretched = stretched * 1.2 - 0.1; // Scale and shift to use more range
        normalized = Math.max(0.0, Math.min(1.0, stretched)); // Final clamp
        
        // Special handling for filter parameter to avoid silence
        if (paramName.equals("filter")) {
            normalized = normalized * 0.8; // Limit filter to 0.0-0.8 range
        }
        
        // 4. Map to [min,max] range
        double mappedValue = config.getMinValue() + normalized * config.getRange();
        
        // 5. Add extreme jitter (small fast noise) - extremely lively but smooth
        double jitter = (perlinNoise.noise(baseTime * 25.0) * Math.max(0.08, config.getJitter())); // Increased from 20.0 and 0.05
        mappedValue += jitter;
        
        // Optional: add massive slow LFO drift for non-grain params to ensure full range coverage:
        if (!"grainSize".equals(paramName)) {
            double lfo = 0.15 * Math.sin(baseTime * 0.15 + config.getOffset()); // ±0.15 (massive increase), slower frequency
            mappedValue += lfo;
        }
        
        // 6. Apply edge behavior
        mappedValue = config.applyEdgeBehavior(mappedValue);
        
        // 7. Apply slew limiting with relaxed limits for perceptual parameters
        double previousValue = previousValues.get(paramName);
        double maxChange = config.getSlewRate() / COMPUTATION_FREQUENCY;
        
        // Allow extremely fast reach for perceptual parameters to ensure full range coverage:
        if ("tone".equals(paramName) || "distortion".equals(paramName) || "filter".equals(paramName)) {
            maxChange *= 5.0; // Increased from 4.0 for maximum audible movement
        }
        
        double slewLimitedValue = applySlewLimiting(mappedValue, previousValue, maxChange);
        
        // 8. Store new value
        previousValues.put(paramName, slewLimitedValue);
        
        // 9. Update atomic reference
        updateAtomicValue(paramName, slewLimitedValue);
        
        // Temporary debug logging (remove after test)
        if (System.currentTimeMillis() % 1000 < 50) { // Log once per second
            if ("tone".equals(paramName) || "distortion".equals(paramName) || "filter".equals(paramName)) {
                System.out.printf("DEBUG: %s - normalized: %.3f, mapped: %.3f%n", 
                    paramName, normalized, slewLimitedValue);
            }
        }
    }
    
    /**
     * Apply slew limiting to prevent excessive parameter changes
     */
    private double applySlewLimiting(double newValue, double previousValue, double maxChange) {
        double difference = newValue - previousValue;
        if (Math.abs(difference) > maxChange) {
            if (difference > 0) {
                return previousValue + maxChange;
            } else {
                return previousValue - maxChange;
            }
        }
        return newValue;
    }
    
    /**
     * Update atomic reference for a parameter
     */
    private void updateAtomicValue(String paramName, double value) {
        switch (paramName) {
            case "volume":
                volumeValue.set(value);
                break;
            case "tone":
                toneValue.set(value);
                break;
            case "distortion":
                distortionValue.set(value);
                break;
            case "filter":
                filterValue.set(value);
                break;
            case "grainSize":
                grainSizeValue.set(value);
                break;
        }
    }
    
    /**
     * Update UI from computed values (non-blocking)
     */
    private void updateUIFromComputedValues() {
        // Update model with current computed values
        // Only update volume if not locked
        if (!model.isVolumeLocked()) {
            model.setVolume(volumeValue.get());
        }
        model.setTone(toneValue.get());
        model.setDistortionLevel(distortionValue.get());
        model.setFilterLevel(filterValue.get());
        model.setGrainSize((int) Math.round(grainSizeValue.get()));
    }
    
    /**
     * Get current computed value for a parameter (lock-free)
     */
    public double getCurrentValue(String paramName) {
        switch (paramName) {
            case "volume":
                return volumeValue.get();
            case "tone":
                return toneValue.get();
            case "distortion":
                return distortionValue.get();
            case "filter":
                return filterValue.get();
            case "grainSize":
                return grainSizeValue.get();
            default:
                return 0.0;
        }
    }
    
    /**
     * Set global animation speed (time scale)
     */
    public void setGlobalTimeScale(double timeScale) {
        this.globalTimeScale = timeScale;
    }
    
    /**
     * Get current global time scale
     */
    public double getGlobalTimeScale() {
        return globalTimeScale;
    }
    
    /**
     * Update parameter configuration
     */
    public void updateParameterConfig(String paramName, PerlinParameterConfig config) {
        parameterConfigs.put(paramName, config);
    }
    
    /**
     * Get parameter configuration
     */
    public PerlinParameterConfig getParameterConfig(String paramName) {
        return parameterConfigs.get(paramName);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        stop();
        if (computationService != null && !computationService.isShutdown()) {
            computationService.shutdown();
        }
    }
}
