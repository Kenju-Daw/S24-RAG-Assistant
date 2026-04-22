# Project Claw — Runtime Decision: MediaPipe LLM vs LiteRT-LM

## Research Date: 2026-04-23

---

## The Question
> "MediaPipe LLM Inference API is available for Android and supports Gemma models.
> Wouldn't it be the simplest solution?"

**Short answer: Yes, for prototyping. No, for production.**

Here's the full analysis.

---

## Status of Both Frameworks

### MediaPipe LLM Inference API
- **Package:** `com.google.mediapipe:tasks-genai:0.10.27`
- **Status:** ⚠️ **DEPRECATED for Android and iOS** (as of March 31, 2026)
- **Web version:** Still active (NOT deprecated)
- **Classification:** Experimental / Research / Prototyping only
- **Google's recommendation:** Migrate to LiteRT-LM

### LiteRT-LM
- **Package:** `com.google.ai.edge.litertlm:litertlm-android:0.10.0`
- **Status:** ✅ Active, production-grade
- **Powers:** Gemini Nano in Chrome, Chromebook Plus, Pixel Watch
- **Classification:** Production framework

---

## Side-by-Side Comparison

| Feature | MediaPipe LLM | LiteRT-LM |
|---------|:------------:|:---------:|
| **Status** | ⚠️ Deprecated (Android) | ✅ Production |
| **API Simplicity** | ✅ Very simple (5 lines) | ✅ Simple (Kotlin API) |
| **GPU Acceleration** | ✅ Supported | ✅ ML Drift (1.4x faster) |
| **Gemma 4 E2B Support** | ❓ Lists Gemma-3n, unclear on Gemma 4 | ✅ Native .litertlm format |
| **Session Management** | Basic | ✅ Advanced (CoW KV-cache cloning) |
| **Constrained Decoding** | ❌ Not available | ✅ ANTLR grammar masking |
| **Tool Calling** | ❌ Not built-in | ✅ Native support |
| **Multimodal (Vision/Audio)** | ✅ Via MPImage/audio | ✅ Via dynamic encoder loading |
| **LoRA Fine-tuning** | ✅ Supported | ✅ Supported |
| **C++ Native API** | ❌ Kotlin/Java only | ✅ Full C++ SDK |
| **Memory Management** | Basic | ✅ mmap, demand-paging |
| **Future Updates** | ❌ No new features | ✅ Actively developed |
| **NPU Delegate** | ❌ | ✅ Via QNN delegate |
| **Streaming** | ✅ generateResponseAsync() | ✅ Flow-based streaming |

---

## What MediaPipe LLM Gets RIGHT

### 1. Dead Simple API
```kotlin
// MediaPipe: 5 lines to inference
val options = LlmInferenceOptions.builder()
    .setModelPath("/data/local/tmp/llm/model.task")
    .setMaxTopK(64)
    .build()
val llm = LlmInference.createFromOptions(context, options)
val result = llm.generateResponse("Hello!")
```

### 2. Multimodal Out of the Box
```kotlin
// Vision support is trivial
val mpImage = BitmapImageBuilder(bitmap).build()
session.addQueryChunk("Describe this image")
session.addImage(mpImage)
val result = session.generateResponse()
```

### 3. Well-Documented with Sample Apps
- [AI Edge Gallery](https://github.com/google-ai-edge/gallery) — Full Android app you can clone and run
- Includes benchmarking, model discovery, multi-turn chat

---

## What MediaPipe LLM Gets WRONG (for Project Claw)

### 1. Deprecated = No Future
Google explicitly states: "For production-grade applications, migrate to LiteRT-LM."
Building on a deprecated API means:
- No bug fixes
- No new model format support
- No performance improvements
- Risk of removal in future Android SDK versions

### 2. No Constrained Decoding
Project Claw's core use case is serving CLI agents that need **structured JSON tool-calling responses**. MediaPipe LLM has no mechanism to force the model to output valid JSON. LiteRT-LM's ANTLR grammar masking is essential.

### 3. No C++ Native Layer
For a headless inference engine that needs zero-copy buffers and precise memory control, the Kotlin-only API is a ceiling. LiteRT-LM exposes the full C++ SDK.

### 4. No Advanced Session Management
Multi-agent scenarios where 3-5 CLI agents hit the local API simultaneously require KV-cache session cloning. MediaPipe LLM doesn't support this.

---

## The Verdict: Dual-Track Strategy

### Track A: MediaPipe LLM (Prototype — Phase 0-1)
Use MediaPipe LLM as the **fastest path to a working proof-of-concept**:
1. Install AI Edge Gallery on S24U
2. Load Gemma 4 E2B (or Gemma 3n E2B)
3. Test inference speed, memory usage, GPU behavior
4. Validate that the hardware can actually run the model
5. **This costs zero code** — just install the app

### Track B: LiteRT-LM (Production — Phase 2+)
Build the actual Project Claw engine on LiteRT-LM:
1. Use the Kotlin API first (equally simple to MediaPipe)
2. Add the Ktor server, OpenAI compatibility layer
3. Graduate to C++/JNI only if Kotlin API hits performance ceilings

### Why This Works
- Track A gives you **immediate hardware validation** tonight
- Track B gives you **production-grade architecture** for the long term
- The model file format (.litertlm) is the SAME for both
- Knowledge from Track A directly transfers to Track B

---

## Immediate Action: Install AI Edge Gallery

The fastest way to validate everything is to install Google's own test app:

```bash
# Clone the AI Edge Gallery
git clone https://github.com/google-ai-edge/gallery.git

# OR download the APK directly from GitHub releases
# and sideload to S24U
```

This app lets you:
- Download Gemma models directly on-device
- Run inference with GPU acceleration
- See real-time benchmarks (TTFT, decode speed)
- Test multimodal (vision + text)
- Import custom .litertlm models

**This is your Phase 0 hardware validation in a single app install.**

---

## Updated Recommendation

```
Phase 0: Install AI Edge Gallery → validate hardware → zero code
Phase 1: Build with LiteRT-LM Kotlin API → equally simple as MediaPipe
Phase 2: Add Ktor server + OpenAI layer
Phase 3: (Optional) Graduate to C++/JNI for peak performance
```

The MediaPipe LLM API and LiteRT-LM Kotlin API are **nearly identical in complexity**. You lose nothing by going straight to LiteRT-LM, and you gain constrained decoding, session cloning, C++ escape hatch, and long-term support.
