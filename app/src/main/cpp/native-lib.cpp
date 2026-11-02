// ============================================================================
//  EXTREME N64 - Libretro native bridge
//  Author: davimikeben
//  Bridges the bundled libretro core (Mupen64Plus-Next) to a GLES3 surface,
//  provides OpenSL ES audio, joypad input and core environment services.
// ============================================================================
#include <jni.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <string>
#include <mutex>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "libretro.h"

#define LOG_TAG "ExtremeN64"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ---------------------------------------------------------------------------
// Core function pointers
// ---------------------------------------------------------------------------
typedef void   (*fn_retro_init)(void);
typedef void   (*fn_retro_deinit)(void);
typedef void   (*fn_retro_get_system_info)(struct retro_system_info *);
typedef void   (*fn_retro_get_system_av_info)(struct retro_system_av_info *);
typedef void   (*fn_retro_set_environment)(retro_environment_t);
typedef void   (*fn_retro_set_video_refresh)(retro_video_refresh_t);
typedef void   (*fn_retro_set_audio_sample)(retro_audio_sample_t);
typedef void   (*fn_retro_set_audio_sample_batch)(retro_audio_sample_batch_t);
typedef void   (*fn_retro_set_input_poll)(retro_input_poll_t);
typedef void   (*fn_retro_set_input_state)(retro_input_state_t);
typedef bool   (*fn_retro_load_game)(const struct retro_game_info *);
typedef void   (*fn_retro_unload_game)(void);
typedef void   (*fn_retro_run)(void);
typedef void   (*fn_retro_reset)(void);

static void *g_core = nullptr;
static fn_retro_init               p_init;
static fn_retro_deinit             p_deinit;
static fn_retro_get_system_info    p_sysinfo;
static fn_retro_get_system_av_info p_avinfo;
static fn_retro_set_environment    p_set_env;
static fn_retro_set_video_refresh  p_set_video;
static fn_retro_set_audio_sample   p_set_audio;
static fn_retro_set_audio_sample_batch p_set_audio_batch;
static fn_retro_set_input_poll     p_set_input_poll;
static fn_retro_set_input_state    p_set_input_state;
static fn_retro_load_game          p_load_game;
static fn_retro_unload_game        p_unload_game;
static fn_retro_run                p_run;
static fn_retro_reset              p_reset;

static bool g_game_loaded = false;
static bool g_hw_render   = false;
static std::string g_system_dir;
static std::string g_save_dir;
static std::string g_content_path;

static retro_hw_render_callback g_hw_cb;
static bool g_context_ready = false;

// geometry reported by the core (used for the software fallback blit)
static unsigned g_geom_w = 640, g_geom_h = 480;
static float    g_aspect  = 4.0f / 3.0f;

// ---------------------------------------------------------------------------
// Input
// ---------------------------------------------------------------------------
static int16_t g_buttons[16];   // port 0 joypad state
static int16_t g_axis[4];       // analog stick: x, y (index 0..1)

// ---------------------------------------------------------------------------
// Software video fallback
// ---------------------------------------------------------------------------
static uint8_t *g_frame = nullptr;
static size_t   g_frame_cap = 0;
static unsigned g_frame_w = 0, g_frame_h = 0;
static bool     g_frame_dirty = false;
static GLenum   g_pix_type = GL_UNSIGNED_SHORT_5_6_5;

static GLuint g_prog = 0, g_tex = 0, g_vbo = 0;
static GLint  g_a_pos = -1, g_a_uv = -1;
static GLint  g_u_tex = -1;

// ---------------------------------------------------------------------------
// Audio (OpenSL ES)
// ---------------------------------------------------------------------------
static const size_t RING_FRAMES = 44100 * 2;  // ~2s stereo
static int16_t g_ring[44100 * 2 * 2];
static size_t  g_ring_read = 0, g_ring_count = 0;  // in samples (stereo pairs*2)
static std::mutex g_audio_mutex;
static unsigned g_sample_rate = 44100;

static SLObjectItf g_sl_engine_obj = nullptr;
static SLEngineItf g_sl_engine = nullptr;
static SLObjectItf g_sl_mix_obj = nullptr;
static SLObjectItf g_sl_player_obj = nullptr;
static SLAndroidSimpleBufferQueueItf g_sl_bq = nullptr;
static bool g_audio_ready = false;

static const size_t  BQ_FRAMES = 1024;
static int16_t       g_bq_buf[2][1024 * 2];
static int           g_bq_idx = 0;

static void bq_callback(SLAndroidSimpleBufferQueueItf bq, void *) {
    if (!g_audio_ready) return;
    int16_t *out = g_bq_buf[g_bq_idx];
    g_bq_idx ^= 1;
    {
        std::lock_guard<std::mutex> lock(g_audio_mutex);
        for (size_t i = 0; i < BQ_FRAMES; ++i) {
            if (g_ring_count >= 2) {
                out[i * 2]     = g_ring[g_ring_read * 2];
                out[i * 2 + 1] = g_ring[g_ring_read * 2 + 1];
                g_ring_read = (g_ring_read + 1) % RING_FRAMES;
                g_ring_count -= 2;
            } else {
                out[i * 2] = 0;
                out[i * 2 + 1] = 0;
            }
        }
    }
    (*bq)->Enqueue(bq, out, BQ_FRAMES * 2 * sizeof(int16_t));
}

static void audio_start(unsigned sample_rate) {
    g_sample_rate = (sample_rate >= 8000 && sample_rate <= 192000) ? sample_rate : 44100;

    if (slCreateEngine(&g_sl_engine_obj, 0, nullptr, 0, nullptr, nullptr) != SL_RESULT_SUCCESS) {
        LOGE("slCreateEngine failed"); return;
    }
    if ((*g_sl_engine_obj)->Realize(g_sl_engine_obj, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
        LOGE("engine Realize failed"); return;
    }
    if ((*g_sl_engine_obj)->GetInterface(g_sl_engine_obj, SL_IID_ENGINE, &g_sl_engine) != SL_RESULT_SUCCESS) {
        LOGE("GetInterface ENGINE failed"); return;
    }
    if ((*g_sl_engine)->CreateOutputMix(g_sl_engine, &g_sl_mix_obj, 0, nullptr, nullptr) != SL_RESULT_SUCCESS) {
        LOGE("CreateOutputMix failed"); return;
    }
    if ((*g_sl_mix_obj)->Realize(g_sl_mix_obj, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
        LOGE("mix Realize failed"); return;
    }

    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM fmt = {
            SL_DATAFORMAT_PCM, 2, (SLuint32)(g_sample_rate * 1000),
            SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSource src = {&loc_bufq, &fmt};
    SLDataLocator_OutputMix loc_out = {SL_DATALOCATOR_OUTPUTMIX, g_sl_mix_obj};
    SLDataSink sink = {&loc_out, nullptr};

    const SLInterfaceID ids[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[] = {SL_BOOLEAN_TRUE};

    if ((*g_sl_engine)->CreateAudioPlayer(g_sl_engine, &g_sl_player_obj, &src, &sink,
                                          1, ids, req) != SL_RESULT_SUCCESS) {
        LOGE("CreateAudioPlayer failed"); return;
    }
    if ((*g_sl_player_obj)->Realize(g_sl_player_obj, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
        LOGE("player Realize failed"); return;
    }
    SLPlayItf play_itf = nullptr;
    if ((*g_sl_player_obj)->GetInterface(g_sl_player_obj, SL_IID_PLAY, &play_itf) != SL_RESULT_SUCCESS) {
        LOGE("GetInterface PLAY failed"); return;
    }
    if ((*g_sl_player_obj)->GetInterface(g_sl_player_obj, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                         &g_sl_bq) != SL_RESULT_SUCCESS) {
        LOGE("GetInterface BQ failed"); return;
    }
    (*g_sl_bq)->RegisterCallback(g_sl_bq, bq_callback, nullptr);

    g_audio_ready = true;
    // prime the queue
    memset(g_bq_buf, 0, sizeof(g_bq_buf));
    (*g_sl_bq)->Enqueue(g_sl_bq, g_bq_buf[0], BQ_FRAMES * 2 * sizeof(int16_t));
    (*g_sl_bq)->Enqueue(g_sl_bq, g_bq_buf[1], BQ_FRAMES * 2 * sizeof(int16_t));
    (*play_itf)->SetPlayState(play_itf, SL_PLAYSTATE_PLAYING);
    LOGI("OpenSL audio started @ %u Hz", g_sample_rate);
}

static void audio_stop() {
    g_audio_ready = false;
    if (g_sl_player_obj) { (*g_sl_player_obj)->Destroy(g_sl_player_obj); g_sl_player_obj = nullptr; }
    if (g_sl_mix_obj)    { (*g_sl_mix_obj)->Destroy(g_sl_mix_obj);       g_sl_mix_obj = nullptr; }
    if (g_sl_engine_obj) { (*g_sl_engine_obj)->Destroy(g_sl_engine_obj); g_sl_engine_obj = nullptr; }
    g_sl_bq = nullptr; g_sl_engine = nullptr;
}

// ---------------------------------------------------------------------------
// GL helpers
// ---------------------------------------------------------------------------
static void *gl_proc(const char *sym) {
    static void *libgl = nullptr;
    if (!libgl) libgl = dlopen("libGLESv3.so", RTLD_LAZY | RTLD_LOCAL);
    void *p = nullptr;
    if (libgl) p = dlsym(libgl, sym);
    if (!p) p = (void *)eglGetProcAddress(sym);
    return p;
}

static GLuint make_program() {
    static const char *VS =
        "#version 300 es\n"
        "in vec2 aPos;\n"
        "in vec2 aUv;\n"
        "out vec2 vUv;\n"
        "void main(){ vUv = aUv; gl_Position = vec4(aPos, 0.0, 1.0); }\n";
    static const char *FS =
        "#version 300 es\n"
        "precision mediump float;\n"
        "in vec2 vUv;\n"
        "uniform sampler2D uTex;\n"
        "out vec4 fragColor;\n"
        "void main(){ fragColor = texture(uTex, vUv); }\n";

    auto compile = [](GLenum type, const char *src) -> GLuint {
        GLuint s = glCreateShader(type);
        glShaderSource(s, 1, &src, nullptr);
        glCompileShader(s);
        GLint ok = 0; glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
        if (!ok) { char log[512]; glGetShaderInfoLog(s, 512, nullptr, log); LOGE("shader: %s", log); }
        return s;
    };
    GLuint vs = compile(GL_VERTEX_SHADER, VS);
    GLuint fs = compile(GL_FRAGMENT_SHADER, FS);
    GLuint prog = glCreateProgram();
    glAttachShader(prog, vs); glAttachShader(prog, fs); glLinkProgram(prog);
    GLint ok = 0; glGetProgramiv(prog, GL_LINK_STATUS, &ok);
    if (!ok) { char log[512]; glGetProgramInfoLog(prog, 512, nullptr, log); LOGE("link: %s", log); }
    glDeleteShader(vs); glDeleteShader(fs);
    return prog;
}

// ---------------------------------------------------------------------------
// Libretro callbacks
// ---------------------------------------------------------------------------
static void cb_log(enum retro_log_level level, const char *fmt, ...) {
    char buf[1024];
    va_list args; va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    int prio = (level == RETRO_LOG_ERROR) ? ANDROID_LOG_ERROR : ANDROID_LOG_INFO;
    __android_log_write(prio, LOG_TAG, buf);
}

static bool cb_environment(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            if (data) *(bool *)data = true;
            return true;

        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            if (!data) return false;
            auto f = *(const enum retro_pixel_format *)data;
            if (f == RETRO_PIXEL_FORMAT_RGB565)        g_pix_type = GL_UNSIGNED_SHORT_5_6_5;
            else if (f == RETRO_PIXEL_FORMAT_XRGB8888) g_pix_type = GL_UNSIGNED_BYTE;
            else                                       return false;
            return true;
        }

        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            if (data) *(const char **)data = g_system_dir.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_PLAYLIST_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_CONTENT_DIRECTORY:  // == GET_CORE_ASSETS_DIRECTORY (30)
            if (data) *(const char **)data = g_save_dir.c_str();
            return true;

        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: {
            if (!data) return false;
            auto *cb = (struct retro_log_callback *)data;
            cb->log = cb_log;
            return true;
        }

        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            if (data) *(unsigned *)data = RETRO_LANGUAGE_PORTUGUESE_BRAZIL;
            return true;

        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            if (data) *(unsigned *)data = 2;
            return true;
        case RETRO_ENVIRONMENT_GET_MESSAGE_INTERFACE_VERSION:
            if (data) *(unsigned *)data = 1;
            return true;

        case RETRO_ENVIRONMENT_GET_USERNAME:
            if (data) *(const char **)data = "davimikeben";
            return true;

        case RETRO_ENVIRONMENT_GET_INPUT_DEVICE_CAPABILITIES:
            if (data) *(uint64_t *)data = (1 << RETRO_DEVICE_JOYPAD);
            return true;

        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            if (data) *(int *)data = 3;  // video | audio
            return true;

        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
            return false;

        case RETRO_ENVIRONMENT_SET_HW_RENDER: {
            if (!data) return false;
            auto *hw = (struct retro_hw_render_callback *)data;
            if (hw->context_type != RETRO_HW_CONTEXT_OPENGLES3 &&
                hw->context_type != RETRO_HW_CONTEXT_OPENGLES2) {
                LOGW("Unsupported HW context %u", hw->context_type);
                return false;
            }
            hw->get_current_framebuffer = [](void) -> uintptr_t { return 0; };
            hw->get_proc_address        = [](const char *sym) -> retro_proc_address_t {
                return (retro_proc_address_t)gl_proc(sym);
            };
            hw->context_reset   = []() { g_context_ready = true; LOGI("HW context reset"); };
            hw->context_destroy = []() { g_context_ready = false; LOGI("HW context destroy"); };
            hw->depth = true;
            hw->stencil = false;
            hw->bottom_left_origin = true;
            hw->version_major = (hw->context_type == RETRO_HW_CONTEXT_OPENGLES3) ? 3 : 2;
            hw->version_minor = 0;
            hw->cache_context = false;
            hw->debug_context = false;
            g_hw_cb = *hw;
            g_hw_render = true;
            LOGI("HW render accepted (GLES%u)", hw->version_major);
            return true;
        }

        case RETRO_ENVIRONMENT_SET_HW_SHARED_CONTEXT:
            return true;

        case RETRO_ENVIRONMENT_SET_GEOMETRY: {
            if (data) {
                auto *g = (struct retro_game_geometry *)data;
                g_geom_w = g->base_width; g_geom_h = g->base_height;
                if (g->aspect_ratio > 0.0f) g_aspect = g->aspect_ratio;
            }
            return true;
        }
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO: {
            if (data) {
                auto *av = (struct retro_system_av_info *)data;
                g_geom_w = av->geometry.base_width; g_geom_h = av->geometry.base_height;
                if (av->geometry.aspect_ratio > 0.0f) g_aspect = av->geometry.aspect_ratio;
            }
            return true;
        }

        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
        case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE:
        case RETRO_ENVIRONMENT_SET_MEMORY_MAPS:
        case RETRO_ENVIRONMENT_SET_SUBSYSTEM_INFO:
        case RETRO_ENVIRONMENT_SET_SUPPORT_ACHIEVEMENTS:
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
        case RETRO_ENVIRONMENT_SET_SERIALIZATION_QUIRKS:
        case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
        case RETRO_ENVIRONMENT_SET_VARIABLES:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_INTL:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2_INTL:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_UPDATE_DISPLAY_CALLBACK:
        case RETRO_ENVIRONMENT_SET_MINIMUM_AUDIO_LATENCY:
            return true;

        default:
            return false;
    }
}

static void cb_video_refresh(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (!data) return;                                  // dupe frame
    if (data == RETRO_HW_FRAME_BUFFER_VALID) {          // core drew into our FBO
        g_frame_dirty = false;
        return;
    }
    if (width == 0 || height == 0) return;

    unsigned bpp = (g_pix_type == GL_UNSIGNED_SHORT_5_6_5) ? 2 : 4;
    size_t need = (size_t)width * height * bpp;
    if (g_frame_cap < need) {
        free(g_frame);
        g_frame = (uint8_t *)malloc(need);
        g_frame_cap = g_frame ? need : 0;
        if (!g_frame) return;
    }
    const uint8_t *src = (const uint8_t *)data;
    for (unsigned y = 0; y < height; ++y) {
        memcpy(g_frame + (size_t)y * width * bpp, src + y * pitch, (size_t)width * bpp);
    }
    g_frame_w = width; g_frame_h = height;
    g_geom_w = width; g_geom_h = height;
    g_frame_dirty = true;
}

static size_t cb_audio_sample_batch(const int16_t *data, size_t frames);

static void cb_audio_sample(int16_t left, int16_t right) {
    int16_t buf[2] = {left, right};
    cb_audio_sample_batch(buf, 1);
}

static size_t cb_audio_sample_batch(const int16_t *data, size_t frames) {
    if (!g_audio_ready || !data) return frames;
    std::lock_guard<std::mutex> lock(g_audio_mutex);
    for (size_t i = 0; i < frames; ++i) {
        if (g_ring_count / 2 >= RING_FRAMES) {  // drop if full
            g_ring_read = (g_ring_read + 1) % RING_FRAMES;
            g_ring_count -= 2;
        }
        size_t w = (g_ring_read + g_ring_count / 2) % RING_FRAMES;
        g_ring[w * 2]     = data[i * 2];
        g_ring[w * 2 + 1] = data[i * 2 + 1];
        g_ring_count += 2;
    }
    return frames;
}

static void cb_input_poll() {}

static int16_t cb_input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port != 0) return 0;
    if (device == RETRO_DEVICE_JOYPAD && index == 0 && id < 16) return g_buttons[id];
    if (device == RETRO_DEVICE_ANALOG) {
        if (index == RETRO_DEVICE_INDEX_ANALOG_LEFT) {
            if (id == RETRO_DEVICE_ID_ANALOG_X) return g_axis[0];
            if (id == RETRO_DEVICE_ID_ANALOG_Y) return g_axis[1];
        }
        if (index == RETRO_DEVICE_INDEX_ANALOG_RIGHT) {
            if (id == RETRO_DEVICE_ID_ANALOG_X) return g_axis[2];
            if (id == RETRO_DEVICE_ID_ANALOG_Y) return g_axis[3];
        }
    }
    return 0;
}

// ---------------------------------------------------------------------------
// JNI: lifecycle
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_nativeSetPaths(JNIEnv *env, jobject,
                                                              jstring system_dir,
                                                              jstring save_dir) {
    const char *s = env->GetStringUTFChars(system_dir, nullptr);
    const char *v = env->GetStringUTFChars(save_dir, nullptr);
    g_system_dir = s ? s : "";
    g_save_dir = v ? v : "";
    env->ReleaseStringUTFChars(system_dir, s);
    env->ReleaseStringUTFChars(save_dir, v);
    LOGI("Paths: system=%s save=%s", g_system_dir.c_str(), g_save_dir.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_loadCore(JNIEnv *env, jobject, jstring core_path) {
    if (g_core) { dlclose(g_core); g_core = nullptr; }

    const char *path = env->GetStringUTFChars(core_path, nullptr);
    LOGI("Loading core: %s", path ? path : "(null)");
    g_core = path ? dlopen(path, RTLD_LAZY | RTLD_LOCAL) : nullptr;
    if (!g_core) {
        // fallback: soname (library already mapped by System.loadLibrary)
        g_core = dlopen("libmupen64plus_libretro.so", RTLD_LAZY | RTLD_LOCAL);
    }
    if (!g_core) {
        LOGE("dlopen failed: %s", dlerror());
        if (path) env->ReleaseStringUTFChars(core_path, path);
        return JNI_FALSE;
    }
    if (path) env->ReleaseStringUTFChars(core_path, path);

#define BIND(name, var) var = (decltype(var))dlsym(g_core, #name); \
    if (!var) LOGW("missing symbol: %s", #name)
    BIND(retro_init, p_init);
    BIND(retro_deinit, p_deinit);
    BIND(retro_get_system_info, p_sysinfo);
    BIND(retro_get_system_av_info, p_avinfo);
    BIND(retro_set_environment, p_set_env);
    BIND(retro_set_video_refresh, p_set_video);
    BIND(retro_set_audio_sample, p_set_audio);
    BIND(retro_set_audio_sample_batch, p_set_audio_batch);
    BIND(retro_set_input_poll, p_set_input_poll);
    BIND(retro_set_input_state, p_set_input_state);
    BIND(retro_load_game, p_load_game);
    BIND(retro_unload_game, p_unload_game);
    BIND(retro_run, p_run);
    BIND(retro_reset, p_reset);
#undef BIND

    if (!p_init || !p_run || !p_load_game || !p_set_env) {
        LOGE("Core missing mandatory symbols");
        return JNI_FALSE;
    }

    p_set_env(cb_environment);
    p_init();

    if (p_set_video)       p_set_video(cb_video_refresh);
    if (p_set_audio)       p_set_audio(cb_audio_sample);
    if (p_set_audio_batch) p_set_audio_batch(cb_audio_sample_batch);
    if (p_set_input_poll)  p_set_input_poll(cb_input_poll);
    if (p_set_input_state) p_set_input_state(cb_input_state);

    if (p_sysinfo) {
        struct retro_system_info si{};
        p_sysinfo(&si);
        LOGI("Core: %s %s (need_fullpath=%d)", si.library_name ? si.library_name : "?",
             si.library_version ? si.library_version : "?", si.need_fullpath);
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_loadGame(JNIEnv *env, jobject, jstring game_path) {
    if (!p_load_game) return JNI_FALSE;
    const char *path = env->GetStringUTFChars(game_path, nullptr);
    g_content_path = path ? path : "";

    struct retro_game_info info{};
    info.path = path;
    info.data = nullptr;
    info.size = 0;
    info.meta = nullptr;

    bool ok = p_load_game(&info);
    env->ReleaseStringUTFChars(game_path, path);
    LOGI("retro_load_game(%s) -> %s", g_content_path.c_str(), ok ? "true" : "false");
    if (!ok) return JNI_FALSE;

    g_game_loaded = true;
    if (p_avinfo) {
        struct retro_system_av_info av{};
        p_avinfo(&av);
        g_geom_w = av.geometry.base_width;
        g_geom_h = av.geometry.base_height;
        if (av.geometry.aspect_ratio > 0.0f) g_aspect = av.geometry.aspect_ratio;
        LOGI("AV: %ux%u @ %.2f fps, audio %f Hz", g_geom_w, g_geom_h,
             av.timing.fps, av.timing.sample_rate);
        audio_start((unsigned)av.timing.sample_rate);
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_runFrame(JNIEnv *, jobject) {
    if (g_game_loaded && p_run) p_run();
}

extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_resetGame(JNIEnv *, jobject) {
    if (g_game_loaded && p_reset) p_reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_unloadGame(JNIEnv *, jobject) {
    audio_stop();
    if (g_game_loaded && p_unload_game) p_unload_game();
    g_game_loaded = false;
}

// ---------------------------------------------------------------------------
// JNI: input
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_setButtonState(JNIEnv *, jobject,
                                                              jint button_id, jboolean pressed) {
    if (button_id >= 0 && button_id < 16) g_buttons[button_id] = pressed ? 1 : 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_setAnalog(JNIEnv *, jobject,
                                                         jint axis_id, jfloat value) {
    if (axis_id >= 0 && axis_id < 4) {
        float v = value;
        if (v >  1.0f) v =  1.0f;
        if (v < -1.0f) v = -1.0f;
        g_axis[axis_id] = (int16_t)(v * 32767.0f);
    }
}

// ---------------------------------------------------------------------------
// JNI: rendering
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_nativeInitGL(JNIEnv *, jobject) {
    g_prog = make_program();
    g_a_pos = glGetAttribLocation(g_prog, "aPos");
    g_a_uv  = glGetAttribLocation(g_prog, "aUv");
    g_u_tex = glGetUniformLocation(g_prog, "uTex");

    glGenTextures(1, &g_tex);
    glBindTexture(GL_TEXTURE_2D, g_tex);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glGenBuffers(1, &g_vbo);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    LOGI("GLES init done (prog=%u)", g_prog);
}

extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_nativeSetViewport(JNIEnv *, jobject,
                                                                 jint w, jint h) {
    glViewport(0, 0, w, h);
    LOGI("Viewport %dx%d", w, h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_nativeRenderFrame(JNIEnv *, jobject) {
    if (g_hw_render || !g_frame_dirty || !g_frame || g_frame_w == 0) return;

    glBindTexture(GL_TEXTURE_2D, g_tex);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    if (g_pix_type == GL_UNSIGNED_BYTE) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, g_frame_w, g_frame_h, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, g_frame);
    } else {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, g_frame_w, g_frame_h, 0,
                     GL_RGB, GL_UNSIGNED_SHORT_5_6_5, g_frame);
    }
    g_frame_dirty = false;

    // full-surface quad (flip V because libretro frames are top-down)
    static const GLfloat verts[] = {
        -1.f, -1.f, 0.f, 1.f,
         1.f, -1.f, 1.f, 1.f,
        -1.f,  1.f, 0.f, 0.f,
         1.f,  1.f, 1.f, 0.f,
    };
    glUseProgram(g_prog);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, g_tex);
    glUniform1i(g_u_tex, 0);
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(verts), verts, GL_DYNAMIC_DRAW);
    glEnableVertexAttribArray(g_a_pos);
    glEnableVertexAttribArray(g_a_uv);
    glVertexAttribPointer(g_a_pos, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void *)0);
    glVertexAttribPointer(g_a_uv,  2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat),
                          (void *)(2 * sizeof(GLfloat)));
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray(g_a_pos);
    glDisableVertexAttribArray(g_a_uv);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_davimikeben_extremen64_LibretroBridge_isHardwareRendering(JNIEnv *, jobject) {
    return g_hw_render ? JNI_TRUE : JNI_FALSE;
}
