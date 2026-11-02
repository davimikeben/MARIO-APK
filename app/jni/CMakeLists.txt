cmake_minimum_required(VERSION 3.18.1)
project("n64emu" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Arquiteturas alvo (definidas no defaultConfig do build.gradle: armeabi-v7a, arm64-v8a, x86_64)

# Otimização para Mali-G52 / dispositivos móveis
add_compile_options(-O3 -ffast-math -fno-finite-math-only -fno-trapping-math)
add_compile_options(-mfpu=neon -mfloat-abi=hard)
add_compile_options(-DANDROID -D__ANDROID_API__=24)

include_directories(src/main/jni)

# Biblioteca nativa do núcleo de emulação N64.
# AQUI deve ser inserido o código-fonte real do núcleo (ex: mupen64plus, ARES, Parallel N64)
# portado para Android. Este esqueleto apenas expõe a ponte JNI.
add_library(n64emu SHARED
    emulator_core.cpp
)

target_link_libraries(n64emu
    android
    log
    GLESv2
    OpenSLES
)

# Otimização de tamanho e símbolos
set_target_properties(n64emu PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
    LINK_FLAGS "-Wl,--gc-sections -s"
)
