#pragma once
#include <cstdint>
#include <cstddef>
#include <memory>

class AudioSink {
public:
    AudioSink();
    ~AudioSink();
    void configure(int rate);
    void pushStereo(int16_t l, int16_t r);
    size_t pushBatch(const int16_t* data, size_t frames);
private:
    struct Impl;
    std::unique_ptr<Impl> pImpl;
};
