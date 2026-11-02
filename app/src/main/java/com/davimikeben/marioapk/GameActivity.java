package com.davimikeben.marioapk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * Tela de execução do jogo. Inicializa o núcleo de emulação, carrega a ROM
 * integrada, renderiza via OpenGL ES, exibe teclado virtual, contador de FPS
 * e menu flutuante.
 */
public class GameActivity extends AppCompatActivity
        implements EmulatorCore.Listener, VirtualKeyboardView.Listener, FloatingMenuView.Listener {

    public static final String EXTRA_ROM_INFO = "rom_info";

    private RomInfo romInfo;
    private AssetRomLoader romLoader;
    private N64EmulatorCore emulator;
    private PerformanceManager perf;
    private SettingsManager settings;
    private AudioManager audio;

    private GameSurfaceView gameView;
    private GameRenderer renderer;
    private VirtualKeyboardView keyboard;
    private FpsCounterOverlay fpsOverlay;
    private FloatingMenuView floatingMenu;
    private TextView tvStatus;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private volatile boolean running = false;
    private long lastFrameTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game);

        romInfo = (RomInfo) getIntent().getSerializableExtra(EXTRA_ROM_INFO);
        if (romInfo == null) {
            Toast.makeText(this, R.string.rom_not_found, Toast.LENGTH_LONG).show();
            finish(); return;
        }
        settings = new SettingsManager(this);
        perf = new PerformanceManager(this);
        audio = new AudioManager(this);
        audio.setEnabled(settings.isAudioEnabled());
        audio.setVolume(settings.getAudioVolume());

        romLoader = new AssetRomLoader(this, romInfo);
        emulator = new N64EmulatorCore(romInfo);
        emulator.setListener(this);

        gameView = findViewById(R.id.game_surface);
        renderer = new GameRenderer(perf);
        gameView.setRenderer(renderer);
        gameView.setRenderMode(GameSurfaceView.RENDERMODE_CONTINUOUSLY);

        keyboard = findViewById(R.id.virtual_keyboard);
        keyboard.setListener(this);
        keyboard.setScale(settings.getKeyboardScale() / 100f);
        keyboard.setAlpha(settings.getKeyboardOpacity());
        keyboard.setEnabled(settings.isKeyboardEnabled());

        fpsOverlay = findViewById(R.id.fps_overlay);
        fpsOverlay.setShowDetailed(false);
        fpsOverlay.setVisibility(settings.isFpsShown() ? android.view.View.VISIBLE : android.view.View.GONE);

        floatingMenu = findViewById(R.id.floating_menu);
        floatingMenu.setListener(this);

        tvStatus = findViewById(R.id.tv_status);
        tvStatus.setText(R.string.rom_loading);

        new Thread(this::initAndLoad).start();
    }
    private void initAndLoad() {
        if (!emulator.init()) {
            uiHandler.post(() -> {
                tvStatus.setText("Erro ao iniciar núcleo: " + emulator.getLastError());
                Toast.makeText(this, emulator.getLastError(), Toast.LENGTH_LONG).show();
            });
            return;
        }
        File romFile;
        try {
            romFile = romLoader.extractToPrivateStorage();
        } catch (Exception e) {
            uiHandler.post(() -> {
                tvStatus.setText("Erro ao extrair ROM: " + e.getMessage());
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            });
            return;
        }
        if (!emulator.loadRom(romFile)) {
            uiHandler.post(() -> {
                tvStatus.setText("Erro ao carregar ROM: " + emulator.getLastError());
                Toast.makeText(this, emulator.getLastError(), Toast.LENGTH_LONG).show();
            });
            return;
        }
        uiHandler.post(() -> {
            tvStatus.setText(R.string.rom_ok);
            renderer.setFrameSize(640, 480);
            audio.init(44100, 2);
            running = true;
            lastFrameTime = System.nanoTime();
            emulator.start();
            startLoop();
        });
    }

    private void startLoop() {
        new Thread(() -> {
            int targetFps = perf.getTargetFps();
            long frameIntervalNs = 1_000_000_000L / targetFps;
            while (running) {
                long now = System.nanoTime();
                long elapsed = now - lastFrameTime;
                if (elapsed >= frameIntervalNs) {
                    lastFrameTime = now;
                    uiHandler.post(fpsOverlay::tick);
                    emulator.onNativeFrameRendered();
                } else {
                    try {
                        Thread.sleep((frameIntervalNs - elapsed) / 1_000_000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    @Override public void onFrameRendered() {}
    @Override public void onStateChanged(int newState) {}
    @Override public void onError(String message) {
        uiHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
    @Override public void onAudioAvailable(byte[] pcm, int sampleRate, int channels) {
        if (audio != null) audio.writePCM(pcm, 0, pcm.length);
    }

    @Override public void onKeyPressed(String id, int keyCode) {
        if (emulator != null) emulator.input(keyCode, true);
    }
    @Override public void onKeyReleased(String id, int keyCode) {
        if (emulator != null) emulator.input(keyCode, false);
    }
    @Override public void onAnalog(float x, float y) {
        if (emulator != null) emulator.analog(x, y);
    }

    @Override public void onMenuOption(String option) {
        switch (option) {
            case "controls": startActivity(new Intent(this, KeyboardSettingsActivity.class)); break;
            case "graphics": startActivity(new Intent(this, GraphicsSettingsActivity.class)); break;
            case "performance": startActivity(new Intent(this, PerformanceSettingsActivity.class)); break;
            case "save_settings": Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show(); break;
            case "restart": if (emulator != null) emulator.reset(); break;
            case "main_menu": finish(); break;
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (emulator != null) emulator.pause();
        if (audio != null) audio.pause();
        running = false;
    }
    @Override protected void onResume() {
        super.onResume();
        if (emulator != null) emulator.resume();
        if (audio != null) audio.play();
        running = true;
    }
    @Override protected void onDestroy() {
        running = false;
        if (emulator != null) emulator.shutdown();
        if (audio != null) audio.release();
        super.onDestroy();
    }
}

