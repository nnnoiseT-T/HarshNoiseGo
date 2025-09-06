package com.harshnoise;

import java.util.Random;

/**
 * PerlinParameterConfig - Enhanced configuration for fBM-based parameter modulation
 * Supports time warping, edge behaviors, slew limiting, and jitter
 */
public class PerlinParameterConfig {
    
    // Parameter range configuration
    private double minValue;
    private double maxValue;
    private double offset;
    
    // Speed and timing configuration
    private double speed;
    private double timeOffset;
    
    // fBM noise characteristics
    private int octaves;
    private double persistence;
    private double lacunarity;
    
    // Advanced modulation features
    private EdgeBehavior edgeBehavior;
    private double slewRate; // Maximum change per second
    private double jitter; // Small random noise amplitude
    
    // Time warping configuration
    private double warpSpeed; // Speed of time warping noise
    private double warpAmplitude; // Amplitude of time warping
    
    public enum EdgeBehavior {
        CLAMP,    // Clamp values to [min, max]
        REFLECT,  // Reflect values off boundaries
        WRAP      // Wrap values around range
    }
    
    /**
     * Constructor with default values
     */
    public PerlinParameterConfig() {
        this(0.0, 1.0, 0.0, 1.0, 0.0, 3, 0.55, 2.0, 
             EdgeBehavior.CLAMP, 0.1, 0.02, 0.5, 0.1);
    }
    
    /**
     * Constructor with custom values
     */
    public PerlinParameterConfig(double minValue, double maxValue, double offset, 
                                double speed, double timeOffset, int octaves, 
                                double persistence, double lacunarity, 
                                EdgeBehavior edgeBehavior, double slewRate, 
                                double jitter, double warpSpeed, double warpAmplitude) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.offset = offset;
        this.speed = speed;
        this.timeOffset = timeOffset;
        this.octaves = octaves;
        this.persistence = persistence;
        this.lacunarity = lacunarity;
        this.edgeBehavior = edgeBehavior;
        this.slewRate = slewRate;
        this.jitter = jitter;
        this.warpSpeed = warpSpeed;
        this.warpAmplitude = warpAmplitude;
    }
    
    // Getters and setters
    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }
    
    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
    
    public double getOffset() { return offset; }
    public void setOffset(double offset) { this.offset = offset; }
    
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    
    public double getTimeOffset() { return timeOffset; }
    public void setTimeOffset(double timeOffset) { this.timeOffset = timeOffset; }
    
    public int getOctaves() { return octaves; }
    public void setOctaves(int octaves) { this.octaves = octaves; }
    
    public double getPersistence() { return persistence; }
    public void setPersistence(double persistence) { this.persistence = persistence; }
    
    public double getLacunarity() { return lacunarity; }
    public void setLacunarity(double lacunarity) { this.lacunarity = lacunarity; }
    
    public EdgeBehavior getEdgeBehavior() { return edgeBehavior; }
    public void setEdgeBehavior(EdgeBehavior edgeBehavior) { this.edgeBehavior = edgeBehavior; }
    
    public double getSlewRate() { return slewRate; }
    public void setSlewRate(double slewRate) { this.slewRate = slewRate; }
    
    public double getJitter() { return jitter; }
    public void setJitter(double jitter) { this.jitter = jitter; }
    
    public double getWarpSpeed() { return warpSpeed; }
    public void setWarpSpeed(double warpSpeed) { this.warpSpeed = warpSpeed; }
    
    public double getWarpAmplitude() { return warpAmplitude; }
    public void setWarpAmplitude(double warpAmplitude) { this.warpAmplitude = warpAmplitude; }
    
    /**
     * Get the effective time value for this parameter with time warping
     */
    public double getEffectiveTime(double baseTime, double warpNoise) {
        double warpedTime = baseTime * speed + timeOffset + warpNoise * warpAmplitude;
        return warpedTime;
    }
    
    /**
     * Get the effective range (max - min)
     */
    public double getRange() {
        return maxValue - minValue;
    }
    
    /**
     * Get the center value of the range
     */
    public double getCenter() {
        return (minValue + maxValue) / 2.0;
    }
    
    /**
     * Apply edge behavior to a value
     */
    public double applyEdgeBehavior(double value) {
        double range = getRange();
        double min = getMinValue();
        double max = getMaxValue();
        
        switch (edgeBehavior) {
            case CLAMP:
                return Math.max(min, Math.min(max, value));
            case REFLECT:
                if (value < min) {
                    return min + (min - value);
                } else if (value > max) {
                    return max - (value - max);
                }
                return value;
            case WRAP:
                if (value < min) {
                    return max - (min - value) % range;
                } else if (value > max) {
                    return min + (value - max) % range;
                }
                return value;
            default:
                return value;
        }
    }
    
    /**
     * Create a preset configuration for Volume parameter
     */
    public static PerlinParameterConfig createVolumePreset() {
        return new PerlinParameterConfig(0.1, 1.0, 0.0, 0.8, 1000, 3, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.15, 0.02, 0.3, 0.2);
    }
    
    /**
     * Create a preset configuration for Tone parameter
     */
    public static PerlinParameterConfig createTonePreset() {
        return new PerlinParameterConfig(0.0, 1.0, 0.0, 0.15, 5000, 3, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.1, 0.02, 0.2, 0.1);
    }
    
    /**
     * Create a preset configuration for Distortion parameter
     */
    public static PerlinParameterConfig createDistortionPreset() {
        return new PerlinParameterConfig(0.0, 1.0, 0.0, 1.2, 2000, 3, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.2, 0.03, 0.4, 0.15);
    }
    
    /**
     * Create a preset configuration for Filter parameter
     */
    public static PerlinParameterConfig createFilterPreset() {
        return new PerlinParameterConfig(0.0, 1.0, 0.0, 0.6, 3000, 4, 0.55, 2.0, 
                                       EdgeBehavior.REFLECT, 0.1, 0.01, 0.2, 0.25);
    }
    
    /**
     * Create a preset configuration for Grain Size parameter
     */
    public static PerlinParameterConfig createGrainSizePreset() {
        return new PerlinParameterConfig(1, 128, 0.0, 0.4, 4000, 3, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.3, 0.02, 0.1, 0.1);
    }
    
    /**
     * Create alternative preset configurations for more variety
     */
    public static PerlinParameterConfig createVolumePreset2() {
        return new PerlinParameterConfig(0.05, 0.95, 0.1, 1.1, 1500, 4, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.12, 0.025, 0.35, 0.18);
    }
    
    public static PerlinParameterConfig createDistortionPreset2() {
        return new PerlinParameterConfig(0.1, 0.9, -0.05, 0.9, 2500, 3, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.18, 0.035, 0.45, 0.12);
    }
    
    public static PerlinParameterConfig createFilterPreset2() {
        return new PerlinParameterConfig(0.05, 0.85, 0.15, 0.7, 3500, 5, 0.55, 2.0, 
                                       EdgeBehavior.REFLECT, 0.08, 0.015, 0.25, 0.22);
    }
    
    public static PerlinParameterConfig createGrainSizePreset2() {
        return new PerlinParameterConfig(2, 96, 0.0, 0.5, 4500, 3, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.25, 0.025, 0.12, 0.08);
    }
    
    /**
     * Create wild preset configurations for extreme variety
     */
    public static PerlinParameterConfig createVolumeWild() {
        return new PerlinParameterConfig(0.01, 1.0, 0.0, 1.5, 500, 5, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.25, 0.04, 0.6, 0.4);
    }
    
    public static PerlinParameterConfig createDistortionWild() {
        return new PerlinParameterConfig(0.0, 1.0, 0.0, 2.0, 1000, 4, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.3, 0.05, 0.7, 0.3);
    }
    
    public static PerlinParameterConfig createFilterWild() {
        return new PerlinParameterConfig(0.0, 1.0, 0.0, 0.3, 4000, 6, 0.55, 2.0, 
                                       EdgeBehavior.REFLECT, 0.06, 0.02, 0.15, 0.35);
    }
    
    public static PerlinParameterConfig createGrainSizeWild() {
        return new PerlinParameterConfig(1, 128, 0.0, 0.8, 2000, 4, 0.55, 2.0, 
                                       EdgeBehavior.CLAMP, 0.4, 0.03, 0.2, 0.15);
    }
    
    /**
     * Randomize offset for phase shifting
     */
    public void randomizeOffset() {
        Random random = new Random();
        this.offset = random.nextDouble() * 2.0 - 1.0; // [-1, 1]
    }
}
