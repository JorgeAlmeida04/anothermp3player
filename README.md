<div align="center">
  <img src="amp3p.png" width="128" alt="AnotherMP3Player Logo">
  <h1>AnotherMP3Player (AMP3P)</h1>
  <p><strong>A modern, high-performance MP3 player inspired by the YouTube Music for Desktop aesthetic.</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk" alt="Java 25">
    <img src="https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=javafx" alt="JavaFX 21">
    <img src="https://img.shields.io/badge/SQLite-3-003B57?style=for-the-badge&logo=sqlite" alt="SQLite">
    <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT License">
  </p>
</div>

---

##  Overview

**AnotherMP3Player (AMP3P)** is yet another desktop music player built with the latest Java technologies. It combines a sleek, modern interface—inspired by the layout of YouTube Music with robust local library management (at least, I tried to) and advanced features like synchronized lyrics.

Whether you're looking for a distraction-free listening experience or a way to manage a large local music collection, AMP3P is designed to be fast, responsive, and visually pleasing.

---

## Features

### Lyrics Support
- **Synced Lyrics (`.lrc`)**: Enjoy a karaoke-style experience with automatic line highlighting as the music plays.
- **Unsynced Lyrics (`.txt`)**: Support for raw text lyrics for songs without precise timing.
- **Automatic Discovery**: Simply place your `.lrc` or `.txt` file in the same folder as your MP3 with the same filename.

### UI
- **YouTube Music Inspired**: A familiar, clean, and interactive layout.
- **Smooth Animations**: Powered by the `animated` library for a fluid user experience.
- **"Now Playing" View**: Full-screen style view with high-quality cover art and interactive controls.

---

## Getting Started

### Prerequisites
- **JDK 25** or higher (Required for latest features and performance)
- **Maven** (For building and dependency management)

### Installation & Running

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/anothermp3player.git
   cd anothermp3player
   ```

2. **Build and Run:**
   ```bash
   mvn clean javafx:run
   ```

---

## 📖 How to Use

1. **Adding Music**: Click the "Load" button or the "Songs" > "Load Songs" menu to select your MP3 files. AMP3P will automatically scan them and add them to your persistent library.
2. **Lyrics**: To see lyrics, ensure a `.lrc` or `.txt` file exists in the same directory as the song (e.g., `Song.mp3` and `Song.lrc`).
3. **Switching Views**: Press the bottom bar to switch between the Home view and the "Now Playing" view.

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## ⚖️ License

Distributed under the MIT License. See `LICENSE` for more information.

---

<div align="center">
  <p>Made with ❤️ using Java & JavaFX</p>
</div>
