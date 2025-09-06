package com.harshnoise;

import com.harshnoise.audio.effects.EffectChain;
import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.ByteArrayOutputStream;

/**
 * Micro-optimized AudioEngine for HarshNoiseGo
 * Based on the proven working audio method with performance enhancements
 */
public class AudioEngine {
    
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int LINE_BUFFER_MULT = 4;
    private static final float DENORMAL_BIAS = 1e-20f;
    private static final int JIT_WARMUP_BLOCKS = 15;
    private static final double FIXED_VOLUME = 0.7; // Fixed output volume
    
    private final NoiseGenerator noiseGenerator;
    private final AtomicBoolean isPlaying;
    private SourceDataLine audioLine;
    private Thread audioThread;
    
    // Effect chain
    private EffectChain effectChain;
    
    // Pre-allocated buffers for performance
    private final float[] audioBuffer;
    private final float[] processedBuffer;
    private final byte[] pcmBuffer;
    
    // Audio parameters
    private volatile double whiteNoiseLevel = 0.3;  // Default white noise
    private volatile double pinkNoiseLevel = 0.0;
    private volatile double granularNoiseLevel = 0.0;
    private volatile double brownNoiseLevel = 0.0;  // New: Brown noise
    private volatile double blueNoiseLevel = 0.0;   // New: Blue noise
    private volatile double violetNoiseLevel = 0.0; // New: Violet noise
    private volatile double impulseNoiseLevel = 0.0; // New: Impulse noise
    private volatile double modulatedNoiseLevel = 0.0; // New: Modulated noise
    private volatile double volume = 0.7;           // Higher default volume
    private volatile double tone = 0.0;            // Tone parameter (0..1, start at left)
    private volatile double distortionLevel = 0.0;
    private volatile double filterLevel = 0.0;
    private volatile int grainSize = 1;
    
    // Tone tilt state for smooth filtering
    private double tiltState = 0.0;
    
    // Waveform visualization buffer - circular buffer for latest samples
    private final float[] waveformBuffer = new float[1024]; // Store last 1024 samples
    private int waveformWriteIndex = 0;
    private volatile boolean waveformBufferReady = false;
    
    // Recording support - optimized with ByteArrayOutputStream
    private volatile boolean isRecording = false;
    private final ByteArrayOutputStream recordedAudioData;
    
    public AudioEngine() {
        this.noiseGenerator = new NoiseGenerator();
        this.isPlaying = new AtomicBoolean(false);
        
        // Pre-allocate buffers for performance
        this.audioBuffer = new float[BUFFER_SIZE];
        this.processedBuffer = new float[BUFFER_SIZE];
        this.pcmBuffer = new byte[BUFFER_SIZE * BYTES_PER_SAMPLE];
        
        // Initialize effect chain
        this.effectChain = new EffectChain(BUFFER_SIZE, SAMPLE_RATE);
        
        // Initialize recording buffer
        this.recordedAudioData = new ByteArrayOutputStream();
        
        // Set default audio parameters - Silent startup (no noise by default)
        this.whiteNoiseLevel = 0.0;  // No noise types selected initially
        this.pinkNoiseLevel = 0.0;
        this.granularNoiseLevel = 0.0;
        this.volume = 0.7;           // Keep reasonable volume for when user selects noise
        this.distortionLevel = 0.0;
        this.filterLevel = 0.0;
        this.grainSize = 1;
        
        System.out.println("DEBUG: AudioEngine created with silent startup - All noise levels: 0.0, Volume: " + volume);
    }
    
    /**
     * Initialize and start audio system
     */
    public void start() {
        if (isPlaying.get()) {
            System.out.println("DEBUG: Audio engine already running");
            return;
        }
        
        System.out.println("DEBUG: Starting audio engine...");
        
        try {
            // Use constants for AudioFormat construction
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, SIGNED, BIG_ENDIAN);
            System.out.println("DEBUG: Audio format: " + format);
            
            // Get audio line
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Audio line not supported for format: " + format);
            }
            
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            // Open with parameterized internal buffer size
            audioLine.open(format, BUFFER_SIZE * BYTES_PER_SAMPLE * LINE_BUFFER_MULT);
            audioLine.start();
            
            System.out.println("DEBUG: Audio line opened and started successfully");
            
            isPlaying.set(true);
            
            // Reset tone tilt state on start
            tiltState = 0.0;
            
            // Start audio generation thread with high priority and exception handler
            audioThread = new Thread(this::audioGenerationLoop);
            audioThread.setDaemon(true);
            audioThread.setPriority(Thread.MAX_PRIORITY);
            audioThread.setUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("Audio thread crashed: " + throwable.getMessage());
                isPlaying.set(false);
            });
            audioThread.start();
            
            System.out.println("DEBUG: Audio engine started successfully");
            
        } catch (Exception e) {
            System.err.println("ERROR starting audio: " + e.getMessage());
            e.printStackTrace();
            isPlaying.set(false);
        }
    }
    
    /**
     * Stop audio playback
     */
    public void stop() {
        System.out.println("DEBUG: Stopping audio engine...");
        
        isPlaying.set(false);
        
        // Reset tone tilt state on stop
        tiltState = 0.0;
        
        if (audioThread != null) {
            audioThread.interrupt();
            try {
                audioThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            audioThread = null;
        }
        
        if (audioLine != null) {
            try {
                audioLine.drain();
                audioLine.stop();
                audioLine.close();
            } catch (Exception e) {
                System.err.println("Error closing audio line: " + e.getMessage());
            }
            audioLine = null;
        }
        
        System.out.println("DEBUG: Audio engine stopped");
    }
    
    /**
     * Micro-optimized audio generation loop
     */
    private void audioGenerationLoop() {
        System.out.println("DEBUG: Audio generation loop started");
        
        // JIT warm-up: run silent blocks before actual audio output
        for (int warmup = 0; warmup < JIT_WARMUP_BLOCKS; warmup++) {
            if (!isPlaying.get() || Thread.currentThread().isInterrupted()) {
                break;
            }
            
            // Generate silent audio for warm-up
            for (int i = 0; i < BUFFER_SIZE; i++) {
                audioBuffer[i] = DENORMAL_BIAS;
            }
            
            // Process through effect chain for warm-up
            effectChain.process(audioBuffer, processedBuffer, BUFFER_SIZE);
            
            try {
                Thread.sleep(1); // Minimal sleep for warm-up
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("DEBUG: JIT warm-up completed");
        
        while (isPlaying.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Snapshot parameters once per buffer block for performance
                final double whiteLevel = whiteNoiseLevel;
                final double pinkLevel = pinkNoiseLevel;
                final double granularLevel = granularNoiseLevel;
                final double brownLevel = brownNoiseLevel;
                final double blueLevel = blueNoiseLevel;
                final double violetLevel = violetNoiseLevel;
                final double impulseLevel = impulseNoiseLevel;
                final double modulatedLevel = modulatedNoiseLevel;
                final double currentTone = tone;
                final double distortionLevel = this.distortionLevel;
                final double filterLevel = this.filterLevel;
                final int currentGrainSize = grainSize;
                
                // Snapshot recording state to reduce volatile reads
                final boolean recordingActive = isRecording;
                
                // Fill buffer with audio samples - optimized with float calculations and denormal guard
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    // Generate mixed noise
                    double sample = noiseGenerator.generateMixedNoise(
                        whiteLevel, 
                        pinkLevel, 
                        granularLevel,
                        brownLevel,
                        blueLevel,
                        violetLevel,
                        impulseLevel,
                        modulatedLevel,
                        currentGrainSize
                    );
                    
                    // Apply effects
                    sample = noiseGenerator.applyDistortion(sample, distortionLevel);
                    sample = noiseGenerator.applyLowPassFilter(sample, filterLevel);
                    
                    // Apply tone tilt and fixed volume
                    sample = applyToneTilt(sample, currentTone);
                    sample *= FIXED_VOLUME;
                    
                    // Add denormal bias and store in float buffer
                    audioBuffer[i] = (float) sample + DENORMAL_BIAS;
                    
                    // Store sample in waveform buffer for visualization
                    waveformBuffer[waveformWriteIndex] = audioBuffer[i];
                    waveformWriteIndex = (waveformWriteIndex + 1) % waveformBuffer.length;
                    waveformBufferReady = true;
                }
                
                // Apply effect chain processing - try in-place processing if supported
                effectChain.process(audioBuffer, audioBuffer, BUFFER_SIZE);
                
                // Convert processed audio to PCM format with hard-clipping and NaN/Infinity guards
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    // Hard-clip to [-1, 1] and guard against NaN/Infinity
                    float sample = audioBuffer[i];
                    
                    // Check for NaN or Infinity
                    if (Float.isNaN(sample) || Float.isInfinite(sample)) {
                        sample = 0.0f;
                    }
                    
                    // Hard-clip to [-1, 1] range
                    if (sample > 1.0f) sample = 1.0f;
                    if (sample < -1.0f) sample = -1.0f;
                    
                    // Convert to 16-bit PCM - optimized with float
                    short pcmSample = (short) (sample * 32767.0f);
                    int bufferIndex = i * BYTES_PER_SAMPLE;
                    pcmBuffer[bufferIndex] = (byte) (pcmSample & 0xFF);
                    pcmBuffer[bufferIndex + 1] = (byte) ((pcmSample >> 8) & 0xFF);
                }
                
                // Write to audio line - blocking behavior drives timing
                audioLine.write(pcmBuffer, 0, pcmBuffer.length);
                
                // Record audio data if recording is active - bulk write for performance
                if (recordingActive) {
                    recordedAudioData.write(pcmBuffer, 0, pcmBuffer.length);
                }
                
            } catch (Throwable t) {
                // Exception handler will deal with this, just break the loop
                break;
            }
        }
        
        System.out.println("DEBUG: Audio generation loop stopped");
    }
    
    // Parameter setters
    public void setWhiteNoiseLevel(double level) {
        this.whiteNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: White noise level set to: " + this.whiteNoiseLevel);
    }
    
    public void setPinkNoiseLevel(double level) {
        this.pinkNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Pink noise level set to: " + this.pinkNoiseLevel);
    }
    
    public void setGranularNoiseLevel(double level) {
        this.granularNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Granular noise level set to: " + this.granularNoiseLevel);
    }
    
    public void setBrownNoiseLevel(double level) {
        this.brownNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Brown noise level set to: " + this.brownNoiseLevel);
    }
    
    public void setBlueNoiseLevel(double level) {
        this.blueNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Blue noise level set to: " + this.blueNoiseLevel);
    }
    
    public void setVioletNoiseLevel(double level) {
        this.violetNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Violet noise level set to: " + this.violetNoiseLevel);
    }
    
    public void setImpulseNoiseLevel(double level) {
        this.impulseNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Impulse noise level set to: " + this.impulseNoiseLevel);
    }
    
    public void setModulatedNoiseLevel(double level) {
        this.modulatedNoiseLevel = Math.max(0.0, Math.min(1.0, level));
        System.out.println("DEBUG: Modulated noise level set to: " + this.modulatedNoiseLevel);
    }
    
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        System.out.println("DEBUG: Volume set to: " + this.volume);
    }
    
    public void setTone(double v) {
        this.tone = Math.max(0.0, Math.min(1.0, v));
    }
    
    public void setDistortionLevel(double level) {
        this.distortionLevel = Math.max(0.0, Math.min(1.0, level));
    }
    
    public void setFilterLevel(double level) {
        this.filterLevel = Math.max(0.0, Math.min(1.0, level));
    }
    
    public void setGrainSize(int size) {
        this.grainSize = Math.max(1, Math.min(1024, size));
    }
    
    /**
     * Apply tone tilt EQ - lightweight tilt filter with improved characteristics
     * Features: reduced tilt strength, loudness compensation, pivot frequency compensation
     */
    private double applyToneTilt(double x, double t) {
        // A. Loudness compensation + gentler tilt strength
        // t in [0,1] → g in [-0.6, 0.6] (reduced from ±0.9)
        double g = (t - 0.5) * 1.2;
        
        // B. Sample rate dependent "pivot frequency" - 1200Hz pivot frequency
        // Calculate first-order LP coefficient to approximate fixed pivot frequency
        double pivotFreq = 1200.0; // Hz
        double a = 1.0 - Math.exp(-2.0 * Math.PI * pivotFreq / SAMPLE_RATE);
        
        // C. Update filter state
        tiltState += a * (x - tiltState);
        
        // Apply tilt
        double tilted = x + g * (x - tiltState);
        
        // A. Loudness compensation - proportional gain compensation based on |g|
        double loudnessComp = 1.0 - Math.abs(g) * 0.15; // Maximum 15% compensation
        return tilted * loudnessComp;
    }
    
    // Parameter getters
    public double getWhiteNoiseLevel() { return whiteNoiseLevel; }
    public double getPinkNoiseLevel() { return pinkNoiseLevel; }
    public double getGranularNoiseLevel() { return granularNoiseLevel; }
    public double getBrownNoiseLevel() { return brownNoiseLevel; }
    public double getBlueNoiseLevel() { return blueNoiseLevel; }
    public double getVioletNoiseLevel() { return violetNoiseLevel; }
    public double getImpulseNoiseLevel() { return impulseNoiseLevel; }
    public double getModulatedNoiseLevel() { return modulatedNoiseLevel; }
    public double getVolume() { return volume; }
    public double getTone() { return tone; }
    public double getDistortionLevel() { return distortionLevel; }
    public double getFilterLevel() { return filterLevel; }
    public int getGrainSize() { return grainSize; }
    
    // Recording control methods - optimized with ByteArrayOutputStream
    public void startRecording() {
        isRecording = true;
        recordedAudioData.reset();
        System.out.println("DEBUG: Recording started in AudioEngine");
    }
    
    public void stopRecording() {
        isRecording = false;
        System.out.println("DEBUG: Recording stopped in AudioEngine");
    }
    
    public byte[] getRecordedAudioData() {
        return recordedAudioData.toByteArray();
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Reset tone tilt state - useful for manual reset or parameter changes
     */
    public void resetToneTilt() {
        tiltState = 0.0;
    }
    
    /**
     * Get latest waveform samples for visualization
     * Returns a copy of the latest samples in the circular buffer
     * @return Array of latest audio samples, or null if no data available
     */
    public float[] getLatestWaveformSamples() {
        if (!waveformBufferReady) {
            return null;
        }
        
        float[] samples = new float[waveformBuffer.length];
        
        // Copy samples starting from current write position
        int readIndex = waveformWriteIndex;
        for (int i = 0; i < waveformBuffer.length; i++) {
            samples[i] = waveformBuffer[readIndex];
            readIndex = (readIndex + 1) % waveformBuffer.length;
        }
        
        return samples;
    }
    
    /**
     * Check if waveform data is available
     * @return true if waveform buffer has been populated
     */
    public boolean isWaveformDataAvailable() {
        return waveformBufferReady;
    }
    
    /**
     * Get effect chain instance
     * @return Effect chain instance
     */
    public EffectChain getEffectChain() {
        return effectChain;
    }
} 