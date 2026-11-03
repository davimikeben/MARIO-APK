package com.davimikeben.sm64emulator.core

/**
 * Constantes da API libretro usadas pelo wrapper.
 * Referência: libretro.h (https://github.com/libretro/RetroArch/blob/master/libretro-common/include/libretro.h)
 */
object LibretroConstants {
    // retro_region_t
    const val RETRO_REGION_NTSC = 0
    const val RETRO_REGION_PAL = 1

    // retro_pixel_format
    const val RETRO_PIXEL_FORMAT_0RGB1555 = 0
    const val RETRO_PIXEL_FORMAT_XRGB8888 = 1
    const val RETRO_PIXEL_FORMAT_RGB565 = 2

    // retro_device
    const val RETRO_DEVICE_NONE = 0
    const val RETRO_DEVICE_JOYPAD = 1
    const val RETRO_DEVICE_MOUSE = 2
    const val RETRO_DEVICE_KEYBOARD = 3
    const val RETRO_DEVICE_LIGHTGUN = 4
    const val RETRO_DEVICE_ANALOG = 5

    // retro_device_id_joypad — botões padrão do controle
    const val RETRO_DEVICE_ID_JOYPAD_B = 0
    const val RETRO_DEVICE_ID_JOYPAD_Y = 1
    const val RETRO_DEVICE_ID_JOYPAD_SELECT = 2
    const val RETRO_DEVICE_ID_JOYPAD_START = 3
    const val RETRO_DEVICE_ID_JOYPAD_UP = 4
    const val RETRO_DEVICE_ID_JOYPAD_DOWN = 5
    const val RETRO_DEVICE_ID_JOYPAD_LEFT = 6
    const val RETRO_DEVICE_ID_JOYPAD_RIGHT = 7
    const val RETRO_DEVICE_ID_JOYPAD_A = 8
    const val RETRO_DEVICE_ID_JOYPAD_X = 9
    const val RETRO_DEVICE_ID_JOYPAD_L = 10
    const val RETRO_DEVICE_ID_JOYPAD_R = 11
    const val RETRO_DEVICE_ID_JOYPAD_L2 = 12
    const val RETRO_DEVICE_ID_JOYPAD_R2 = 13
    const val RETRO_DEVICE_ID_JOYPAD_L3 = 14
    const val RETRO_DEVICE_ID_JOYPAD_R3 = 15

    // retro_environment_t cmds essenciais
    const val RETRO_ENVIRONMENT_SET_PIXEL_FORMAT = 10
    const val RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS = 11
    const val RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9
    const val RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE = 17
    const val RETRO_ENVIRONMENT_GET_VARIABLE = 15
    const val RETRO_ENVIRONMENT_SET_VARIABLES = 16
    const val RETRO_ENVIRONMENT_GET_LIBRETRO_PATH = 19
    const val RETRO_ENVIRONMENT_SET_FRAME_TIME_CALLBACK = 21
    const val RETRO_ENVIRONMENT_SET_AUDIO_CALLBACK = 22
    const val RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE = 23
    const val RETRO_ENVIRONMENT_GET_INPUT_INTERFACE = 24
    const val RETRO_ENVIRONMENT_GET_GEOMETRY = 31
    const val RETRO_ENVIRONMENT_GET_SYSTEM_AV_INFO = 32
    const val RETRO_ENVIRONMENT_SET_HW_RENDER = 41
    const val RETRO_ENVIRONMENT_GET_CANVAS_GEOMETRY = 52

    const val HW_RENDER_CONTEXT_NONE = 0
    const val HW_RENDER_CONTEXT_OPENGL = 1
    const val HW_RENDER_CONTEXT_OPENGLES2 = 2
    const val HW_RENDER_CONTEXT_OPENGLES3 = 3
    const val HW_RENDER_CONTEXT_OPENGL_CORE = 4
    const val HW_RENDER_CONTEXT_OPENGLES_VERSION = 5
    const val HW_RENDER_CONTEXT_VULKAN = 6
}
