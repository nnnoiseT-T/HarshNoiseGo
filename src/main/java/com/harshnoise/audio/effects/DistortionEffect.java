package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo distortion effect
 * Implements hard clipping, soft clipping, and tone control functionality
 */
public class DistortionEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "distortion";
    private static final String EFFECT_NAME = "Distortion";
    
    // Default parameters
    private static final float DEFAULT_DRIVE = 6.0f;         // 6x gain
    private static final float DEFAULT_SHAPE = 0.5f;         // 50% soft clipping
    private static final float DEFAULT_TONE = 0.55f;         // 55% brightness
    private static final float DEFAULT_WET = 0.35f;          // 35%
    
    // Parameter ranges
    private static final float MIN_DRIVE = 1.0f;             // 1x gain
    private static final float MAX_DRIVE = 20.0f;            // 20x gain
    private static final float MIN_SHAPE = 0.0f;             // 0% soft clipping (hard clipping)
    private static final float MAX_SHAPE = 1.0f;              // 100% soft clipping
    private static final float MIN_TONE = 0.0f;               // 0% brightness
    private static final float MAX_TONE = 1.0f;               // 100% brightness
    private static final float MIN_WET = 0.0f;                // 0%
    private static final float MAX_WET = 1.0f;                // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float drive = DEFAULT_DRIVE;
    private volatile float shape = DEFAULT_SHAPE;
    private volatile float tone = DEFAULT_TONE;
    
    // Low-pass filter state (for tone control)
    private float lowpassStateL = 0.0f;
    private float lowpassStateR = 0.0f;
    private float lowpassCoeff = 0.0f;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public DistortionEffect() {
        // Initialize default parameters
        parameters.put("drive", DEFAULT_DRIVE);
        parameters.put("shape", DEFAULT_SHAPE);
        parameters.put("tone", DEFAULT_TONE);
        parameters.put("wet", DEFAULT_WET);
        
        // Initialize low-pass filter coefficient
        updateLowpassCoeff();
    }
    
    @Override
    public String getId() {
        return EFFECT_ID;
    }
    
    @Override
    public String getName() {
        return EFFECT_NAME;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setParameter(String parameterName, float value) {
        switch (parameterName) {
            case "drive":
                this.drive = clamp(value, MIN_DRIVE, MAX_DRIVE);
                parameters.put("drive", this.drive);
                break;
            case "shape":
                this.shape = clamp(value, MIN_SHAPE, MAX_SHAPE);
                parameters.put("shape", this.shape);
                break;
            case "tone":
                this.tone = clamp(value, MIN_TONE, MAX_TONE);
                parameters.put("tone", this.tone);
                updateLowpassCoeff();
                break;
            case "wet":
            case "mix":
                this.wetDryRatio = clamp(value, MIN_WET, MAX_WET);
                parameters.put("wet", this.wetDryRatio);
                break;
            default:
                parameters.put(parameterName, value);
                break;
        }
    }
    
    @Override
    public float getParameter(String parameterName) {
        return parameters.getOrDefault(parameterName, 0.0f);
    }
    
    @Override
    public void setWetDryRatio(float wetDryRatio) {
        this.wetDryRatio = clamp(wetDryRatio, MIN_WET, MAX_WET);
        parameters.put("wet", this.wetDryRatio);
    }
    
    @Override
    public float getWetDryRatio() {
        return wetDryRatio;
    }
    
    @Override
    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
        updateLowpassCoeff();
    }
    
    @Override
    public float getSampleRate() {
        return sampleRate;
    }
    
    @Override
    public void process(float[] inputBuffer, float[] outputBuffer, int bufferSize) {
        if (!enabled) {
            // If disabled, copy input to output directly
            System.arraycopy(inputBuffer, 0, outputBuffer, 0, bufferSize);
            return;
        }
        
        // Get current parameter snapshot (thread-safe)
        float currentDrive = this.drive;
        float currentShape = this.shape;
        float currentTone = this.tone;
        float currentWet = this.wetDryRatio;
        float currentLowpassCoeff = this.lowpassCoeff;
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Apply distortion effect
            float processedSampleL = processSample(inputSample, true, currentDrive, currentShape, currentLowpassCoeff);
            float processedSampleR = processSample(inputSample, false, currentDrive, currentShape, currentLowpassCoeff);
            
            // Calculate output (equal power mixing)
            float drySignal = inputSample * dryGain;
            float wetSignalL = processedSampleL * wetGain;
            float wetSignalR = processedSampleR * wetGain;
            
            // Stereo output (simplified to mono mix)
            float wetSignal = (wetSignalL + wetSignalR) * 0.5f;
            outputBuffer[i] = drySignal + wetSignal;
        }
    }
    
    @Override
    public void reset() {
        // Reset low-pass filter state
        lowpassStateL = 0.0f;
        lowpassStateR = 0.0f;
    }
    
    /**
     * Process single sample (distortion + tone control)
     */
    private float processSample(float input, boolean isLeft, float drive, float shape, float lowpassCoeff) {
        // 1. Input gain (Drive)
        float gainedInput = input * drive;
        
        // 2. Distortion processing (mix of hard clipping and soft clipping)
        float distorted = applyDistortion(gainedInput, shape);
        
        // 3. Output gain compensation (avoid signal being too large or too small)
        float compensated = distorted / drive;
        
        // 4. Tone control (low-pass filter)
        float filtered = applyToneFilter(compensated, isLeft, lowpassCoeff);
        
        return filtered;
    }
    
    /**
     * Apply distortion (mix of hard clipping and soft clipping)
     */
    private float applyDistortion(float input, float shape) {
        // Clamp input to [-1, 1] range
        input = clamp(input, -1.0f, 1.0f);
        
        // Calculate mix of hard clipping and soft clipping
        float hardClipped = hardClip(input);
        float softClipped = softClip(input);
        
        // Mix two types of clipping based on shape parameter
        return hardClipped * (1.0f - shape) + softClipped * shape;
    }
    
    /**
     * Hard clipping function
     */
    private float hardClip(float input) {
        // Hard clipping: directly truncate to ±1
        return clamp(input, -1.0f, 1.0f);
    }
    
    /**
     * Soft clipping function (using tanh)
     */
    private float softClip(float input) {
        // Use tanh function to implement soft clipping
        return (float) Math.tanh(input);
    }
    
    /**
     * Apply tone filter
     */
    private float applyToneFilter(float input, boolean isLeft, float coeff) {
        float state = isLeft ? lowpassStateL : lowpassStateR;
        
        // First-order low-pass filter
        float filtered = state + coeff * (input - state);
        
        // Update state
        if (isLeft) {
            lowpassStateL = filtered;
        } else {
            lowpassStateR = filtered;
        }
        
        return filtered;
    }
    
    /**
     * Update low-pass filter coefficient
     */
    private void updateLowpassCoeff() {
        // Calculate low-pass filter coefficient based on tone parameter
        // tone = 0: complete low-pass (dark)
        // tone = 1: no filtering (bright)
        float cutoffFreq = 1500.0f + tone * 12000.0f; // 1.5kHz - 13.5kHz
        float normalizedFreq = cutoffFreq / sampleRate;
        
        // First-order low-pass filter coefficient
        lowpassCoeff = normalizedFreq / (1.0f + normalizedFreq);
        
        // Limit coefficient range
        lowpassCoeff = clamp(lowpassCoeff, 0.0f, 0.99f);
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
