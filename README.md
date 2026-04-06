# PocketTrack

PocketTrack is an Android application built with Kotlin and Jetpack Compose. It provides a lightweight local data store using Room and a modern Compose UI. This repository contains the Android app module, Gradle build config, and dependency management for development and testing.

## Key details

- Android app using Kotlin and Jetpack Compose
- Local persistence with Room (KSP + annotation processor present)
- Navigation with Navigation Compose
- Material 3 and Material icons
- Minimum Android SDK: 24, Target/Compile SDK: 36
- JVM target: 11

## Features (high level)

- Compose-based UI and navigation
- Local database storage powered by Room
- Modular, Gradle Kotlin DSL build files

> Note: This README lists inferred capabilities from the project configuration. For feature specifics, check the app source under `app/src/main/java`.

## Requirements

- Android Studio (recommended: latest stable) or the Android SDK + command-line tools
- JDK 11
- Gradle (use the provided Gradle wrapper)

## Getting started

1. Clone the repository:

```bash
git clone https://github.com/anggrainyshia/PocketTrack.git
cd PocketTrack
```

2. Open the project in Android Studio (File → Open) or import the Gradle project.

3. Let Android Studio sync Gradle. If prompted, install any recommended SDK components.

4. Run the app on an emulator or device from Android Studio (Run ▶) or via the command line:

```bash
./gradlew installDebug
```

## Build & test

- Build debug APK:

```bash
./gradlew assembleDebug
```

- Run unit tests:

```bash
./gradlew test
```

- Run instrumentation tests (connected device/emulator):

```bash
./gradlew connectedAndroidTest
```

## Project layout

- `app/` — Android application module (source, resources, Gradle config)
- `gradle/` — Gradle wrapper and version catalog (`libs.versions.toml`)

## Dependencies

Core dependencies are declared in `app/build.gradle.kts`. Key libraries include:

- AndroidX Core, Lifecycle
- Jetpack Compose (UI, Material3, tooling)
- Navigation Compose
- Room (runtime, ktx) with KSP for code generation
- Material Components

For full list, see `app/build.gradle.kts`.

## Contributing

Contributions are welcome. Good first steps:

1. Open an issue describing a bug or improvement.
2. Create a feature branch and a focused pull request.
3. Add tests for new behavior where applicable.

If you plan to contribute, please add a `LICENSE` file to clarify terms.

## License

No license file is included in this repository. If you own this code, consider adding a license (for example, MIT) by adding a `LICENSE` file at the repository root.

## Contact

Repository owner: `anggrainyshia` (on GitHub). For questions, open an issue.
