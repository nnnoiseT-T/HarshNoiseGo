package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Stereo looper effect
 * Implements recording buffer, loop playback, overdub, feedback, speed/reverse, and slice functionality
 */
public class LooperEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "looper";
    private static final String EFFECT_NAME = "Looper";
    
    // Default parameters
    private static final float DEFAULT_LOOP_LEN = 2000.0f;     // 2000ms loop length
    private static final float DEFAULT_OVERDUB = 0.0f;         // 0% overdub
    private static final float DEFAULT_FEEDBACK = 0.3f;         // 30% feedback
    private static final float DEFAULT_SPEED = 1.0f;           // 1.0x speed
    private static final boolean DEFAULT_REVERSE = false;      // No reverse
    private static final boolean DEFAULT_STUTTER_ON = false;   // Don't enable slice
    private static final float DEFAULT_STUTTER_PROB = 0.2f;    // 20% slice probability
    private static final float DEFAULT_SLICE_LEN = 60.0f;      // 60ms slice length
    private static final float DEFAULT_JITTER = 0.1f;         // 10% jitter
    private static final float DEFAULT_WET = 0.35f;           // 35%
    
    // Parameter ranges
    private static final float MIN_LOOP_LEN = 200.0f;          // 200ms
    private static final float MAX_LOOP_LEN = 8000.0f;         // 8000ms
    private static final float MIN_OVERDUB = 0.0f;             // 0%
    private static final float MAX_OVERDUB = 1.0f;             // 100%
    private static final float MIN_FEEDBACK = 0.0f;            // 0%
    private static final float MAX_FEEDBACK = 0.99f;           // 99%
    private static final float MIN_SPEED = 0.5f;              // 0.5x
    private static final float MAX_SPEED = 2.0f;               // 2.0x
    private static final float MIN_STUTTER_PROB = 0.0f;        // 0%
    private static final float MAX_STUTTER_PROB = 1.0f;        // 100%
    private static final float MIN_SLICE_LEN = 10.0f;          // 10ms
    private static final float MAX_SLICE_LEN = 200.0f;         // 200ms
    private static final float MIN_JITTER = 0.0f;              // 0%
    private static final float MAX_JITTER = 1.0f;              // 100%
    private static final float MIN_WET = 0.0f;                 // 0%
    private static final float MAX_WET = 1.0f;                 // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float loopLen = DEFAULT_LOOP_LEN;
    private volatile float overdub = DEFAULT_OVERDUB;
    private volatile float feedback = DEFAULT_FEEDBACK;
    private volatile float speed = DEFAULT_SPEED;
    private volatile boolean reverse = DEFAULT_REVERSE;
    private volatile boolean stutterOn = DEFAULT_STUTTER_ON;
    private volatile float stutterProb = DEFAULT_STUTTER_PROB;
    private volatile float sliceLen = DEFAULT_SLICE_LEN;
    private volatile float jitter = DEFAULT_JITTER;
    
    // Looper state
    public enum LooperState {
        IDLE,       // Idle state
        RECORD,     // Recording state
        PLAY,       // Playback state
        OVERDUB     // Overdub state
    }
    
    private volatile LooperState state = LooperState.IDLE;
    
    // Ring buffer system
    private float[] loopBufferL;      // Left channel loop buffer
    private float[] loopBufferR;      // Right channel loop buffer
    private int bufferSize;           // Buffer size (samples)
    private int loopLengthSamples;    // Loop length (samples)
    
    // Read/write pointers
    private int writeIndexL = 0;       // Left channel write pointer
    private int writeIndexR = 0;       // Right channel write pointer
    private float readIndexL = 0.0f;   // Left channel read pointer (float for speed changes)
    private float readIndexR = 0.0f;   // Right channel read pointer (float for speed changes)
    
    // Slice system
    private boolean inStutter = false;    // Whether in slice mode
    private int stutterCounter = 0;       // Slice counter
    private int stutterLength = 0;        // Slice length (samples)
    private int stutterStartIndex = 0;    // Slice start position
    private int stutterRepeatCount = 0;   // Slice repeat count
    private int stutterMaxRepeats = 16;   // Maximum repeat count (increased)
    private float stutterIntensity = 1.0f; // Slice intensity
    
    // Random number generator
    private final Random random = new Random();
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    public LooperEffect() {
        // Initialize default parameters
        parameters.put("loopLen", DEFAULT_LOOP_LEN);
        parameters.put("overdub", DEFAULT_OVERDUB);
        parameters.put("feedback", DEFAULT_FEEDBACK);
        parameters.put("speed", DEFAULT_SPEED);
        parameters.put("reverse", DEFAULT_REVERSE ? 1.0f : 0.0f);
        parameters.put("stutterOn", DEFAULT_STUTTER_ON ? 1.0f : 0.0f);
        parameters.put("stutterProb", DEFAULT_STUTTER_PROB);
        parameters.put("sliceLen", DEFAULT_SLICE_LEN);
        parameters.put("jitter", DEFAULT_JITTER);
        parameters.put("wet", DEFAULT_WET);
        
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
            case "loopLen":
                this.loopLen = clamp(value, MIN_LOOP_LEN, MAX_LOOP_LEN);
                parameters.put("loopLen", this.loopLen);
                updateLoopLength();
                break;
            case "overdub":
                this.overdub = clamp(value, MIN_OVERDUB, MAX_OVERDUB);
                parameters.put("overdub", this.overdub);
                break;
            case "feedback":
                this.feedback = clamp(value, MIN_FEEDBACK, MAX_FEEDBACK);
                parameters.put("feedback", this.feedback);
                break;
            case "speed":
                this.speed = clamp(value, MIN_SPEED, MAX_SPEED);
                parameters.put("speed", this.speed);
                break;
            case "reverse":
                this.reverse = value > 0.5f;
                parameters.put("reverse", this.reverse ? 1.0f : 0.0f);
                break;
            case "stutterOn":
                this.stutterOn = value > 0.5f;
                parameters.put("stutterOn", this.stutterOn ? 1.0f : 0.0f);
                break;
            case "stutterProb":
                this.stutterProb = clamp(value, MIN_STUTTER_PROB, MAX_STUTTER_PROB);
                parameters.put("stutterProb", this.stutterProb);
                break;
            case "sliceLen":
                this.sliceLen = clamp(value, MIN_SLICE_LEN, MAX_SLICE_LEN);
                parameters.put("sliceLen", this.sliceLen);
                updateSliceLength();
                break;
            case "jitter":
                this.jitter = clamp(value, MIN_JITTER, MAX_JITTER);
                parameters.put("jitter", this.jitter);
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
        initializeBuffers();
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
        float currentOverdub = this.overdub;
        float currentFeedback = this.feedback;
        float currentSpeed = this.speed;
        boolean currentReverse = this.reverse;
        boolean currentStutterOn = this.stutterOn;
        float currentStutterProb = this.stutterProb;
        float currentJitter = this.jitter;
        float currentWet = this.wetDryRatio;
        LooperState currentState = this.state;
        
        // Calculate equal power mixing gains
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            float loopOutput = 0.0f;
            
            switch (currentState) {
                case IDLE:
                    // Idle state: auto-start recording
                    if (i == 0) {
                        // Auto-start recording at first sample
                        state = LooperState.RECORD;
                        writeIndexL = 0;
                        writeIndexR = 0;
                        currentState = LooperState.RECORD;
                    }
                    loopOutput = inputSample;
                    break;
                    
                case RECORD:
                    // Recording state: write to buffer and output input signal
                    loopBufferL[writeIndexL] = inputSample;
                    loopBufferR[writeIndexR] = inputSample;
                    
                    // Update write pointers
                    writeIndexL = (writeIndexL + 1) % loopLengthSamples;  // Fix: use loopLengthSamples for consistency
                    writeIndexR = (writeIndexR + 1) % loopLengthSamples;  // Fix: use loopLengthSamples for consistency
                    
                    // Check if recording is complete
                    if (writeIndexL >= loopLengthSamples) {
                        // Switch to playback state
                        state = LooperState.PLAY;
                        // Fix: optimize reverse switching start point, if reverse=true from start, begin from end
                        if (currentReverse) {
                            readIndexL = loopLengthSamples - 1.0f;  // Start from end, more "typical inhale feel"
                            readIndexR = loopLengthSamples - 1.0f;
                        } else {
                            readIndexL = 0.0f;
                            readIndexR = 0.0f;
                        }
                        currentState = LooperState.PLAY;
                    }
                    
                    loopOutput = inputSample;
                    break;
                    
                case PLAY:
                case OVERDUB:
                    // Playback/overdub state: read from buffer and output
                    loopOutput = processPlayback(inputSample, currentOverdub, currentFeedback, 
                                               currentSpeed, currentReverse, currentStutterOn, 
                                               currentStutterProb, currentJitter);
                    break;
            }
            
            // Calculate output (equal power mixing)
            float drySignal = inputSample * dryGain;
            float wetSignal = loopOutput * wetGain;
            outputBuffer[i] = drySignal + wetSignal;
        }
    }
    
    @Override
    public void reset() {
        // Reset state
        state = LooperState.IDLE;
        
        // Reset buffers
        if (loopBufferL != null) {
            for (int i = 0; i < bufferSize; i++) {
                loopBufferL[i] = 0.0f;
                loopBufferR[i] = 0.0f;
            }
        }
        
        // Reset pointers
        writeIndexL = 0;
        writeIndexR = 0;
        readIndexL = 0.0f;
        readIndexR = 0.0f;
        
        // Reset slice system
        inStutter = false;
        stutterCounter = 0;
        stutterLength = 0;
        stutterStartIndex = 0;
        stutterRepeatCount = 0;
    }
    
    /**
     * Process playback logic
     */
    private float processPlayback(float inputSample, float overdub, float feedback, 
                                 float speed, boolean reverse, boolean stutterOn, 
                                 float stutterProb, float jitter) {
        
        // Process slice
        if (stutterOn && !inStutter && random.nextFloat() < stutterProb) {
            // Start slice
            startStutter(jitter);
        }
        
        float outputSample = 0.0f;
        
        if (inStutter) {
            // Slice mode: repeat slice playback
            outputSample = processStutter();
        } else {
            // Normal playback mode
            outputSample = processNormalPlayback(inputSample, overdub, feedback, speed, reverse);
        }
        
        return outputSample;
    }
    
    /**
     * Process normal playback
     */
    private float processNormalPlayback(float inputSample, float overdub, float feedback, 
                                       float speed, boolean reverse) {
        
        // Calculate read position
        float readPosL = readIndexL;
        float readPosR = readIndexR;
        
        // Apply speed (fix: use more intuitive bidirectional wrapping)
        float speedStep = reverse ? -speed : speed;
        readPosL += speedStep;
        readPosR += speedStep;
        
        // Loop boundary handling (fix: bidirectional wrapping)
        if (readPosL >= loopLengthSamples) {
            readPosL -= loopLengthSamples;
            // Apply feedback decay
            applyFeedback();
        }
        if (readPosL < 0) {
            readPosL += loopLengthSamples;
            // Apply feedback decay
            applyFeedback();
        }
        if (readPosR >= loopLengthSamples) {
            readPosR -= loopLengthSamples;
        }
        if (readPosR < 0) {
            readPosR += loopLengthSamples;
        }
        
        // Update read pointers
        readIndexL = readPosL;
        readIndexR = readPosR;
        
        // Read samples (linear interpolation)
        float sampleL, sampleR;
        
        // Fix: read pointer already stepped by reverse positive/negative, directly sample by readPos*
        sampleL = getInterpolatedSample(loopBufferL, readPosL);
        sampleR = getInterpolatedSample(loopBufferR, readPosR);
        
        float outputSample = (sampleL + sampleR) * 0.5f;
        
        // Overdub processing
        if (state == LooperState.OVERDUB && overdub > 0.0f) {
            int writePos = (int) readPosL;
            loopBufferL[writePos] = loopBufferL[writePos] * (1.0f - overdub) + inputSample * overdub;
            loopBufferR[writePos] = loopBufferR[writePos] * (1.0f - overdub) + inputSample * overdub;
        }
        
        return outputSample;
    }
    
    /**
     * Start slice
     */
    private void startStutter(float jitter) {
        inStutter = true;
        stutterCounter = 0;
        stutterRepeatCount = 0;
        
        // Calculate slice start position (consider jitter)
        int baseStartIndex = (int) readIndexL;
        int jitterRange = Math.max(1, (int)(stutterLength * jitter));
        stutterStartIndex = baseStartIndex + random.nextInt(jitterRange * 2) - jitterRange;
        stutterStartIndex = (stutterStartIndex + loopLengthSamples) % loopLengthSamples;  // Fix: use loopLengthSamples
        
        // Enhance slice intensity
        stutterIntensity = 1.5f + (jitter * 3.0f);
    }
    
    /**
     * Process slice
     */
    private float processStutter() {
        // Calculate position within slice
        int stutterPos = stutterStartIndex + (stutterCounter % stutterLength);
        stutterPos = stutterPos % loopLengthSamples;  // Fix: use loopLengthSamples instead of bufferSize
        
        // Read sample
        float sampleL = loopBufferL[stutterPos];
        float sampleR = loopBufferR[stutterPos];
        float outputSample = (sampleL + sampleR) * 0.5f;
        
        // Enhance slice effect
        outputSample *= stutterIntensity;
        
        // Add slice modulation (enhanced version)
        float modulation = (float) Math.sin(2.0 * Math.PI * stutterCounter * 0.1f) * 0.5f;
        float randomModulation = (float) Math.sin(2.0 * Math.PI * stutterCounter * 0.3f) * 0.3f;
        float amplitudeModulation = 1.0f + (float) Math.sin(2.0 * Math.PI * stutterCounter * 0.05f) * 0.4f;
        
        outputSample += modulation + randomModulation;
        outputSample *= amplitudeModulation;
        
        // Add slice distortion effect
        if (Math.abs(outputSample) > 0.8f) {
            outputSample = Math.signum(outputSample) * (0.8f + (Math.abs(outputSample) - 0.8f) * 0.3f);
        }
        
        // Update slice counter
        stutterCounter++;
        
        // Check if slice is complete
        if (stutterCounter >= stutterLength) {
            stutterRepeatCount++;
            stutterCounter = 0;
            
            // Dynamically adjust repeat count (based on probability)
            int maxRepeats = (int)(stutterMaxRepeats * (0.5f + random.nextFloat() * 0.5f));
            
            if (stutterRepeatCount >= maxRepeats) {
                // Slice complete, resume normal playback
                inStutter = false;
                stutterIntensity = 1.0f;
            }
        }
        
        return outputSample;
    }
    
    /**
     * Apply feedback decay
     */
    private void applyFeedback() {
        if (feedback < 0.99f) {
            for (int i = 0; i < loopLengthSamples; i++) {
                loopBufferL[i] *= feedback;
                loopBufferR[i] *= feedback;
            }
        }
    }
    
    /**
     * Get interpolated sample
     */
    private float getInterpolatedSample(float[] buffer, float position) {
        int index1 = (int) position;
        int index2 = (index1 + 1) % loopLengthSamples;  // Fix: use loopLengthSamples instead of bufferSize
        float fraction = position - index1;
        
        float sample1 = buffer[index1];
        float sample2 = buffer[index2];
        
        return sample1 + (sample2 - sample1) * fraction;
    }
    
    /**
     * Initialize buffers
     */
    private void initializeBuffers() {
        // Allocate buffer for maximum loop length
        bufferSize = (int)(MAX_LOOP_LEN * sampleRate / 1000.0f);
        loopBufferL = new float[bufferSize];
        loopBufferR = new float[bufferSize];
        
        // Clear buffers
        for (int i = 0; i < bufferSize; i++) {
            loopBufferL[i] = 0.0f;
            loopBufferR[i] = 0.0f;
        }
        
        updateLoopLength();
        updateSliceLength();
    }
    
    /**
     * Update loop length
     */
    private void updateLoopLength() {
        loopLengthSamples = (int)(loopLen * sampleRate / 1000.0f);
        loopLengthSamples = Math.max(loopLengthSamples, 1);
    }
    
    /**
     * Update slice length
     */
    private void updateSliceLength() {
        stutterLength = (int)(sliceLen * sampleRate / 1000.0f);
        stutterLength = Math.max(stutterLength, 1);
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // Public methods for UI control
    
    /**
     * Start recording
     */
    public void startRecord() {
        if (state == LooperState.IDLE) {
            state = LooperState.RECORD;
            writeIndexL = 0;
            writeIndexR = 0;
        }
    }
    
    /**
     * Start playback
     */
    public void startPlay() {
        if (state == LooperState.RECORD || state == LooperState.IDLE) {
            state = LooperState.PLAY;
            readIndexL = 0.0f;
            readIndexR = 0.0f;
        }
    }
    
    /**
     * Start overdub
     */
    public void startOverdub() {
        if (state == LooperState.PLAY) {
            state = LooperState.OVERDUB;
        }
    }
    
    /**
     * Clear loop
     */
    public void clear() {
        state = LooperState.IDLE;
        reset();
    }
    
    /**
     * Get current state
     */
    public LooperState getState() {
        return state;
    }
}
