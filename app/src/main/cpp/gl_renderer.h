#pragma once
#include <cstdint>
#include <cstddef>
#include <memory>
struct ANativeWindow;

class GLRenderer {
public:
    GLRenderer();
    ~GLRenderer();
    void setWindow(ANativeWindow* w);
    void setQualityMode(int mode);
    void setHwRender(bool hw);
    void submitFrame(const void* data, unsigned w, unsigned h, size_t pitch);
    static size_t bytesPerPixel(int fmt);
private:
    struct Impl;
    std::unique_ptr<Impl> pImpl;
};
