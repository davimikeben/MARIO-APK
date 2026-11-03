# CMake para o wrapper nativo sm64_emulator.
# O core libretro (.so) NÃO é linkado aqui — ele é dlopen'd em tempo de execução.
cmake_minimum_required(VERSION 3.22.1)
project(sm64_emulator LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(sm64_emulator SHARED
    sm64_jni.cpp
    libretro_dlopen.cpp
    gl_renderer.cpp
    audio_sink.cpp
    perf_pacer.cpp
)

target_link_libraries(sm64_emulator
    log
    android
    EGL
    GLESv3
    OpenSLES
    dl
)

target_include_directories(sm64_emulator PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
