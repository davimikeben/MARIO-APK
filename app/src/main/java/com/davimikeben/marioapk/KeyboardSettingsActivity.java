package com.davimikeben.marioapk;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ContentLoadingProgressBar;
import android.widget.LinearLayout;

/**
 * Configuração do teclado virtual: opacidade, tamanho (escala), ativação.
 */
public class KeyboardSettingsActivity extends AppCompatActivity {
    private SettingsManager settings;
    private VirtualKeyboardView keyboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xFF0B0E14);

        TextView title = new TextView(this);
        title.setText("🎮 Configuração do teclado");
        title.setTextSize(22f);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        // Opacidade
        TextView opLbl = new TextView(this);
        opLbl.setText("Opacidade: " + settings.getKeyboardOpacity() + "%");
        opLbl.setTextSize(16f);
        opLbl.setTextColor(0xFFB0B8C8);
        root.addView(opLbl);
        SeekBar opBar = new SeekBar(this);
        opBar.setMax(100);
        opBar.setProgress(settings.getKeyboardOpacity());
        opBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean b) {
                opLbl.setText("Opacidade: " + p + "%");
                if (keyboard != null) keyboard.setAlpha(p * 255 / 100);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {
                settings.setKeyboardOpacity(sb.getProgress());
                Toast.makeText(KeyboardSettingsActivity.this, "Opacidade salva", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(opBar);

        // Escala
        TextView scLbl = new TextView(this);
        scLbl.setText("Tamanho: " + settings.getKeyboardScale() + "%");
        scLbl.setTextSize(16f);
        scLbl.setTextColor(0xFFB0B8C8);
        root.addView(scLbl);
        SeekBar scBar = new SeekBar(this);
        scBar.setMax(200);
        scBar.setProgress(settings.getKeyboardScale());
        scBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean b) {
                scLbl.setText("Tamanho: " + p + "%");
                if (keyboard != null) keyboard.setScale(p / 100f);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {
                settings.setKeyboardScale(sb.getProgress());
                if (keyboard != null) keyboard.setScale(sb.getProgress() / 100f);
                Toast.makeText(KeyboardSettingsActivity.this, "Tamanho salvo", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(scBar);

        Switch swEnabled = new Switch(this);
        swEnabled.setText("Teclado virtual ativado");
        swEnabled.setChecked(settings.isKeyboardEnabled());
        swEnabled.setOnCheckedChangeListener((cb, b) -> {
            settings.setKeyboardEnabled(b);
            if (keyboard != null) keyboard.setEnabled(b);
        });
        root.addView(swEnabled);

        setContentView(root);
    }
}
