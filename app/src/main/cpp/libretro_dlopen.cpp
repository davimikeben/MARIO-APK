/*
 * libretro_dlopen.cpp — Carrega dinamicamente um core libretro (.so) via dlopen.
 *
 * Esta camada permite que o APK use qualquer core libretro compatível (mupen64plus_next,
 * parallel_n64) sem precisar linká-lo em tempo de compilação. O core é empacotado em
 * jniLibs/<abi>/libretro_<nome>.so pelo script scripts/fetch_libretro_core.sh.
 */
#include "libretro_dlopen.h"
#include <dlfcn.h>
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

// ====== libretro.h subset ======
#define RETRO_API_VERSION 1
#define RETRO_DEVICE_JOYPAD 1

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
typedef void    (*retro_cheat_reset_t)(void);
typedef void    (*retro_cheat_set_t)(unsigned, bool, const char*);
typedef bool    (*retro_load_game_t)(const retro_game_info*);
typedef void    (*retro_unload_game_t)(void);
typedef unsigned (*retro_get_region_t)(void);
typedef void*   (*retro_get_memory_data_t)(unsigned);
typedef size_t  (*retro_get_memory_size_t)(unsigned);

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
};

struct InputCache {
    std::mutex m;
    int16_t states[4][32]{};
};

struct EnvContext {
    std::string systemDir;
    std::string saveDir;
    std::string corePath;
    retro_system_av_info avInfo{};
    int pixelFormat = 0;
};

struct LibretroDlopen::Impl {
    CoreSymbols sym;
    InputCache input;
    EnvContext env;
    std::string coreName, coreVersion;
    std::vector<uint8_t> stateBuffer;
};

LibretroDlopen::LibretroDlopen() : pImpl(new Impl) {}
LibretroDlopen::~LibretroDlopen() {
    if (pImpl->sym.handle) {
        if (pImpl->sym.unload_game) pImpl->sym.unload_game();
        if (pImpl->sym.deinit) pImpl->sym.deinit();
        dlclose(pImpl->sym.handle);
    }
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
    return ok;
}

int LibretroDlopen::init(const char* corePath, const char* sysDir, const char* saveDir,
        EnvironmentCb, VideoCb, AudioSampleCb, AudioBatchCb, InputCb, FrameTimeCb) {
    pImpl->env.systemDir = sysDir;
    pImpl->env.saveDir = saveDir;
    pImpl->env.corePath = corePath;

    pImpl->sym.handle = dlopen(corePath, RTLD_NOW | RTLD_LOCAL);
    if (!pImpl->sym.handle) { LOGE("dlopen(%s) falhou: %s", corePath, dlerror()); return -1; }
    if (!resolveAll(pImpl->sym.handle, pImpl->sym)) return -2;

    retro_system_info info{};
    pImpl->sym.get_system_info(&info);
    pImpl->coreName = info.library_name ? info.library_name : "libretro";
    pImpl->coreVersion = info.library_version ? info.library_version : "0.0";
    LOGI("Core: %s v%s (ext=%s need_fullpath=%d block_extract=%d)",
         pImpl->coreName.c_str(), pImpl->coreVersion.c_str(),
         info.valid_extensions ? info.valid_extensions : "",
         info.need_fullpath, info.block_extract);

    pImpl->sym.init();
    if (pImpl->sym.set_port_device) {
        pImpl->sym.set_port_device(0, RETRO_DEVICE_JOYPAD);
    }
    return 0;
}

int LibretroDlopen::loadRom(const char* romPath) {
    if (!pImpl->sym.handle || !pImpl->sym.load_game) return -1;
    retro_game_info gi{};
    gi.path = romPath;
    if (!pImpl->sym.load_game(&gi)) {
        LOGE("retro_load_game falhou para %s", romPath);
        return -2;
    }
    if (pImpl->sym.get_system_av_info) {
        pImpl->sym.get_system_av_info(&pImpl->env.avInfo);
    }
    return 0;
}

void LibretroDlopen::run() {
    if (pImpl->sym.run) pImpl->sym.run();
}

void LibretroDlopen::reset() { if (pImpl->sym.reset) pImpl->sym.reset(); }
void LibretroDlopen::setPaused(bool p) { (void)p; }

void LibretroDlopen::setInput(int port, int device, int button, int value) {
    (void)device;
    if (port < 0 || port >= 4 || button < 0 || button >= 32) return;
    std::lock_guard<std::mutex> lk(pImpl->input.m);
    pImpl->input.states[port][button] = (int16_t)value;
}

void LibretroDlopen::setInputState(int port, int device, int index, int id, int value) {
    (void)device; (void)index;
    if (port < 0 || port >= 4 || id < 0 || id >= 32) return;
    std::lock_guard<std::mutex> lk(pImpl->input.m);
    pImpl->input.states[port][id] = (int16_t)value;
}

int16_t LibretroDlopen::pollInput(unsigned port, unsigned device, unsigned index, unsigned id) {
    (void)device; (void)index;
    if (port >= 4 || id >= 32) return 0;
    std::lock_guard<std::mutex> lk(pImpl->input.m);
    return pImpl->input.states[port][id];
}

unsigned LibretroDlopen::videoWidth() const  { return pImpl->env.avInfo.geometry.max_width; }
unsigned LibretroDlopen::videoHeight() const { return pImpl->env.avInfo.geometry.max_height; }
int LibretroDlopen::pixelFormat() const { return pImpl->env.pixelFormat; }
const char* LibretroDlopen::coreName() const { return pImpl->coreName.c_str(); }
const char* LibretroDlopen::coreVersion() const { return pImpl->coreVersion.c_str(); }
int LibretroDlopen::region() const { return pImpl->sym.get_region ? (int)pImpl->sym.get_region() : 0; }

bool LibretroDlopen::handleEnvironment(unsigned cmd, void* data,
        VideoCb, AudioSampleCb, AudioBatchCb, InputCb, FrameTimeCb) {
    switch (cmd) {
        case 9:  // RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY
            if (data) *(const char**)data = pImpl->env.systemDir.c_str();
            return true;
        case 15: // RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY
            if (data) *(const char**)data = pImpl->env.saveDir.c_str();
            return true;
        case 19: // RETRO_ENVIRONMENT_GET_LIBRETRO_PATH
            if (data) *(const char**)data = pImpl->env.corePath.c_str();
            return true;
        case 10: // RETRO_ENVIRONMENT_SET_PIXEL_FORMAT
            if (data) pImpl->env.pixelFormat = *(const int*)data;
            return true;
        default:
            return false;
    }
}

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
