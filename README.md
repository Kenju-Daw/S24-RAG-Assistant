# Project Claw

Headless Android AI Inference Engine — Gemma 4 E2B on Samsung Galaxy S24 Ultra

## Overview

Project Claw is a persistent, headless AI inference backend running as an Android Foreground Service. It exposes an OpenAI-compatible REST API (`/v1/chat/completions`) on `127.0.0.1:8080`, enabling CLI agents and local sandbox environments to route inference requests to a native, GPU-accelerated Gemma 4 E2B model.

## Hardware Target

- **Device:** Samsung Galaxy S24 Ultra
- **SoC:** Qualcomm Snapdragon 8 Gen 3 (SM8650)
- **GPU:** Adreno 750
- **RAM:** 12 GB LPDDR5X
- **Model:** Gemma 4 E2B (4-bit quantized, 2.58 GB)

## Architecture

```
┌─────────────────────────────────────────────┐
│           CLI Agent / Termux / OpenClaw      │
│           (OpenAI-compatible client)         │
└───────────────────┬─────────────────────────┘
                    │ HTTP POST /v1/chat/completions
                    │ (127.0.0.1:8080)
┌───────────────────▼─────────────────────────┐
│           Ktor HTTP Server (Netty)           │
│           Content Negotiation (JSON)         │
│           SSE Streaming Support              │
├─────────────────────────────────────────────┤
│           OpenAI Compatibility Layer         │
│           Prompt Formatter (Gemma 4 tokens)  │
│           Tool Calling / Constrained Decode  │
├─────────────────────────────────────────────┤
│           LiteRT-LM Engine (Kotlin API)      │
│           Session Management (CoW KV-cache)  │
├─────────────────────────────────────────────┤
│           ML Drift GPU Delegate              │
│           Adreno 750 (OpenCL/GLES)           │
│           Zero-Copy UMA Buffers              │
├─────────────────────────────────────────────┤
│           Android Foreground Service         │
│           PARTIAL_WAKE_LOCK                  │
│           OOM Priority Elevation             │
└─────────────────────────────────────────────┘
```

## Project Structure

```
project-claw/
├── README.md                          # This file
├── docs/
│   └── architectural_blueprint.md     # Full technical spec (from Google Doc)
├── research/
│   └── mediapipe_vs_litert_lm.md      # Runtime comparison analysis
└── app/                               # Android project (Phase 1+)
    └── (to be scaffolded)
```

## Key Documents

| Document | Location |
|----------|----------|
| Architectural Blueprint | `docs/` + Knowledge (`knowledge/project-claw/`) |
| MediaPipe vs LiteRT-LM Analysis | `research/mediapipe_vs_litert_lm.md` |
| Implementation Plan | Antigravity artifact (session-scoped) |

## Runtime Decision

**LiteRT-LM** (not MediaPipe LLM). See [research/mediapipe_vs_litert_lm.md](research/mediapipe_vs_litert_lm.md) for full analysis.

| Factor | MediaPipe LLM | LiteRT-LM (chosen) |
|--------|:---:|:---:|
| Status | ⚠️ Deprecated | ✅ Production |
| Constrained Decoding | ❌ | ✅ |
| Session Cloning | ❌ | ✅ |
| C++ API | ❌ | ✅ |
| GPU (ML Drift) | ✅ Basic | ✅ Advanced |

## Phase 0: Hardware Validation

**Fastest validation path:** Install [AI Edge Gallery](https://github.com/google-ai-edge/gallery) on S24U. Zero code required — tests model loading, GPU acceleration, and inference speed immediately.

## Status

- [x] Architectural blueprint researched and documented
- [x] Runtime decision made (LiteRT-LM)
- [x] MediaPipe vs LiteRT-LM comparison completed
- [x] Project folder created
- [x] Archived to Antigravity knowledge system
- [ ] Phase 0: Hardware validation on S24U
- [ ] Phase 1: Android project scaffolding
- [ ] Phase 2: Kotlin engine + Ktor server
- [ ] Phase 3: Native C++/JNI bridge (optional optimization)
- [ ] Phase 4: Hardening (memory, thermals, security)
- [ ] Phase 5: Agent integration
- [ ] Phase 6: Polish & release
