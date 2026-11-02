package com.davimikeben.marioapk;

/**
 * Interface do núcleo de emulação incorporado ao aplicativo.
 * Todas as operações são síncronas/assíncronas conforme implementação nativa (JNI).
 */
public interface EmulatorCore {
    /** Estado do núcleo */
    int STATE_UNINITIALIZED = 0;
    int STATE_INITIALIZED = 1;
    int STATE_LOADING = 2;
    int STATE_READY = 3;
    int STATE_RUNNING = 4;
    int STATE_PAUSED = 5;
    int STATE_ERROR = 6;

    /** Inicializa o núcleo de emulação. Retorna true se成功. */
    boolean init();

    /** Carrega a ROM a partir de um caminho físico ou InputStream. */
    boolean loadRom(File romFile);

    /** Inicia a execução do jogo. */
    boolean start();

    /** Pausa a emulação. */
    void pause();

    /** Retorna da pausa. */
    void resume();

    /** Reinicia a partida. */
    void reset();

    /** Desliga o núcleo e libera memória. */
    void shutdown();

    /** Injeta um evento de entrada (botão pressionado/solto). */
    void input(int keyCode, boolean pressed);

    /** Define a posição de um toque multitouch (analógico). */
    void analog(float x, float y);

    /** Retorna o estado atual. */
    int getState();

    /** Retorna a última mensagem de erro. */
    String getLastError();

    /** Listener para eventos do núcleo (frame renderizado, erro, estado). */
    interface Listener {
        void onFrameRendered();
        void onStateChanged(int newState);
        void onError(String message);
        void onAudioAvailable(byte[] pcm, int sampleRate, int channels);
    }

    void setListener(Listener listener);
}
