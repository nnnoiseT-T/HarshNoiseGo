package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;

/**
 * Stereo reverse delay effect
 * Implements block buffering, reverse playback, and feedback loop functionality
 */
public class ReverseDelayEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "reverseDelay";
    private static final String EFFECT_NAME = "Reverse Delay";
    
    // Default parameters
    private static final float DEFAULT_TIME = 600.0f;         // 600ms block length
    private static final float DEFAULT_FEEDBACK = 0.35f;      // 35% feedback
    private static final float DEFAULT_WET = 0.35f;           // 35%
    private static final float DEFAULT_CROSSFADE = 8.0f;      // 8ms crossfade
    private static final boolean DEFAULT_FREEZE = false;      // No freeze
    
    // Parameter ranges
    private static final float MIN_TIME = 200.0f;             // 200ms
    private static final float MAX_TIME = 2000.0f;           // 2000ms
    private static final float MIN_FEEDBACK = 0.0f;           // 0%
    private static final float MAX_FEEDBACK = 0.95f;          // 95%
    private static final float MIN_WET = 0.0f;                // 0%
    private static final float MAX_WET = 1.0f;                // 100%
    private static final float MIN_CROSSFADE = 0.0f;         // 0ms
    private static final float MAX_CROSSFADE = 20.0f;        // 20ms
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float time = DEFAULT_TIME;
    private volatile float feedback = DEFAULT_FEEDBACK;
    private volatile float crossfade = DEFAULT_CROSSFADE;
    private volatile boolean freeze = DEFAULT_FREEZE;
    
    // Block buffer system
    private float[] blockBufferL;      // Left channel block buffer
    private float[] blockBufferR;      // Right channel block buffer
    private float[] outputBufferL;     // Left channel output buffer
    private float[] outputBufferR;     // Right channel output buffer
    private int blockSize;             // Block size (samples)
    private int crossfadeSamples;      // Crossfade samples
    
    // Read/write pointers
    private int writeIndexL = 0;        // Left channel write pointer
    private int writeIndexR = 0;        // Right channel write pointer
    private int readIndexL = 0;         // Left channel read pointer
    private int readIndexR = 0;         // Right channel read pointer
    
    // Block state
    private boolean isRecording = true; // Whether recording
    private boolean isPlaying = false; // Whether playing
    private int currentBlock = 0;       // Current block index (0 or 1)
    private int blockCount = 0;         // Number of recorded blocks
    
    // Crossfade state
    private float crossfadeGain = 1.0f;
    private int crossfadeCounter = 0;
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public ReverseDelayEffect() {
        // Initialize default parameters
        parameters.put("time", DEFAULT_TIME);
        parameters.put("feedback", DEFAULT_FEEDBACK);
        parameters.put("wet", DEFAULT_WET);
        parameters.put("crossfade", DEFAULT_CROSSFADE);
        parameters.put("freeze", DEFAULT_FREEZE ? 1.0f : 0.0f);
        
        // Initialize buffers
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
                this.time = clamp(value, MIN_TIME, MAX_TIME);
                parameters.put("time", this.time);
                updateBlockSize();
                break;
            case "feedback":
                this.feedback = clamp(value, MIN_FEEDBACK, MAX_FEEDBACK);
                parameters.put("feedback", this.feedback);
                break;
            case "crossfade":
                this.crossfade = clamp(value, MIN_CROSSFADE, MAX_CROSSFADE);
                parameters.put("crossfade", this.crossfade);
                updateCrossfadeSamples();
                break;
            case "freeze":
                this.freeze = value > 0.5f;
                parameters.put("freeze", this.freeze ? 1.0f : 0.0f);
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
        updateBlockSize();
        updateCrossfadeSamples();
        reset();
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
        float currentTime = this.time;
        float currentFeedback = this.feedback;
        float currentWet = this.wetDryRatio;
        float currentCrossfade = this.crossfade;
        boolean currentFreeze = this.freeze;
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Apply reverse delay effect
            float processedSampleL = processSample(inputSample, true, currentFeedback, currentFreeze);
            float processedSampleR = processSample(inputSample, false, currentFeedback, currentFreeze);
            
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
        // Reset all buffers
        if (blockBufferL != null) {
            for (int i = 0; i < blockBufferL.length; i++) {
                blockBufferL[i] = 0.0f;
                blockBufferR[i] = 0.0f;
                outputBufferL[i] = 0.0f;
                outputBufferR[i] = 0.0f;
            }
        }
        
        // Reset pointers and state
        writeIndexL = 0;
        writeIndexR = 0;
        readIndexL = 0;
        readIndexR = 0;
        isRecording = true;
        isPlaying = false;
        currentBlock = 0;
        blockCount = 0;
        crossfadeGain = 1.0f;
        crossfadeCounter = 0;
    }
    
    /**
     * Process single sample (reverse delay + feedback)
     */
    private float processSample(float input, boolean isLeft, float feedback, boolean freeze) {
        float[] blockBuffer = isLeft ? blockBufferL : blockBufferR;
        float[] outputBuffer = isLeft ? outputBufferL : outputBufferR;
        int writeIndex = isLeft ? writeIndexL : writeIndexR;
        int readIndex = isLeft ? readIndexL : readIndexR;
        
        float outputSample = 0.0f;
        
        if (isRecording) {
            // Recording mode: write to block buffer
            if (!freeze) {
                // Add feedback
                float feedbackSample = 0.0f;
                if (isPlaying && blockCount > 0) {
                    feedbackSample = outputBuffer[readIndex] * feedback;
                }
                
                // Write current sample + feedback
                blockBuffer[writeIndex] = input + feedbackSample;
            }
            
            // Update write pointer
            writeIndex++;
            if (writeIndex >= blockSize) {
                // Block recording complete, start playback
                isRecording = false;
                isPlaying = true;
                writeIndex = 0;
                currentBlock = (currentBlock + 1) % 2;
                blockCount++;
                
                // Prepare reverse playback
                prepareReversePlayback(isLeft);
            }
        } else if (isPlaying) {
            // Playback mode: read and output in reverse
            outputSample = outputBuffer[readIndex];
            
            // Update read pointer (reverse)
            readIndex--;
            if (readIndex < 0) {
                // Playback complete, start recording next block
                isRecording = true;
                isPlaying = false;
                readIndex = 0;
                
                // Prepare recording next block
                prepareRecording(isLeft);
            }
        }
        
        // Update pointers
        if (isLeft) {
            writeIndexL = writeIndex;
            readIndexL = readIndex;
        } else {
            writeIndexR = writeIndex;
            readIndexR = readIndex;
        }
        
        return outputSample;
    }
    
    /**
     * Prepare reverse playback
     */
    private void prepareReversePlayback(boolean isLeft) {
        float[] blockBuffer = isLeft ? blockBufferL : blockBufferR;
        float[] outputBuffer = isLeft ? outputBufferL : outputBufferR;
        
        // Copy block buffer to output buffer in reverse
        for (int i = 0; i < blockSize; i++) {
            outputBuffer[i] = blockBuffer[blockSize - 1 - i];
        }
        
        // Set read pointer to end (reverse playback)
        if (isLeft) {
            readIndexL = blockSize - 1;
        } else {
            readIndexR = blockSize - 1;
        }
        
        // Reset crossfade
        crossfadeGain = 1.0f;
        crossfadeCounter = 0;
    }
    
    /**
     * Prepare recording
     */
    private void prepareRecording(boolean isLeft) {
        // Set write pointer to start
        if (isLeft) {
            writeIndexL = 0;
        } else {
            writeIndexR = 0;
        }
        
        // Clear block buffer
        float[] blockBuffer = isLeft ? blockBufferL : blockBufferR;
        for (int i = 0; i < blockSize; i++) {
            blockBuffer[i] = 0.0f;
        }
    }
    
    /**
     * Initialize buffers
     */
    private void initializeBuffers() {
        updateBlockSize();
        updateCrossfadeSamples();
        
        // Allocate buffers (maximum block size)
        int maxBlockSize = (int)(MAX_TIME * sampleRate / 1000.0f);
        blockBufferL = new float[maxBlockSize];
        blockBufferR = new float[maxBlockSize];
        outputBufferL = new float[maxBlockSize];
        outputBufferR = new float[maxBlockSize];
        
        // Clear buffers
        for (int i = 0; i < maxBlockSize; i++) {
            blockBufferL[i] = 0.0f;
            blockBufferR[i] = 0.0f;
            outputBufferL[i] = 0.0f;
            outputBufferR[i] = 0.0f;
        }
    }
    
    /**
     * Update block size
     */
    private void updateBlockSize() {
        blockSize = (int)(time * sampleRate / 1000.0f);
        blockSize = Math.max(blockSize, 1); // Ensure at least 1 sample
    }
    
    /**
     * Update crossfade samples
     */
    private void updateCrossfadeSamples() {
        crossfadeSamples = (int)(crossfade * sampleRate / 1000.0f);
        crossfadeSamples = Math.max(crossfadeSamples, 0);
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
