package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo overdrive effect
 * Implements soft clipping and tone control functionality
 */
public class OverdriveEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "overdrive";
    private static final String EFFECT_NAME = "Overdrive";
    
    // Default parameters
    private static final float DEFAULT_DRIVE = 4.0f;         // 4x gain
    private static final float DEFAULT_TONE = 0.6f;          // 60% brightness
    private static final float DEFAULT_WET = 0.35f;          // 35%
    
    // Parameter ranges
    private static final float MIN_DRIVE = 1.0f;             // 1x gain
    private static final float MAX_DRIVE = 20.0f;            // 20x gain
    private static final float MIN_TONE = 0.0f;              // 0% brightness
    private static final float MAX_TONE = 1.0f;               // 100% brightness
    private static final float MIN_WET = 0.0f;               // 0%
    private static final float MAX_WET = 1.0f;               // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float drive = DEFAULT_DRIVE;
    private volatile float tone = DEFAULT_TONE;
    
    // Low-pass filter state (for tone control)
    private float lowpassStateL = 0.0f;
    private float lowpassStateR = 0.0f;
    private float lowpassCoeff = 0.0f;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public OverdriveEffect() {
        // Initialize default parameters
        parameters.put("drive", DEFAULT_DRIVE);
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
        float currentTone = this.tone;
        float currentWet = this.wetDryRatio;
        float currentLowpassCoeff = this.lowpassCoeff;
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Apply overdrive effect
            float processedSampleL = processSample(inputSample, true, currentDrive, currentLowpassCoeff);
            float processedSampleR = processSample(inputSample, false, currentDrive, currentLowpassCoeff);
            
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
     * Process single sample (overdrive + tone control)
     */
    private float processSample(float input, boolean isLeft, float drive, float lowpassCoeff) {
        // 1. Input gain (Drive)
        float gainedInput = input * drive;
        
        // 2. Soft clipping (using tanh function)
        float clipped = softClip(gainedInput);
        
        // 3. Output gain compensation (avoid signal being too small)
        float compensated = clipped / drive;
        
        // 4. Tone control (low-pass filter)
        float filtered = applyToneFilter(compensated, isLeft, lowpassCoeff);
        
        return filtered;
    }
    
    /**
     * Soft clipping function (using tanh)
     */
    private float softClip(float input) {
        // Use tanh function to implement soft clipping
        // tanh(x) = (e^x - e^(-x)) / (e^x + e^(-x))
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
        float cutoffFreq = 2000.0f + tone * 8000.0f; // 2kHz - 10kHz
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
