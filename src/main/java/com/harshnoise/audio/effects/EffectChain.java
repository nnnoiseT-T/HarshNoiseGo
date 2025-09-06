package com.harshnoise.audio.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Effect Chain Manager
 * Manages multiple effects in sequence, processing audio in series
 */
public class EffectChain {
    
    private final List<AudioEffect> effects;
    private final float[] tempBuffer;
    private final int maxBufferSize;
    private float sampleRate;
    private final Object processLock = new Object(); // Add processing lock
    
    /**
     * Constructor
     * @param maxBufferSize Maximum buffer size
     * @param sampleRate Sample rate
     */
    public EffectChain(int maxBufferSize, float sampleRate) {
        this.effects = new CopyOnWriteArrayList<>();
        this.maxBufferSize = maxBufferSize;
        this.tempBuffer = new float[maxBufferSize];
        this.sampleRate = sampleRate;
    }
    
    /**
     * Add effect to chain
     * @param effect Effect to add
     */
    public void addEffect(AudioEffect effect) {
        if (effect != null) {
            synchronized (processLock) {
                effect.setSampleRate(sampleRate);
                effects.add(effect);
            }
        }
    }
    
    /**
     * Remove effect from chain
     * @param effect Effect to remove
     * @return true if successfully removed, false if effect doesn't exist
     */
    public boolean removeEffect(AudioEffect effect) {
        synchronized (processLock) {
            return effects.remove(effect);
        }
    }
    
    /**
     * Remove effect by index
     * @param index Effect index
     * @return Removed effect, or null if index is invalid
     */
    public AudioEffect removeEffect(int index) {
        synchronized (processLock) {
            if (index >= 0 && index < effects.size()) {
                return effects.remove(index);
            }
            return null;
        }
    }
    
    /**
     * Reorder effects
     * @param fromIndex Source index
     * @param toIndex Target index
     * @return true if successfully reordered, false if indices are invalid
     */
    public boolean reorderEffect(int fromIndex, int toIndex) {
        synchronized (processLock) {
            if (fromIndex >= 0 && fromIndex < effects.size() && 
                toIndex >= 0 && toIndex < effects.size()) {
                AudioEffect effect = effects.remove(fromIndex);
                effects.add(toIndex, effect);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Get effect chain snapshot
     * @return Copy of effects list
     */
    public List<AudioEffect> getEffectsSnapshot() {
        synchronized (processLock) {
            return new ArrayList<>(effects);
        }
    }
    
    /**
     * Get number of effects in chain
     * @return Number of effects
     */
    public int getEffectCount() {
        synchronized (processLock) {
            return effects.size();
        }
    }
    
    /**
     * Get effect by index
     * @param index Effect index
     * @return Effect instance, or null if index is invalid
     */
    public AudioEffect getEffect(int index) {
        synchronized (processLock) {
            if (index >= 0 && index < effects.size()) {
                return effects.get(index);
            }
            return null;
        }
    }
    
    /**
     * Process audio buffer
     * Call all enabled effects in sequence
     * @param inputBuffer Input audio buffer
     * @param outputBuffer Output audio buffer
     * @param bufferSize Buffer size
     */
    public void process(float[] inputBuffer, float[] outputBuffer, int bufferSize) {
        if (bufferSize > maxBufferSize) {
            throw new IllegalArgumentException("Buffer size exceeds maximum allowed size");
        }
        
        // If no effects, copy input to output directly
        if (effects.isEmpty()) {
            System.arraycopy(inputBuffer, 0, outputBuffer, 0, bufferSize);
            return;
        }
        
        // Copy input to temporary buffer
        System.arraycopy(inputBuffer, 0, tempBuffer, 0, bufferSize);
        
        // Process effects one by one (use snapshot to avoid concurrent modification issues)
        List<AudioEffect> effectsSnapshot = getEffectsSnapshot();
        for (AudioEffect effect : effectsSnapshot) {
            if (effect.isEnabled()) {
                effect.process(tempBuffer, outputBuffer, bufferSize);
                // Copy output back to temporary buffer for next effect
                System.arraycopy(outputBuffer, 0, tempBuffer, 0, bufferSize);
            }
        }
        
        // Final output is in outputBuffer
    }
    
    /**
     * Set sample rate
     * @param sampleRate New sample rate
     */
    public void setSampleRate(float sampleRate) {
        synchronized (processLock) {
            this.sampleRate = sampleRate;
            // Update sample rate for all effects
            for (AudioEffect effect : effects) {
                effect.setSampleRate(sampleRate);
            }
        }
    }
    
    /**
     * Get current sample rate
     * @return Sample rate
     */
    public float getSampleRate() {
        synchronized (processLock) {
            return sampleRate;
        }
    }
    
    /**
     * Reset all effects
     */
    public void reset() {
        synchronized (processLock) {
            for (AudioEffect effect : effects) {
                effect.reset();
            }
        }
    }
    
    /**
     * Clear effect chain
     */
    public void clear() {
        synchronized (processLock) {
            effects.clear();
        }
    }
}
