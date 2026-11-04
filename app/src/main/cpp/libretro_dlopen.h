/*
 * libretro_dlopen.h — cabeçalho do wrapper de carregamento dinâmico de cores libretro.
 * v2 — callbacks tipados corretamente + suporte HW render + input analógico.
 */
#pragma once
#include <cstdint>
#include <cstddef>
#include <memory>
#include <cstdarg>

typedef unsigned long long retro_usec_t;
typedef void* retro_hw_get_current_framebuffer_fn_t();
typedef void* (*retro_hw_get_proc_address_fn_t)(const char*);

class LibretroDlopen {
public:
    // Callbacks de saída do core (core → frontend)
    using EnvironmentCb = bool (*)(unsigned, void*);
    using VideoCb        = void (*)(const void* data, unsigned w, unsigned h, size_t pitch);
    using AudioSampleCb  = void (*)(int16_t l, int16_t r);
    using AudioBatchCb   = size_t (*)(const int16_t* data, size_t frames);
    using InputFn        = int16_t (*)(unsigned port, unsigned device, unsigned index, unsigned id);
    using InputPollFn    = void (*)(void);
    using FrameTimeCb    = void (*)(retro_usec_t);

    // Thunks estáticos que o core recebe via retro_set_*
    static bool     environmentThunk(unsigned cmd, void* data);
    static void     videoThunk(const void* data, unsigned w, unsigned h, size_t pitch);
    static void     audioSampleThunk(int16_t l, int16_t r);
    static size_t   audioBatchThunk(const int16_t* data, size_t frames);
    static void     inputPollThunk(void);
    static int16_t  inputStateThunk(unsigned port, unsigned device, unsigned index, unsigned id);
    static void     coreLogThunk(void* ud, unsigned level, const char* fmt, ...);

    LibretroDlopen();
    ~LibretroDlopen();

    int  init(const char* corePath, const char* sysDir, const char* saveDir,
              EnvironmentCb envCb, VideoCb videoCb, AudioSampleCb asc, AudioBatchCb abc,
              InputFn icb, FrameTimeCb ftc);
    int  loadRom(const char* romPath);
    void run();
    void reset();
    void setPaused(bool p);
    void setInput(int port, int device, int button, int value);
    void setInputState(int port, int device, int index, int id, int value);
    int16_t pollInput(unsigned port, unsigned device, unsigned index, unsigned id);
    int  saveState(int slot);
    int  loadState(int slot);
    unsigned videoWidth() const;
    unsigned videoHeight() const;
    int  pixelFormat() const;
    bool isHwRenderActive() const;
    const char* coreName() const;
    const char* coreVersion() const;
    int  region() const;
    // Última mensagem de erro (dlopen/dlsym) p/ diagnóstico no app
    const char* lastError() const;

    // Impl definido no .cpp; público para permitir acesso dos callbacks C estáticos
    struct Impl;
    std::unique_ptr<Impl> pImpl;
};
