package com.harshnoise;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Recorder class responsible for recording audio and exporting WAV files
 * Handles WAV file creation and playback of recorded audio
 */
public class Recorder {
    
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    
    /**
     * Export recorded audio data to WAV file
     * @param audioData raw PCM audio data
     * @param outputPath output file path (optional, will generate default if null)
     * @return path to the exported WAV file
     */
    public String exportToWav(byte[] audioData, String outputPath) throws IOException {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data is empty");
        }
        
        // Generate default filename if none provided
        if (outputPath == null || outputPath.trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            outputPath = "HarshNoise_" + timestamp + ".wav";
        }
        
        // Ensure .wav extension
        if (!outputPath.toLowerCase().endsWith(".wav")) {
            outputPath += ".wav";
        }
        
        Path filePath = Paths.get(outputPath);
        
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             DataOutputStream dos = new DataOutputStream(fos)) {
            
            // Write WAV header
            writeWavHeader(dos, audioData.length);
            
            // Write audio data
            dos.write(audioData);
            
            System.out.println("WAV file exported successfully: " + filePath.toAbsolutePath());
            return filePath.toAbsolutePath().toString();
            
        } catch (IOException e) {
            System.err.println("Error exporting WAV file: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Write WAV file header
     * @param dos DataOutputStream for writing
     * @param dataLength length of audio data in bytes
     */
    private void writeWavHeader(DataOutputStream dos, int dataLength) throws IOException {
        // RIFF header
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + dataLength));
        dos.writeBytes("WAVE");
        
        // Format chunk
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16)); // Chunk size
        dos.writeShort(Short.reverseBytes((short) 1)); // Audio format (PCM)
        dos.writeShort(Short.reverseBytes((short) CHANNELS)); // Channels
        dos.writeInt(Integer.reverseBytes(SAMPLE_RATE)); // Sample rate
        dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8)); // Byte rate
        dos.writeShort(Short.reverseBytes((short) (CHANNELS * BITS_PER_SAMPLE / 8))); // Block align
        dos.writeShort(Short.reverseBytes((short) BITS_PER_SAMPLE)); // Bits per sample
        
        // Data chunk
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(dataLength));
    }
    
    /**
     * Play recorded audio from WAV file
     * @param wavFilePath path to WAV file
     */
    public void playWavFile(String wavFilePath) {
        try {
            File audioFile = new File(wavFilePath);
            if (!audioFile.exists()) {
                System.err.println("WAV file not found: " + wavFilePath);
                return;
            }
            
            // Get audio input stream
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            
            // Get audio format
            AudioFormat format = audioInputStream.getFormat();
            
            // Get audio line
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Audio line not supported for this format");
                return;
            }
            
            SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();
            
            // Play audio in background thread
            Thread playbackThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                        audioLine.write(buffer, 0, bytesRead);
                    }
                    
                    audioLine.drain();
                    audioLine.stop();
                    audioLine.close();
                    audioInputStream.close();
                    
                } catch (IOException e) {
                    System.err.println("Error during playback: " + e.getMessage());
                }
            });
            
            playbackThread.setDaemon(true);
            playbackThread.start();
            
        } catch (Exception e) {
            System.err.println("Error playing WAV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get default export directory (user's desktop or current directory)
     * @return path to export directory
     */
    public String getDefaultExportDirectory() {
        String desktop = System.getProperty("user.home") + "/Desktop";
        if (Files.exists(Paths.get(desktop))) {
            return desktop;
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
} 