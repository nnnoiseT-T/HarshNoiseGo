package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder test effect
 * Does not change sound, used for verifying effect chain
 */
public class BypassEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "bypass";
    private static final String EFFECT_NAME = "Bypass";
    
    private boolean enabled = true;
    private float wetDryRatio = 1.0f;
    private float sampleRate = 44100.0f;
    private final Map<String, Float> parameters = new HashMap<>();
    
    public BypassEffect() {
        // Initialize default parameters
        parameters.put("gain", 1.0f);
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
        parameters.put(parameterName, value);
    }
    
    @Override
    public float getParameter(String parameterName) {
        return parameters.getOrDefault(parameterName, 0.0f);
    }
    
    @Override
    public void setWetDryRatio(float wetDryRatio) {
        this.wetDryRatio = Math.max(0.0f, Math.min(1.0f, wetDryRatio));
    }
    
    @Override
    public float getWetDryRatio() {
        return wetDryRatio;
    }
    
    @Override
    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
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
        
        float gain = getParameter("gain");
        
        // Apply Wet/Dry mixing
        for (int i = 0; i < bufferSize; i++) {
            float drySignal = inputBuffer[i];
            float wetSignal = drySignal * gain; // Bypass effect actually doesn't change the signal
            outputBuffer[i] = drySignal * (1.0f - wetDryRatio) + wetSignal * wetDryRatio;
        }
    }
    
    @Override
    public void reset() {
        // Bypass effect has no internal state to reset
    }
}
