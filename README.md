# S24-RAG-Assistant

A fully private, on-device RAG (Retrieval-Augmented Generation) personal assistant for Samsung S24 Ultra with GitHub integration.

## Features

- **100% Private**: All processing happens on your device
- **GitHub Integration**: Pull and push changes directly from the app
- **Knowledge Base**: Upload and query your personal documents
- **Remote Control**: Execute Termux commands via GitHub issues
- **Voice Interface**: Talk to your assistant (coming soon)

## Quick Start

### Option 1: One-Command Installation

1. Open Termux on your Samsung S24 Ultra
2. Run:
   ```bash
   bash <(curl -sSL https://raw.githubusercontent.com/Kenju-Daw/S24-RAG-Assistant/main/install.sh)
   ```

---

# Project Claw — Headless Android AI Inference Engine

Gemma 4 E2B on Samsung Galaxy S24 Ultra via LiteRT-LM

## Overview

Project Claw is the **local inference core** of the S24 RAG Assistant. It runs as a persistent Android Foreground Service exposing an OpenAI-compatible REST API (`/v1/chat/completions`) on `127.0.0.1:8080`, enabling all local agents and Termux workflows to route inference to a native, GPU-accelerated Gemma 4 E2B model — with zero cloud dependency.

## Hardware Target

- **Device:** Samsung Galaxy S24 Ultra
- **SoC:** Qualcomm Snapdragon 8 Gen 3 (SM8650)
- **GPU:** Adreno 750 (ML Drift delegate — 3808 tk/s prefill)
- **RAM:** 12 GB LPDDR5X
- **Model:** Gemma 4 E2B (4-bit quantized, 2.58 GB, 128K context)

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
│   └── architectural_blueprint.md     # Full technical spec
├── research/
│   └── mediapipe_vs_litert_lm.md      # Runtime comparison analysis
└── app/                               # Android project (Phase 1+)
    └── (to be scaffolded)
```

## Status

- [x] Architectural blueprint researched and documented
- [x] Runtime decision made (LiteRT-LM v0.10.2)
- [x] MediaPipe vs LiteRT-LM comparison completed
- [x] Pushed to S24-RAG-Assistant repo
- [ ] Phase 0: Hardware validation (AI Edge Gallery on S24U)
- [ ] Phase 1: Android project scaffolding
- [ ] Phase 2: Kotlin engine + Ktor server
- [ ] Phase 3: Native C++/JNI bridge
- [ ] Phase 4: Hardening (memory, thermals, security)
- [ ] Phase 5: RAG integration with S24-RAG-Assistant
- [ ] Phase 6: Polish & release
