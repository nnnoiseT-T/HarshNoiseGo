# HarshNoiseGo!

A real-time harsh noise generator and audio effects processor built with JavaFX. Create experimental soundscapes, industrial textures, and extreme audio experiences.

## Changelog

### September 6, 2025
- **Added Effects Chain** - Comprehensive audio effects processing system
- **Added Joystick Control** - Interactive joystick for real-time parameter manipulation
- **Added Perlin Noise Modulation** - Algorithmic parameter modulation using Perlin noise algorithms
- **Updated Volume to Tone** - Replaced volume control with tone tilt EQ for better frequency shaping

## Features

HarshNoiseGo offers a comprehensive suite of audio generation and processing capabilities. The application features eight distinct noise types including classic white noise, warm pink noise, textured granular noise, low-frequency brown noise, bright blue noise, piercing violet noise, sharp impulse noise, and dynamic modulated noise with LFO control. Complementing the noise generation is a effects processing system with ten audio effects: distortion with hard/soft clipping and tone control, tube-style overdrive saturation, high-gain asymmetric fuzz distortion, bitcrusher for bit depth and sample rate reduction, stereo delay with feedback, Schroeder reverb algorithm, real-time granular synthesis, block-based reverse delay, threshold reflection wavefolder, multi-track loop recording, and effect bypass utility. Real-time control is provided through intuitive parameter adjustments including frequency tilt EQ for brightness control, distortion character shaping, low-pass filtering for timbre control, and grain size manipulation for texture quality. The application includes comprehensive recording and export functionality with one-click WAV recording, automatic desktop saving, real-time waveform visualization, and recorded audio playback capabilities. Creative exploration is enhanced through multiple modes including instant parameter randomization, automatic chaos mode for parameter evolution, algorithmic Perlin noise modulation, and interactive joystick control for real-time parameter manipulation. Perfect for when you want to make your neighbors question their life choices♪(´ε｀ ).

## Quick Start

### Prerequisites
- **Java 17** or higher
- **Maven 3.6** or higher
- **JavaFX 20** (included in dependencies)

### Installation & Run
```bash
# Clone or download the project
cd HarshNoiseGo-main

# Compile the project
mvn compile

# Run the application
mvn javafx:run
```

## Project Structure

```
src/main/java/com/harshnoise/
├── Main.java                    # Application entry point
├── MainGUI.java                 # Main interface coordinator
├── AudioEngine.java             # Core audio processing engine
├── NoiseGenerator.java          # Multi-type noise generation
├── Recorder.java                # Audio recording functionality
├── PerlinNoise.java             # Perlin noise algorithms
├── PerlinModulator.java         # Parameter modulation
├── PerlinParameterConfig.java   # Configuration management
├── controller/
│   └── AudioController.java     # MVC controller
├── model/
│   └── AudioModel.java          # MVC model
├── view/                        # MVC view components
│   ├── EffectsRackBar.java      # Effects management UI
│   ├── EffectCard.java          # Individual effect UI
│   ├── NoiseTypePanel.java      # Noise type selection
│   ├── ParameterPanel.java      # Parameter controls
│   ├── WaveformPanel.java       # Real-time waveform display
│   ├── JoystickCard.java        # Interactive joystick control
│   └── JoystickControl.java     # Joystick widget implementation
└── audio/effects/               # 16 audio effects
    ├── AudioEffect.java         # Effect interface
    ├── DistortionEffect.java    # Distortion processing
    ├── OverdriveEffect.java     # Overdrive saturation
    ├── FuzzEffect.java          # Fuzz distortion
    ├── BitcrusherEffect.java    # Bit depth reduction
    ├── DelayEffect.java         # Stereo delay
    ├── ReverbEffect.java        # Reverb processing
    ├── GranularEffect.java      # Granular synthesis
    ├── ReverseDelayEffect.java  # Reverse delay
    ├── WavefolderEffect.java    # Waveform folding
    ├── LooperEffect.java        # Loop recording
    ├── EffectsController.java   # Effects management
    ├── EffectChain.java         # Effect chaining
    ├── EffectRegistry.java      # Effect registration
    └── BypassEffect.java        # Effect bypass
```

## About Harsh Noise

Harsh Noise is an experimental music genre characterized by extreme sonic textures, aggressive frequency content, and unconventional sound manipulation techniques. Originating from the industrial and noise music movements of the 1970s-80s, harsh noise explores the boundaries of what constitutes "music" by embracing distortion, feedback, and chaotic audio elements.

## Contributing

This project is open for contributions!

## License

This project is licensed under the terms specified in the LICENSE file.

---

**Start your harsh noise creation journey!** 