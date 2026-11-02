package com.davimikeben.marioapk;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Sistema de áudio simples e de baixa latência para o núcleo de emulação.
 * Recebe buffers PCM do núcleo (via EmulatorCore.Listener) e reproduz.
 */
public class AudioManager {
    private static final String TAG = "AudioManager";
    private AudioTrack track;
    private boolean enabled = true;
    private float volume = 1.0f;
    private boolean started = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && track != null) {
            try { track.pause(); } catch (Throwable t) {}
        } else if (enabled && started) {
            play();
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        if (track != null) {
            track.setStereoVolume(volume, volume);
        }
    }

    public void init(int sampleRate, int channels) {
        stop();
        if (!enabled) return;
        int channelConfig = (channels == 1) ?
                AudioTrack.CHANNEL_OUT_MONO : AudioTrack.CHANNEL_OUT_STEREO;
        int format = android.media.AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, format);
        if (minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
            Log.e(TAG, "Configuração de áudio inválida");
            return;
        }
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        try {
            track = new AudioTrack(attrs, sampleRate, channelConfig, format,
                    minBufferSize * 2, AudioTrack.MODE_STREAM);
            started = true;
            play();
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao inicializar AudioTrack", t);
            track = null;
        }
    }

    public void play() {
        if (track != null && started) {
            try { track.play(); } catch (Throwable t) {}
        }
    }

    public void writePCM(byte[] pcm, int offset, int length) {
        if (!enabled || track == null || !started) return;
        try {
            // Aplica volume simples (atenução linear)
            if (volume < 1.0f && length >= 2) {
                for (int i = offset; i < offset + length; i += 2) {
                    int sample = ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
                    sample = (int) (sample * volume);
                    pcm[i] = (byte) (sample & 0xFF);
                    pcm[i + 1] = (byte) ((sample >> 8) & 0xFF);
                }
            }
            track.write(pcm, offset, length);
        } catch (Throwable t) {
            // áudio não bloqueia a emulação
        }
    }

    public void pause() {
        if (track != null) {
            try { track.pause(); } catch (Throwable t) {}
        }
    }

    public void release() {
        stop();
        if (track != null) {
            try { track.release(); } catch (Throwable t) {}
            track = null;
        }
        started = false;
    }

    private void stop() {
        if (track != null) {
            try { track.stop(); } catch (Throwable t) {}
        }
    }
}
