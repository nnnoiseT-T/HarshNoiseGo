package com.harshnoise;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Optimized Recorder class for robust audio recording and WAV file handling
 * Handles WAV file creation and playback with improved I/O efficiency and safety
 */
public class Recorder {
    
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private static final int PLAYBACK_BUFFER_SIZE = 16384; // Larger buffer for playback
    
    // Playback thread management
    private volatile Thread playbackThread;
    
    /**
     * Export recorded audio data to WAV file with atomic write and validation
     * @param audioData raw PCM audio data
     * @param outputPath output file path (optional, will generate default if null)
     * @return absolute path to the exported WAV file
     */
    public String exportToWav(byte[] audioData, String outputPath) throws IOException {
        // Validate audio data
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data is null or empty");
        }
        
        // Validate even byte count for 16-bit PCM
        if (audioData.length % 2 != 0) {
            throw new IllegalArgumentException("Audio data length must be even for 16-bit PCM, got: " + audioData.length);
        }
        
        // Validate RIFF size fits in unsigned 32-bit
        long riffSize = 36L + audioData.length;
        if (riffSize > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Audio data too large for WAV format: " + audioData.length + " bytes");
        }
        
        // Generate default path if none provided
        if (outputPath == null || outputPath.trim().isEmpty()) {
            outputPath = getDefaultExportDirectory() + "/" + generateUniqueFilename();
        }
        
        // Ensure .wav extension
        outputPath = ensureWavExtension(outputPath);
        
        Path targetPath = Paths.get(outputPath);
        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");
        
        // Ensure parent directory exists
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            throw new IOException("Failed to create directory: " + targetPath.getParent(), e);
        }
        
        // Write to temporary file first
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempPath.toFile()));
             DataOutputStream dos = new DataOutputStream(bos)) {
            
            // Write WAV header
            writeWavHeader(dos, audioData.length);
            
            // Write audio data
            dos.write(audioData);
            dos.flush();
            
        } catch (IOException e) {
            // Clean up temp file on error
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupError) {
                // Ignore cleanup errors
            }
            throw new IOException("Failed to write WAV file: " + e.getMessage(), e);
        }
        
        // Atomically move temp file to target
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Clean up temp file on error
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupError) {
                // Ignore cleanup errors
            }
            throw new IOException("Failed to finalize WAV file: " + e.getMessage(), e);
        }
        
        String absolutePath = targetPath.toAbsolutePath().toString();
        System.out.println("WAV file exported successfully: " + absolutePath + " (" + audioData.length + " bytes)");
        return absolutePath;
    }
    
    /**
     * Write little-endian 16-bit value
     */
    private void writeLE16(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
    }
    
    /**
     * Write little-endian 32-bit value
     */
    private void writeLE32(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
        dos.writeByte((v >> 16) & 0xFF);
        dos.writeByte((v >> 24) & 0xFF);
    }
    
    /**
     * Write WAV file header with little-endian helpers
     * @param dos DataOutputStream for writing
     * @param dataLength length of audio data in bytes
     */
    private void writeWavHeader(DataOutputStream dos, int dataLength) throws IOException {
        // RIFF header
        dos.writeBytes("RIFF");
        writeLE32(dos, 36 + dataLength);
        dos.writeBytes("WAVE");
        
        // Format chunk
        dos.writeBytes("fmt ");
        writeLE32(dos, 16); // Chunk size
        writeLE16(dos, 1); // Audio format (PCM)
        writeLE16(dos, CHANNELS); // Channels
        writeLE32(dos, SAMPLE_RATE); // Sample rate
        writeLE32(dos, SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8); // Byte rate
        writeLE16(dos, CHANNELS * BITS_PER_SAMPLE / 8); // Block align
        writeLE16(dos, BITS_PER_SAMPLE); // Bits per sample
        
        // Data chunk
        dos.writeBytes("data");
        writeLE32(dos, dataLength);
    }
    
    /**
     * Play recorded audio from WAV file with improved reliability
     * @param wavFilePath path to WAV file
     */
    public void playWavFile(String wavFilePath) {
        // Validate file exists
        File audioFile = new File(wavFilePath);
        if (!audioFile.exists()) {
            System.err.println("WAV file not found: " + wavFilePath);
            return;
        }
        
        // Stop any existing playback
        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playbackThread.interrupt();
                playbackThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Start new playback in daemon thread
        playbackThread = new Thread(() -> {
            AudioInputStream audioInputStream = null;
            SourceDataLine audioLine = null;
            
            try {
                // Get audio input stream
                audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat originalFormat = audioInputStream.getFormat();
                
                // Check if format is supported
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, originalFormat);
                AudioFormat targetFormat = originalFormat;
                
                if (!AudioSystem.isLineSupported(info)) {
                    // Try to convert to compatible PCM format
                    targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16,
                        originalFormat.getChannels(),
                        originalFormat.getChannels() * 2,
                        originalFormat.getSampleRate(),
                        false // little-endian
                    );
                    
                    audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                    System.out.println("Converted audio format for playback: " + targetFormat);
                }
                
                // Get audio line with larger buffer
                info = new DataLine.Info(SourceDataLine.class, targetFormat);
                audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(targetFormat, PLAYBACK_BUFFER_SIZE);
                audioLine.start();
                
                // Play audio
                byte[] buffer = new byte[PLAYBACK_BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    audioLine.write(buffer, 0, bytesRead);
                }
                
                // Ensure proper cleanup
                if (!Thread.currentThread().isInterrupted()) {
                    audioLine.drain();
                }
                
            } catch (Exception e) {
                System.err.println("Error during playback of " + wavFilePath + ": " + e.getMessage());
            } finally {
                // Clean up resources
                if (audioLine != null) {
                    try {
                        audioLine.stop();
                        audioLine.close();
                    } catch (Exception e) {
                        System.err.println("Error closing audio line: " + e.getMessage());
                    }
                }
                
                if (audioInputStream != null) {
                    try {
                        audioInputStream.close();
                    } catch (IOException e) {
                        System.err.println("Error closing audio stream: " + e.getMessage());
                    }
                }
            }
        });
        
        playbackThread.setDaemon(true);
        playbackThread.start();
    }
    
    /**
     * Get default export directory (Desktop if exists, otherwise current directory)
     * @return path to export directory
     */
    public String getDefaultExportDirectory() {
        try {
            String desktop = System.getProperty("user.home") + "/Desktop";
            Path desktopPath = Paths.get(desktop);
            if (Files.exists(desktopPath)) {
                return desktop;
            }
        } catch (Exception e) {
            // Fall back to current directory
        }
        return System.getProperty("user.dir");
    }
    
    /**
     * Generate unique filename for export
     * @return unique filename
     */
    public String generateUniqueFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "HarshNoise_" + timestamp + ".wav";
    }
    
    /**
     * Ensure filename has .wav extension
     * @param name filename to check
     * @return filename with .wav extension
     */
    private static String ensureWavExtension(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "audio.wav";
        }
        
        String trimmed = name.trim();
        if (!trimmed.toLowerCase().endsWith(".wav")) {
            return trimmed + ".wav";
        }
        return trimmed;
    }
} 