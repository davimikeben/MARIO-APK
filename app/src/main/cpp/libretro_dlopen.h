/*
 * libretro_dlopen.h — cabeçalho do wrapper de carregamento dinâmico de cores libretro.
 */
#pragma once
#include <cstdint>
#include <cstddef>
#include <string>
#include <memory>

typedef unsigned long long retro_usec_t;

class LibretroDlopen {
public:
    using EnvironmentCb = bool (*)(unsigned, void*);
    using VideoCb        = void (*)(const void*, unsigned, unsigned, size_t);
    using AudioSampleCb  = void (*)(int16_t, int16_t);
    using AudioBatchCb   = size_t (*)(const int16_t*, size_t);
    using InputCb        = int16_t (*)(unsigned, unsigned, unsigned, unsigned);
    using FrameTimeCb    = void (*)(retro_usec_t);

    LibretroDlopen();
    ~LibretroDlopen();

    int  init(const char* corePath, const char* sysDir, const char* saveDir,
              EnvironmentCb envCb, VideoCb, AudioSampleCb, AudioBatchCb, InputCb,
              FrameTimeCb);
    int  loadRom(const char* romPath);
    void run();
    void reset();
    void setPaused(bool p);
    void setInput(int port, int device, int button, int value);
    void setInputState(int port, int device, int index, int id, int value);
    int16_t pollInput(unsigned port, unsigned device, unsigned index, unsigned id);
    bool handleEnvironment(unsigned cmd, void* data,
              VideoCb, AudioSampleCb, AudioBatchCb, InputCb, FrameTimeCb);
    int  saveState(int slot);
    int  loadState(int slot);
    unsigned videoWidth() const;
    unsigned videoHeight() const;
    int  pixelFormat() const;
    const char* coreName() const;
    const char* coreVersion() const;
    int  region() const;

private:
    struct Impl;
    std::unique_ptr<Impl> pImpl;
};
