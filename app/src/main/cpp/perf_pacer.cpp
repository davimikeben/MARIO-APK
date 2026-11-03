/*
 * perf_pacer.cpp — Frame pacing e medição de FPS para Mali-G52.
 *
 * Modos:
 *  0 PERFORMANCE: target 60fps, aguarda de vsync leve (sem dormir)
 *  1 BALANCED:    target ~57fps, micro-sleep para aliviar GPU
 *  2 QUALITY:     target ~50fps, sleep maior para maior qualidade de render
 *
 * Não sacrifica estabilidade — se FPS real cair, mantém target sem forçar skip.
 */
#include "perf_pacer.h"
#include <android/log.h>
#include <chrono>
#include <thread>
#include <algorithm>

#define LOG_TAG "perf_pacer"

struct PerfPacer::Impl {
    int mode = 1;
    std::chrono::steady_clock::time_point lastFrame;
    int frameCounter = 0;
    std::chrono::steady_clock::time_point lastFpsUpdate;
    float currentFps = 0.0f;
    double targetFps = 60.0;
};

PerfPacer::PerfPacer() : pImpl(new Impl) {
    pImpl->lastFrame = std::chrono::steady_clock::now();
    pImpl->lastFpsUpdate = pImpl->lastFrame;
}

PerfPacer::~PerfPacer() {}

void PerfPacer::setMode(int mode) {
    pImpl->mode = mode;
    switch (mode) {
        case 0: pImpl->targetFps = 60.0; break;   // PERFORMANCE
        case 1: pImpl->targetFps = 57.0; break;   // BALANCED
        case 2: pImpl->targetFps = 50.0; break;   // QUALITY
        default: pImpl->targetFps = 60.0;
    }
}

void PerfPacer::beginFrame() {
    pImpl->lastFrame = std::chrono::steady_clock::now();
}

void PerfPacer::endFrame() {
    auto now = std::chrono::steady_clock::now();
    double elapsedMs = std::chrono::duration<double, std::milli>(now - pImpl->lastFrame).count();
    double targetMs = 1000.0 / pImpl->targetFps;

    // Sleep apenas se sobrou tempo — nunca força se já está lento.
    if (elapsedMs < targetMs) {
        int mode = pImpl->mode;
        if (mode == 1) {
            std::this_thread::sleep_for(std::chrono::milliseconds((long)(targetMs - elapsedMs) / 2));
        } else if (mode == 2) {
            std::this_thread::sleep_for(std::chrono::milliseconds((long)(targetMs - elapsedMs)));
        }
        // mode == 0: no sleep, full speed
    }

    pImpl->frameCounter++;
    auto sinceFps = std::chrono::duration<double>(now - pImpl->lastFpsUpdate).count();
    if (sinceFps >= 1.0) {
        pImpl->currentFps = (float)(pImpl->frameCounter / sinceFps);
        pImpl->frameCounter = 0;
        pImpl->lastFpsUpdate = now;
    }
}

float PerfPacer::lastFps() const { return pImpl->currentFps; }
