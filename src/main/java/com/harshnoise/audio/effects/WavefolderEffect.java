package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo wavefolder effect
 * Implements threshold reflection folding and tone control functionality
 */
public class WavefolderEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "wavefolder";
    private static final String EFFECT_NAME = "Wavefolder";
    
    // Default parameters
    private static final float DEFAULT_FOLD = 3.0f;           // 3x fold strength
    private static final float DEFAULT_SYMMETRY = 0.0f;       // 0% symmetry
    private static final float DEFAULT_TONE = 0.6f;           // 60% brightness
    private static final float DEFAULT_WET = 0.30f;           // 30%
    
    // Parameter ranges
    private static final float MIN_FOLD = 0.0f;               // 0x fold
    private static final float MAX_FOLD = 10.0f;              // 10x fold
    private static final float MIN_SYMMETRY = -1.0f;          // -100% bias
    private static final float MAX_SYMMETRY = 1.0f;           // +100% bias
    private static final float MIN_TONE = 0.0f;                // 0% brightness
    private static final float MAX_TONE = 1.0f;                // 100% brightness
    private static final float MIN_WET = 0.0f;                // 0%
    private static final float MAX_WET = 1.0f;                // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float fold = DEFAULT_FOLD;
    private volatile float symmetry = DEFAULT_SYMMETRY;
    private volatile float tone = DEFAULT_TONE;
    
    // Low-pass filter state (for tone control)
    private float lowpassStateL = 0.0f;
    private float lowpassStateR = 0.0f;
    private float lowpassCoeff = 0.0f;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public WavefolderEffect() {
        // Initialize default parameters
        parameters.put("fold", DEFAULT_FOLD);
        parameters.put("symmetry", DEFAULT_SYMMETRY);
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
            case "fold":
                this.fold = clamp(value, MIN_FOLD, MAX_FOLD);
                parameters.put("fold", this.fold);
                break;
            case "symmetry":
                this.symmetry = clamp(value, MIN_SYMMETRY, MAX_SYMMETRY);
                parameters.put("symmetry", this.symmetry);
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
        float currentFold = this.fold;
        float currentSymmetry = this.symmetry;
        float currentTone = this.tone;
        float currentWet = this.wetDryRatio;
        float currentLowpassCoeff = this.lowpassCoeff;
        
        // Calculate equal power mixing gains (improved version)
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Add crossfade to avoid level jumps
        float crossfade = 0.1f; // 10% crossfade
        dryGain *= (1.0f - crossfade);
        wetGain *= (1.0f + crossfade);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Apply wavefolder effect
            float processedSampleL = processSample(inputSample, true, currentFold, currentSymmetry, currentLowpassCoeff);
            float processedSampleR = processSample(inputSample, false, currentFold, currentSymmetry, currentLowpassCoeff);
            
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
     * Process single sample (wavefolder + tone control)
     */
    private float processSample(float input, boolean isLeft, float fold, float symmetry, float lowpassCoeff) {
        // 1. Pre-gain (Fold) - enhance gain effect
        float gainedInput = input * (1.0f + fold * 2.0f);
        
        // 2. Asymmetric bias (Symmetry) - enhance bias effect
        float biasedInput = gainedInput + symmetry * 1.5f;
        
        // 3. Wavefolder processing
        float folded = applyWavefolding(biasedInput, fold, symmetry);
        
        // 4. Output gain compensation (avoid signal being too large or too small)
        float compensated = folded / (1.0f + fold * 1.5f);
        
        // 5. Tone control (low-pass filter)
        float filtered = applyToneFilter(compensated, isLeft, lowpassCoeff);
        
        return filtered;
    }
    
    /**
     * Apply wavefolder (further improved threshold reflection folding algorithm)
     */
    private float applyWavefolding(float input, float fold, float symmetry) {
        // Clamp input to [-1, 1] range
        input = clamp(input, -1.0f, 1.0f);
        
        // Further improved threshold reflection folding algorithm
        float threshold = 0.4f; // Further reduce folding threshold, easier to trigger folding
        
        // Adjust folding threshold based on symmetry
        float symmetryThreshold = threshold * (1.0f + Math.abs(symmetry) * 0.5f);
        
        float folded = input;
        
        // Dynamically adjust reflection count based on Fold parameter
        int maxReflections = Math.max(3, (int)(fold * 2));
        for (int i = 0; i < maxReflections; i++) {
            if (Math.abs(folded) > symmetryThreshold) {
                // Calculate excess beyond threshold
                float excess = Math.abs(folded) - symmetryThreshold;
                
                // Reflect around threshold, add some randomness
                float reflectionFactor = 1.0f + (float)(Math.random() - 0.5) * 0.2f;
                if (folded > 0) {
                    folded = symmetryThreshold - excess * reflectionFactor;
                } else {
                    folded = -symmetryThreshold + excess * reflectionFactor;
                }
                
                // Clamp to [-1, 1] range
                folded = clamp(folded, -1.0f, 1.0f);
            }
        }
        
        // Enhance harmonic enhancement effect
        float harmonic = (float) Math.sin(folded * Math.PI * 2) * 0.3f;
        folded += harmonic;
        
        // Add more "glitch" feel
        float noise = (float) (Math.random() - 0.5) * 0.1f * fold;
        folded += noise;
        
        // Add second harmonic
        float secondHarmonic = (float) Math.sin(folded * Math.PI * 4) * 0.15f;
        folded += secondHarmonic;
        
        // Final clamp
        return clamp(folded, -1.0f, 1.0f);
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
        // tone = 0: complete low-pass (dark, suppress folding harmonics)
        // tone = 1: no filtering (bright, preserve folding harmonics)
        float cutoffFreq = 800.0f + tone * 12000.0f; // 800Hz - 12.8kHz, wider frequency range
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
