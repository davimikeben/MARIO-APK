/*
 * gl_renderer.cpp — Renderizador OpenGL ES 3.0 para frames do core libretro.
 * Otimizado para Mali-G52: usa textura GL_RGB565 / GL_RGBA8 conforme pixel format,
 * evita realloc de textura a cada frame (apenas glTexSubImage2D), VAO estático.
 */
#include "gl_renderer.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <cstring>
#include <vector>

#define LOG_TAG "gl_renderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static const char* kVS = R"(#version 300 es
layout(location=0) in vec2 aPos;
layout(location=1) in vec2 aUV;
out vec2 vUV;
void main() { vUV = aUV; gl_Position = vec4(aPos, 0.0, 1.0); })";

static const char* kFS = R"(#version 300 es
precision mediump float;
in vec2 vUV;
uniform sampler2D uTex;
out vec4 frag;
void main() { frag = texture(uTex, vUV); })";

struct GLRenderer::Impl {
    ANativeWindow* window = nullptr;
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
    EGLContext context = EGL_NO_CONTEXT;
    EGLConfig  config  = nullptr;
    GLuint program = 0, vao = 0, vbo = 0, tex = 0;
    GLsizei texW = 0, texH = 0;
    GLint  pixelFormat = -1; // 0=0RGB1555,1=XRGB8888,2=RGB565,-1=unknown
    int qualityMode = 1;     // 0=performance 1=balanced 2=quality
    bool initedGL = false;
    bool hwRender = false;   // true se core ativou SET_HW_RENDER
    std::vector<uint16_t> convBuf; // buffer de conversão 0RGB1555→RGB565

    ~Impl() { destroyGL(); }

    bool initGL() {
        if (initedGL || !window) return false;
        display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        eglInitialize(display, nullptr, nullptr);
        EGLint cfgAttribs[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
            EGL_DEPTH_SIZE, 0, EGL_STENCIL_SIZE, 0,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_NONE
        };
        EGLint num = 0;
        eglChooseConfig(display, cfgAttribs, &config, 1, &num);
        if (num < 1) { LOGE("eglChooseConfig falhou"); return false; }
        eglCreateWindowSurface(display, config, window, nullptr);
        EGLint ctxAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
        context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctxAttribs);
        eglMakeCurrent(display, surface, surface, context);

        program = buildProgram();
        glGenVertexArrays(1, &vao);
        glBindVertexArray(vao);
        glGenBuffers(1, &vbo);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        const float verts[] = {
            -1.f,-1.f, 0.f,1.f,
             1.f,-1.f, 1.f,1.f,
            -1.f, 1.f, 0.f,0.f,
             1.f, 1.f, 1.f,0.f,
        };
        glBufferData(GL_ARRAY_BUFFER, sizeof(verts), verts, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4*sizeof(float), (void*)0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4*sizeof(float), (void*)(2*sizeof(float)));
        glBindVertexArray(0);
        glGenTextures(1, &tex);
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        initedGL = true;
        LOGI("OpenGL ES 3.0 renderer ready");
        return true;
    }

    void destroyGL() {
        if (!initedGL) return;
        if (program) glDeleteProgram(program);
        if (vao) glDeleteVertexArrays(1, &vao);
        if (vbo) glDeleteBuffers(1, &vbo);
        if (tex) glDeleteTextures(1, &tex);
        if (display != EGL_NO_DISPLAY) {
            eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
            if (context) eglDestroyContext(display, context);
            if (surface) eglDestroySurface(display, surface);
            eglTerminate(display);
        }
        initedGL = false;
    }

    GLuint buildShader(GLenum type, const char* src) {
        GLuint s = glCreateShader(type);
        glShaderSource(s, 1, &src, nullptr);
        glCompileShader(s);
        GLint ok = 0; glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
        if (!ok) { char log[1024]; glGetShaderInfoLog(s, sizeof(log), nullptr, log); LOGE("shader compile: %s", log); }
        return s;
    }
    GLuint buildProgram() {
        GLuint vs = buildShader(GL_VERTEX_SHADER, kVS);
        GLuint fs = buildShader(GL_FRAGMENT_SHADER, kFS);
        GLuint p = glCreateProgram();
        glAttachShader(p, vs); glAttachShader(p, fs); glLinkProgram(p);
        GLint ok = 0; glGetProgramiv(p, GL_LINK_STATUS, &ok);
        if (!ok) { char log[1024]; glGetProgramInfoLog(p, sizeof(log), nullptr, log); LOGE("program link: %s", log); }
        glDeleteShader(vs); glDeleteShader(fs);
        return p;
    }
};

GLRenderer::GLRenderer() : pImpl(new Impl) {}
GLRenderer::~GLRenderer() {}

void GLRenderer::setWindow(ANativeWindow* w) {
    pImpl->window = w;
    if (w) pImpl->initGL();
}

void GLRenderer::setQualityMode(int mode) { pImpl->qualityMode = mode; }

void GLRenderer::setHwRender(bool hw) { pImpl->hwRender = hw; }

void GLRenderer::submitFrame(const void* data, unsigned w, unsigned h, size_t pitch) {
    if (!pImpl->initedGL) return;

    // ===== MODO HW RENDER =====
    // O core (mupen64plus_next_gles3) desenha DIRETO no framebuffer default.
    // O callback de vídeo só precisa fazer swap (ou é ignorado se RETRO_HW_FRAME_BUFFER_VALID).
    if (pImpl->hwRender) {
        // data == (void*)-1 => RETRO_HW_FRAME_BUFFER_VALID (core já desenhou no FB)
        // Swap para apresentar. Não usa textura/software path.
        eglSwapBuffers(pImpl->display, pImpl->surface);
        return;
    }

    // ===== MODO SOFTWARE (fallback) =====
    if (!data) return;

    GLint internalFmt, fmt, type;
    switch (pImpl->pixelFormat) {
        case 2: internalFmt = GL_RGB565;  fmt = GL_RGB;  type = GL_UNSIGNED_SHORT_5_6_5; break;
        case 1: internalFmt = GL_RGBA8;   fmt = GL_RGBA; type = GL_UNSIGNED_BYTE; break;
        default: internalFmt = GL_RGB565; fmt = GL_RGB;  type = GL_UNSIGNED_SHORT_5_6_5; break;
    }

    // 0RGB1555 (formato 0): converte para RGB565 em buffer temporário —
    // GLES3 não aceita upload 1_5_5_5_REV diretamente.
    if (pImpl->pixelFormat == 0 || pImpl->pixelFormat == -1) {
        const uint16_t* src = (const uint16_t*)data;
        size_t px = (size_t)w * h;
        pImpl->convBuf.resize(px);
        uint16_t* dst = pImpl->convBuf.data();
        size_t rowPixels = pitch / 2;
        for (unsigned y = 0; y < h; ++y) {
            const uint16_t* row = src + (size_t)y * rowPixels;
            uint16_t* drow = dst + (size_t)y * w;
            for (unsigned x = 0; x < w; ++x) {
                uint16_t v = row[x];
                // 0RGB1555: X RRRRR GGGGG BBBBB → RGB565: RRRRR GGGGGG BBBBB
                uint16_t r5 = (v >> 10) & 0x1F;
                uint16_t g5 = (v >> 5)  & 0x1F;
                uint16_t b5 =  v        & 0x1F;
                uint16_t g6 = (g5 << 1) | (g5 >> 4);
                drow[x] = (uint16_t)((r5 << 11) | (g6 << 5) | b5);
            }
        }
        internalFmt = GL_RGB565; fmt = GL_RGB; type = GL_UNSIGNED_SHORT_5_6_5;
        data = pImpl->convBuf.data();
        pitch = (size_t)w * 2;
    }

    // Recria textura apenas se mudou tamanho.
    if ((GLsizei)w != pImpl->texW || (GLsizei)h != pImpl->texH) {
        glBindTexture(GL_TEXTURE_2D, pImpl->tex);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFmt, (GLsizei)w, (GLsizei)h, 0, fmt, type, nullptr);
        pImpl->texW = (GLsizei)w; pImpl->texH = (GLsizei)h;
    }
    glBindTexture(GL_TEXTURE_2D, pImpl->tex);
    glPixelStorei(GL_UNPACK_ROW_LENGTH, (GLint)(pitch / bytesPerPixel(pImpl->pixelFormat)));
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, (GLsizei)w, (GLsizei)h, fmt, type, data);
    glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

    glViewport(0, 0, pImpl->window ? ANativeWindow_getWidth(pImpl->window) : w,
                        pImpl->window ? ANativeWindow_getHeight(pImpl->window) : h);
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(pImpl->program);
    glBindVertexArray(pImpl->vao);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, pImpl->tex);
    glUniform1i(glGetUniformLocation(pImpl->program, "uTex"), 0);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    eglSwapBuffers(pImpl->display, pImpl->surface);
}

size_t GLRenderer::bytesPerPixel(int fmt) {
    switch (fmt) { case 2: return 2; case 1: return 4; default: return 2; }
}
