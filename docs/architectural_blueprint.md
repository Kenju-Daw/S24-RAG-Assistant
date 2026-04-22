# Architectural Blueprint for Project Claw: Headless Android AI Inference Engine via LiteRT-LM and Native Bridging

## Executive Architectural Overview
The deployment of large language models (LLMs) at the extreme edge requires a fundamental shift in system architecture, moving away from thin-client cloud dependencies toward highly autonomous, thick-client execution environments. Project Claw represents the vanguard of this architectural transition. The primary objective is to instantiate a localized, headless artificial intelligence inference backend capable of operating as a persistent microservice. By engineering an Android Foreground Service embedded with a lightweight HTTP server, Project Claw establishes an industry-standard, OpenAI-compatible REST API bridge. This bridge enables arbitrary command-line interface (CLI) agents and local sandbox environments to route tool-calling schemas and inference requests to a native, highly accelerated LLM seamlessly.

**Hardware Target:** Samsung Galaxy S24 Ultra — Qualcomm Snapdragon 8 Gen 3 (SM8650), Adreno 750 GPU, 12GB LPDDR5X RAM.
**Target Model:** Gemma 4 E2B, 4-bit quantized (.litertlm format), 2.58 GB on disk.
**Runtime:** LiteRT-LM (replaces deprecated MediaPipe LLM Inference SDK).

## Key Architecture Decisions

### 1. Runtime: LiteRT-LM (NOT MediaPipe)
- MediaPipe LLM Inference API for Android/iOS is **officially deprecated**
- LiteRT-LM is the production replacement, powers Gemini Nano in Chrome and Pixel Watch
- Natively supports `.litertlm` format
- GPU acceleration via ML Drift delegate

### 2. Hardware Acceleration: ML Drift GPU Delegate
- Bypasses Vulkan driver instability on Adreno GPUs
- Uses OpenCL and OpenGL ES compute shaders tuned by Google+Qualcomm
- 1.4x latency reduction vs legacy TFLite GPU delegate
- Zero-copy buffer interoperability via UMA (no CPU↔GPU memory copies)

**Benchmarks (LiteRT-LM GPU delegate):**
| Backend | Prefill (tk/s) | Decode (tk/s) | TTFT (s) | Peak Memory (MB) |
|---------|----------------|---------------|----------|-------------------|
| CPU (XNNPack) | 557 | 47 | 1.8 | 1733 |
| GPU (ML Drift) | 3808 | 52 | 0.3 | 676 |

### 3. Memory Strategy: mmap + Foreground Service
- Model weights are memory-mapped (mmap) — never fully loaded into RAM
- Text decoder: ~0.79 GB active pinned RAM
- Embeddings: ~1.12 GB memory-mapped (demand-paged)
- Vision/Audio encoders: loaded dynamically only when multimodal input detected
- Android Foreground Service elevates OOM priority, prevents LMKD termination

### 4. Native C++ Integration via JNI
- Ktor HTTP server (Kotlin/JVM) ↔ JNI Bridge ↔ LiteRT-LM C++ Engine
- Direct ByteBuffer for zero-copy JNI crossing
- Token streaming via JNI callback interface
- CMake build linking libLiteRt.so + Android NDK

### 5. OpenAI-Compatible API
- Embedded Ktor server on 127.0.0.1:8080
- Full /v1/chat/completions endpoint
- SSE streaming support
- Gemma 4 prompt formatting (control tokens)
- Tool calling with constrained decoding (ANTLR grammar masking)

### 6. KV-Cache Management
- 128K token context window
- Copy-on-Write (CoW) session cloning for parallel requests (<10ms)
- Prevents OOM from multiple concurrent agent sessions

### 7. Android 14 Foreground Service
- foregroundServiceType="specialUse"
- PARTIAL_WAKE_LOCK for responsive HTTP server
- Thread decoupling: Dispatchers.IO for Ktor, Dispatchers.Default for JNI inference

## Gemma 4 Model Family Reference
| Variant | Effective Params | Total w/ Embeddings | Context | Modalities | Size (4-bit) |
|---------|-----------------|---------------------|---------|------------|---------------|
| E2B | 2.3B | 5.1B | 128K | Text, Image, Audio | 2.58 GB |
| E4B | 4.5B | 8.0B | 128K | Text, Image, Audio | ~4-5 GB |
| 26B A4B | 3.8B (MoE) | 25.2B | 256K | Text, Image | N/A |
| 31B Dense | 31B | 31B | 256K | Text, Image | N/A |
