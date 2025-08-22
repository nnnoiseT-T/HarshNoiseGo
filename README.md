# HarshNoiseGo! 

A real-time harsh noise generator built with JavaFX desktop application.

## Features

### 8 Noise Types
- **White Noise** - Classic white noise
- **Pink Noise** - Warm pink noise
- **Granular Noise** - Granular texture noise
- **Brown Noise** - Low-frequency brown noise
- **Blue Noise** - High-frequency blue noise
- **Violet Noise** - Ultra high-frequency violet noise
- **Impulse Noise** - Impulse/spike noise
- **Modulated Noise** - LFO modulated noise

### Real-time Controls
- **Volume** - 0.0 to 1.0
- **Distortion** - Add harsh character
- **Low-pass Filter** - Control timbre
- **Grain Size** - Affect texture quality

### Recording
- One-click recording, export to WAV
- Auto-save to desktop
- Playback recorded audio

### Creative Modes
- **Randomize** - Randomize all parameters
- **Chaos Mode** - Automatic parameter changes

## Quick Start

### Requirements
- Java 17+
- Maven 3.6+

### Run
```bash
# Compile
mvn compile

# Run
mvn javafx:run
```

## Usage

1. **Launch** - App starts silent, manually select noise types
2. **Select Noise** - Check any combination of noise types
3. **Adjust Parameters** - Use sliders to control sound and effects
4. **Start Recording** - Click record button in top-right
5. **Save Work** - Auto-export WAV file after recording

## Interface Layout

- **Left** - Noise type selection panel
- **Center** - Real-time waveform + recording controls
- **Right** - Parameter adjustment panel
- **Bottom** - Control button area

## Tech Stack

- **JavaFX** - User interface framework
- **Java Sound API** - Audio processing
- **Maven** - Project management and build
- **Real-time Audio Generation** - Low latency audio output

## Project Structure

```
src/main/java/com/harshnoise/
├── Main.java              # App entry point
├── MainGUI.java           # Main interface
├── AudioEngine.java       # Audio engine
├── NoiseGenerator.java    # Noise generator
└── Recorder.java          # Recording functionality
```

## Creative Suggestions

- **Meditation Soundscape**: Brown + Modulated + Low grain size
- **Industrial Effects**: Blue + Violet + Impulse + High distortion
- **Ambient Textures**: All types + Low volume + High grain size
- **Extreme Harsh Noise**: Violet + Impulse + High distortion + Low grain size

## Development Journey

This project started from simple audio tests and evolved through multiple iterations:
- Fixed audio playback issues
- Added 5 new noise types
- Optimized user interface layout
- Implemented recording functionality
- Perfected parameter control system

## About Harsh Noise

Harsh noise is an experimental music genre that produces intense, dissonant sounds through electronic devices. This app lets you create and experiment with various noise combinations in real-time, exploring the boundaries of sound.

---

**Enjoy your harsh noise creation journey!**  
