cmake_minimum_required(VERSION 3.22.1)
project("extremen64" CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(extremen64 SHARED native-lib.cpp)

find_library(log-lib log)
find_library(android-lib android)
find_library(gles-lib GLESv3)
find_library(egl-lib EGL)
find_library(opensles-lib OpenSLES)

target_compile_options(extremen64 PRIVATE
    -O2
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
)

target_link_options(extremen64 PRIVATE -Wl,--gc-sections)

target_link_libraries(extremen64
    ${log-lib}
    ${android-lib}
    ${gles-lib}
    ${egl-lib}
    ${opensles-lib}
    dl
)
