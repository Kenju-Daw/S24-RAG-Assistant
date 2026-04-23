# Project Claw 🦾

> **Headless Android AI Inference Engine — Gemma 4 E2B on Samsung Galaxy S24 Ultra**

A persistent, on-device LLM server running as an Android Foreground Service. Exposes an OpenAI-compatible REST API (`POST /v1/chat/completions`) on `127.0.0.1:8080` — letting any local CLI agent, Termux script, or tool call route inference to a native, GPU-accelerated Gemma 4 E2B model with **zero cloud dependency**.

---

## Hardware Target

| Component | Spec |
|-----------|------|
| Device | Samsung Galaxy S24 Ultra |
| SoC | Qualcomm Snapdragon 8 Gen 3 (SM8650) |
| GPU | Adreno 750 |
| RAM | 12 GB LPDDR5X |
| Model | Gemma 4 E2B — 4-bit quantized, 2.58 GB, 128K context |

## Performance (ML Drift GPU Delegate)

| Backend | Prefill (tk/s) | Decode (tk/s) | TTFT (s) | Peak RAM |
|---------|---------------|---------------|----------|----------|
| CPU (XNNPack) | 557 | 47 | 1.8s | 1.7 GB |
| **GPU (ML Drift)** | **3808** | **52** | **0.3s** | **676 MB** |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│       CLI Agent / Termux / Any HTTP Client   │
│         (OpenAI-compatible client)           │
└───────────────────┬─────────────────────────┘
                    │ POST /v1/chat/completions
                    │ (127.0.0.1:8080)
┌───────────────────▼─────────────────────────┐
│           Ktor HTTP Server (Netty)           │
│           SSE Streaming Support              │
├─────────────────────────────────────────────┤
│           OpenAI Compatibility Layer         │
│     Prompt Formatter (Gemma 4 ctrl tokens)   │
│       Tool Calling / Constrained Decode      │
├─────────────────────────────────────────────┤
│       LiteRT-LM Engine (Kotlin API v0.10.2)  │
│       Session Management (CoW KV-cache)      │
├─────────────────────────────────────────────┤
│          ML Drift GPU Delegate               │
│        Adreno 750 (OpenCL/GLES)              │
│          Zero-Copy UMA Buffers               │
├─────────────────────────────────────────────┤
│         Android Foreground Service           │
│         PARTIAL_WAKE_LOCK                    │
│         OOM Priority Elevation               │
└─────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | **LiteRT-LM v0.10.2** (replaces deprecated MediaPipe) |
| HTTP Server | Ktor Netty (embedded) |
| Language | Kotlin + Android NDK (C++ JNI optional) |
| GPU | ML Drift delegate (OpenCL/GLES, zero-copy UMA) |
| Memory | mmap model weights (demand-paged, ~0.79 GB active) |
| Android Service | Foreground Service, `specialUse` type |
| Model Format | `.litertlm` (4-bit INT4) |

---

## Project Structure

```
project-claw/
├── README.md
├── build.gradle.kts                   # Root build config
├── settings.gradle.kts                # Root settings
├── gradle.properties                  # Gradle flags
├── .github/
│   └── workflows/                     # CI/CD pipelines
├── docs/
│   └── architectural_blueprint.md     # Full technical spec
├── research/
│   └── mediapipe_vs_litert_lm.md      # Runtime comparison analysis
├── app/                               # Android project (Phase 1+)
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/kenjudaw/projectclaw/
│           ├── InferenceService.kt    # Foreground Service + Ktor
│           ├── LlmEngine.kt           # LiteRT-LM wrapper
│           ├── KtorServer.kt          # OpenAI API endpoint
│           └── MainActivity.kt
└── archive/
    └── s24-rag-assistant/             # Previous RAG assistant code
```

---

## Build Status

- [x] Architecture researched and documented
- [x] Runtime decision: LiteRT-LM v0.10.2 (see `research/`)
- [x] Repo initialized on GitHub
- [x] **Phase 0:** Hardware validation — ✅ Confirmed Gemma 4 runs on Adreno 750 via AI Edge Gallery
- [x] **Phase 1:** Android project scaffold — ✅ Project structure fixed and CI/CD pipelines running
- [x] **Phase 2:** Ktor HTTP server + OpenAI compatibility layer — ✅ Implemented `/v1/chat/completions`
- [x] **Phase 3:** LiteRT-LM engine integration — ✅ Connected real v0.10.2 Kotlin API and GPU backend
- [ ] **Phase 4:** Native C++/JNI bridge (zero-copy optimization)
- [ ] **Phase 5:** Hardening (memory, thermals, security)
- [ ] **Phase 6:** Agent integration

---

## Key References

- [LiteRT-LM GitHub](https://github.com/google-ai-edge/LiteRT-LM)
- [Android SDK Guide](https://ai.google.dev/edge/litert-lm/android)
- [AI Edge Gallery (Phase 0 test)](https://github.com/google-ai-edge/gallery)
- [Gemma 4 on Edge — Blog](https://developers.googleblog.com/bring-state-of-the-art-agentic-skills-to-the-edge-with-gemma-4/)
