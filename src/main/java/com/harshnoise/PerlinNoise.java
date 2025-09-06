package com.harshnoise;

import java.util.Random;

/**
 * PerlinNoise - Corrected implementation with proper 1D/2D Perlin noise
 * Supports fBM (fractal Brownian motion), time warping, and optimized performance
 */
public class PerlinNoise {
    
    private static final int PERMUTATION_SIZE = 256;
    private static final int PERMUTATION_MASK = PERMUTATION_SIZE - 1;
    
    // 2D gradient vectors for proper 2D Perlin noise
    private static final float[] GRADIENT_2D = {
        1.0f, 1.0f,   -1.0f, 1.0f,   1.0f, -1.0f,   -1.0f, -1.0f,
        1.0f, 0.0f,   -1.0f, 0.0f,   0.0f, 1.0f,    0.0f, -1.0f
    };
    private static final int GRADIENT_2D_SIZE = GRADIENT_2D.length / 2;
    
    private final int[] permutation;
    private final float[] gradients1D; // 1D gradients as float for performance
    
    public PerlinNoise() {
        this(System.currentTimeMillis());
    }
    
    public PerlinNoise(long seed) {
        Random random = new Random(seed);
        
        // Initialize permutation table
        permutation = new int[PERMUTATION_SIZE * 2];
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            permutation[i] = i;
        }
        
        // Shuffle permutation table
        for (int i = PERMUTATION_SIZE - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Duplicate for overflow handling
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            permutation[PERMUTATION_SIZE + i] = permutation[i];
        }
        
        // Initialize 1D gradients as float for performance
        gradients1D = new float[PERMUTATION_SIZE * 2];
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            // Use discrete gradients {+1, -1} for proper 1D Perlin
            gradients1D[i] = random.nextBoolean() ? 1.0f : -1.0f;
            gradients1D[PERMUTATION_SIZE + i] = gradients1D[i];
        }
    }
    
    /**
     * Reseed the noise generator without creating a new object
     */
    public void reseed(long seed) {
        Random random = new Random(seed);
        
        // Reshuffle permutation table
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            permutation[i] = i;
        }
        
        for (int i = PERMUTATION_SIZE - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Duplicate for overflow handling
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            permutation[PERMUTATION_SIZE + i] = permutation[i];
        }
        
        // Reinitialize 1D gradients
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            gradients1D[i] = random.nextBoolean() ? 1.0f : -1.0f;
            gradients1D[PERMUTATION_SIZE + i] = gradients1D[i];
        }
    }
    
    /**
     * Quintic fade function: 6t^5 - 15t^4 + 10t^3
     * Provides smooth interpolation for Perlin noise
     */
    private static final float fade(float t) {
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }
    
    /**
     * Linear interpolation helper
     */
    private static final float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
    
    /**
     * Hash function for 1D coordinates
     */
    private final int hash(int x) {
        return permutation[x & PERMUTATION_MASK];
    }
    
    /**
     * Hash function for 2D coordinates
     */
    private final int hash(int x, int y) {
        return permutation[(permutation[x & PERMUTATION_MASK] + y) & PERMUTATION_MASK];
    }
    
    /**
     * Generate correct 1D Perlin noise
     * Uses proper gradient dot products and quintic fade interpolation
     */
    public double noise(double x) {
        // Convert to float for performance
        float fx = (float) x;
        
        // Compute lattice coordinates
        int x0 = (int) Math.floor(fx);
        int x1 = x0 + 1;
        
        // Compute displacement from lattice points
        float dx = fx - x0;
        
        // Hash gradients
        int h0 = hash(x0);
        int h1 = hash(x1);
        
        // Get discrete gradients {+1, -1}
        float g0 = gradients1D[h0];
        float g1 = gradients1D[h1];
        
        // Compute dot products with displacement vectors
        float n0 = g0 * dx;
        float n1 = g1 * (dx - 1.0f);
        
        // Apply quintic fade
        float t = fade(dx);
        
        // Interpolate and normalize to [-1, 1] range
        float result = lerp(n0, n1, t);
        
        // Scale to maintain qualitative range similar to previous implementation
        return result * 0.5;
    }
    
    /**
     * Generate proper 2D Perlin noise
     * Uses four lattice corners with 2D gradient vectors
     */
    private double noise2D(double x, double y) {
        // Convert to float for performance
        float fx = (float) x;
        float fy = (float) y;
        
        // Compute lattice coordinates
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        
        // Compute displacement from lattice points
        float dx = fx - x0;
        float dy = fy - y0;
        
        // Hash gradients for four corners
        int h00 = hash(x0, y0);
        int h10 = hash(x1, y0);
        int h01 = hash(x0, y1);
        int h11 = hash(x1, y1);
        
        // Get 2D gradient vectors
        int g00 = h00 % GRADIENT_2D_SIZE;
        int g10 = h10 % GRADIENT_2D_SIZE;
        int g01 = h01 % GRADIENT_2D_SIZE;
        int g11 = h11 % GRADIENT_2D_SIZE;
        
        // Compute dot products with displacement vectors
        float n00 = GRADIENT_2D[g00 * 2] * dx + GRADIENT_2D[g00 * 2 + 1] * dy;
        float n10 = GRADIENT_2D[g10 * 2] * (dx - 1.0f) + GRADIENT_2D[g10 * 2 + 1] * dy;
        float n01 = GRADIENT_2D[g01 * 2] * dx + GRADIENT_2D[g01 * 2 + 1] * (dy - 1.0f);
        float n11 = GRADIENT_2D[g11 * 2] * (dx - 1.0f) + GRADIENT_2D[g11 * 2 + 1] * (dy - 1.0f);
        
        // Apply quintic fade on both axes
        float tx = fade(dx);
        float ty = fade(dy);
        
        // Bilinear interpolation
        float nx0 = lerp(n00, n10, tx);
        float nx1 = lerp(n01, n11, tx);
        float result = lerp(nx0, nx1, ty);
        
        // Normalize to [-1, 1] range
        return result * 0.5;
    }
    
    /**
     * Generate fBM (fractal Brownian motion) noise with multiple octaves
     * @param x Input coordinate
     * @param octaves Number of octaves to combine
     * @param persistence How much each octave contributes (typically 0.5)
     * @param lacunarity How much each octave is scaled (typically 2.0)
     * @return fBM noise value in range [-1, 1]
     */
    public double fbm(double x, int octaves, double persistence, double lacunarity) {
        double amplitude = 1.0;
        double frequency = 1.0;
        double total = 0.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        // Normalize to [-1, 1]
        return total / maxValue;
    }
    
    /**
     * Generate fBM noise with default parameters (octaves=3, persistence=0.55, lacunarity=2.0)
     */
    public double fbm(double x) {
        return fbm(x, 3, 0.55, 2.0);
    }
    
    /**
     * Generate time-warped fBM noise
     * @param x Base coordinate
     * @param warpX Warp coordinate (typically slower)
     * @param octaves Number of octaves
     * @param persistence Persistence value
     * @param lacunarity Lacunarity value
     * @param warpAmplitude Amplitude of time warping
     * @return Warped fBM noise value
     */
    public double warpedFbm(double x, double warpX, int octaves, double persistence, 
                           double lacunarity, double warpAmplitude) {
        // Generate warp offset using corrected noise
        double warpOffset = noise(warpX) * warpAmplitude;
        
        // Apply warp to main coordinate
        double warpedX = x + warpOffset;
        
        // Generate fBM with warped coordinate
        return fbm(warpedX, octaves, persistence, lacunarity);
    }
    
    /**
     * Generate time-warped fBM noise with default parameters
     */
    public double warpedFbm(double x, double warpX) {
        return warpedFbm(x, warpX, 3, 0.55, 2.0, 0.1);
    }
    
    /**
     * Generate multi-dimensional fBM noise using proper 2D Perlin
     */
    public double fbm2D(double x, double y, int octaves, double persistence, double lacunarity) {
        double amplitude = 1.0;
        double frequency = 1.0;
        double total = 0.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Generate turbulence noise (absolute value of fBM)
     */
    public double turbulence(double x, int octaves, double persistence, double lacunarity) {
        return Math.abs(fbm(x, octaves, persistence, lacunarity));
    }
    
    /**
     * Generate ridged noise (1 - turbulence)
     */
    public double ridgedNoise(double x, int octaves, double persistence, double lacunarity) {
        return 1.0 - turbulence(x, octaves, persistence, lacunarity);
    }
    
    /**
     * Get permutation table for debugging/testing
     */
    public int[] getPermutation() {
        return permutation.clone();
    }
    
    /**
     * Get gradients for debugging/testing
     */
    public double[] getGradients() {
        double[] result = new double[gradients1D.length];
        for (int i = 0; i < gradients1D.length; i++) {
            result[i] = gradients1D[i];
        }
        return result;
    }
}
