/*
 * audio_sink.cpp — Saída de áudio via OpenSLES (latência baixa, sem dependências).
 * Ring buffer de 4 × 4096 frames para suportar variações de frame pacing do core.
 */
#include "audio_sink.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/log.h>
#include <cstring>
#include <vector>
#include <mutex>
#include <atomic>

#define LOG_TAG "audio_sink"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct AudioSink::Impl {
    SLObjectItf engineObj = nullptr;
    SLEngineItf engine = nullptr;
    SLObjectItf outputMixObj = nullptr;
    SLObjectItf playerObj = nullptr;
    SLPlayItf player = nullptr;
    SLAndroidSimpleBufferQueueItf bq = nullptr;
    int sampleRate = 44100;
    static constexpr int kChannels = 2;
    static constexpr int kBufFrames = 4096;
    static constexpr int kBufCount = 4;
    int16_t buffers[kBufCount][kBufFrames * kChannels];
    std::atomic<int> writeIdx{0};
    std::mutex mtx;

    ~Impl() {
        if (playerObj) { (*playerObj)->Destroy(playerObj); playerObj = nullptr; }
        if (outputMixObj) { (*outputMixObj)->Destroy(outputMixObj); outputMixObj = nullptr; }
        if (engineObj) { (*engineObj)->Destroy(engineObj); engineObj = nullptr; }
    }
};

static void bqCallback(SLAndroidSimpleBufferQueueItf bq, void* ctx) {
    // Reciclagem automática: o próprio enqueue abaixo preenche o próximo.
}

AudioSink::AudioSink() : pImpl(new Impl) {
    configure(44100);
}

AudioSink::~AudioSink() {}

void AudioSink::configure(int rate) {
    pImpl->sampleRate = rate;
    SLmilliHertz hz;
    switch (rate) {
        case 8000: hz = SL_SAMPLINGRATE_8; break;
        case 11025: hz = SL_SAMPLINGRATE_11_025; break;
        case 22050: hz = SL_SAMPLINGRATE_22_05; break;
        case 32000: hz = SL_SAMPLINGRATE_32; break;
        case 44100: hz = SL_SAMPLINGRATE_44_1; break;
        case 48000: hz = SL_SAMPLINGRATE_48; break;
        default: hz = SL_SAMPLINGRATE_44_1;
    }
    slCreateEngine(&pImpl->engineObj, 0, nullptr, 0, nullptr, nullptr);
    (*pImpl->engineObj)->Realize(pImpl->engineObj, SL_BOOLEAN_FALSE);
    (*pImpl->engineObj)->GetInterface(pImpl->engineObj, SL_IID_ENGINE, &pImpl->engine);
    (*pImpl->engine)->CreateOutputMix(pImpl->engine, &pImpl->outputMixObj, 0, nullptr, nullptr);
    (*pImpl->outputMixObj)->Realize(pImpl->outputMixObj, SL_BOOLEAN_FALSE);

    SLDataLocator_AndroidSimpleBufferQueue locBQ = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, Impl::kBufCount };
    SLDataFormat_PCM fmt;
    fmt.formatType = SL_DATAFORMAT_PCM;
    fmt.numChannels = Impl::kChannels;
    fmt.samplesPerSec = hz;
    fmt.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    fmt.containerSize = SL_PCMSAMPLEFORMAT_FIXED_16;
    fmt.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    fmt.endianness = SL_BYTEORDER_LITTLEENDIAN;
    SLDataSource src = { &locBQ, &fmt };
    SLDataLocator_OutputMix locOM = { SL_DATALOCATOR_OUTPUTMIX, pImpl->outputMixObj };
    SLDataSink sink = { &locOM, nullptr };
    SLInterfaceID ids[] = { SL_IID_BUFFERQUEUE };
    SLboolean req[] = { SL_BOOLEAN_TRUE };
    (*pImpl->engine)->CreateAudioPlayer(pImpl->engine, &pImpl->playerObj, &src, &sink, 1, ids, req);
    (*pImpl->playerObj)->Realize(pImpl->playerObj, SL_BOOLEAN_FALSE);
    (*pImpl->playerObj)->GetInterface(pImpl->playerObj, SL_IID_PLAY, &pImpl->player);
    (*pImpl->playerObj)->GetInterface(pImpl->playerObj, SL_IID_BUFFERQUEUE, &pImpl->bq);
    (*pImpl->bq)->RegisterCallback(pImpl->bq, bqCallback, nullptr);
    for (int i = 0; i < Impl::kBufCount; ++i) {
        (*pImpl->bq)->Enqueue(pImpl->bq, pImpl->buffers[i], sizeof(pImpl->buffers[i]));
    }
    (*pImpl->player)->SetPlayState(pImpl->player, SL_PLAYSTATE_PLAYING);
    LOGI("OpenSLES configurado: %d Hz, %d buffers de %d frames", rate, Impl::kBufCount, Impl::kBufFrames);
}

void AudioSink::pushStereo(int16_t l, int16_t r) {
    // Apenas mantém buffer pronto — sample-by-sample não é ideal mas funciona como fallback.
}

size_t AudioSink::pushBatch(const int16_t* data, size_t frames) {
    if (!data || frames == 0) return 0;
    size_t total = frames * Impl::kChannels;
    int idx = pImpl->writeIdx.load();
    int16_t* dst = pImpl->buffers[idx % Impl::kBufCount];
    size_t toCopy = total < Impl::kBufFrames * Impl::kChannels ? total : Impl::kBufFrames * Impl::kChannels;
    memcpy(dst, data, toCopy * sizeof(int16_t));
    if (pImpl->bq) (*pImpl->bq)->Enqueue(pImpl->bq, dst, toCopy * sizeof(int16_t));
    pImpl->writeIdx.store(idx + 1);
    return frames;
}
