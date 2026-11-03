/*
 * sm64_jni.cpp — ponte JNI entre Kotlin (LibretroBridge) e o runtime C++.
 * Carrega dinamicamente o core libretro e repassa as chamadas.
 *
 * Projeto: davimikeben SM64 Emulator
 */
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <string>
#include <mutex>
#include "libretro_dlopen.h"
#include "gl_renderer.h"
#include "audio_sink.h"
#include "perf_pacer.h"

#define LOG_TAG "sm64_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::mutex g_mutex;
static LibretroDlopen* g_core = nullptr;
static GLRenderer*     g_renderer = nullptr;
static AudioSink*      g_audio = nullptr;
static PerfPacer*      g_pacer = nullptr;
static ANativeWindow*  g_window = nullptr;

extern "C" {

static void video_refresh(const void* data, unsigned w, unsigned h, size_t pitch) {
    if (g_renderer) g_renderer->submitFrame(data, w, h, pitch);
}
static void audio_sample(int16_t left, int16_t right) {
    if (g_audio) g_audio->pushStereo(left, right);
}
static size_t audio_batch(const int16_t* data, size_t frames) {
    if (g_audio) return g_audio->pushBatch(data, frames);
    return frames;
}
static int16_t input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    // Polling via setInput — valores cacheados em LibretroDlopen.
    return g_core ? g_core->pollInput(port, device, index, id) : 0;
}
static void environment_set_frame_time(retro_usec_t) {}
static bool environment(unsigned cmd, void* data) {
    if (!g_core) return false;
    return g_core->handleEnvironment(cmd, data,
        video_refresh, audio_sample, audio_batch, input_state,
        environment_set_frame_time);
}

JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeInit(
        JNIEnv* env, jclass, jstring corePathJ, jstring sysDirJ, jstring saveDirJ) {
    std::lock_guard<std::mutex> lk(g_mutex);
    if (g_core) return 0;
    const char* corePath = env->GetStringUTFChars(corePathJ, nullptr);
    const char* sysDir   = env->GetStringUTFChars(sysDirJ,   nullptr);
    const char* saveDir  = env->GetStringUTFChars(saveDirJ,  nullptr);
    g_core = new LibretroDlopen();
    int rc = g_core->init(corePath, sysDir, saveDir,
        environment, video_refresh, audio_sample, audio_batch, input_state,
        environment_set_frame_time);
    LOGI("nativeInit core=%s rc=%d", corePath, rc);
    env->ReleaseStringUTFChars(corePathJ, corePath);
    env->ReleaseStringUTFChars(sysDirJ,   sysDir);
    env->ReleaseStringUTFChars(saveDirJ,  saveDir);
    if (rc == 0) {
        g_audio   = new AudioSink();
        g_pacer   = new PerfPacer();
        g_renderer = new GLRenderer();
    }
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeLoadRom(
        JNIEnv* env, jclass, jstring romPathJ) {
    if (!g_core) return -1;
    const char* romPath = env->GetStringUTFChars(romPathJ, nullptr);
    int rc = g_core->loadRom(romPath);
    LOGI("nativeLoadRom path=%s rc=%d", romPath, rc);
    env->ReleaseStringUTFChars(romPathJ, romPath);
    return rc;
}

JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeRunFrame(JNIEnv*, jclass) {
    if (!g_core) return -1;
    if (g_pacer) g_pacer->beginFrame();
    g_core->run();
    if (g_pacer) g_pacer->endFrame();
    return 0;
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeReset(JNIEnv*, jclass) {
    if (g_core) g_core->reset();
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativePause(JNIEnv*, jclass, jboolean p) {
    if (g_core) g_core->setPaused(p == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeSetInput(
        JNIEnv*, jclass, jint port, jint device, jint button, jint value) {
    if (g_core) g_core->setInput(port, device, button, value);
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeSetInputState(
        JNIEnv*, jclass, jint port, jint device, jint index, jint id, jint value) {
    if (g_core) g_core->setInputState(port, device, index, id, value);
}

JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetVideoWidth(JNIEnv*, jclass) {
    return g_core ? g_core->videoWidth() : 0;
}
JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetVideoHeight(JNIEnv*, jclass) {
    return g_core ? g_core->videoHeight() : 0;
}
JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetPixelFormat(JNIEnv*, jclass) {
    return g_core ? g_core->pixelFormat() : 0;
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeDeinit(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> lk(g_mutex);
    if (g_renderer) { delete g_renderer; g_renderer = nullptr; }
    if (g_audio)    { delete g_audio;    g_audio    = nullptr; }
    if (g_pacer)    { delete g_pacer;    g_pacer    = nullptr; }
    if (g_core)     { delete g_core;     g_core     = nullptr; }
    if (g_window)   { ANativeWindow_release(g_window); g_window = nullptr; }
}

JNIEXPORT jfloat JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetLastFps(JNIEnv*, jclass) {
    return g_pacer ? g_pacer->lastFps() : 0.0f;
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeSetPerformanceMode(
        JNIEnv*, jclass, jint mode) {
    if (g_pacer) g_pacer->setMode(mode);
    if (g_renderer) g_renderer->setQualityMode(mode);
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeSetSurface(
        JNIEnv* env, jclass, jobject surface) {
    if (g_window) ANativeWindow_release(g_window);
    g_window = surface ? ANativeWindow_fromSurface(env, surface) : nullptr;
    if (g_renderer && g_window) g_renderer->setWindow(g_window);
}

JNIEXPORT void JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeSetAudioSampleRate(
        JNIEnv*, jclass, jint rate) {
    if (g_audio) g_audio->configure(rate);
}

JNIEXPORT jstring JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetCoreName(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_core ? g_core->coreName() : "unknown");
}
JNIEXPORT jstring JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetCoreVersion(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_core ? g_core->coreVersion() : "0.0.0");
}

JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeSaveState(JNIEnv*, jclass, jint slot) {
    return g_core ? g_core->saveState(slot) : -1;
}
JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeLoadState(JNIEnv*, jclass, jint slot) {
    return g_core ? g_core->loadState(slot) : -1;
}
JNIEXPORT jint JNICALL
Java_com_davimikeben_sm64emulator_core_LibretroBridge_nativeGetRegion(JNIEnv*, jclass) {
    return g_core ? g_core->region() : 0;
}

} // extern "C"
