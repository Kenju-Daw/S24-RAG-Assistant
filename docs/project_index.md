# Project Claw Directory Index

This document provides a comprehensive overview of the current folder and file structure for **Project Claw** following the completion of Phase 3.

## Root Directory

| Path | Description |
|------|-------------|
| `build.gradle.kts` | Root Gradle build configuration. Coordinates plugins for the `app` module. |
| `settings.gradle.kts` | Gradle settings file. Defines the project name (`ProjectClaw`) and includes sub-modules (e.g., `:app`). |
| `gradle.properties` | Global Gradle flags (e.g., JVM arguments, memory constraints, AndroidX enablement). |
| `README.md` | The main project documentation, build status, and high-level architectural overview. |

## CI/CD Pipeline
| Path | Description |
|------|-------------|
| `.github/workflows/build.yml` | Automates the `assembleDebug` Gradle task on every push to verify compilation and upload APK artifacts. |
| `.github/workflows/lint.yml` | Enforces code quality using Android Lint and ktlint. |
| `.github/workflows/security.yml` | Runs OWASP Dependency Check and Gitleaks to catch vulnerabilities and hardcoded secrets. |
| `.github/workflows/release.yml` | Automatically cuts a release and uploads a signed APK when a new Git tag is created. |

## Android App Source (`app/`)
This module contains the Android Foreground Service and the LiteRT-LM integration.

| Path | Description |
|------|-------------|
| `app/build.gradle.kts` | App-level build file. Declares dependencies on Ktor Server, Kotlinx Serialization, and the LiteRT-LM SDK. |
| `app/src/main/AndroidManifest.xml` | Declares required permissions (Internet, Foreground Service, Storage) and registers `InferenceService` with the `specialUse` type to prevent Android from aggressively killing it. |
| `app/src/main/java/com/kenjudaw/projectclaw/InferenceService.kt` | The heart of the background process. Elevates the app to a Foreground Service to keep the OS from killing the LLM in memory. Automatically initializes `LlmEngine` and starts `KtorServer` on boot. |
| `app/src/main/java/com/kenjudaw/projectclaw/LlmEngine.kt` | The direct Kotlin wrapper around the Google `com.google.ai.edge.litertlm` API. Responsible for initializing the GPU Backend (`Adreno 750`), loading the `.litertlm` weights, and exposing `generate()` and `generateBlocking()` methods. |
| `app/src/main/java/com/kenjudaw/projectclaw/KtorServer.kt` | An embedded Netty HTTP Server running on `127.0.0.1:8080`. Implements the `/v1/chat/completions` endpoint with full OpenAI schema compatibility. Handles Server-Sent Events (SSE) streaming from the LiteRT-LM engine back to the client. |
| `app/src/main/java/com/kenjudaw/projectclaw/MainActivity.kt` | A minimal launcher UI. It requests storage permissions, kicks off the `InferenceService`, and provides a tiny debug chat window to test local inference without needing a `curl` command. |

## Documentation and Research (`docs/`, `research/`)

| Path | Description |
|------|-------------|
| `docs/architectural_blueprint.md` | The master technical specification detailing the JNI bridge plans, memory mapping strategy, and exact API schemas. |
| `research/mediapipe_vs_litert_lm.md` | Research notes from Phase 0 that dictated the pivot away from the deprecated MediaPipe framework to the raw LiteRT-LM API. |

## Archive (`archive/`)

| Path | Description |
|------|-------------|
| `archive/s24-rag-assistant/` | Old codebase from earlier attempts. Kept for reference but entirely isolated from the new, optimized Project Claw build. |
