package com.harshnoise.audio.effects;

/**
 * Audio effect unified interface
 * Defines methods that all audio effect classes must implement
 */
public interface AudioEffect {
    
    /**
     * Get the unique identifier of the effect
     * @return Effect ID string
     */
    String getId();
    
    /**
     * Get the display name of the effect
     * @return Effect name
     */
    String getName();
    
    /**
     * Enable or disable the effect
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);
    
    /**
     * Check if the effect is enabled
     * @return true if enabled, false if disabled
     */
    boolean isEnabled();
    
    /**
     * Set effect parameter
     * @param parameterName Parameter name
     * @param value Parameter value
     */
    void setParameter(String parameterName, float value);
    
    /**
     * Get effect parameter value
     * @param parameterName Parameter name
     * @return Parameter value
     */
    float getParameter(String parameterName);
    
    /**
     * Set Wet/Dry mix ratio
     * @param wetDryRatio 0.0 for completely Dry, 1.0 for completely Wet
     */
    void setWetDryRatio(float wetDryRatio);
    
    /**
     * Get Wet/Dry mix ratio
     * @return Wet/Dry ratio value
     */
    float getWetDryRatio();
    
    /**
     * Set sample rate
     * @param sampleRate Sample rate (Hz)
     */
    void setSampleRate(float sampleRate);
    
    /**
     * Get current sample rate
     * @return Sample rate (Hz)
     */
    float getSampleRate();
    
    /**
     * Process audio buffer
     * @param inputBuffer Input audio buffer
     * @param outputBuffer Output audio buffer
     * @param bufferSize Buffer size
     */
    void process(float[] inputBuffer, float[] outputBuffer, int bufferSize);
    
    /**
     * Reset effect state
     * Clear internal state, such as delay lines, filter states, etc.
     */
    void reset();
}
