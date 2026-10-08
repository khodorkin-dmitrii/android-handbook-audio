# AGENTS.md

# Project: Android Handbook Audio

## Purpose

Android Handbook Audio is a native Android learning/audio application built around public audio content from Android Dev Handbook.

The app reads public JSON manifests published by the Handbook GitHub Pages site and streams the referenced audio files over HTTPS.

The project is also intended to be a high-quality Android portfolio project demonstrating modern Android development practices.

The implementation should favor clarity, maintainability, testability, and idiomatic Android architecture over unnecessary abstraction.


# Product concept

The application presents educational audio content organized as playlists and tracks.

Initial content source:

Android Dev Handbook - Short Audio Notes.

The first release contains one playlist:

- Short Audio Notes

It currently contains 10 topic-based tracks available in:

- English
- Russian

Examples:

- Computer Science
- Kotlin
- Android basics
- Jetpack Compose
- Coroutines & Flow
- Architecture
- Dependency Injection
- Networking
- Libraries & Build
- Testing


# Initial screens

The first product iteration has three primary screens plus Settings.


## 1. Playlists

Displays available playlists returned by the remote catalog.

Initial state:

Short Audio Notes
EN / RU

The UI should make it clear that the playlist is available in two languages.

Do not hardcode the assumption that only one playlist will ever exist.


## 2. Track list

Displays tracks belonging to the selected playlist.

Each item should support:

- track title;
- available languages;
- play action directly from the list;
- indication that transcript/subtitle support exists or is planned;
- playback state when relevant.

Language selection behavior will evolve later.

Do not tightly couple track identity to a particular language.


## 3. Player

Full-screen playback experience.

Initial controls:

- track title;
- play / pause;
- previous track;
- next track;
- seek backward;
- seek forward;
- playback progress;
- current position;
- duration;
- playback speed if appropriate.

Audio should continue playing when the user leaves the player screen.


## 4. Settings

Initial Settings can remain minimal.

Likely future options:

- preferred content language;
- playback speed;
- seek interval;
- automatic next-track playback;
- other playback preferences.

Do not build speculative settings before they are needed.


# Future transcript experience

A later milestone will add synchronized transcripts.

The intended experience is similar to synchronized lyrics in YouTube Music.

The transcript screen/area will display vertically ordered text segments.

During playback:

- the currently spoken segment is highlighted;
- the list follows playback automatically;
- previous and upcoming segments remain visible;
- tapping a transcript segment seeks the player to that segment's timestamp.

Conceptually:

player.currentPosition
→ determine active transcript segment
→ highlight segment
→ keep it visible in LazyColumn

User interaction:

tap transcript segment
→ player.seekTo(segment.startMs)
→ playback continues from that position

Timed transcripts will be provided by the Handbook repository as separate public JSON resources.

Do not implement timed transcript synchronization in the initial MVP unless explicitly requested.


# Data source

There is no application backend.

Android Dev Handbook GitHub Pages acts as the static content backend.

The app must consume the public manifest API.

Conceptually:

GitHub Pages
    ↓
catalog.json
    ↓
playlist manifest
    ↓
track metadata
    ↓
MP3 URLs / future transcript URLs


## Root catalog

The app starts by loading the public audio catalog.

The catalog contains:

- schema version;
- playlists;
- stable playlist IDs;
- localized titles;
- available languages;
- playlist manifest URLs.


## Playlist manifests

Each playlist has its own manifest containing:

- stable playlist ID;
- localized metadata;
- ordered tracks;
- stable track IDs;
- localized track titles;
- language-specific media entries;
- public audio URLs;
- future transcript URLs;
- future timed transcript URLs.


# Stable IDs

Stable IDs are domain identifiers.

Examples:

shorts
shorts.kotlin
shorts.jetpack-compose
shorts.coroutines-flow

Never use:

- list position;
- MP3 filename;
- URL;
- localized title

as the persistent identity of a playlist or track.

URLs and filenames may change while IDs remain stable.

# Catalog
https://khodorkin-dmitrii.github.io/android-dev-handbook/assets/audio/catalog.json

# Short Audio Notes manifest
https://khodorkin-dmitrii.github.io/android-dev-handbook/assets/audio/shorts/manifest.json


# Networking

Use normal HTTPS requests for manifests.

The manifest repository is the source of truth for available remote content.

Preferred approach:

- Retrofit + OkHttp
- Kotlin Serialization

Avoid introducing a backend SDK, Firebase, GraphQL, or database unless the product later requires it.

Network DTOs should not leak directly into presentation code.

Map remote DTOs to domain models.


# Audio playback

Use AndroidX Media3.

Primary components:

- ExoPlayer
- MediaSession
- MediaSessionService or MediaLibraryService as appropriate

Audio must be streamed directly from the remote HTTPS MP3 URLs.

Do not download the complete MP3 before playback.

Media3 should handle:

- buffering;
- progressive HTTP playback;
- seeking;
- playback state;
- duration;
- playback position;
- track transitions.

Playback must be independent from Activity/Composable lifecycle.

The player must continue working:

- when navigating between screens;
- while the app is backgrounded;
- through notification/media controls where appropriate.

Do not place ExoPlayer directly inside a ViewModel.


# Architecture

Use clean, conventional MVVM.

Target architecture:

UI
↓
ViewModel
↓
Use cases only when they provide real value
↓
Repository
↓
Remote data source / playback abstraction


## Presentation

Jetpack Compose.

Each screen exposes immutable UI state.

Preferred shape:

ViewModel
→ StateFlow<UiState>
→ Compose UI

User interactions flow back as explicit ViewModel calls or UI actions.

Avoid mutable presentation state scattered across composables.


## ViewModel

ViewModels:

- coordinate screen state;
- call repositories/use cases;
- transform domain data into UI state;
- react to playback state.

They must not:

- own Android Views;
- own Activities;
- directly create ExoPlayer;
- perform raw Retrofit calls.


## Repository

Repositories define the application's data boundary.

Likely repositories:

CatalogRepository
PlaybackRepository or PlayerController abstraction
SettingsRepository
TranscriptRepository later

CatalogRepository provides domain playlists and tracks regardless of how remote manifests are represented.


## Data layer

Remote implementation:

Retrofit / OkHttp
+
Kotlin Serialization

Suggested flow:

Manifest DTO
→ mapper
→ domain model
→ ViewModel/UI


# Playback architecture

Playback is application-level state, not screen-level state.

The Media3 service owns the player.

UI observes a playback abstraction exposing data such as:

PlaybackState(
    trackId,
    isPlaying,
    position,
    duration,
    bufferedPosition,
    playbackSpeed
)

Exact implementation may evolve, but avoid coupling Compose screens directly to ExoPlayer internals.


# Navigation

Use AndroidX Navigation 3 as the project navigation stack.

Do not use legacy Navigation Compose / Navigation 2 APIs unless explicitly required by a future migration or interoperability need.

Navigation should follow Navigation 3 idioms:

- use typed navigation keys rather than string route patterns;
- model destination keys as stable, serializable types where appropriate;
- keep navigation state explicit and separate from screen UI state;
- pass stable IDs through navigation keys rather than serialized domain objects;
- resolve required models through ViewModel/repository state;
- avoid recreating Navigation 2 route-string patterns on top of Navigation 3.

Initial destinations conceptually represent:

- Playlists
- Playlist details for a stable `playlistId`
- Player for a stable `trackId`
- Settings

Keep the initial Navigation 3 setup minimal.
Do not implement speculative navigation abstractions or the complete final destination hierarchy ahead of product requirements.


# Dependency Injection

Use Hilt.

Use constructor injection by default.

Prefer interfaces at architectural boundaries where they provide testability or meaningful separation.

Do not create interfaces for every class mechanically.


# Persistence

For early iterations, use DataStore for lightweight preferences and playback metadata.

Possible persisted data:

- preferred language;
- playback speed;
- last played track;
- last playback position;
- completed/listened state if later required.

Do not introduce Room until there is a real persistence/query requirement.


# Modern Android stack

Use:

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Navigation 3
- ViewModel
- StateFlow / Flow
- Coroutines
- Hilt
- Retrofit
- OkHttp
- Kotlin Serialization
- AndroidX Media3
- DataStore
- JUnit
- appropriate coroutine testing utilities

Prefer current stable AndroidX APIs.

Avoid deprecated APIs and legacy XML unless there is a concrete reason to use them.


# UI principles

Keep the design focused on listening.

Prefer:

- clean typography;
- clear playback state;
- large accessible playback controls;
- simple hierarchy;
- good dark mode support;
- adaptive layouts where useful.

Do not overload the first iteration with visual effects.

The player screen should feel like a real media application rather than a demo screen.


# Language model

Content language is part of a media rendition, not the identity of the track.

Example:

Track:
shorts.kotlin

Media renditions:

shorts.kotlin / en
shorts.kotlin / ru

Switching language should eventually preserve the logical track identity.

Do not create separate domain tracks such as:

shorts.kotlin.en
shorts.kotlin.ru

unless a future requirement proves this model insufficient.


# Error handling

Represent loading/error/empty/content states explicitly.

Handle:

- catalog request failure;
- playlist request failure;
- malformed manifest;
- missing language rendition;
- unavailable audio URL;
- playback failure;
- network interruption.

Do not crash because one remote track is invalid.

A malformed single media entry should not necessarily make the entire catalog unusable if graceful recovery is possible.


# Testing priorities

Prioritize tests around behavior rather than implementation details.

Important early tests:

- manifest DTO → domain mapping;
- playlist ordering;
- language availability;
- stable IDs;
- repository success/error behavior;
- ViewModel state transitions;
- playback state mapping;
- track next/previous behavior.

Later:

- transcript timestamp lookup;
- active transcript segment calculation;
- seeking from transcript selection.


# Project structure

Start simple.

Do not create many Gradle modules solely to demonstrate modularization.

A single app module with clear package boundaries is acceptable for MVP.

Example:

app/
  data/
    remote/
    repository/
    mapper/
  domain/
    model/
    repository/
    usecase/
  playback/
  ui/
    playlists/
    tracks/
    player/
    settings/
  navigation/
  di/

If the project grows substantially, modularization can be introduced later based on real boundaries.


# Engineering rules for Codex

Before changing architecture:

1. inspect the existing project;
2. preserve established conventions unless there is a strong reason to change them;
3. prefer the smallest coherent implementation;
4. avoid speculative abstractions;
5. keep domain models independent from Retrofit and Media3 implementation details;
6. keep playback lifecycle outside screen ViewModels;
7. maintain unidirectional state flow;
8. use Navigation 3 idioms and typed navigation keys rather than Navigation 2-style string routes;
9. add tests for non-trivial behavior;
10. run formatting/build/tests after meaningful changes;
11. report architectural decisions and any deviations from this file.

Do not silently replace an agreed architectural decision with a different pattern.

If a requirement conflicts with this document, call out the conflict before making a broad architectural change.


# Device installation

Do not install the app on a device as part of a routine build or verification.

If the user requests installation, first ask which connected device and Android user/profile to target. After the user confirms, build the APK and install it explicitly for that user with `adb install --user <user-id> -r -t <apk-path>`; do not use `gradlew installDebug`, which does not select an Android user/profile.

Do not install, enable, uninstall, or otherwise modify the app in other users or profiles unless explicitly requested.
