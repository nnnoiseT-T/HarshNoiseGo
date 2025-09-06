package com.harshnoise;

import java.util.Random;

/**
 * Optimized NoiseGenerator class for real-time audio performance
 * Implements white noise, pink noise, and granular noise generation with CPU optimizations
 */
public class NoiseGenerator {
    
    // Static lookup table for sine function
    private static final int SIN_N = 1024;
    private static final float[] SIN = new float[SIN_N];
    static {
        for (int i = 0; i < SIN_N; i++) {
            SIN[i] = (float) Math.sin(2 * Math.PI * i / SIN_N);
        }
    }
    
    // Xorshift32 PRNG for performance
    private int rng = 0x12345678;
    
    // LFO state
    private float lfoPhase = 0f;
    private float lfoInc = 0f;
    
    // Blue/Violet noise state
    private float lastWhite = 0f;
    private float prevWhite = 0f;
    
    // Pink/Brown noise DC blocker state
    private float pinkDcPrev = 0f;
    private float pinkDcOut = 0f;
    private float brownDcPrev = 0f;
    private float brownDcOut = 0f;
    
    // Granular noise state
    private float grainPhase = 0f;
    private float grainInc = 1f;
    
    private final Random random = new Random();
    private final double[] pinkBuffer = new double[7];
    private final double[] brownBuffer = new double[3]; // Brown noise buffer
    private final double[] granularBuffer;
    private int granularIndex = 0;
    
    // Pink noise filter coefficients
    private static final double[] PINK_COEFFICIENTS = {0.99886, 0.99332, 0.96900, 0.86650, 0.55000, 0.7616, 0.0};
    
    public NoiseGenerator() {
        // Initialize granular buffer with random values
        granularBuffer = new double[1024];
        for (int i = 0; i < granularBuffer.length; i++) {
            granularBuffer[i] = (random.nextDouble() * 2.0 - 1.0) * 0.5;
        }
    }
    
    /**
     * Constructor with seed for deterministic output
     */
    public NoiseGenerator(long seed) {
        this();
        this.rng = (int) (seed ^ (seed >>> 32));
    }
    
    /**
     * Reset all filter states and indices
     */
    public void reset() {
        // Reset PRNG
        rng = 0x12345678;
        
        // Reset LFO
        lfoPhase = 0f;
        lfoInc = 0f;
        
        // Reset Blue/Violet state
        lastWhite = 0f;
        prevWhite = 0f;
        
        // Reset DC blockers
        pinkDcPrev = 0f;
        pinkDcOut = 0f;
        brownDcPrev = 0f;
        brownDcOut = 0f;
        
        // Reset granular state
        grainPhase = 0f;
        grainInc = 1f;
        
        // Reset filter buffers
        for (int i = 0; i < pinkBuffer.length; i++) {
            pinkBuffer[i] = 0.0;
        }
        for (int i = 0; i < brownBuffer.length; i++) {
            brownBuffer[i] = 0.0;
        }
        
        // Reset granular buffer
        granularIndex = 0;
        for (int i = 0; i < granularBuffer.length; i++) {
            granularBuffer[i] = (random.nextDouble() * 2.0 - 1.0) * 0.5;
        }
        
        // Reset low-pass filter
        lastSample = 0.0;
    }
    
    /**
     * Set LFO frequency in Hz
     */
    public void setLfoHz(float hz, float sampleRate) {
        lfoInc = hz / sampleRate;
    }
    
    /**
     * Xorshift32 PRNG returning float in [-1, 1]
     */
    private float frand() {
        rng ^= rng << 13;
        rng ^= rng >>> 17;
        rng ^= rng << 5;
        return (rng & 0x7FFFFFFF) / 1073741824.0f - 1.0f;
    }
    
    /**
     * Fast sine lookup with phase normalized to [0, 1)
     */
    private float fastSin(float phase01) {
        float index = phase01 * SIN_N;
        int i = (int) index;
        float frac = index - i;
        int i1 = (i + 1) % SIN_N;
        return SIN[i] + frac * (SIN[i1] - SIN[i]);
    }
    
    /**
     * Prevent denormal numbers
     */
    private static float undenorm(float v) {
        return Math.abs(v) < 1e-20f ? 0f : v;
    }
    
    /**
     * Soft clipping function
     */
    private static float softClip(float x) {
        return (float) Math.tanh(x * 1.2f);
    }
    
    /**
     * Generate white noise sample
     * @return white noise value between -1.0 and 1.0
     */
    public double generateWhiteNoise() {
        return softClip(frand() * 0.8f); // Keep ~0.8 amplitude
    }
    
    /**
     * Generate pink noise sample using filtering with DC blocker
     * @return pink noise value between -1.0 and 1.0
     */
    public double generatePinkNoise() {
        float white = (float) generateWhiteNoise();
        
        // Apply pink noise filter
        pinkBuffer[0] = 0.99886 * pinkBuffer[0] + white * 0.0555179;
        pinkBuffer[1] = 0.99332 * pinkBuffer[1] + white * 0.0750759;
        pinkBuffer[2] = 0.96900 * pinkBuffer[2] + white * 0.1538520;
        pinkBuffer[3] = 0.86650 * pinkBuffer[3] + white * 0.3104856;
        pinkBuffer[4] = 0.55000 * pinkBuffer[4] + white * 0.5329522;
        pinkBuffer[5] = 0.7616 * pinkBuffer[5] - white * 0.0168980;
        
        float result = (float) (pinkBuffer[0] + pinkBuffer[1] + pinkBuffer[2] + 
                               pinkBuffer[3] + pinkBuffer[4] + pinkBuffer[5]);
        
        // Apply DC blocker
        float dcOut = result - pinkDcPrev + 0.995f * pinkDcOut;
        pinkDcPrev = result;
        pinkDcOut = dcOut;
        
        return undenorm(softClip(dcOut * 0.8f));
    }
    
    /**
     * Generate brown noise sample (1/f² spectrum) with DC blocker
     * @return brown noise value between -1.0 and 1.0
     */
    public double generateBrownNoise() {
        float white = (float) generateWhiteNoise();
        
        // Apply brown noise filter (stronger low-pass than pink)
        brownBuffer[0] = 0.9995 * brownBuffer[0] + white * 0.0224;
        brownBuffer[1] = 0.9990 * brownBuffer[1] + white * 0.0316;
        brownBuffer[2] = 0.9980 * brownBuffer[2] + white * 0.0447;
        
        float result = (float) (brownBuffer[0] + brownBuffer[1] + brownBuffer[2]);
        
        // Apply DC blocker
        float dcOut = result - brownDcPrev + 0.995f * brownDcOut;
        brownDcPrev = result;
        brownDcOut = dcOut;
        
        return undenorm(softClip(dcOut * 0.9f));
    }
    
    /**
     * Generate granular noise sample with optimized phase accumulation
     * @param grainSize size of the grain (affects repetition pattern)
     * @return granular noise value between -1.0 and 1.0
     */
    public double generateGranularNoise(int grainSize) {
        if (grainSize <= 0) grainSize = 1;
        
        // Update grain phase
        grainInc = 1f / Math.max(1, grainSize);
        grainPhase = (grainPhase + grainInc) % 1f;
        
        // Get sample from granular buffer
        float sample = (float) granularBuffer[granularIndex];
        
        // Move to next sample
        granularIndex = (granularIndex + 1) % granularBuffer.length;
        
        // Create rhythmic pattern using fast sine lookup
        float rhythm = fastSin(grainPhase) * 0.5f;
        
        // Event-based grain triggering
        float grainDensity = Math.max(0.1f, 1f - (grainSize / 128f));
        if (frand() < grainDensity * 0.1f) {
            // Stronger effect for smaller grain sizes
            float grainIntensity = Math.max(0.3f, 1.0f - (grainSize / 128.0f));
            sample += frand() * grainIntensity;
            
            // Add rhythmic modulation
            sample *= (1.0f + rhythm * 0.4f);
        }
        
        // Add frequency modulation for very small grain sizes
        if (grainSize < 16) {
            float fm = fastSin(grainPhase * 0.5f) * 0.3f;
            sample += fm * sample;
        }
        
        return undenorm(softClip(sample * 0.8f));
    }
    
    /**
     * Generate blue noise sample using first-order differencing
     * @return blue noise value between -1.0 and 1.0
     */
    public double generateBlueNoise() {
        float white = frand();
        
        // First-order differencing: y = white - lastWhite
        float blue = white - lastWhite;
        lastWhite = white;
        
        return undenorm(softClip(blue * 0.8f));
    }
    
    /**
     * Generate violet noise sample using second-order differencing
     * @return violet noise value between -1.0 and 1.0
     */
    public double generateVioletNoise() {
        float white = frand();
        
        // Second-order differencing: d1 = white - lastWhite, d2 = lastWhite - prevWhite, y = d1 - d2
        float d1 = white - lastWhite;
        float d2 = lastWhite - prevWhite;
        float violet = d1 - d2;
        
        prevWhite = lastWhite;
        lastWhite = white;
        
        return undenorm(softClip(violet * 0.6f));
    }
    
    /**
     * Generate impulse noise sample (random spikes)
     * @return impulse noise value between -1.0 and 1.0
     */
    public double generateImpulseNoise() {
        // Generate random impulses with low probability
        if (frand() < 0.05f) { // 5% chance of impulse
            // Strong impulse with random polarity
            return (frand() > 0 ? 1.0f : -1.0f) * (0.7f + Math.abs(frand()) * 0.3f);
        } else {
            // Normal noise level
            return frand() * 0.1f;
        }
    }
    
    /**
     * Generate modulated noise sample with phase accumulator LFO
     * @return modulated noise value between -1.0 and 1.0
     */
    public double generateModulatedNoise() {
        float white = frand();
        
        // Update LFO phase
        lfoPhase += lfoInc;
        if (lfoPhase >= 1f) {
            lfoPhase -= 1f;
        }
        
        // Compute LFO using fast sine lookup
        float lfo = 0.5f * (fastSin(lfoPhase) + 1f);
        
        // Apply modulation
        float modulated = white * lfo;
        
        return undenorm(softClip(modulated));
    }
    
    /**
     * Generate mixed noise from multiple sources
     * @param whiteLevel white noise level (0.0 to 1.0)
     * @param pinkLevel pink noise level (0.0 to 1.0)
     * @param granularLevel granular noise level (0.0 to 1.0)
     * @param brownLevel brown noise level (0.0 to 1.0)
     * @param blueLevel blue noise level (0.0 to 1.0)
     * @param violetLevel violet noise level (0.0 to 1.0)
     * @param impulseLevel impulse noise level (0.0 to 1.0)
     * @param modulatedLevel modulated noise level (0.0 to 1.0)
     * @param grainSize granular noise grain size (affects all noise types)
     * @return mixed noise value
     */
    public double generateMixedNoise(double whiteLevel, double pinkLevel, double granularLevel, 
                                   double brownLevel, double blueLevel, double violetLevel,
                                   double impulseLevel, double modulatedLevel, int grainSize) {
        float mixed = 0.0f;
        
        if (whiteLevel > 0.0) {
            float whiteSample = (float) generateWhiteNoise();
            // Apply grain size effect to white noise too
            if (grainSize < 64) {
                whiteSample *= 1.0f + fastSin(grainPhase) * 0.2f;
            }
            mixed += whiteSample * (float) whiteLevel;
        }
        
        if (pinkLevel > 0.0) {
            float pinkSample = (float) generatePinkNoise();
            // Apply grain size effect to pink noise too
            if (grainSize < 64) {
                pinkSample *= 1.0f + fastSin(grainPhase) * 0.15f;
            }
            mixed += pinkSample * (float) pinkLevel;
        }
        
        if (granularLevel > 0.0) {
            mixed += (float) generateGranularNoise(grainSize) * (float) granularLevel;
        }
        
        if (brownLevel > 0.0) {
            float brownSample = (float) generateBrownNoise();
            // Apply grain size effect to brown noise too
            if (grainSize < 64) {
                brownSample *= 1.0f + fastSin(grainPhase) * 0.1f;
            }
            mixed += brownSample * (float) brownLevel;
        }
        
        if (blueLevel > 0.0) {
            float blueSample = (float) generateBlueNoise();
            // Apply grain size effect to blue noise too
            if (grainSize < 64) {
                blueSample *= 1.0f + fastSin(grainPhase) * 0.25f;
            }
            mixed += blueSample * (float) blueLevel;
        }
        
        if (violetLevel > 0.0) {
            float violetSample = (float) generateVioletNoise();
            // Apply grain size effect to violet noise too
            if (grainSize < 64) {
                violetSample *= 1.0f + fastSin(grainPhase) * 0.3f;
            }
            mixed += violetSample * (float) violetLevel;
        }
        
        if (impulseLevel > 0.0) {
            mixed += (float) generateImpulseNoise() * (float) impulseLevel;
        }
        
        if (modulatedLevel > 0.0) {
            mixed += (float) generateModulatedNoise() * (float) modulatedLevel;
        }
        
        // Only normalize if multiple noise types are active to prevent volume reduction
        float totalLevel = (float) (whiteLevel + pinkLevel + granularLevel + brownLevel + 
                                   blueLevel + violetLevel + impulseLevel + modulatedLevel);
        if (totalLevel > 1.0f) {
            mixed /= totalLevel;
        }
        
        // Single soft clip with dither for denormal safety
        return undenorm(softClip(mixed + 1e-12f));
    }
    
    /**
     * Apply distortion effect to audio sample
     * @param sample input sample
     * @param distortionLevel distortion intensity (0.0 to 1.0)
     * @return distorted sample
     */
    public double applyDistortion(double sample, double distortionLevel) {
        if (distortionLevel <= 0.0) return sample;
        
        // Soft clipping distortion
        float distorted = (float) sample * (1.0f + (float) distortionLevel);
        return softClip(distorted) * (1.0f - (float) distortionLevel * 0.5f);
    }
    
    /**
     * Apply low-pass filter effect
     * @param sample input sample
     * @param filterLevel filter intensity (0.0 to 1.0)
     * @return filtered sample
     */
    public double applyLowPassFilter(double sample, double filterLevel) {
        if (filterLevel <= 0.0) return sample;
        
        // Simple low-pass filter
        float filtered = (float) lastSample + ((float) sample - (float) lastSample) * (1.0f - (float) filterLevel);
        lastSample = filtered;
        return undenorm(filtered);
    }
    
    // Instance variable for low-pass filter
    private double lastSample = 0.0;
} 