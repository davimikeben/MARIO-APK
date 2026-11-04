/*
 * libretro_dlopen.cpp — Carrega dinamicamente um core libretro (.so) via dlopen.
 *
 * v2 — Correções críticas (auditoria davimikeben):
 *  1. VINCOLA retro_set_environment/video_refresh/audio_sample/audio_sample_batch/
 *     input_poll/input_state ANTES de retro_init() — sem isso o core não se
 *     comunica com o frontend e nenhum frame/áudio é produzido (showstopper v1).
 *  2. Environment codes corrigidos: GET_VARIABLE=15 (retorna false = defaults),
 *     GET_SAVE_DIRECTORY=31 (v1 usava 15 incorretamente p/ save dir).
 *  3. SET_PIXEL_FORMAT: aceita XRGB8888/RGB565; recusa 0RGB1555 com conversão
 *     por software no renderer.
 *  4. SET_HW_RENDER (41): suporte a hardware rendering (GLES3) com
 *     get_current_framebuffer/get_proc_address/context_reset — necessário para
 *     mupen64plus_next_gles3 produzir imagem.
 *  5. GET_LOG_INTERFACE (27): logs do core vão para logcat (debug).
 *  6. Input analógico em array separado (não colide mais com o botão B).
 */
#include "libretro_dlopen.h"
#include <dlfcn.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/log.h>
#include <cstring>
#include <cstdio>
#include <fstream>
#include <vector>
#include <chrono>
#include <mutex>

#define LOG_TAG "libretro_dlopen"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ====== libretro.h subset (valores oficiais verificados) ======
#define RETRO_DEVICE_NONE      0
#define RETRO_DEVICE_JOYPAD    1
#define RETRO_DEVICE_ANALOG    5

#define RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY   9
#define RETRO_ENVIRONMENT_SET_PIXEL_FORMAT       10
#define RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS  11
#define RETRO_ENVIRONMENT_GET_VARIABLE           15
#define RETRO_ENVIRONMENT_SET_VARIABLES          16
#define RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE    17
#define RETRO_ENVIRONMENT_GET_LOG_INTERFACE      27
#define RETRO_ENVIRONMENT_GET_PERF_INTERFACE     28
#define RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY     31
#define RETRO_ENVIRONMENT_SET_HW_RENDER          41
#define RETRO_ENVIRONMENT_GET_INPUT_BITMASKS     51

#define RETRO_PIXEL_FORMAT_0RGB1555 0
#define RETRO_PIXEL_FORMAT_XRGB8888 1
#define RETRO_PIXEL_FORMAT_RGB565   2

#define RETRO_HW_CONTEXT_NONE        0
#define RETRO_HW_CONTEXT_OPENGL      1
#define RETRO_HW_CONTEXT_OPENGLES2   2
#define RETRO_HW_CONTEXT_OPENGL_CORE 3
#define RETRO_HW_CONTEXT_OPENGLES3   4
#define RETRO_HW_CONTEXT_VULKAN      5

// Forward declarations dos callbacks estáticos (usados antes da definição)
typedef void* retro_proc_address_t;
typedef uintptr_t (*retro_hw_get_current_framebuffer_t)(void);
typedef retro_proc_address_t (*retro_hw_get_proc_address_t)(const char*);
typedef void (*retro_hw_context_reset_t)(void);
typedef void (*retro_hw_context_destroy_t)(void);

static bool     core_environment(unsigned cmd, void* data);
static void     core_video_refresh(const void* data, unsigned w, unsigned h, size_t pitch);
static void     core_audio_sample(int16_t l, int16_t r);
static size_t   core_audio_batch(const int16_t* data, size_t frames);
static void     core_input_poll(void);
static int16_t  core_input_state(unsigned port, unsigned device, unsigned index, unsigned id);
static uintptr_t core_hw_get_current_framebuffer(void);
static retro_proc_address_t core_hw_get_proc_address(const char* name);
static void     core_hw_context_reset(void);
static void     core_hw_context_destroy(void);

// Log callback com assinatura libretro exata (sem varargs no ponteiro)
static void core_log_print(void* userdata, unsigned level, const char* fmt, ...) {
    (void)userdata;
    char buf[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    size_t n = strlen(buf);
    while (n > 0 && (buf[n-1] == '\n' || buf[n-1] == '\r')) buf[--n] = 0;
    __android_log_print(level > 2 ? ANDROID_LOG_ERROR : ANDROID_LOG_INFO, "libretro_core", "%s", buf);
}

typedef void* retro_proc_address_t;
typedef uintptr_t (*retro_hw_get_current_framebuffer_t)(void);
typedef retro_proc_address_t (*retro_hw_get_proc_address_t)(const char*);
typedef void (*retro_hw_context_reset_t)(void);
typedef void (*retro_hw_context_destroy_t)(void);

struct retro_system_info {
    const char *library_name;
    const char *library_version;
    const char *valid_extensions;
    bool need_fullpath;
    bool block_extract;
};

struct retro_game_info {
    const char *path;
    const void *data;
    size_t size;
    const char *meta;
};

struct retro_variable { const char* key; const char* value; };
typedef void (*retro_log_print_t)(void* userdata, unsigned level, const char* fmt, ...);
struct retro_log_printf { retro_log_print_t log; void* userdata; };

// Layout EXATO de libretro.h — ordem dos campos importa!
struct retro_hw_render_callback {
    retro_hw_get_current_framebuffer_t get_current_framebuffer;
    retro_hw_get_proc_address_t get_proc_address;
    bool depth;
    bool stencil;
    bool bottom_left_origin;
    unsigned context_type;
    bool cache_context;
    retro_hw_context_reset_t context_reset;
    void* context_userdata;
    retro_hw_context_destroy_t context_destroy;
    bool debug_context;
};

struct retro_system_av_info {
    struct {
        unsigned base_width;
        unsigned base_height;
        unsigned max_width;
        unsigned max_height;
        float aspect_ratio;
    } geometry;
    struct {
        double fps;
        double sample_rate;
    } timing;
};

typedef void    (*retro_init_t)(void);
typedef void    (*retro_deinit_t)(void);
typedef unsigned (*retro_api_version_t)(void);
typedef void    (*retro_get_system_info_t)(retro_system_info*);
typedef void    (*retro_get_system_av_info_t)(retro_system_av_info*);
typedef void    (*retro_set_controller_port_device_t)(unsigned, unsigned);
typedef void    (*retro_reset_t)(void);
typedef void    (*retro_run_t)(void);
typedef size_t  (*retro_serialize_size_t)(void);
typedef bool    (*retro_serialize_t)(void*, size_t);
typedef bool    (*retro_unserialize_t)(const void*, size_t);
typedef bool    (*retro_load_game_t)(const retro_game_info*);
typedef void    (*retro_unload_game_t)(void);
typedef unsigned (*retro_get_region_t)(void);
typedef void    (*retro_set_environment_t)(LibretroDlopen::EnvironmentCb);
typedef void    (*retro_set_video_refresh_t)(LibretroDlopen::VideoCb);
typedef void    (*retro_set_audio_sample_t)(LibretroDlopen::AudioSampleCb);
typedef void    (*retro_set_audio_sample_batch_t)(LibretroDlopen::AudioBatchCb);
typedef void    (*retro_set_input_poll_t)(LibretroDlopen::InputPollFn);
typedef void    (*retro_set_input_state_t)(LibretroDlopen::InputFn);

struct CoreSymbols {
    void* handle = nullptr;
    retro_init_t init = nullptr;
    retro_deinit_t deinit = nullptr;
    retro_api_version_t api_version = nullptr;
    retro_get_system_info_t get_system_info = nullptr;
    retro_get_system_av_info_t get_system_av_info = nullptr;
    retro_set_controller_port_device_t set_port_device = nullptr;
    retro_reset_t reset = nullptr;
    retro_run_t run = nullptr;
    retro_serialize_size_t serialize_size = nullptr;
    retro_serialize_t serialize = nullptr;
    retro_unserialize_t unserialize = nullptr;
    retro_load_game_t load_game = nullptr;
    retro_unload_game_t unload_game = nullptr;
    retro_get_region_t get_region = nullptr;
    retro_set_environment_t set_environment = nullptr;
    retro_set_video_refresh_t set_video_refresh = nullptr;
    retro_set_audio_sample_t set_audio_sample = nullptr;
    retro_set_audio_sample_batch_t set_audio_sample_batch = nullptr;
    retro_set_input_poll_t set_input_poll = nullptr;
    retro_set_input_state_t set_input_state = nullptr;
};

// Estado de input: joypad[4][32] + analógico separado anal[4][2 sticks][2 eixos]
struct InputCache {
    std::mutex m;
    int16_t joypad[4][32]{};
    int16_t anal[4][2][2]{}; // [port][stick][axis]  stick 0=left
};

// Singleton para os thunks estáticos (o core recebe ponteiros de função C puros)
static LibretroDlopen::Impl* g_self = nullptr;

struct EnvContext {
    std::string systemDir;
    std::string saveDir;
    std::string corePath;
    retro_system_av_info avInfo{};
    int pixelFormat = -1;       // -1 = core não anunciou ainda
    bool hwRenderActive = false;
    unsigned hwContextType = 0;
    retro_hw_render_callback hwCb{};
    LibretroDlopen::VideoCb videoCb = nullptr;
    LibretroDlopen::AudioSampleCb audioSampleCb = nullptr;
    LibretroDlopen::AudioBatchCb audioBatchCb = nullptr;
    LibretroDlopen::InputFn inputCb = nullptr;
    LibretroDlopen::FrameTimeCb frameTimeCb = nullptr;
    bool avInfoCaptured = false;
    std::mutex avMutex;
};

struct LibretroDlopen::Impl {
    CoreSymbols sym;
    InputCache input;
    EnvContext env;
    std::string coreName, coreVersion;
    std::string lastError;
    std::vector<uint8_t> stateBuffer;
    std::vector<uint8_t> romBuffer; // bytes da ROM quando need_fullpath=0
};

LibretroDlopen::LibretroDlopen() : pImpl(new Impl) { g_self = pImpl.get(); }
LibretroDlopen::~LibretroDlopen() {
    // Garante que callbacks disparados durante deinit/unload usem ESTA instância
    // (corrige dangling de g_self quando múltiplas instâncias coexistem).
    g_self = pImpl.get();
    if (pImpl->sym.handle) {
        if (pImpl->sym.unload_game) pImpl->sym.unload_game();
        if (pImpl->sym.deinit) pImpl->sym.deinit();
        dlclose(pImpl->sym.handle);
    }
    if (g_self == pImpl.get()) g_self = nullptr;
}

static bool resolveSym(void* h, const char* name, void** out) {
    *out = dlsym(h, name);
    if (!*out) {
        LOGE("dlsym %s falhou: %s", name, dlerror());
        return false;
    }
    return true;
}

static bool resolveAll(void* h, CoreSymbols& s) {
    bool ok = true;
    ok &= resolveSym(h, "retro_init", (void**)&s.init);
    ok &= resolveSym(h, "retro_deinit", (void**)&s.deinit);
    ok &= resolveSym(h, "retro_api_version", (void**)&s.api_version);
    ok &= resolveSym(h, "retro_get_system_info", (void**)&s.get_system_info);
    ok &= resolveSym(h, "retro_get_system_av_info", (void**)&s.get_system_av_info);
    ok &= resolveSym(h, "retro_set_controller_port_device", (void**)&s.set_port_device);
    ok &= resolveSym(h, "retro_reset", (void**)&s.reset);
    ok &= resolveSym(h, "retro_run", (void**)&s.run);
    ok &= resolveSym(h, "retro_serialize_size", (void**)&s.serialize_size);
    ok &= resolveSym(h, "retro_serialize", (void**)&s.serialize);
    ok &= resolveSym(h, "retro_unserialize", (void**)&s.unserialize);
    ok &= resolveSym(h, "retro_load_game", (void**)&s.load_game);
    ok &= resolveSym(h, "retro_unload_game", (void**)&s.unload_game);
    ok &= resolveSym(h, "retro_get_region", (void**)&s.get_region);
    ok &= resolveSym(h, "retro_set_environment", (void**)&s.set_environment);
    ok &= resolveSym(h, "retro_set_video_refresh", (void**)&s.set_video_refresh);
    ok &= resolveSym(h, "retro_set_audio_sample", (void**)&s.set_audio_sample);
    ok &= resolveSym(h, "retro_set_audio_sample_batch", (void**)&s.set_audio_sample_batch);
    ok &= resolveSym(h, "retro_set_input_poll", (void**)&s.set_input_poll);
    ok &= resolveSym(h, "retro_set_input_state", (void**)&s.set_input_state);
    return ok;
}

// ====== Callbacks que o core invocará ======

static bool core_environment(unsigned cmd, void* data) {
    LibretroDlopen::Impl& P = *g_self;
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            if (data) *(const char**)data = P.env.systemDir.c_str();
            return true;

        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:  // 31 (corrigido)
            if (data) *(const char**)data = P.env.saveDir.c_str();
            return true;

        case RETRO_ENVIRONMENT_GET_VARIABLE: {      // 15 — usa DEFAULTS do core
            if (!data) return false;
            retro_variable* var = (retro_variable*)data;
            LOGI("GET_VARIABLE: %s (usando default do core)", var->key ? var->key : "(null)");
            // v1.2.0 CORREÇÃO CRÍTICA: NÃO forçar angrylion/cxd4.
            // O core gles3 usa GLideN64 (default) com HW render — forçar angrylion
            // (plugin SW) quebra a renderização e pode nem existir nesta variante.
            return false; // responde false → core aplica seus defaults
        }

        case RETRO_ENVIRONMENT_SET_VARIABLES: {     // 16 — loga opções oferecidas
            if (data) {
                const struct retro_variable* vars = (const struct retro_variable*)data;
                for (int i = 0; vars[i].key; ++i) {
                    LOGI("CORE OPTION: %s = %s", vars[i].key,
                         vars[i].value ? vars[i].value : "");
                }
            }
            return true;
        }

        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: // 17
            if (data) *(bool*)data = false;
            return true;

        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {  // 10
            if (!data) return false;
            int fmt = *(const int*)data;
            if (fmt == RETRO_PIXEL_FORMAT_XRGB8888 || fmt == RETRO_PIXEL_FORMAT_RGB565) {
                P.env.pixelFormat = fmt;
                LOGI("SET_PIXEL_FORMAT: %s", fmt == 1 ? "XRGB8888" : "RGB565");
                return true;
            }
            // 0RGB1555: recusamos para que o core negocie outro formato.
            LOGI("SET_PIXEL_FORMAT 0RGB1555 recusado (negociando XRGB8888/RGB565)");
            return false;
        }

        case RETRO_ENVIRONMENT_SET_HW_RENDER: {     // 41 — HW rendering
            if (!data) return false;
            retro_hw_render_callback* cb = (retro_hw_render_callback*)data;
            if (cb->context_type == RETRO_HW_CONTEXT_OPENGLES3 ||
                cb->context_type == RETRO_HW_CONTEXT_OPENGLES2 ||
                cb->context_type == RETRO_HW_CONTEXT_OPENGL) {
                // Frontend completa os callbacks que o core precisa:
                cb->get_current_framebuffer = core_hw_get_current_framebuffer;
                cb->get_proc_address        = core_hw_get_proc_address;
                cb->context_reset           = core_hw_context_reset;
                cb->context_destroy         = core_hw_context_destroy;
                cb->debug_context           = false;
                P.env.hwCb = *cb;
                P.env.hwContextType = cb->context_type;
                P.env.hwRenderActive = true;
                LOGI("SET_HW_RENDER aceito: context_type=%u depth=%d stencil=%d bottom_left_origin=%d",
                     cb->context_type, cb->depth, cb->stencil, cb->bottom_left_origin);
                return true;
            }
            LOGI("SET_HW_RENDER recusado: context_type=%u não suportado", cb->context_type);
            return false;
        }

        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: { // 27 — logs do core no logcat
            if (data) {
                auto* logCb = (retro_log_printf*)data;
                logCb->log = &LibretroDlopen::coreLogThunk;
                logCb->userdata = nullptr;
            }
            return true;
        }

        case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:  // 51 — não suportado
            return false;

        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS: // 11 — informativo
            return true;

        default:
            return false;
    }
}

// ====== Thunks estáticos chamados pelo core (delegam p/ g_self) ======
bool LibretroDlopen::environmentThunk(unsigned cmd, void* data) {
    return core_environment(cmd, data);
}
void LibretroDlopen::videoThunk(const void* d, unsigned w, unsigned h, size_t p) {
    core_video_refresh(d, w, h, p);
}
void LibretroDlopen::audioSampleThunk(int16_t l, int16_t r) {
    core_audio_sample(l, r);
}
size_t LibretroDlopen::audioBatchThunk(const int16_t* d, size_t f) {
    return core_audio_batch(d, f);
}
void LibretroDlopen::inputPollThunk(void) {
    core_input_poll();
}
int16_t LibretroDlopen::inputStateThunk(unsigned port, unsigned dev, unsigned idx, unsigned id) {
    return core_input_state(port, dev, idx, id);
}
void LibretroDlopen::coreLogThunk(void* ud, unsigned level, const char* fmt, ...) {
    (void)ud;
    char buf[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);
    __android_log_print(ANDROID_LOG_INFO, "libretro_core", "%s", buf);
}

static void core_video_refresh(const void* data, unsigned w, unsigned h, size_t pitch) {
    LibretroDlopen::Impl& P = *g_self;
    if (P.env.videoCb) P.env.videoCb(data, w, h, pitch);
}

static void core_audio_sample(int16_t l, int16_t r) {
    LibretroDlopen::Impl& P = *g_self;
    if (P.env.audioSampleCb) P.env.audioSampleCb(l, r);
}

static size_t core_audio_batch(const int16_t* data, size_t frames) {
    LibretroDlopen::Impl& P = *g_self;
    if (P.env.audioBatchCb) return P.env.audioBatchCb(data, frames);
    return frames;
}

static void core_input_poll(void) {
    // No-op — polling é push-based pelo app.
}

static uintptr_t core_hw_get_current_framebuffer(void) {
    // Renderizamos no framebuffer default (0) do EGL window surface.
    return 0;
}

static retro_proc_address_t core_hw_get_proc_address(const char* name) {
    // Usa eglGetProcAddress para extensões/entry points GL que o core precise.
    return (retro_proc_address_t)eglGetProcAddress(name);
}

static void core_hw_context_reset(void) {
    LOGI("HW context reset (frontend recriou o contexto GL)");
}

static void core_hw_context_destroy(void) {
    LOGI("HW context destroy");
}

static int16_t core_input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    LibretroDlopen::Impl& P = *g_self;
    std::lock_guard<std::mutex> lk(P.input.m);
    if (port >= 4) return 0;
    if (device == RETRO_DEVICE_JOYPAD && id < 32) {
        return P.input.joypad[port][id];
    }
    if (device == RETRO_DEVICE_ANALOG) {
        unsigned stick = (index == 1) ? 1 : 0; // 0=left, 1=right
        unsigned axis  = (id == 1) ? 1 : 0;    // 0=X, 1=Y
        return P.input.anal[port][stick][axis];
    }
    return 0;
}

// ====== API pública ======

int LibretroDlopen::init(const char* corePath, const char* sysDir, const char* saveDir,
        EnvironmentCb envCb, VideoCb videoCb, AudioSampleCb asc, AudioBatchCb abc, InputFn icb,
        FrameTimeCb ftc) {
    (void)envCb; (void)ftc;
    pImpl->env.systemDir = sysDir;
    pImpl->env.saveDir = saveDir;
    pImpl->env.corePath = corePath;
    // Guarda callbacks para repassar aos trampolins
    pImpl->env.videoCb = videoCb;
    pImpl->env.audioSampleCb = asc;
    pImpl->env.audioBatchCb = abc;
    pImpl->env.inputCb = icb;

    pImpl->sym.handle = dlopen(corePath, RTLD_NOW | RTLD_LOCAL);
    if (!pImpl->sym.handle) {
        const char* err = dlerror();
        pImpl->lastError = std::string("dlopen falhou: ") + (err ? err : "erro desconhecido");
        LOGE("dlopen(%s) falhou: %s", corePath, pImpl->lastError.c_str());
        return -1;
    }
    if (!resolveAll(pImpl->sym.handle, pImpl->sym)) {
        pImpl->lastError = "dlsym falhou: core nao expõe todos os simbolos retro_*";
        return -2;
    }

    retro_system_info info{};
    pImpl->sym.get_system_info(&info);
    pImpl->coreName = info.library_name ? info.library_name : "libretro";
    pImpl->coreVersion = info.library_version ? info.library_version : "0.0";
    LOGI("Core: %s v%s (ext=%s need_fullpath=%d block_extract=%d)",
         pImpl->coreName.c_str(), pImpl->coreVersion.c_str(),
         info.valid_extensions ? info.valid_extensions : "",
         info.need_fullpath, info.block_extract);

    // *** CORREÇÃO CRÍTICA v2: vincula TODOS os callbacks antes de retro_init ***
    pImpl->sym.set_environment(&LibretroDlopen::environmentThunk);
    pImpl->sym.set_video_refresh(&LibretroDlopen::videoThunk);
    pImpl->sym.set_audio_sample(&LibretroDlopen::audioSampleThunk);
    pImpl->sym.set_audio_sample_batch(&LibretroDlopen::audioBatchThunk);
    pImpl->sym.set_input_poll(&LibretroDlopen::inputPollThunk);
    pImpl->sym.set_input_state(&LibretroDlopen::inputStateThunk);

    pImpl->sym.init();
    if (pImpl->sym.set_port_device) {
        pImpl->sym.set_port_device(0, RETRO_DEVICE_JOYPAD);
    }
    return 0;
}

int LibretroDlopen::loadRom(const char* romPath) {
    if (!pImpl->sym.handle || !pImpl->sym.load_game) return -1;

    // Sempre lê a ROM em memória e preenche data+size.
    // Cores com need_fullpath=0 (como mupen64plus_next) EXIGEM os bytes;
    // cores com need_fullpath=1 também aceitam data preenchido.
    retro_system_info info{};
    pImpl->sym.get_system_info(&info);
    bool needFullpath = info.need_fullpath;

    retro_game_info gi{};
    gi.path = romPath;

    if (!needFullpath) {
        // Lê a ROM inteira em memória (8 MiB para SM64 — ok)
        std::ifstream f(romPath, std::ios::binary | std::ios::ate);
        if (!f) {
            LOGE("loadRom: não abriu %s", romPath);
            return -3;
        }
        std::streamsize sz = f.tellg();
        f.seekg(0);
        pImpl->romBuffer.resize((size_t)sz);
        f.read((char*)pImpl->romBuffer.data(), sz);
        f.close();
        gi.data = pImpl->romBuffer.data();
        gi.size = (size_t)sz;
        LOGI("loadRom: %s lido em memória (%zu bytes)", romPath, (size_t)sz);
    }

    if (!pImpl->sym.load_game(&gi)) {
        LOGE("retro_load_game falhou para %s", romPath);
        return -2;
    }
    if (pImpl->sym.get_system_av_info) {
        std::lock_guard<std::mutex> lk(pImpl->env.avMutex);
        pImpl->sym.get_system_av_info(&pImpl->env.avInfo);
        pImpl->env.avInfoCaptured = true;
        LOGI("AV: %ux%u (max %ux%u) fps=%.2f rate=%.1f",
             pImpl->env.avInfo.geometry.base_width,
             pImpl->env.avInfo.geometry.base_height,
             pImpl->env.avInfo.geometry.max_width,
             pImpl->env.avInfo.geometry.max_height,
             pImpl->env.avInfo.timing.fps,
             pImpl->env.avInfo.timing.sample_rate);
    }
    return 0;
}

void LibretroDlopen::run() {
    if (pImpl->sym.run) pImpl->sym.run();
}

void LibretroDlopen::reset() { if (pImpl->sym.reset) pImpl->sym.reset(); }
void LibretroDlopen::setPaused(bool p) { (void)p; }

void LibretroDlopen::setInput(int port, int device, int button, int value) {
    if (port < 0 || port >= 4 || button < 0 || button >= 32) return;
    std::lock_guard<std::mutex> lk(pImpl->input.m);
    if (device == RETRO_DEVICE_ANALOG) {
        unsigned stick = (button / 2) & 1;
        unsigned axis  = button & 1;
        pImpl->input.anal[port][stick][axis] = (int16_t)value;
    } else {
        pImpl->input.joypad[port][button] = (int16_t)value;
    }
}

void LibretroDlopen::setInputState(int port, int device, int index, int id, int value) {
    if (port < 0 || port >= 4) return;
    std::lock_guard<std::mutex> lk(pImpl->input.m);
    if (device == RETRO_DEVICE_ANALOG) {
        unsigned stick = (index == 1) ? 1 : 0;
        unsigned axis  = (id == 1) ? 1 : 0;
        pImpl->input.anal[port][stick][axis] = (int16_t)value;
    } else if (id < 32) {
        pImpl->input.joypad[port][id] = (int16_t)value;
    }
}

int16_t LibretroDlopen::pollInput(unsigned port, unsigned device, unsigned index, unsigned id) {
    std::lock_guard<std::mutex> lk(pImpl->input.m);
    if (port >= 4) return 0;
    if (device == RETRO_DEVICE_ANALOG) {
        unsigned stick = (index == 1) ? 1 : 0;
        unsigned axis  = (id == 1) ? 1 : 0;
        return pImpl->input.anal[port][stick][axis];
    }
    if (id < 32) return pImpl->input.joypad[port][id];
    return 0;
}

unsigned LibretroDlopen::videoWidth() const  {
    std::lock_guard<std::mutex> lk(pImpl->env.avMutex);
    return pImpl->env.avInfo.geometry.base_width;
}
unsigned LibretroDlopen::videoHeight() const {
    std::lock_guard<std::mutex> lk(pImpl->env.avMutex);
    return pImpl->env.avInfo.geometry.base_height;
}
int LibretroDlopen::pixelFormat() const { return pImpl->env.pixelFormat; }
bool LibretroDlopen::isHwRenderActive() const { return pImpl->env.hwRenderActive; }
const char* LibretroDlopen::coreName() const { return pImpl->coreName.c_str(); }
const char* LibretroDlopen::coreVersion() const { return pImpl->coreVersion.c_str(); }
int LibretroDlopen::region() const { return pImpl->sym.get_region ? (int)pImpl->sym.get_region() : 0; }
const char* LibretroDlopen::lastError() const { return pImpl->lastError.c_str(); }

int LibretroDlopen::saveState(int slot) {
    if (!pImpl->sym.serialize_size || !pImpl->sym.serialize) return -1;
    size_t n = pImpl->sym.serialize_size();
    pImpl->stateBuffer.resize(n);
    if (!pImpl->sym.serialize(pImpl->stateBuffer.data(), n)) return -2;
    char path[512];
    snprintf(path, sizeof(path), "%s/sm64_slot%d.state", pImpl->env.saveDir.c_str(), slot);
    std::ofstream f(path, std::ios::binary);
    if (!f) return -3;
    f.write((const char*)pImpl->stateBuffer.data(), n);
    return 0;
}

int LibretroDlopen::loadState(int slot) {
    if (!pImpl->sym.unserialize) return -1;
    char path[512];
    snprintf(path, sizeof(path), "%s/sm64_slot%d.state", pImpl->env.saveDir.c_str(), slot);
    std::ifstream f(path, std::ios::binary | std::ios::ate);
    if (!f) return -2;
    size_t n = (size_t)f.tellg();
    f.seekg(0);
    pImpl->stateBuffer.resize(n);
    f.read((char*)pImpl->stateBuffer.data(), n);
    if (!pImpl->sym.unserialize(pImpl->stateBuffer.data(), n)) return -3;
    return 0;
}
