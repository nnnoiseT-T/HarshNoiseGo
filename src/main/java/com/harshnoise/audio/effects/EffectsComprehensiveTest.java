package com.harshnoise.audio.effects;

/**
 * Comprehensive effect testing program
 * Test all available audio effects
 */
public class EffectsComprehensiveTest {
    
    public static void main(String[] args) {
        System.out.println("=== Comprehensive Effect Testing Started ===");
        
        // Get all available effects
        System.out.println("\n📋 Available Effects List:");
        System.out.println("Available effect count: " + EffectRegistry.getEffectCount());
        System.out.println("Available effect IDs: " + EffectRegistry.getAvailableEffectIds());
        
        // Test each effect
        String[] effectIds = EffectRegistry.getAvailableEffectIds().toArray(new String[0]);
        
        for (String effectId : effectIds) {
            if (effectId.equals("bypass")) {
                continue; // Skip bypass effect
            }
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("🎛️ Testing Effect: " + effectId);
            System.out.println("=".repeat(50));
            
            testEffect(effectId);
        }
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🎉 All Effects Testing Completed!");
        System.out.println("=".repeat(50));
    }
    
    private static void testEffect(String effectId) {
        try {
            // 1. Create effect
            AudioEffect effect = EffectRegistry.createEffect(effectId);
            if (effect == null) {
                System.out.println("❌ Effect creation failed: " + effectId);
                return;
            }
            
            System.out.println("✅ Effect created successfully");
            System.out.println("  Effect ID: " + effect.getId());
            System.out.println("  Effect Name: " + effect.getName());
            
            // 2. Test default parameters
            System.out.println("\n📊 Default Parameters:");
            testDefaultParameters(effect);
            
            // 3. Test parameter setting
            System.out.println("\n🔧 Parameter Setting Test:");
            testParameterSetting(effect);
            
            // 4. Test audio processing
            System.out.println("\n🎵 Audio Processing Test:");
            testAudioProcessing(effect);
            
            // 5. Test enable/disable
            System.out.println("\n⚡ Enable/Disable Test:");
            testEnableDisable(effect);
            
            // 6. Test effect chain integration
            System.out.println("\n🔗 Effect Chain Integration Test:");
            testEffectChainIntegration(effect);
            
            // 7. Test special features of specific effects
            System.out.println("\n🎯 Special Features Test:");
            testSpecialFeatures(effect);
            
        } catch (Exception e) {
            System.out.println("❌ Exception occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDefaultParameters(AudioEffect effect) {
        // Test basic parameters
        System.out.println("  Enabled: " + effect.isEnabled());
        System.out.println("  Sample Rate: " + effect.getSampleRate() + "Hz");
        System.out.println("  Wet/Dry Ratio: " + effect.getWetDryRatio());
        
        // Test specific parameters (based on effect type)
        String effectId = effect.getId();
        switch (effectId) {
            case "delay":
                System.out.println("  Delay Time: " + effect.getParameter("delayTime") + "ms");
                System.out.println("  Feedback: " + effect.getParameter("feedback"));
                break;
            case "reverb":
                System.out.println("  Room Size: " + effect.getParameter("roomSize"));
                System.out.println("  Damping: " + effect.getParameter("damping"));
                break;
            case "overdrive":
                System.out.println("  Gain: " + effect.getParameter("gain"));
                System.out.println("  Threshold: " + effect.getParameter("threshold"));
                break;
            case "distortion":
                System.out.println("  Distortion Amount: " + effect.getParameter("distortion"));
                break;
            case "fuzz":
                System.out.println("  Fuzz Amount: " + effect.getParameter("fuzz"));
                break;
            case "bitcrusher":
                System.out.println("  Bit Depth: " + effect.getParameter("bitDepth"));
                System.out.println("  Sample Rate: " + effect.getParameter("sampleRate"));
                break;
            case "wavefolder":
                System.out.println("  Fold Amount: " + effect.getParameter("fold"));
                System.out.println("  Offset: " + effect.getParameter("offset"));
                break;
            case "reverseDelay":
                System.out.println("  Delay Time: " + effect.getParameter("delayTime") + "ms");
                System.out.println("  Feedback: " + effect.getParameter("feedback"));
                break;
            case "granular":
                System.out.println("  Grain Size: " + effect.getParameter("grainSize") + "ms");
                System.out.println("  Density: " + effect.getParameter("density"));
                System.out.println("  Pitch: " + effect.getParameter("pitch"));
                System.out.println("  Jitter: " + effect.getParameter("jitter"));
                System.out.println("  Reverse Probability: " + effect.getParameter("reverseProb"));
                break;
            case "looper":
                System.out.println("  Loop Length: " + effect.getParameter("loopLen") + "ms");
                System.out.println("  Overdub: " + effect.getParameter("overdub"));
                System.out.println("  Feedback: " + effect.getParameter("feedback"));
                System.out.println("  Speed: " + effect.getParameter("speed") + "x");
                System.out.println("  Reverse: " + effect.getParameter("reverse"));
                System.out.println("  Stutter On: " + effect.getParameter("stutterOn"));
                System.out.println("  Stutter Probability: " + effect.getParameter("stutterProb"));
                System.out.println("  Slice Length: " + effect.getParameter("sliceLen") + "ms");
                System.out.println("  Jitter: " + effect.getParameter("jitter"));
                break;
        }
    }
    
    private static void testParameterSetting(AudioEffect effect) {
        String effectId = effect.getId();
        
        // Test parameter range limits
        System.out.println("  Testing parameter range limits:");
        
        switch (effectId) {
            case "delay":
                effect.setParameter("delayTime", 5000.0f); // Out of range
                effect.setParameter("feedback", 1.5f); // Out of range
                System.out.println("    Delay Time (after limit): " + effect.getParameter("delayTime") + "ms");
                System.out.println("    Feedback (after limit): " + effect.getParameter("feedback"));
                break;
            case "reverb":
                effect.setParameter("roomSize", 2.0f); // Out of range
                effect.setParameter("damping", 2.0f); // Out of range
                System.out.println("    Room Size (after limit): " + effect.getParameter("roomSize"));
                System.out.println("    Damping (after limit): " + effect.getParameter("damping"));
                break;
            case "overdrive":
                effect.setParameter("gain", 100.0f); // Out of range
                effect.setParameter("threshold", 2.0f); // Out of range
                System.out.println("    Gain (after limit): " + effect.getParameter("gain"));
                System.out.println("    Threshold (after limit): " + effect.getParameter("threshold"));
                break;
            case "distortion":
                effect.setParameter("distortion", 2.0f); // Out of range
                System.out.println("    Distortion Amount (after limit): " + effect.getParameter("distortion"));
                break;
            case "fuzz":
                effect.setParameter("fuzz", 2.0f); // Out of range
                System.out.println("    Fuzz Amount (after limit): " + effect.getParameter("fuzz"));
                break;
            case "bitcrusher":
                effect.setParameter("bitDepth", 20.0f); // Out of range
                effect.setParameter("sampleRate", 50000.0f); // Out of range
                System.out.println("    Bit Depth (after limit): " + effect.getParameter("bitDepth"));
                System.out.println("    Sample Rate (after limit): " + effect.getParameter("sampleRate"));
                break;
            case "wavefolder":
                effect.setParameter("fold", 2.0f); // Out of range
                effect.setParameter("offset", 2.0f); // Out of range
                System.out.println("    Fold Amount (after limit): " + effect.getParameter("fold"));
                System.out.println("    Offset (after limit): " + effect.getParameter("offset"));
                break;
            case "reverseDelay":
                effect.setParameter("delayTime", 5000.0f); // Out of range
                effect.setParameter("feedback", 1.5f); // Out of range
                System.out.println("    Delay Time (after limit): " + effect.getParameter("delayTime") + "ms");
                System.out.println("    Feedback (after limit): " + effect.getParameter("feedback"));
                break;
            case "granular":
                effect.setParameter("grainSize", 500.0f); // Out of range
                effect.setParameter("density", 100.0f); // Out of range
                effect.setParameter("pitch", 5.0f); // Out of range
                effect.setParameter("jitter", 2.0f); // Out of range
                effect.setParameter("reverseProb", 2.0f); // Out of range
                System.out.println("    Grain Size (after limit): " + effect.getParameter("grainSize") + "ms");
                System.out.println("    Density (after limit): " + effect.getParameter("density"));
                System.out.println("    Pitch (after limit): " + effect.getParameter("pitch"));
                System.out.println("    Jitter (after limit): " + effect.getParameter("jitter"));
                System.out.println("    Reverse Probability (after limit): " + effect.getParameter("reverseProb"));
                break;
            case "looper":
                effect.setParameter("loopLen", 15000.0f); // Out of range
                effect.setParameter("overdub", 2.0f); // Out of range
                effect.setParameter("feedback", 2.0f); // Out of range
                effect.setParameter("speed", 5.0f); // Out of range
                effect.setParameter("stutterProb", 2.0f); // Out of range
                effect.setParameter("sliceLen", 500.0f); // Out of range
                effect.setParameter("jitter", 2.0f); // Out of range
                System.out.println("    Loop Length (after limit): " + effect.getParameter("loopLen") + "ms");
                System.out.println("    Overdub (after limit): " + effect.getParameter("overdub"));
                System.out.println("    Feedback (after limit): " + effect.getParameter("feedback"));
                System.out.println("    Speed (after limit): " + effect.getParameter("speed") + "x");
                System.out.println("    Stutter Probability (after limit): " + effect.getParameter("stutterProb"));
                System.out.println("    Slice Length (after limit): " + effect.getParameter("sliceLen") + "ms");
                System.out.println("    Jitter (after limit): " + effect.getParameter("jitter"));
                break;
        }
        
        // Test wet/dry ratio setting
        effect.setWetDryRatio(0.8f);
        System.out.println("  Wet/Dry Ratio Setting: " + effect.getWetDryRatio());
    }
    
    private static void testAudioProcessing(AudioEffect effect) {
        // Generate test audio data
        float[] inputBuffer = new float[2048];
        float[] outputBuffer = new float[2048];
        
        // Generate complex test signal
        for (int i = 0; i < 2048; i++) {
            float t = (float) i / 44100.0f;
            // Multi-frequency composite signal
            inputBuffer[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * t) * 0.3f +  // 440Hz
                            (float) Math.sin(2.0 * Math.PI * 880.0 * t) * 0.2f +  // 880Hz
                            (float) Math.sin(2.0 * Math.PI * 1320.0 * t) * 0.1f + // 1320Hz
                            (float) Math.sin(2.0 * Math.PI * 220.0 * t) * 0.4f;   // 220Hz
        }
        
        // Process audio
        effect.process(inputBuffer, outputBuffer, 2048);
        
        // Analyze output
        float maxInput = 0.0f;
        float maxOutput = 0.0f;
        float rmsInput = 0.0f;
        float rmsOutput = 0.0f;
        boolean hasEffect = false;
        
        for (int i = 0; i < 2048; i++) {
            maxInput = Math.max(maxInput, Math.abs(inputBuffer[i]));
            maxOutput = Math.max(maxOutput, Math.abs(outputBuffer[i]));
            rmsInput += inputBuffer[i] * inputBuffer[i];
            rmsOutput += outputBuffer[i] * outputBuffer[i];
            
            if (Math.abs(inputBuffer[i] - outputBuffer[i]) > 0.001f) {
                hasEffect = true;
            }
        }
        
        rmsInput = (float) Math.sqrt(rmsInput / 2048.0f);
        rmsOutput = (float) Math.sqrt(rmsOutput / 2048.0f);
        
        System.out.println("  Input Peak: " + maxInput);
        System.out.println("  Output Peak: " + maxOutput);
        System.out.println("  Input RMS: " + rmsInput);
        System.out.println("  Output RMS: " + rmsOutput);
        System.out.println("  Effect Detection: " + (hasEffect ? "✅ Effective" : "⚠️ Weak effect"));
        
        // Calculate spectral change
        float spectralChange = Math.abs(rmsOutput - rmsInput) / rmsInput;
        System.out.println("  Spectral Change: " + (spectralChange * 100) + "%");
    }
    
    private static void testEnableDisable(AudioEffect effect) {
        // Generate test audio
        float[] inputBuffer = new float[1024];
        float[] outputBuffer = new float[1024];
        
        for (int i = 0; i < 1024; i++) {
            inputBuffer[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0) * 0.5f;
        }
        
        // Test enabled state
        effect.setEnabled(true);
        effect.process(inputBuffer, outputBuffer, 1024);
        float enabledOutput = 0.0f;
        for (int i = 0; i < 1024; i++) {
            enabledOutput += Math.abs(outputBuffer[i]);
        }
        enabledOutput /= 1024.0f;
        
        // Test disabled state
        effect.setEnabled(false);
        effect.process(inputBuffer, outputBuffer, 1024);
        float disabledOutput = 0.0f;
        for (int i = 0; i < 1024; i++) {
            disabledOutput += Math.abs(outputBuffer[i]);
        }
        disabledOutput /= 1024.0f;
        
        System.out.println("  Enabled State Output: " + enabledOutput);
        System.out.println("  Disabled State Output: " + disabledOutput);
        System.out.println("  Bypass Function: " + (Math.abs(enabledOutput - disabledOutput) > 0.01f ? "✅ Normal" : "⚠️ May be abnormal"));
    }
    
    private static void testEffectChainIntegration(AudioEffect effect) {
        EffectChain effectChain = new EffectChain(2048, 44100.0f);
        EffectsController effectsController = new EffectsController(effectChain);
        
        AudioEffect chainEffect = effectsController.addEffect(effect.getId());
        if (chainEffect != null) {
            System.out.println("  ✅ Effect added to effect chain successfully");
            System.out.println("  Effect count in chain: " + effectsController.getEffectCount());
        } else {
            System.out.println("  ❌ Failed to add effect to effect chain");
        }
    }
    
    private static void testSpecialFeatures(AudioEffect effect) {
        String effectId = effect.getId();
        
        switch (effectId) {
            case "delay":
                testDelayFeatures(effect);
                break;
            case "reverb":
                testReverbFeatures(effect);
                break;
            case "granular":
                testGranularFeatures(effect);
                break;
            case "looper":
                testLooperFeatures(effect);
                break;
            case "bitcrusher":
                testBitcrusherFeatures(effect);
                break;
            case "wavefolder":
                testWavefolderFeatures(effect);
                break;
            default:
                System.out.println("  ⚠️ No special feature tests for this effect");
                break;
        }
    }
    
    private static void testDelayFeatures(AudioEffect effect) {
        System.out.println("  🎵 Delay Effect Test:");
        
        // Test different delay times
        float[] delays = {100.0f, 300.0f, 500.0f, 1000.0f};
        for (float delay : delays) {
            effect.setParameter("delayTime", delay);
            effect.setParameter("feedback", 0.3f);
            effect.setParameter("wet", 1.0f); // Set to full wet signal
            
            float[] inputBuffer = new float[2048];
            float[] outputBuffer = new float[2048];
            
            // Generate impulse signal
            for (int i = 0; i < 2048; i++) {
                inputBuffer[i] = (i < 100) ? 0.5f : 0.0f; // First 100 samples as impulse
            }
            
            effect.process(inputBuffer, outputBuffer, 2048);
            
            // Detect delay
            int delaySamples = (int)(delay * 44.1f); // Convert to samples
            float delayOutput = 0.0f;
            if (delaySamples < 2048) {
                delayOutput = outputBuffer[delaySamples];
            }
            
            System.out.println("    Delay Time " + delay + "ms: Delay Output=" + delayOutput);
        }
    }
    
    private static void testReverbFeatures(AudioEffect effect) {
        System.out.println("  🏠 Reverb Effect Test:");
        
        // Test different room sizes
        float[] roomSizes = {0.1f, 0.5f, 0.9f};
        for (float roomSize : roomSizes) {
            effect.setParameter("roomSize", roomSize);
            effect.setParameter("damping", 0.5f);
            effect.setParameter("wet", 1.0f); // Set to full wet signal
            
            float[] inputBuffer = new float[2048];
            float[] outputBuffer = new float[2048];
            
            // Generate short impulse
            for (int i = 0; i < 2048; i++) {
                inputBuffer[i] = (i < 50) ? 0.5f : 0.0f;
            }
            
            effect.process(inputBuffer, outputBuffer, 2048);
            
            // Calculate reverb tail energy
            float tailEnergy = 0.0f;
            for (int i = 1000; i < 2048; i++) {
                tailEnergy += outputBuffer[i] * outputBuffer[i];
            }
            tailEnergy = (float) Math.sqrt(tailEnergy / 1048.0f);
            
            System.out.println("    Room Size " + roomSize + ": Tail Energy=" + tailEnergy);
        }
    }
    
    private static void testGranularFeatures(AudioEffect effect) {
        System.out.println("  🌟 Granular Synthesis Effect Test:");
        
        // Test different densities
        float[] densities = {5.0f, 15.0f, 25.0f};
        for (float density : densities) {
            effect.setParameter("density", density);
            effect.setParameter("grainSize", 60.0f);
            effect.setParameter("pitch", 1.0f);
            effect.setParameter("jitter", 0.2f);
            
            float[] inputBuffer = new float[2048];
            float[] outputBuffer = new float[2048];
            
            // Generate sine wave
            for (int i = 0; i < 2048; i++) {
                inputBuffer[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0) * 0.5f;
            }
            
            effect.process(inputBuffer, outputBuffer, 2048);
            
            // Calculate grain cloud density
            float grainDensity = 0.0f;
            for (int i = 1; i < 2048; i++) {
                grainDensity += Math.abs(outputBuffer[i] - outputBuffer[i-1]);
            }
            grainDensity /= 2047.0f;
            
            System.out.println("    Density " + density + ": Grain Cloud Density=" + grainDensity);
        }
    }
    
    private static void testLooperFeatures(AudioEffect effect) {
        System.out.println("  🔄 Looper Effect Test:");
        
        // Test different loop lengths
        float[] loopLengths = {1000.0f, 2000.0f, 4000.0f};
        for (float loopLen : loopLengths) {
            effect.setParameter("loopLen", loopLen);
            effect.setParameter("feedback", 0.3f);
            effect.setParameter("speed", 1.0f);
            effect.setParameter("reverse", 0.0f);
            
            float[] inputBuffer = new float[4096];
            float[] outputBuffer = new float[4096];
            
            // Generate rising ramp signal
            for (int i = 0; i < 4096; i++) {
                inputBuffer[i] = (float) i / 4096.0f;
            }
            
            effect.process(inputBuffer, outputBuffer, 4096);
            
            // Calculate loop density
            float loopDensity = 0.0f;
            for (int i = 0; i < 4096; i++) {
                loopDensity += Math.abs(outputBuffer[i]);
            }
            loopDensity /= 4096.0f;
            
            System.out.println("    Loop Length " + loopLen + "ms: Loop Density=" + loopDensity);
        }
        
        // Test reverse function
        effect.setParameter("reverse", 1.0f);
        effect.setParameter("loopLen", 2000.0f);
        
        float[] inputBuffer = new float[2048];
        float[] outputBuffer = new float[2048];
        
        // Generate rising ramp
        for (int i = 0; i < 2048; i++) {
            inputBuffer[i] = (float) i / 2048.0f;
        }
        
        effect.process(inputBuffer, outputBuffer, 2048);
        
        float inputRise = inputBuffer[2047] - inputBuffer[0];
        float outputRise = outputBuffer[2047] - outputBuffer[0];
        
        System.out.println("    Reverse Test: Input Ramp=" + inputRise + ", Output Ramp=" + outputRise);
        System.out.println("    Reverse Effect: " + (Math.abs(outputRise) < Math.abs(inputRise) * 0.5f ? "✅ Normal" : "⚠️ May be abnormal"));
    }
    
    private static void testBitcrusherFeatures(AudioEffect effect) {
        System.out.println("  🔧 Bit Depth Destruction Effect Test:");
        
        // Test different bit depths
        float[] bitDepths = {16.0f, 8.0f, 4.0f, 2.0f};
        for (float bitDepth : bitDepths) {
            effect.setParameter("bitDepth", bitDepth);
            effect.setParameter("sampleRate", 44100.0f);
            effect.setParameter("wet", 1.0f); // Set to full wet signal
            
            float[] inputBuffer = new float[2048];
            float[] outputBuffer = new float[2048];
            
            // Generate sine wave
            for (int i = 0; i < 2048; i++) {
                inputBuffer[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0) * 0.5f;
            }
            
            effect.process(inputBuffer, outputBuffer, 2048);
            
            // Calculate quantization noise
            float quantizationNoise = 0.0f;
            for (int i = 0; i < 2048; i++) {
                quantizationNoise += Math.abs(inputBuffer[i] - outputBuffer[i]);
            }
            quantizationNoise /= 2048.0f;
            
            System.out.println("    Bit Depth " + bitDepth + ": Quantization Noise=" + quantizationNoise);
        }
    }
    
    private static void testWavefolderFeatures(AudioEffect effect) {
        System.out.println("  📐 Wave Folding Effect Test:");
        
        // Test different fold amounts
        float[] folds = {0.1f, 0.5f, 0.9f};
        for (float fold : folds) {
            effect.setParameter("fold", fold);
            effect.setParameter("offset", 0.0f);
            effect.setParameter("wet", 1.0f); // Set to full wet signal
            
            float[] inputBuffer = new float[2048];
            float[] outputBuffer = new float[2048];
            
            // Generate large amplitude sine wave
            for (int i = 0; i < 2048; i++) {
                inputBuffer[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0) * 0.8f;
            }
            
            effect.process(inputBuffer, outputBuffer, 2048);
            
            // Calculate harmonic content
            float harmonicContent = 0.0f;
            for (int i = 0; i < 2048; i++) {
                harmonicContent += Math.abs(outputBuffer[i]);
            }
            harmonicContent /= 2048.0f;
            
            System.out.println("    Fold Amount " + fold + ": Harmonic Content=" + harmonicContent);
        }
    }
}
