package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo bit depth compression effect
 * Implements bit depth quantization and downsampling functionality
 */
public class BitcrusherEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "bitcrusher";
    private static final String EFFECT_NAME = "Bitcrusher";
    
    // Default parameters
    private static final int DEFAULT_BITDEPTH = 10;         // 10-bit
    private static final float DEFAULT_RATE = 12000.0f;     // 12kHz
    private static final float DEFAULT_WET = 0.35f;          // 35% (fix: increase default wet/dry ratio)
    
    // Parameter ranges
    private static final int MIN_BITDEPTH = 6;               // 6-bit
    private static final int MAX_BITDEPTH = 12;              // 12-bit
    private static final float MIN_RATE = 8000.0f;          // 8kHz
    private static final float MAX_RATE = 24000.0f;         // 24kHz
    private static final float MIN_WET = 0.0f;               // 0%
    private static final float MAX_WET = 1.0f;               // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile int bitdepth = DEFAULT_BITDEPTH;
    private volatile float rate = DEFAULT_RATE;
    
    // Downsampling state
    private float sampleHoldValueL = 0.0f;
    private float sampleHoldValueR = 0.0f;
    private float phaseL = 1.0f;  // Fix: initialize to 1.0f, ensure first sample update
    private float phaseR = 1.0f;  // Fix: initialize to 1.0f, ensure first sample update
    private float stepSize = 1.0f;
    
    // Quantization parameters
    private int quantizationLevels = 1024; // 2^10
    private float quantizationStep = 2.0f / 1024.0f;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public BitcrusherEffect() {
        // Initialize default parameters
        parameters.put("bitdepth", (float) DEFAULT_BITDEPTH);
        parameters.put("rate", DEFAULT_RATE);
        parameters.put("wet", DEFAULT_WET);
        
        // Initialize quantization parameters
        updateQuantizationParameters();
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
            case "bitdepth":
            case "bitDepth":  // Add compatibility parameter name
                this.bitdepth = (int) clamp(value, MIN_BITDEPTH, MAX_BITDEPTH);
                parameters.put("bitdepth", (float) this.bitdepth);
                parameters.put("bitDepth", (float) this.bitdepth);  // Store both parameter names
                updateQuantizationParameters();
                break;
            case "rate":
            case "sampleRate":  // Add compatibility parameter name
                this.rate = clamp(value, MIN_RATE, MAX_RATE);
                parameters.put("rate", this.rate);
                parameters.put("sampleRate", this.rate);  // Store both parameter names
                updateStepSize();
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
        // Handle compatibility parameter names
        if ("bitDepth".equals(parameterName)) {
            return parameters.getOrDefault("bitdepth", (float) DEFAULT_BITDEPTH);
        }
        if ("sampleRate".equals(parameterName)) {
            return parameters.getOrDefault("rate", DEFAULT_RATE);
        }
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
        updateStepSize();
        updateQuantizationParameters();  // Fix: update quantization parameters after sample rate change
        reset();  // Fix: reset state after sample rate change
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
        int currentBitdepth = this.bitdepth;
        float currentRate = this.rate;
        float currentWet = this.wetDryRatio;
        float currentStepSize = this.stepSize;
        float currentQuantizationStep = this.quantizationStep;
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Apply bit depth quantization and downsampling
            float processedSampleL = processSample(inputSample, true, currentStepSize, currentQuantizationStep);
            float processedSampleR = processSample(inputSample, false, currentStepSize, currentQuantizationStep);
            
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
        // Reset downsampling state
        sampleHoldValueL = 0.0f;
        sampleHoldValueR = 0.0f;
        phaseL = 1.0f;  // Fix: reset to 1.0f, ensure first sample update
        phaseR = 1.0f;  // Fix: reset to 1.0f, ensure first sample update
        
        // Recalculate step size
        updateStepSize();
    }
    
    /**
     * Process single sample (bit depth quantization + downsampling)
     */
    private float processSample(float input, boolean isLeft, float stepSize, float quantizationStep) {
        float sampleHoldValue = isLeft ? sampleHoldValueL : sampleHoldValueR;
        float phase = isLeft ? phaseL : phaseR;
        
        // Bit depth quantization
        float quantizedInput = quantizeSample(input, quantizationStep);
        
        // Downsampling (Sample & Hold)
        phase += stepSize;
        while (phase >= 1.0f) {  // Fix: use while loop, prevent missing updates when stepSize>1
            // Update hold value
            sampleHoldValue = quantizedInput;
            phase -= 1.0f;
        }
        
        // Update state
        if (isLeft) {
            sampleHoldValueL = sampleHoldValue;
            phaseL = phase;
        } else {
            sampleHoldValueR = sampleHoldValue;
            phaseR = phase;
        }
        
        return sampleHoldValue;
    }
    
    /**
     * Bit depth quantization
     */
    private float quantizeSample(float sample, float quantizationStep) {
        // Clamp to [-1, 1] range
        sample = clamp(sample, -1.0f, 1.0f);
        
        // Optional: pre-gain before quantization, make steps more obvious
        float preGain = 1.5f;  // Fix: add pre-gain
        sample *= preGain;
        
        // Quantize to discrete levels
        float quantized = Math.round(sample / quantizationStep) * quantizationStep;
        
        // Ensure within range
        return clamp(quantized, -1.0f, 1.0f);
    }
    
    /**
     * Update quantization parameters
     */
    private void updateQuantizationParameters() {
        quantizationLevels = 1 << bitdepth; // 2^bitdepth
        quantizationStep = 2.0f / (quantizationLevels - 1);  // Fix: use (levels-1) to include endpoints, quantization more "stiff"
    }
    
    /**
     * Update downsampling step size
     */
    private void updateStepSize() {
        if (sampleRate > 0) {
            stepSize = rate / sampleRate;
        } else {
            stepSize = 1.0f;
        }
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
