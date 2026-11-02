package com.davimikeben.marioapk;

import android.util.Log;

import java.io.File;

/**
 * Ponte entre o app Java e o núcleo de emulação N64 nativo (C/C++ via JNI/NDK).
 *
 * Observação de implementação:
 * - A lógica real de emulação N64 (mupen64plus / ARES / Parallel N64) deve ser
 *   compilada como biblioteca nativa (.so) com o Android NDK e colocada em
 *   app/src/main/jniLibs/<abi>/.
 * - Este arquivo expõe os métodos nativos (declare abaixo) e a camada Java.
 * - Como este sandbox não possui Android NDK, os métodos nativos são declarados
 *   mas a biblioteca .so deve ser fornecida/compilada separadamente (ver jni/ e
 *   ROM_INTEGRATION.md).
 */
public class N64EmulatorCore implements EmulatorCore {
    private static final String TAG = "N64EmulatorCore";

    static {
        // Nome da biblioteca nativa do núcleo de emulação (ex: libn64emu.so).
        // Deve ser empacotada no APK via src/main/jniLibs/<abi>/.
        try {
            System.loadLibrary("n64emu");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Biblioteca nativa n64emu não carregada: " + e.getMessage() +
                    ". O núcleo funcionará em modo stub (sem emulação real).");
        }
    }

    private final RomInfo romInfo;
    private int state = STATE_UNINITIALIZED;
    private String lastError = "";
    private Listener listener;
    private boolean nativeLibLoaded = false;

    private static native boolean nativeInit();
    private static native boolean nativeLoadRom(String absolutePath);
    private static native boolean nativeStart();
    private static native void nativePause();
    private static native void nativeResume();
    private static native void nativeReset();
    private static native void nativeShutdown();
    private static native int nativeGetState();
    private static native String nativeGetLastError();
    private static native void nativeInput(int keyCode, boolean pressed);
    private static native void nativeAnalog(float x, float y);
    private static native void nativeSetListener(long nativeListenerPtr);

    private static boolean isNativeLibLoaded() {
        try {
            System.loadLibrary("n64emu");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public N64EmulatorCore(RomInfo romInfo) {
        this.romInfo = romInfo;
        this.nativeLibLoaded = isNativeLibLoaded();
    }

    private void ensureListenerNative() {
        if (nativeLibLoaded && listener != null) {
            nativeSetListener(System.identityHashCode(this));
        }
    }
    @Override
    public boolean init() {
        lastError = "";
        if (!nativeLibLoaded) {
            state = STATE_INITIALIZED;
            Log.w(TAG, "Modo stub: biblioteca nativa ausente.");
            return true;
        }
        boolean ok = nativeInit();
        state = ok ? STATE_INITIALIZED : STATE_ERROR;
        if (!ok) lastError = "Falha ao inicializar núcleo nativo.";
        return ok;
    }

    @Override
    public boolean loadRom(File romFile) {
        if (romFile == null || !romFile.exists()) {
            lastError = "Arquivo da ROM não encontrado: " +
                    (romFile == null ? "null" : romFile.getPath());
            state = STATE_ERROR;
            return false;
        }
        state = STATE_LOADING;
        if (!nativeLibLoaded) {
            if (romFile.length() == 0) {
                lastError = "ROM vazia.";
                state = STATE_ERROR;
                return false;
            }
            state = STATE_READY;
            Log.d(TAG, "Modo stub: ROM carregada " + romFile.getName() +
                    " (" + romFile.length() + " bytes)");
            return true;
        }
        boolean ok = nativeLoadRom(romFile.getAbsolutePath());
        state = ok ? STATE_READY : STATE_ERROR;
        if (!ok) lastError = nativeGetLastError();
        return ok;
    }

    @Override
    public boolean start() {
        if (state != STATE_READY && state != STATE_PAUSED) {
            lastError = "Núcleo não está pronto para iniciar. Estado: " + state;
            return false;
        }
        if (!nativeLibLoaded) {
            state = STATE_RUNNING;
            Log.d(TAG, "Modo stub: emulação iniciada.");
            return true;
        }
        boolean ok = nativeStart();
        state = ok ? STATE_RUNNING : STATE_ERROR;
        if (!ok) lastError = nativeGetLastError();
        return ok;
    }

    @Override
    public void pause() {
        if (state == STATE_RUNNING) {
            state = STATE_PAUSED;
            if (nativeLibLoaded) nativePause();
        }
    }

    @Override
    public void resume() {
        if (state == STATE_PAUSED) {
            state = STATE_RUNNING;
            if (nativeLibLoaded) nativeResume();
        }
    }

    @Override
    public void reset() {
        state = STATE_READY;
        lastError = "";
        if (nativeLibLoaded) nativeReset();
    }

    @Override
    public void shutdown() {
        state = STATE_UNINITIALIZED;
        if (nativeLibLoaded) nativeShutdown();
    }

    @Override
    public void input(int keyCode, boolean pressed) {
        if (nativeLibLoaded) nativeInput(keyCode, pressed);
    }

    @Override
    public void analog(float x, float y) {
        if (nativeLibLoaded) nativeAnalog(x, y);
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
        ensureListenerNative();
    }

    public static void onNativeFrameRendered() {
    }

    public static void onNativeStateChanged(int newState) {
    }

    public static void onNativeError(String message) {
    }

    public static void onNativeAudio(byte[] pcm, int sampleRate, int channels) {
    }
}

