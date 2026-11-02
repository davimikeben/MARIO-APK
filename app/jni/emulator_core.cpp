#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstring>

#define TAG "N64Emu"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" {

// Ponteiros para a classe Java e callbacks (a serem inicializados em JNI_OnLoad)
static JavaVM* g_jvm = nullptr;
static jclass g_coreCls = nullptr;
static jmethodID g_midFrame = nullptr;
static jmethodID g_midState = nullptr;
static jmethodID g_midError = nullptr;
static jmethodID g_midAudio = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    g_coreCls = env->FindClass("com/davimikeben/marioapk/N64EmulatorCore");
    if (g_coreCls) {
        g_midFrame = env->GetStaticMethodID(g_coreCls, "onNativeFrameRendered", "()V");
        g_midState   = env->GetStaticMethodID(g_coreCls, "onNativeStateChanged", "(I)V");
        g_midError   = env->GetStaticMethodID(g_coreCls, "onNativeError", "(Ljava/lang/String;)V");
        g_midAudio   = env->GetStaticMethodID(g_coreCls, "onNativeAudio", "([BIII)V");
    }
    LOGI("Núcleo n64emu carregado (stub).");
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeInit(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    LOGI("nativeInit");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeLoadRom(JNIEnv* env, jclass clazz, jstring path) {
    (void)env; (void)clazz;
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    LOGI("nativeLoadRom: %s", cpath ? cpath : "(null)");
    if (cpath) env->ReleaseStringUTFChars(path, cpath);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeStart(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    LOGI("nativeStart");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativePause(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz; LOGI("nativePause");
}
JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeResume(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz; LOGI("nativeResume");
}
JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeReset(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz; LOGI("nativeReset");
}
JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeShutdown(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz; LOGI("nativeShutdown");
}
JNIEXPORT jint JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeGetState(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz; return 3; // READY
}
JNIEXPORT jstring JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeGetLastError(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return env->NewStringUTF("");
}
JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeInput(JNIEnv* env, jclass clazz, jint key, jboolean pressed) {
    (void)env; (void)clazz; LOGI("nativeInput: key=%d pressed=%d", (int)key, (int)pressed);
}
JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeAnalog(JNIEnv* env, jclass clazz, jfloat x, jfloat y) {
    (void)env; (void)clazz; LOGI("nativeAnalog: %f %f", x, y);
}
JNIEXPORT void JNICALL
Java_com_davimikeben_marioapk_N64EmulatorCore_nativeSetListener(JNIEnv* env, jclass clazz, jlong ptr) {
    (void)env; (void)clazz; (void)ptr;
}

} // extern "C"
