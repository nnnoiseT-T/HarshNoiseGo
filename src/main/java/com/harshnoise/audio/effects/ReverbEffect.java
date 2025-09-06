package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo reverb effect
 * Simplified implementation based on Schroeder algorithm
 */
public class ReverbEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "reverb";
    private static final String EFFECT_NAME = "Reverb";
    
    // Default parameters
    private static final float DEFAULT_ROOM = 0.6f;         // 60%
    private static final float DEFAULT_DAMP = 0.4f;         // 40%
    private static final float DEFAULT_WET = 0.35f;         // 35% (fix: increase default wet/dry ratio)
    private static final float DEFAULT_PREDELAY = 0.0f;     // 0ms
    
    // Parameter ranges
    private static final float MIN_ROOM = 0.0f;             // 0%
    private static final float MAX_ROOM = 1.0f;             // 100%
    private static final float MIN_DAMP = 0.0f;              // 0%
    private static final float MAX_DAMP = 1.0f;             // 100%
    private static final float MIN_WET = 0.0f;               // 0%
    private static final float MAX_WET = 1.0f;               // 100%
    private static final float MIN_PREDELAY = 0.0f;         // 0ms
    private static final float MAX_PREDELAY = 20.0f;        // 20ms
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float room = DEFAULT_ROOM;
    private volatile float damp = DEFAULT_DAMP;
    private volatile float predelay = DEFAULT_PREDELAY;
    
    // Schroeder reverb parameters
    private static final int NUM_COMB_FILTERS = 4;
    private static final int NUM_ALLPASS_FILTERS = 2;
    
    // Comb filter delay lengths (prime numbers, reduce comb peaks)
    private static final int[] COMB_DELAYS = {1116, 1188, 1277, 1356};
    private static final int[] ALLPASS_DELAYS = {556, 441};
    
    // Reverb buffers
    private float[] leftCombBuffers[];
    private float[] rightCombBuffers[];
    private float[] leftAllpassBuffers[];
    private float[] rightAllpassBuffers[];
    private int[] combWriteIndices;
    private int[] allpassWriteIndices;
    
    // Fix: add LPF state arrays for each comb channel
    private float[] combLpL = new float[NUM_COMB_FILTERS];  // Left channel comb low-pass state
    private float[] combLpR = new float[NUM_COMB_FILTERS];  // Right channel comb low-pass state
    
    // Pre-delay buffers
    private float[] predelayBufferL;
    private float[] predelayBufferR;
    private int predelayWriteIndexL;
    private int predelayWriteIndexR;
    private int predelaySamples;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public ReverbEffect() {
        // Initialize default parameters
        parameters.put("room", DEFAULT_ROOM);
        parameters.put("damp", DEFAULT_DAMP);
        parameters.put("wet", DEFAULT_WET);
        parameters.put("predelay", DEFAULT_PREDELAY);
        
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
            case "room":
            case "roomSize":  // Add compatibility parameter name
                this.room = clamp(value, MIN_ROOM, MAX_ROOM);
                parameters.put("room", this.room);
                parameters.put("roomSize", this.room);  // Store both parameter names
                break;
            case "damp":
            case "damping":  // Add compatibility parameter name
                this.damp = clamp(value, MIN_DAMP, MAX_DAMP);
                parameters.put("damp", this.damp);
                parameters.put("damping", this.damp);  // Store both parameter names
                break;
            case "wet":
            case "mix":
                this.wetDryRatio = clamp(value, MIN_WET, MAX_WET);
                parameters.put("wet", this.wetDryRatio);
                break;
            case "predelay":
                this.predelay = clamp(value, MIN_PREDELAY, MAX_PREDELAY);
                parameters.put("predelay", this.predelay);
                updatePredelaySamples();
                break;
            default:
                parameters.put(parameterName, value);
                break;
        }
    }
    
    @Override
    public float getParameter(String parameterName) {
        // Handle compatibility parameter names
        if ("roomSize".equals(parameterName)) {
            return parameters.getOrDefault("room", DEFAULT_ROOM);
        }
        if ("damping".equals(parameterName)) {
            return parameters.getOrDefault("damp", DEFAULT_DAMP);
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
        float currentRoom = this.room;
        float currentDamp = this.damp;
        float currentWet = this.wetDryRatio;
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Apply pre-delay
            float delayedInputL = applyPredelay(inputSample, true);
            float delayedInputR = applyPredelay(inputSample, false);
            
            // Through Comb filters (early reflections)
            float combOutputL = 0.0f;
            float combOutputR = 0.0f;
            
            for (int j = 0; j < NUM_COMB_FILTERS; j++) {
                combOutputL += processCombFilter(delayedInputL, j, true);
                combOutputR += processCombFilter(delayedInputR, j, false);
            }
            
            // Through Allpass filters (diffusion)
            float allpassOutputL = combOutputL;
            float allpassOutputR = combOutputR;
            
            for (int j = 0; j < NUM_ALLPASS_FILTERS; j++) {
                allpassOutputL = processAllpassFilter(allpassOutputL, j, true);
                allpassOutputR = processAllpassFilter(allpassOutputR, j, false);
            }
            
            // Calculate output (equal power mixing)
            float drySignal = inputSample * dryGain;
            float wetSignalL = allpassOutputL * wetGain;
            float wetSignalR = allpassOutputR * wetGain;
            
            // Stereo output (simplified to mono mix)
            float wetSignal = (wetSignalL + wetSignalR) * 0.5f;
            outputBuffer[i] = drySignal + wetSignal;
        }
    }
    
    @Override
    public void reset() {
        // Clear all buffers
        if (leftCombBuffers != null) {
            for (int i = 0; i < NUM_COMB_FILTERS; i++) {
                for (int j = 0; j < leftCombBuffers[i].length; j++) {
                    leftCombBuffers[i][j] = 0.0f;
                    rightCombBuffers[i][j] = 0.0f;
                }
            }
        }
        
        if (leftAllpassBuffers != null) {
            for (int i = 0; i < NUM_ALLPASS_FILTERS; i++) {
                for (int j = 0; j < leftAllpassBuffers[i].length; j++) {
                    leftAllpassBuffers[i][j] = 0.0f;
                    rightAllpassBuffers[i][j] = 0.0f;
                }
            }
        }
        
        // Clear pre-delay buffers
        if (predelayBufferL != null) {
            for (int i = 0; i < predelayBufferL.length; i++) {
                predelayBufferL[i] = 0.0f;
                predelayBufferR[i] = 0.0f;
            }
        }
        
        // Fix: clear comb low-pass states
        for (int i = 0; i < NUM_COMB_FILTERS; i++) {
            combLpL[i] = 0.0f;
            combLpR[i] = 0.0f;
        }
        
        // Reset indices
        for (int i = 0; i < NUM_COMB_FILTERS; i++) {
            combWriteIndices[i] = 0;
        }
        for (int i = 0; i < NUM_ALLPASS_FILTERS; i++) {
            allpassWriteIndices[i] = 0;
        }
        predelayWriteIndexL = 0;
        predelayWriteIndexR = 0;
    }
    
    /**
     * Initialize reverb buffers
     */
    private void initializeBuffers() {
        // Allocate Comb filter buffers
        leftCombBuffers = new float[NUM_COMB_FILTERS][];
        rightCombBuffers = new float[NUM_COMB_FILTERS][];
        combWriteIndices = new int[NUM_COMB_FILTERS];
        
        for (int i = 0; i < NUM_COMB_FILTERS; i++) {
            leftCombBuffers[i] = new float[COMB_DELAYS[i]];
            rightCombBuffers[i] = new float[COMB_DELAYS[i]];
        }
        
        // Allocate Allpass filter buffers
        leftAllpassBuffers = new float[NUM_ALLPASS_FILTERS][];
        rightAllpassBuffers = new float[NUM_ALLPASS_FILTERS][];
        allpassWriteIndices = new int[NUM_ALLPASS_FILTERS];
        
        for (int i = 0; i < NUM_ALLPASS_FILTERS; i++) {
            leftAllpassBuffers[i] = new float[ALLPASS_DELAYS[i]];
            rightAllpassBuffers[i] = new float[ALLPASS_DELAYS[i]];
        }
        
        // Allocate pre-delay buffers
        updatePredelaySamples();
        
        // Clear buffers
        reset();
    }
    
    /**
     * Update pre-delay sample count
     */
    private void updatePredelaySamples() {
        predelaySamples = (int) (predelay * sampleRate / 1000.0f);
        predelayBufferL = new float[Math.max(predelaySamples, 1)];
        predelayBufferR = new float[Math.max(predelaySamples, 1)];
        predelayWriteIndexL = 0;
        predelayWriteIndexR = 0;
    }
    
    /**
     * Apply pre-delay
     */
    private float applyPredelay(float input, boolean isLeft) {
        if (predelaySamples == 0) {
            return input;
        }
        
        float[] buffer = isLeft ? predelayBufferL : predelayBufferR;
        int writeIndex = isLeft ? predelayWriteIndexL : predelayWriteIndexR;
        
        // Read delayed sample
        float delayed = buffer[writeIndex];
        
        // Write new sample
        buffer[writeIndex] = input;
        
        // Update write pointer
        writeIndex = (writeIndex + 1) % predelaySamples;
        
        if (isLeft) {
            predelayWriteIndexL = writeIndex;
        } else {
            predelayWriteIndexR = writeIndex;
        }
        
        return delayed;
    }
    
    /**
     * Process Comb filter
     */
    private float processCombFilter(float input, int filterIndex, boolean isLeft) {
        float[] buffer = isLeft ? leftCombBuffers[filterIndex] : rightCombBuffers[filterIndex];
        int writeIndex = combWriteIndices[filterIndex];
        int delayLength = COMB_DELAYS[filterIndex];
        
        // Calculate read pointer
        int readIndex = (writeIndex - delayLength + delayLength) % delayLength;
        if (readIndex < 0) readIndex += delayLength;
        
        // Read delayed sample
        float delayed = buffer[readIndex];
        
        // Fix: stateful damping + larger feedback
        float g = 0.7f + 0.28f * room;         // Map room to larger feedback (avoid too small)
        float damp1 = damp;                     // Damping coefficient (0..1)
        float damp2 = 1.0f - damp1;
        
        // Typical: low-pass in feedback loop (with memory)
        float[] lpStore = isLeft ? combLpL : combLpR;
        lpStore[filterIndex] = delayed * damp2 + lpStore[filterIndex] * damp1;
        float fbSample = lpStore[filterIndex] * g;
        
        // Write new sample
        buffer[writeIndex] = input + fbSample;
        
        // Update write pointer
        combWriteIndices[filterIndex] = (writeIndex + 1) % delayLength;
        
        return delayed;
    }
    
    /**
     * Process Allpass filter
     */
    private float processAllpassFilter(float input, int filterIndex, boolean isLeft) {
        float[] buffer = isLeft ? leftAllpassBuffers[filterIndex] : rightAllpassBuffers[filterIndex];
        int writeIndex = allpassWriteIndices[filterIndex];
        int delayLength = ALLPASS_DELAYS[filterIndex];
        
        // Calculate read pointer
        int readIndex = (writeIndex - delayLength + delayLength) % delayLength;
        if (readIndex < 0) readIndex += delayLength;
        
        // Read delayed sample
        float delayed = buffer[readIndex];
        
        // Allpass filter calculation
        float allpassGain = 0.7f;  // Fix: increase allpass gain, stronger diffusion
        float output = -allpassGain * input + delayed;
        buffer[writeIndex] = input + allpassGain * output;
        
        // Update write pointer
        allpassWriteIndices[filterIndex] = (writeIndex + 1) % delayLength;
        
        return output;
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
