package com.harshnoise.audio.effects;

import java.util.List;

/**
 * Effects Controller
 * Bridge between UI and effect chain
 */
public class EffectsController {
    
    private final EffectChain effectChain;
    
    /**
     * Constructor
     * @param effectChain Effect chain instance
     */
    public EffectsController(EffectChain effectChain) {
        this.effectChain = effectChain;
    }
    
    /**
     * Add effect to chain
     * @param effectId Effect ID
     * @return Added effect instance, or null if ID is invalid
     */
    public AudioEffect addEffect(String effectId) {
        AudioEffect effect = EffectRegistry.createEffect(effectId);
        if (effect != null) {
            effectChain.addEffect(effect);
            
            // Set default parameters for delay effect
            if (effectId.equals("delay")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "time", 320.0f);
                setEffectParameter(effectIndex, "feedback", 0.35f);
                setEffectWetDryRatio(effectIndex, 0.25f);
            }
            
            // Set default parameters for reverb effect
            if (effectId.equals("reverb")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "room", 0.6f);
                setEffectParameter(effectIndex, "damp", 0.4f);
                setEffectWetDryRatio(effectIndex, 0.30f);
            }
            
            // Set default parameters for bitcrusher effect
            if (effectId.equals("bitcrusher")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "bitdepth", 10.0f);
                setEffectParameter(effectIndex, "rate", 12000.0f);
                setEffectWetDryRatio(effectIndex, 0.20f);
            }
            
            // Set default parameters for overdrive effect
            if (effectId.equals("overdrive")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "drive", 4.0f);
                setEffectParameter(effectIndex, "tone", 0.6f);
                setEffectWetDryRatio(effectIndex, 0.35f);
            }
            
            // Set default parameters for distortion effect
            if (effectId.equals("distortion")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "drive", 6.0f);
                setEffectParameter(effectIndex, "shape", 0.5f);
                setEffectParameter(effectIndex, "tone", 0.55f);
                setEffectWetDryRatio(effectIndex, 0.35f);
            }
            
            // Set default parameters for fuzz effect
            if (effectId.equals("fuzz")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "drive", 12.0f);
                setEffectParameter(effectIndex, "bias", 0.15f);
                setEffectParameter(effectIndex, "tone", 0.45f);
                setEffectWetDryRatio(effectIndex, 0.30f);
            }
            
            // Set default parameters for wavefolder effect
            if (effectId.equals("wavefolder")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "fold", 3.0f);
                setEffectParameter(effectIndex, "symmetry", 0.0f);
                setEffectParameter(effectIndex, "tone", 0.6f);
                setEffectWetDryRatio(effectIndex, 0.30f);
            }
            
            // Set default parameters for reverse delay effect
            if (effectId.equals("reverseDelay")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "time", 600.0f);
                setEffectParameter(effectIndex, "feedback", 0.35f);
                setEffectParameter(effectIndex, "crossfade", 8.0f);
                setEffectParameter(effectIndex, "freeze", 0.0f);
                setEffectWetDryRatio(effectIndex, 0.35f);
            }
            
            // Set default parameters for granular effect
            if (effectId.equals("granular")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "grainSize", 60.0f);
                setEffectParameter(effectIndex, "density", 25.0f);
                setEffectParameter(effectIndex, "pitch", 1.0f);
                setEffectParameter(effectIndex, "jitter", 0.4f);
                setEffectParameter(effectIndex, "reverseProb", 0.2f);
                setEffectWetDryRatio(effectIndex, 0.6f);
            }
            
            // Set default parameters for looper effect
            if (effectId.equals("looper")) {
                int effectIndex = effectChain.getEffectCount() - 1;
                setEffectParameter(effectIndex, "loopLen", 2000.0f);
                setEffectParameter(effectIndex, "overdub", 0.0f);
                setEffectParameter(effectIndex, "feedback", 0.3f);
                setEffectParameter(effectIndex, "speed", 1.0f);
                setEffectParameter(effectIndex, "reverse", 0.0f);
                setEffectParameter(effectIndex, "stutterOn", 0.0f);
                setEffectParameter(effectIndex, "stutterProb", 0.2f);
                setEffectParameter(effectIndex, "sliceLen", 60.0f);
                setEffectParameter(effectIndex, "jitter", 0.1f);
                setEffectWetDryRatio(effectIndex, 0.35f);
            }
        }
        return effect;
    }
    
    /**
     * Remove effect by index
     * @param index Effect index
     * @return Removed effect, or null if index is invalid
     */
    public AudioEffect removeEffect(int index) {
        return effectChain.removeEffect(index);
    }
    
    /**
     * Reorder effects
     * @param fromIndex Source index
     * @param toIndex Target index
     * @return true if successfully reordered, false if indices are invalid
     */
    public boolean reorderEffect(int fromIndex, int toIndex) {
        return effectChain.reorderEffect(fromIndex, toIndex);
    }
    
    /**
     * Set effect parameter
     * @param effectIndex Effect index
     * @param parameterName Parameter name
     * @param value Parameter value
     * @return true if successfully set, false if index is invalid
     */
    public boolean setEffectParameter(int effectIndex, String parameterName, float value) {
        AudioEffect effect = effectChain.getEffect(effectIndex);
        if (effect != null) {
            effect.setParameter(parameterName, value);
            return true;
        }
        return false;
    }
    
    /**
     * Get effect parameter value
     * @param effectIndex Effect index
     * @param parameterName Parameter name
     * @return Parameter value, or 0.0f if index is invalid
     */
    public float getEffectParameter(int effectIndex, String parameterName) {
        AudioEffect effect = effectChain.getEffect(effectIndex);
        if (effect != null) {
            return effect.getParameter(parameterName);
        }
        return 0.0f;
    }
    
    /**
     * Set effect enabled state
     * @param effectIndex Effect index
     * @param enabled Enabled state
     * @return true if successfully set, false if index is invalid
     */
    public boolean setEffectEnabled(int effectIndex, boolean enabled) {
        AudioEffect effect = effectChain.getEffect(effectIndex);
        if (effect != null) {
            effect.setEnabled(enabled);
            return true;
        }
        return false;
    }
    
    /**
     * Get effect enabled state
     * @param effectIndex Effect index
     * @return Enabled state, or false if index is invalid
     */
    public boolean isEffectEnabled(int effectIndex) {
        AudioEffect effect = effectChain.getEffect(effectIndex);
        if (effect != null) {
            return effect.isEnabled();
        }
        return false;
    }
    
    /**
     * Set effect Wet/Dry ratio
     * @param effectIndex Effect index
     * @param wetDryRatio Wet/Dry ratio
     * @return true if successfully set, false if index is invalid
     */
    public boolean setEffectWetDryRatio(int effectIndex, float wetDryRatio) {
        AudioEffect effect = effectChain.getEffect(effectIndex);
        if (effect != null) {
            effect.setWetDryRatio(wetDryRatio);
            return true;
        }
        return false;
    }
    
    /**
     * Get effect Wet/Dry ratio
     * @param effectIndex Effect index
     * @return Wet/Dry ratio, or 0.0f if index is invalid
     */
    public float getEffectWetDryRatio(int effectIndex) {
        AudioEffect effect = effectChain.getEffect(effectIndex);
        if (effect != null) {
            return effect.getWetDryRatio();
        }
        return 0.0f;
    }
    
    /**
     * Get effect chain snapshot
     * @return Effects list
     */
    public List<AudioEffect> getEffectsSnapshot() {
        return effectChain.getEffectsSnapshot();
    }
    
    /**
     * Get effect count
     * @return Effect count
     */
    public int getEffectCount() {
        return effectChain.getEffectCount();
    }
    
    /**
     * Get effect by index
     * @param index Effect index
     * @return Effect instance, or null if index is invalid
     */
    public AudioEffect getEffect(int index) {
        return effectChain.getEffect(index);
    }
    
    /**
     * Get all available effect IDs
     * @return Effect ID set
     */
    public java.util.Set<String> getAvailableEffectIds() {
        return EffectRegistry.getAvailableEffectIds();
    }
    
    /**
     * Reset all effects
     */
    public void resetAllEffects() {
        effectChain.reset();
    }
    
    /**
     * Clear effect chain
     */
    public void clearEffects() {
        effectChain.clear();
    }
    
    /**
     * Get effect chain instance
     * @return Effect chain instance
     */
    public EffectChain getEffectChain() {
        return effectChain;
    }
}
