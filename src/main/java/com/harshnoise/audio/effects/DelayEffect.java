package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo delay effect
 * Implements stereo delay with time, feedback, and mix parameter control
 */
public class DelayEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "delay";
    private static final String EFFECT_NAME = "Delay";
    
    // Default parameters
    private static final float DEFAULT_TIME = 320.0f;      // 320ms
    private static final float DEFAULT_FEEDBACK = 0.35f;    // 35%
    private static final float DEFAULT_WET = 0.25f;         // 25%
    
    // Parameter ranges
    private static final float MIN_TIME = 20.0f;            // 20ms
    private static final float MAX_TIME = 1200.0f;          // 1200ms
    private static final float MIN_FEEDBACK = 0.0f;         // 0%
    private static final float MAX_FEEDBACK = 0.95f;        // 95%
    private static final float MIN_WET = 0.0f;              // 0%
    private static final float MAX_WET = 1.0f;              // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float timeMs = DEFAULT_TIME;
    private volatile float feedback = DEFAULT_FEEDBACK;
    
    // Delay buffers
    private float[] leftBuffer;
    private float[] rightBuffer;
    private int delayBufferSize;  // Fix: rename to avoid conflict with process parameter
    private int writeIndexL;
    private int writeIndexR;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public DelayEffect() {
        // Initialize default parameters
        parameters.put("time", DEFAULT_TIME);
        parameters.put("feedback", DEFAULT_FEEDBACK);
        parameters.put("wet", DEFAULT_WET);
        
        // Initialize buffers (will be reallocated in setSampleRate)
        initializeBuffers();
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
            case "time":
            case "delayTime":  // Add compatibility parameter name
                this.timeMs = clamp(value, MIN_TIME, MAX_TIME);
                parameters.put("time", this.timeMs);
                parameters.put("delayTime", this.timeMs);  // Store both parameter names
                break;
            case "feedback":
                this.feedback = clamp(value, MIN_FEEDBACK, MAX_FEEDBACK);
                parameters.put("feedback", this.feedback);
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
        if ("delayTime".equals(parameterName)) {
            return parameters.getOrDefault("time", DEFAULT_TIME);
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
        initializeBuffers();
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
        float currentTimeMs = this.timeMs;
        float currentFeedback = this.feedback;
        float currentWet = this.wetDryRatio;
        
        // Calculate delay samples
        int delaySamples = (int) (currentTimeMs * sampleRate / 1000.0f);
        
        // Safety check: ensure delay samples don't exceed buffer size
        delaySamples = Math.min(delaySamples, delayBufferSize - 1);
        delaySamples = Math.max(delaySamples, 0);
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Calculate read pointers (wrap around)
            int readIndexL = (writeIndexL - delaySamples + delayBufferSize) % delayBufferSize;
            int readIndexR = (writeIndexR - delaySamples + delayBufferSize) % delayBufferSize;
            
            // Ensure indices are within valid range
            if (readIndexL < 0) readIndexL += delayBufferSize;
            if (readIndexR < 0) readIndexR += delayBufferSize;
            
            // Read delayed samples (simple implementation, can be optimized with interpolation later)
            float delayedSampleL = leftBuffer[readIndexL];
            float delayedSampleR = rightBuffer[readIndexR];
            
            // Calculate feedback samples
            float feedbackSampleL = delayedSampleL * currentFeedback;
            float feedbackSampleR = delayedSampleR * currentFeedback;
            
            // Write to delay buffers
            leftBuffer[writeIndexL] = inputSample + feedbackSampleL;
            rightBuffer[writeIndexR] = inputSample + feedbackSampleR;
            
            // Calculate output (equal power mixing)
            float drySignal = inputSample * dryGain;
            float wetSignalL = delayedSampleL * wetGain;
            float wetSignalR = delayedSampleR * wetGain;
            
            // Stereo output (simplified to mono mix here, actual applications need stereo processing)
            float wetSignal = (wetSignalL + wetSignalR) * 0.5f;
            outputBuffer[i] = drySignal + wetSignal;
            
            // Update write pointers (wrap around)
            writeIndexL = (writeIndexL + 1) % delayBufferSize;
            writeIndexR = (writeIndexR + 1) % delayBufferSize;
        }
    }
    
    @Override
    public void reset() {
        // Clear delay buffers
        if (leftBuffer != null) {
            for (int i = 0; i < leftBuffer.length; i++) {
                leftBuffer[i] = 0.0f;
                rightBuffer[i] = 0.0f;
            }
        }
        writeIndexL = 0;
        writeIndexR = 0;
    }
    
    /**
     * Initialize delay buffers
     */
    private void initializeBuffers() {
        // Calculate buffer size (at least 2 seconds of delay)
        int maxDelaySamples = (int) (MAX_TIME * sampleRate / 1000.0f);
        this.delayBufferSize = maxDelaySamples;  // Fix: use new field name
        
        // Allocate buffers
        this.leftBuffer = new float[delayBufferSize];
        this.rightBuffer = new float[delayBufferSize];
        
        // Clear buffers
        reset();
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
