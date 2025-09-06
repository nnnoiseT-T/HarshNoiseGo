package com.harshnoise.audio.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Effect registry
 * Register and create effect instances
 */
public class EffectRegistry {
    
    private static final Map<String, EffectFactory> effectFactories = new HashMap<>();
    
    /**
     * Effect factory interface
     */
    public interface EffectFactory {
        AudioEffect createEffect();
    }
    
    static {
        // Register built-in effects
        registerEffect("bypass", BypassEffect::new);
        registerEffect("delay", DelayEffect::new);
        registerEffect("reverb", ReverbEffect::new);
        registerEffect("bitcrusher", BitcrusherEffect::new);
        registerEffect("overdrive", OverdriveEffect::new);
        registerEffect("distortion", DistortionEffect::new);
        registerEffect("fuzz", FuzzEffect::new);
        registerEffect("wavefolder", WavefolderEffect::new);
        registerEffect("reverseDelay", ReverseDelayEffect::new);
        registerEffect("granular", GranularEffect::new);
        registerEffect("looper", LooperEffect::new);
        
        // TODO: Add more effects later
        // registerEffect("filter", FilterEffect::new);
        // registerEffect("compressor", CompressorEffect::new);
    }
    
    /**
     * Register effect factory
     * @param effectId Effect ID
     * @param factory Effect factory
     */
    public static void registerEffect(String effectId, EffectFactory factory) {
        effectFactories.put(effectId, factory);
    }
    
    /**
     * Create effect instance by ID
     * @param effectId Effect ID
     * @return Effect instance, returns null if ID doesn't exist
     */
    public static AudioEffect createEffect(String effectId) {
        EffectFactory factory = effectFactories.get(effectId);
        if (factory != null) {
            return factory.createEffect();
        }
        return null;
    }
    
    /**
     * List all available effect IDs
     * @return Set of effect IDs
     */
    public static Set<String> getAvailableEffectIds() {
        return effectFactories.keySet();
    }
    
    /**
     * Check if effect ID exists
     * @param effectId Effect ID
     * @return true if effect exists, false if it doesn't exist
     */
    public static boolean hasEffect(String effectId) {
        return effectFactories.containsKey(effectId);
    }
    
    /**
     * Get effect count
     * @return Number of registered effects
     */
    public static int getEffectCount() {
        return effectFactories.size();
    }
}
