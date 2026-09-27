# Android Handbook Audio 🎧

Android Handbook Audio is a native Android companion app for [Android Dev Handbook](https://khodorkin-dmitrii.github.io/android-dev-handbook/en/). It is designed to stream educational audio playlists published through the Handbook's GitHub Pages manifests.

The first content source is **Short Audio Notes**: concise topics covering Android, Kotlin, Computer Science, Coroutines, Architecture, Networking, Testing, and related areas in English and Russian.

## ✨ Goals

- Browse remote playlists and tracks.
- Stream audio directly from the Handbook.
- Support English and Russian content.
- Provide background playback and media controls.
- Later add synchronized transcripts with tap-to-seek.

## 🛠 Tech stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Navigation 3
- MVVM + StateFlow
- Hilt
- Retrofit / OkHttp / Kotlin Serialization
- AndroidX Media3
- DataStore

## 🌐 Data source

Android Dev Handbook GitHub Pages acts as the app's static backend. The public catalog is available at:

https://khodorkin-dmitrii.github.io/android-dev-handbook/assets/audio/catalog.json

## 🗺 Roadmap

- [x] Project foundation
- [ ] Remote catalog and playlist loading
- [ ] Playlist screen
- [ ] Track list
- [ ] Streaming playback
- [ ] Full player screen
- [ ] Background playback / MediaSession
- [ ] Playback polish and persistence
- [ ] Language switching
- [ ] Static transcripts
- [ ] Timed synchronized transcripts
- [ ] Offline/caching experiments

## 🔗 Related

- [Android Dev Handbook](https://khodorkin-dmitrii.github.io/android-dev-handbook/en/)
