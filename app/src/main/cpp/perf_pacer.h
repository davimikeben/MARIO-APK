#pragma once
#include <memory>

class PerfPacer {
public:
    PerfPacer();
    ~PerfPacer();
    void setMode(int mode);   // 0=perf 1=balanced 2=quality
    void beginFrame();
    void endFrame();
    float lastFps() const;
private:
    struct Impl;
    std::unique_ptr<Impl> pImpl;
};
