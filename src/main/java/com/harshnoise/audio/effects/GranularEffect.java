package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Stereo granular synthesis effect
 * Implements granular cloud synthesis, randomized playback, envelope control
 */
public class GranularEffect implements AudioEffect {
    
    private static final String EFFECT_ID = "granular";
    private static final String EFFECT_NAME = "Granular";
    
    // Default parameters
    private static final float DEFAULT_GRAIN_SIZE = 60.0f;      // 60ms grain length (shorter, more noticeable)
    private static final float DEFAULT_DENSITY = 25.0f;         // 25 grains/sec (higher density)
    private static final float DEFAULT_PITCH = 1.0f;           // 1.0x playback speed
    private static final float DEFAULT_JITTER = 0.4f;          // 40% randomization (stronger randomization)
    private static final float DEFAULT_REVERSE_PROB = 0.2f;    // 20% reverse probability (more reverse)
    private static final float DEFAULT_WET = 0.6f;             // 60% (more wet signal)
    
    // Parameter ranges
    private static final float MIN_GRAIN_SIZE = 20.0f;         // 20ms
    private static final float MAX_GRAIN_SIZE = 200.0f;        // 200ms
    private static final float MIN_DENSITY = 1.0f;             // 1 grain/sec
    private static final float MAX_DENSITY = 50.0f;            // 50 grains/sec
    private static final float MIN_PITCH = 0.5f;               // 0.5x speed
    private static final float MAX_PITCH = 2.0f;                // 2.0x speed
    private static final float MIN_JITTER = 0.0f;              // 0% randomization
    private static final float MAX_JITTER = 1.0f;               // 100% randomization
    private static final float MIN_REVERSE_PROB = 0.0f;         // 0% reverse
    private static final float MAX_REVERSE_PROB = 1.0f;         // 100% reverse
    private static final float MIN_WET = 0.0f;                  // 0%
    private static final float MAX_WET = 1.0f;                  // 100%
    
    // State
    private volatile boolean enabled = true;
    private volatile float wetDryRatio = DEFAULT_WET;
    private volatile float sampleRate = 44100.0f;
    
    // Parameters (thread-safe)
    private volatile float grainSize = DEFAULT_GRAIN_SIZE;
    private volatile float density = DEFAULT_DENSITY;
    private volatile float pitch = DEFAULT_PITCH;
    private volatile float jitter = DEFAULT_JITTER;
    private volatile float reverseProb = DEFAULT_REVERSE_PROB;
    
    // Ring buffer system
    private float[] ringBufferL;      // Left channel ring buffer
    private float[] ringBufferR;      // Right channel ring buffer
    private int ringSize;             // Fix: ring buffer size (samples)
    private int writeIndexL = 0;      // Left channel write pointer
    private int writeIndexR = 0;      // Right channel write pointer
    
    // Grain system
    private static final int MAX_GRAINS = 256;  // Maximum grains
    private Grain[] grains;           // Grain array
    private int activeGrains = 0;     // Active grain count
    private float densityAcc = 0.0f;  // Fix: density accumulator
    
    // Random number generator
    private final Random random = new Random();
    
    // Parameter storage
    private final Map<String, Float> parameters = new HashMap<>();
    
    /**
     * Grain class
     */
    private static class Grain {
        boolean active = false;        // Whether active
        int startIndex = 0;           // Start position
        float position = 0.0f;        // Current position
        float speed = 1.0f;           // Playback speed
        boolean reverse = false;      // Whether reverse
        float envelope = 0.0f;        // Envelope progress
        float envelopeStep = 0.0f;    // Envelope step
        float amplitude = 1.0f;       // Amplitude
        int grainLength = 0;          // Grain length (samples)
        
        void reset() {
            active = false;
            startIndex = 0;
            position = 0.0f;
            speed = 1.0f;
            reverse = false;
            envelope = 0.0f;
            envelopeStep = 0.0f;
            amplitude = 1.0f;
            grainLength = 0;
        }
    }
    
    public GranularEffect() {
        // Initialize default parameters
        parameters.put("grainSize", DEFAULT_GRAIN_SIZE);
        parameters.put("density", DEFAULT_DENSITY);
        parameters.put("pitch", DEFAULT_PITCH);
        parameters.put("jitter", DEFAULT_JITTER);
        parameters.put("reverseProb", DEFAULT_REVERSE_PROB);
        parameters.put("wet", DEFAULT_WET);
        
        // Initialize grain system
        grains = new Grain[MAX_GRAINS];
        for (int i = 0; i < MAX_GRAINS; i++) {
            grains[i] = new Grain();
        }
        
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
            case "grainSize":
                this.grainSize = clamp(value, MIN_GRAIN_SIZE, MAX_GRAIN_SIZE);
                parameters.put("grainSize", this.grainSize);
                break;
            case "density":
                this.density = clamp(value, MIN_DENSITY, MAX_DENSITY);
                parameters.put("density", this.density);
                break;
            case "pitch":
                this.pitch = clamp(value, MIN_PITCH, MAX_PITCH);
                parameters.put("pitch", this.pitch);
                break;
            case "jitter":
                this.jitter = clamp(value, MIN_JITTER, MAX_JITTER);
                parameters.put("jitter", this.jitter);
                break;
            case "reverseProb":
                this.reverseProb = clamp(value, MIN_REVERSE_PROB, MAX_REVERSE_PROB);
                parameters.put("reverseProb", this.reverseProb);
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
        float currentGrainSize = this.grainSize;
        float currentDensity = this.density;
        float currentPitch = this.pitch;
        float currentJitter = this.jitter;
        float currentReverseProb = this.reverseProb;
        float currentWet = this.wetDryRatio;
        
                // Calculate equal power mixing gains (fix equal power mixing)
        float dryGain = (float) Math.sqrt(1.0f - currentWet);
        float wetGain = (float) Math.sqrt(currentWet);
        
        // Process each sample
        for (int i = 0; i < bufferSize; i++) {
            float inputSample = inputBuffer[i];
            
            // Write to ring buffer
            ringBufferL[writeIndexL] = inputSample;
            ringBufferR[writeIndexR] = inputSample;
            
            // Update write pointers
            writeIndexL = (writeIndexL + 1) % ringSize;  // Fix: use ringSize
            writeIndexR = (writeIndexR + 1) % ringSize;  // Fix: use ringSize
            
            // Update grain system
            updateGrains(currentGrainSize, currentDensity, currentPitch, currentJitter, currentReverseProb);
            
            // Calculate grain output
            float grainOutputL = 0.0f;
            float grainOutputR = 0.0f;
            int activeGrainCount = 0;
            
            for (int g = 0; g < MAX_GRAINS; g++) {
                if (grains[g].active) {
                    float grainSampleL = getGrainSample(grains[g], true);
                    float grainSampleR = getGrainSample(grains[g], false);
                    
                    // Apply envelope
                    float envelope = getEnvelope(grains[g].envelope);
                    grainOutputL += grainSampleL * envelope * grains[g].amplitude;
                    grainOutputR += grainSampleR * envelope * grains[g].amplitude;
                    activeGrainCount++;
                    
                    // Update grain
                    updateGrain(grains[g]);
                }
            }
            
            // Normalize grain output (prevent overload)
            if (activeGrainCount > 0) {
                float normalization = 1.0f / (float) Math.sqrt(activeGrainCount);
                grainOutputL *= normalization;
                grainOutputR *= normalization;
                
                // Fix: remove boostFactor to avoid volume fluctuation with grain count
                // float boostFactor = 2.0f + (activeGrainCount * 0.1f);
                // grainOutputL *= boostFactor;
                // grainOutputR *= boostFactor;
            }
            
            // Calculate output (fix equal power mixing)
            float drySignal = inputSample * dryGain;
            float wetSignalL = grainOutputL * wetGain;
            float wetSignalR = grainOutputR * wetGain;
            
            // Stereo output (simplified to mono mix)
            float wetSignal = (wetSignalL + wetSignalR) * 0.5f;
            
            // Ensure there's output (prevent output=0 when wet=1)
            if (currentWet > 0.0f && Math.abs(wetSignal) < 0.001f) {
                                  wetSignal = inputSample * 0.1f; // At least some effect
            }
            
            // Equal power mixing: ensure constant total power
            float totalPower = drySignal * drySignal + wetSignal * wetSignal;
                          float targetPower = inputSample * inputSample; // Target power
            float powerRatio = (float) Math.sqrt(targetPower / Math.max(totalPower, 0.001f));
            
            outputBuffer[i] = (drySignal + wetSignal) * powerRatio;
        }
    }
    
    @Override
    public void reset() {
        // Reset ring buffer
        if (ringBufferL != null) {
            for (int i = 0; i < ringSize; i++) {  // Fix: use ringSize
                ringBufferL[i] = 0.0f;
                ringBufferR[i] = 0.0f;
            }
        }
        
        // Reset grain system
        for (int i = 0; i < MAX_GRAINS; i++) {
            grains[i].reset();
        }
        activeGrains = 0;
        densityAcc = 0.0f;  // Fix: reset density accumulator
        
        // Reset pointers
        writeIndexL = 0;
        writeIndexR = 0;
    }
    
    /**
     * Initialize buffers
     */
    private void initializeBuffers() {
        // Allocate 5-second ring buffer
        ringSize = (int)(5.0f * sampleRate);  // Fix: use ringSize
        ringBufferL = new float[ringSize];
        ringBufferR = new float[ringSize];
        
        // Clear buffers
                    for (int i = 0; i < ringSize; i++) {  // Fix: use ringSize
            ringBufferL[i] = 0.0f;
            ringBufferR[i] = 0.0f;
        }
    }
    
    /**
     * Update grain system
     */
    private void updateGrains(float grainSize, float density, float pitch, float jitter, float reverseProb) {
        // Fix: use accumulator method, linearly controllable
        densityAcc += density / sampleRate;  // Accumulate expected grain rate per sample
        while (densityAcc >= 1.0f) {
            triggerGrain(grainSize, pitch, jitter, reverseProb);
            densityAcc -= 1.0f;
        }
        
        // Fix: remove fallback trigger and extra probability trigger, let accumulator decide grain triggering
        // Removed: if (activeGrains < 8 && density > 0.1f)
        // Removed: extraTriggerProb trigger
    }
    
    /**
     * Trigger new grain
     */
    private void triggerGrain(float grainSize, float pitch, float jitter, float reverseProb) {
        // Find free grain slot
        int grainIndex = -1;
        for (int i = 0; i < MAX_GRAINS; i++) {
            if (!grains[i].active) {
                grainIndex = i;
                break;
            }
        }
        
        if (grainIndex == -1) {
            // No free slot, reuse oldest grain
            grainIndex = 0;
        }
        
        Grain grain = grains[grainIndex];
        
        // Calculate grain parameters
        int grainLengthSamples = (int)(grainSize * sampleRate / 1000.0f);
        grainLengthSamples = Math.max(grainLengthSamples, 1);
        
        // Random start position (consider jitter)
        int maxStartIndex = ringSize - grainLengthSamples;  // Fix: use ringSize
        int baseStartIndex = (writeIndexL - grainLengthSamples + ringSize) % ringSize;  // Fix: use ringSize
        int jitterRange = Math.max(1, (int)(maxStartIndex * jitter)); // Ensure at least 1
        int startIndex = baseStartIndex + random.nextInt(jitterRange * 2) - jitterRange;
        startIndex = (startIndex + ringSize) % ringSize;  // Fix: use ringSize
        
        // Random playback speed (consider jitter)
        float speedJitter = jitter * 1.5f; // Further increase randomization strength
        float speed = pitch + (random.nextFloat() - 0.5f) * speedJitter;
        speed = clamp(speed, 0.1f, 4.0f);
        
        // Random reverse (based on probability)
        boolean reverse = random.nextFloat() < reverseProb;
        
        // Random amplitude (enhance randomization)
        float amplitudeVariation = 0.4f + random.nextFloat() * 1.2f; // 0.4-1.6 random amplitude (larger range)
        
        // Random phase offset (increase randomization)
        float phaseOffset = (random.nextFloat() - 0.5f) * jitter * 0.5f;
        
        // Initialize grain
        grain.active = true;
        grain.startIndex = startIndex;
        grain.position = reverse ? grainLengthSamples - 1 + phaseOffset : phaseOffset;
        grain.speed = reverse ? -speed : speed;
        grain.reverse = reverse;
        grain.envelope = 0.0f;
        grain.envelopeStep = 1.0f / grainLengthSamples;
        grain.amplitude = amplitudeVariation;
        grain.grainLength = grainLengthSamples;
        
        activeGrains++;
    }
    
    /**
     * Update single grain
     */
    private void updateGrain(Grain grain) {
        // Update position
        grain.position += grain.speed;
        
        // Update envelope
        grain.envelope += grain.envelopeStep;
        
        // Check if grain is finished
        if (grain.envelope >= 1.0f || 
            (grain.reverse && grain.position < 0) || 
            (!grain.reverse && grain.position >= grain.grainLength)) {
            grain.active = false;
            activeGrains--;
        }
    }
    
    /**
     * Get grain sample
     */
    private float getGrainSample(Grain grain, boolean isLeft) {
        float[] ringBuffer = isLeft ? ringBufferL : ringBufferR;
        
        // Calculate read position (use float position for interpolation)
        float readPos = grain.startIndex + grain.position;
        int readIndex = (int) readPos;
        float fraction = readPos - readIndex;
        
        // Boundary check
        readIndex = (readIndex + ringSize) % ringSize;  // Fix: use ringSize
        int nextIndex = (readIndex + 1) % ringSize;  // Fix: use ringSize
        
        // Linear interpolation
        float sample1 = ringBuffer[readIndex];
        float sample2 = ringBuffer[nextIndex];
        float sample = sample1 + (sample2 - sample1) * fraction;
        
        // Apply amplitude
        return sample * grain.amplitude;
    }
    
    /**
     * Get envelope value (enhanced Hann window)
     */
    private float getEnvelope(float envelope) {
        // Enhanced Hann window: sin²(π * envelope) + slight modulation
        float sinValue = (float) Math.sin(Math.PI * envelope);
        float envelopeValue = sinValue * sinValue;
        
        // Add slight modulation to enhance effect
        float modulation = 0.15f * (float) Math.sin(2.0 * Math.PI * envelope * 3.0f);
        
        // Add randomization modulation
        float randomModulation = 0.1f * (float) Math.sin(2.0 * Math.PI * envelope * 7.0f);
        
        // Fix: envelope function may be >1, need to clamp
        return clamp(envelopeValue + modulation + randomModulation, 0.0f, 1.0f);
    }
    
    /**
     * Clamp value to specified range
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
