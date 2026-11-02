package com.davimikeben.marioapk;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

/**
 * Configurações de áudio do emulador.
 */
public class AudioSettingsActivity extends AppCompatActivity {
    private SettingsManager settings;
    private AudioManager audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);
        audio = new AudioManager(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xFF0B0E14);

        TextView title = new TextView(this);
        title.setText("🔊 Áudio");
        title.setTextSize(22f);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        Switch sw = new Switch(this);
        sw.setText("Áudio ativado");
        sw.setChecked(settings.isAudioEnabled());
        sw.setOnCheckedChangeListener((cb, b) -> {
            settings.setAudioEnabled(b);
            audio.setEnabled(b);
        });
        root.addView(sw);

        TextView lbl = new TextView(this);
        lbl.setText("Volume: " + Math.round(settings.getAudioVolume() * 100) + "%");
        lbl.setTextSize(16f);
        lbl.setTextColor(0xFFB0B8C8);
        root.addView(lbl);
        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(Math.round(settings.getAudioVolume() * 100));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean b) {
                lbl.setText("Volume: " + p + "%");
                audio.setVolume(p / 100f);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {
                settings.setAudioVolume(sb.getProgress() / 100f);
                Toast.makeText(AudioSettingsActivity.this, "Volume salvo", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(bar);

        setContentView(root);
    }
}
