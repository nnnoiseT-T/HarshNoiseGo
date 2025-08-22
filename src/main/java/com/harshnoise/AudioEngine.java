package com.harshnoise;

import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

/**
 * Working AudioEngine for HarshNoiseGo
 * Based on the proven working audio method
 */
public class AudioEngine {
    
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;
    private static final int BYTES_PER_SAMPLE = 2;
    
    private final NoiseGenerator noiseGenerator;
    private final AtomicBoolean isPlaying;
    private SourceDataLine audioLine;
    private Thread audioThread;
    
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
    private volatile double distortionLevel = 0.0;
    private volatile double filterLevel = 0.0;
    private volatile int grainSize = 64;
    
    // Recording support
    private volatile boolean isRecording = false;
    private final List<Byte> recordedAudioData = new ArrayList<>();
    
    public AudioEngine() {
        this.noiseGenerator = new NoiseGenerator();
        this.isPlaying = new AtomicBoolean(false);
        
        // Set default audio parameters - Silent startup (no noise by default)
        this.whiteNoiseLevel = 0.0;  // No noise types selected initially
        this.pinkNoiseLevel = 0.0;
        this.granularNoiseLevel = 0.0;
        this.volume = 0.7;           // Keep reasonable volume for when user selects noise
        this.distortionLevel = 0.0;
        this.filterLevel = 0.0;
        this.grainSize = 64;
        
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
            // Use exactly the same format as working test
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            System.out.println("DEBUG: Audio format: " + format);
            
            // Get audio line
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Audio line not supported for format: " + format);
            }
            
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();
            
            System.out.println("DEBUG: Audio line opened and started successfully");
            
            isPlaying.set(true);
            
            // Start audio generation thread
            audioThread = new Thread(this::audioGenerationLoop);
            audioThread.setDaemon(true);
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
     * Main audio generation loop - exactly like working test
     */
    private void audioGenerationLoop() {
        byte[] buffer = new byte[BUFFER_SIZE * BYTES_PER_SAMPLE];
        
        System.out.println("DEBUG: Audio generation loop started");
        System.out.println("DEBUG: Initial noise levels - White: " + whiteNoiseLevel + 
                          ", Pink: " + pinkNoiseLevel + ", Granular: " + granularNoiseLevel + 
                          ", Volume: " + volume);
        
        while (isPlaying.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Fill buffer with audio samples - exactly like working test
                for (int i = 0; i < BUFFER_SIZE; i++) {
                                    // Generate mixed noise
                double sample = noiseGenerator.generateMixedNoise(
                    whiteNoiseLevel, 
                    pinkNoiseLevel, 
                    granularNoiseLevel,
                    brownNoiseLevel,
                    blueNoiseLevel,
                    violetNoiseLevel,
                    impulseNoiseLevel,
                    modulatedNoiseLevel,
                    grainSize
                );
                    
                    // Apply effects
                    sample = noiseGenerator.applyDistortion(sample, distortionLevel);
                    sample = noiseGenerator.applyLowPassFilter(sample, filterLevel);
                    
                    // Apply volume
                    sample *= volume;
                    
                    // Convert to 16-bit PCM - exactly like working test
                    short pcmSample = (short) (sample * 32767.0);
                    int bufferIndex = i * BYTES_PER_SAMPLE;
                    buffer[bufferIndex] = (byte) (pcmSample & 0xFF);
                    buffer[bufferIndex + 1] = (byte) ((pcmSample >> 8) & 0xFF);
                }
                
                // Write to audio line - exactly like working test
                audioLine.write(buffer, 0, buffer.length);
                
                // Record audio data if recording is active
                if (isRecording) {
                    for (byte b : buffer) {
                        recordedAudioData.add(b);
                    }
                }
                
                // Small delay - exactly like working test
                Thread.sleep(10);
                
            } catch (Exception e) {
                System.err.println("ERROR in audio generation: " + e.getMessage());
                e.printStackTrace();
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
    
    public void setDistortionLevel(double level) {
        this.distortionLevel = Math.max(0.0, Math.min(1.0, level));
    }
    
    public void setFilterLevel(double level) {
        this.filterLevel = Math.max(0.0, Math.min(1.0, level));
    }
    
    public void setGrainSize(int size) {
        this.grainSize = Math.max(1, Math.min(1024, size));
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
    public double getDistortionLevel() { return distortionLevel; }
    public double getFilterLevel() { return filterLevel; }
    public int getGrainSize() { return grainSize; }
    
    // Recording control methods
    public void startRecording() {
        isRecording = true;
        recordedAudioData.clear();
        System.out.println("DEBUG: Recording started in AudioEngine");
    }
    
    public void stopRecording() {
        isRecording = false;
        System.out.println("DEBUG: Recording stopped in AudioEngine");
    }
    
    public byte[] getRecordedAudioData() {
        byte[] data = new byte[recordedAudioData.size()];
        for (int i = 0; i < recordedAudioData.size(); i++) {
            data[i] = recordedAudioData.get(i);
        }
        return data;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
} 