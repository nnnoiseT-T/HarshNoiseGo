package com.harshnoise.model;

import java.util.Observable;

/**
 * Optimized AudioModel - Manages audio state and parameters with performance enhancements
 * Part of MVC architecture for HarshNoiseGo
 */
public class AudioModel extends Observable {
    
    private static final double EPSILON = 1e-9; // Epsilon for double comparison
    
    // Audio parameters - volatile for cross-thread visibility
    private volatile double whiteNoiseLevel = 0.0;
    private volatile double pinkNoiseLevel = 0.0;
    private volatile double granularNoiseLevel = 0.0;
    private volatile double brownNoiseLevel = 0.0;
    private volatile double blueNoiseLevel = 0.0;
    private volatile double violetNoiseLevel = 0.0;
    private volatile double impulseNoiseLevel = 0.0;
    private volatile double modulatedNoiseLevel = 0.0;
    private volatile double volume = 0.7;
    private volatile double tone = 0.0; // 0..1, start at left
    private volatile double distortionLevel = 0.0;
    private volatile double filterLevel = 0.0;
    private volatile int grainSize = 1;
    
    // Chaos mode state - volatile for cross-thread visibility
    private volatile boolean chaosModeActive = false;
    
    // Perlin noise state - volatile for cross-thread visibility
    private volatile boolean perlinNoiseActive = false;
    
    // Recording state - volatile for cross-thread visibility
    private volatile boolean isRecording = false;
    
    // Volume lock state - volatile for cross-thread visibility
    private volatile boolean volumeLocked = false;
    
    // Batching support
    private int updateDepth = 0;
    private boolean dirty = false;
    
    // Getters and setters with notification and change detection
    public double getWhiteNoiseLevel() { return whiteNoiseLevel; }
    public void setWhiteNoiseLevel(double level) {
        if (Math.abs(this.whiteNoiseLevel - level) < EPSILON) return;
        this.whiteNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getPinkNoiseLevel() { return pinkNoiseLevel; }
    public void setPinkNoiseLevel(double level) {
        if (Math.abs(this.pinkNoiseLevel - level) < EPSILON) return;
        this.pinkNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getGranularNoiseLevel() { return granularNoiseLevel; }
    public void setGranularNoiseLevel(double level) {
        if (Math.abs(this.granularNoiseLevel - level) < EPSILON) return;
        this.granularNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getBrownNoiseLevel() { return brownNoiseLevel; }
    public void setBrownNoiseLevel(double level) {
        if (Math.abs(this.brownNoiseLevel - level) < EPSILON) return;
        this.brownNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getBlueNoiseLevel() { return blueNoiseLevel; }
    public void setBlueNoiseLevel(double level) {
        if (Math.abs(this.blueNoiseLevel - level) < EPSILON) return;
        this.blueNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getVioletNoiseLevel() { return violetNoiseLevel; }
    public void setVioletNoiseLevel(double level) {
        if (Math.abs(this.violetNoiseLevel - level) < EPSILON) return;
        this.violetNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getImpulseNoiseLevel() { return impulseNoiseLevel; }
    public void setImpulseNoiseLevel(double level) {
        if (Math.abs(this.impulseNoiseLevel - level) < EPSILON) return;
        this.impulseNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getModulatedNoiseLevel() { return modulatedNoiseLevel; }
    public void setModulatedNoiseLevel(double level) {
        if (Math.abs(this.modulatedNoiseLevel - level) < EPSILON) return;
        this.modulatedNoiseLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getVolume() { return volume; }
    public void setVolume(double volume) {
        // Check if volume is locked
        if (volumeLocked) return;
        
        if (Math.abs(this.volume - volume) < EPSILON) return;
        this.volume = volume;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getTone() { return tone; }
    public void setTone(double v) {
        if (Math.abs(this.tone - v) < EPSILON) return;
        this.tone = Math.max(0.0, Math.min(1.0, v));
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    // Volume lock methods
    public boolean isVolumeLocked() { return volumeLocked; }
    public void setVolumeLocked(boolean locked) {
        if (this.volumeLocked == locked) return;
        this.volumeLocked = locked;
        setChanged();
        notifyObservers();
    }
    
    public double getDistortionLevel() { return distortionLevel; }
    public void setDistortionLevel(double level) {
        if (Math.abs(this.distortionLevel - level) < EPSILON) return;
        this.distortionLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public double getFilterLevel() { return filterLevel; }
    public void setFilterLevel(double level) {
        if (Math.abs(this.filterLevel - level) < EPSILON) return;
        this.filterLevel = level;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public int getGrainSize() { return grainSize; }
    public void setGrainSize(int size) {
        if (this.grainSize == size) return;
        this.grainSize = size;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public boolean isChaosModeActive() { return chaosModeActive; }
    public void setChaosModeActive(boolean active) {
        if (this.chaosModeActive == active) return;
        this.chaosModeActive = active;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public boolean isPerlinNoiseActive() { return perlinNoiseActive; }
    public void setPerlinNoiseActive(boolean active) {
        if (this.perlinNoiseActive == active) return;
        this.perlinNoiseActive = active;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    public boolean isRecording() { return isRecording; }
    public void setRecording(boolean recording) {
        if (this.isRecording == recording) return;
        this.isRecording = recording;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    /**
     * Reset all noise levels to 0 (silent)
     */
    public void resetNoiseLevels() {
        whiteNoiseLevel = 0.0;
        pinkNoiseLevel = 0.0;
        granularNoiseLevel = 0.0;
        brownNoiseLevel = 0.0;
        blueNoiseLevel = 0.0;
        violetNoiseLevel = 0.0;
        impulseNoiseLevel = 0.0;
        modulatedNoiseLevel = 0.0;
        if (updateDepth > 0) {
            dirty = true;
        } else {
            setChanged();
            notifyObservers();
        }
    }
    
    // Optional batching API
    
    /**
     * Begin a batch update - suppresses notifications until endUpdate()
     */
    public void beginUpdate() {
        updateDepth++;
    }
    
    /**
     * End a batch update - sends notification if any changes occurred
     */
    public void endUpdate() {
        if (--updateDepth <= 0) {
            updateDepth = 0;
            if (dirty) {
                setChanged();
                notifyObservers();
                dirty = false;
            }
        }
    }
    
    /**
     * Convenience method for batch updates
     * @param r Runnable containing the batch operations
     */
    public void setParamsBatch(Runnable r) {
        beginUpdate();
        try {
            r.run();
        } finally {
            endUpdate();
        }
    }
}
