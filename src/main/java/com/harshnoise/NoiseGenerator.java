package com.harshnoise;

import java.util.Random;

/**
 * NoiseGenerator class responsible for generating different types of noise
 * Implements white noise, pink noise, and granular noise generation
 */
public class NoiseGenerator {
    
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
     * Generate white noise sample
     * @return white noise value between -1.0 and 1.0
     */
    public double generateWhiteNoise() {
        return (random.nextDouble() * 2.0 - 1.0) * 0.8; // Boost amplitude
    }
    
    /**
     * Generate pink noise sample using filtering
     * @return pink noise value between -1.0 and 1.0
     */
    public double generatePinkNoise() {
        double white = generateWhiteNoise();
        
        // Apply pink noise filter
        pinkBuffer[0] = 0.99886 * pinkBuffer[0] + white * 0.0555179;
        pinkBuffer[1] = 0.99332 * pinkBuffer[1] + white * 0.0750759;
        pinkBuffer[2] = 0.96900 * pinkBuffer[2] + white * 0.1538520;
        pinkBuffer[3] = 0.86650 * pinkBuffer[3] + white * 0.3104856;
        pinkBuffer[4] = 0.55000 * pinkBuffer[4] + white * 0.5329522;
        pinkBuffer[5] = 0.7616 * pinkBuffer[5] - white * 0.0168980;
        
        double result = pinkBuffer[0] + pinkBuffer[1] + pinkBuffer[2] + pinkBuffer[3] + pinkBuffer[4] + pinkBuffer[5];
        return Math.max(-1.0, Math.min(1.0, result * 0.8)); // Boost amplitude
    }
    
    /**
     * Generate brown noise sample (1/f² spectrum)
     * @return brown noise value between -1.0 and 1.0
     */
    public double generateBrownNoise() {
        double white = generateWhiteNoise();
        
        // Apply brown noise filter (stronger low-pass than pink)
        brownBuffer[0] = 0.9995 * brownBuffer[0] + white * 0.0224;
        brownBuffer[1] = 0.9990 * brownBuffer[1] + white * 0.0316;
        brownBuffer[2] = 0.9980 * brownBuffer[2] + white * 0.0447;
        
        double result = brownBuffer[0] + brownBuffer[1] + brownBuffer[2];
        return Math.max(-1.0, Math.min(1.0, result * 0.9)); // Boost amplitude for brown noise
    }
    
    /**
     * Generate granular noise sample
     * @param grainSize size of the grain (affects repetition pattern)
     * @return granular noise value between -1.0 and 1.0
     */
    public double generateGranularNoise(int grainSize) {
        if (grainSize <= 0) grainSize = 1;
        
        // Get sample from granular buffer
        double sample = granularBuffer[granularIndex];
        
        // Move to next sample
        granularIndex = (granularIndex + 1) % granularBuffer.length;
        
        // Much more dramatic grain size effect
        double normalizedGrainSize = Math.max(1, grainSize);
        
        // Create rhythmic pattern based on grain size
        double rhythm = Math.sin(granularIndex * (2.0 * Math.PI / normalizedGrainSize)) * 0.5;
        
        // Add grain effect - smaller grain size = more dramatic effect
        if (random.nextInt(Math.max(1, (int)(normalizedGrainSize / 2))) == 0) {
            // Stronger effect for smaller grain sizes
            double grainIntensity = Math.max(0.3, 1.0 - (normalizedGrainSize / 128.0));
            sample += (random.nextDouble() * 2.0 - 1.0) * grainIntensity;
            
            // Add rhythmic modulation
            sample *= (1.0 + rhythm * 0.4);
        }
        
        // Add frequency modulation for very small grain sizes
        if (normalizedGrainSize < 16) {
            double fm = Math.sin(granularIndex * 0.5) * 0.3;
            sample += fm * sample;
        }
        
        return Math.max(-1.0, Math.min(1.0, sample * 0.8)); // Boost amplitude
    }
    
    /**
     * Generate blue noise sample (f spectrum - high frequency emphasis)
     * @return blue noise value between -1.0 and 1.0
     */
    public double generateBlueNoise() {
        double white = generateWhiteNoise();
        
        // High-pass filter to emphasize high frequencies
        double blue = white * 0.8;
        blue += (random.nextDouble() * 2.0 - 1.0) * 0.6; // Add high frequency content
        
        return Math.max(-1.0, Math.min(1.0, blue));
    }
    
    /**
     * Generate violet noise sample (f² spectrum - ultra high frequency emphasis)
     * @return violet noise value between -1.0 and 1.0
     */
    public double generateVioletNoise() {
        double white = generateWhiteNoise();
        
        // Ultra high-pass filter for extreme high frequencies
        double violet = white * 0.6;
        violet += (random.nextDouble() * 2.0 - 1.0) * 0.8; // Strong high frequency emphasis
        
        return Math.max(-1.0, Math.min(1.0, violet));
    }
    
    /**
     * Generate impulse noise sample (random spikes)
     * @return impulse noise value between -1.0 and 1.0
     */
    public double generateImpulseNoise() {
        // Generate random impulses with low probability
        if (random.nextDouble() < 0.05) { // 5% chance of impulse
            // Strong impulse with random polarity
            return (random.nextBoolean() ? 1.0 : -1.0) * (0.7 + random.nextDouble() * 0.3);
        } else {
            // Normal noise level
            return (random.nextDouble() * 2.0 - 1.0) * 0.1;
        }
    }
    
    /**
     * Generate modulated noise sample (LFO modulated)
     * @return modulated noise value between -1.0 and 1.0
     */
    public double generateModulatedNoise() {
        double white = generateWhiteNoise();
        
        // Simple LFO modulation
        double lfo = Math.sin(System.currentTimeMillis() * 0.001) * 0.5 + 0.5;
        
        // Apply modulation
        double modulated = white * lfo;
        
        return Math.max(-1.0, Math.min(1.0, modulated));
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
        double mixed = 0.0;
        
        if (whiteLevel > 0.0) {
            double whiteSample = generateWhiteNoise();
            // Apply grain size effect to white noise too
            if (grainSize < 64) {
                whiteSample *= 1.0 + Math.sin(granularIndex * (2.0 * Math.PI / Math.max(1, grainSize))) * 0.2;
            }
            mixed += whiteSample * whiteLevel;
        }
        
        if (pinkLevel > 0.0) {
            double pinkSample = generatePinkNoise();
            // Apply grain size effect to pink noise too
            if (grainSize < 64) {
                pinkSample *= 1.0 + Math.sin(granularIndex * (2.0 * Math.PI / Math.max(1, grainSize))) * 0.15;
            }
            mixed += pinkSample * pinkLevel;
        }
        
        if (granularLevel > 0.0) {
            mixed += generateGranularNoise(grainSize) * granularLevel;
        }
        
        if (brownLevel > 0.0) {
            double brownSample = generateBrownNoise();
            // Apply grain size effect to brown noise too
            if (grainSize < 64) {
                brownSample *= 1.0 + Math.sin(granularIndex * (2.0 * Math.PI / Math.max(1, grainSize))) * 0.1;
            }
            mixed += brownSample * brownLevel;
        }
        
        if (blueLevel > 0.0) {
            double blueSample = generateBlueNoise();
            // Apply grain size effect to blue noise too
            if (grainSize < 64) {
                blueSample *= 1.0 + Math.sin(granularIndex * (2.0 * Math.PI / Math.max(1, grainSize))) * 0.25;
            }
            mixed += blueSample * blueLevel;
        }
        
        if (violetLevel > 0.0) {
            double violetSample = generateVioletNoise();
            // Apply grain size effect to violet noise too
            if (grainSize < 64) {
                violetSample *= 1.0 + Math.sin(granularIndex * (2.0 * Math.PI / Math.max(1, grainSize))) * 0.3;
            }
            mixed += violetSample * violetLevel;
        }
        
        if (impulseLevel > 0.0) {
            mixed += generateImpulseNoise() * impulseLevel;
        }
        
        if (modulatedLevel > 0.0) {
            mixed += generateModulatedNoise() * modulatedLevel;
        }
        
        // Only normalize if multiple noise types are active to prevent volume reduction
        double totalLevel = whiteLevel + pinkLevel + granularLevel + brownLevel + blueLevel + violetLevel + impulseLevel + modulatedLevel;
        if (totalLevel > 1.0) {
            mixed /= totalLevel;
        }
        
        return Math.max(-1.0, Math.min(1.0, mixed));
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
        double distorted = sample * (1.0 + distortionLevel);
        return Math.tanh(distorted) * (1.0 - distortionLevel * 0.5);
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
        double filtered = lastSample + (sample - lastSample) * (1.0 - filterLevel);
        lastSample = filtered;
        return filtered;
    }
    
    // Instance variable for low-pass filter
    private double lastSample = 0.0;
} 