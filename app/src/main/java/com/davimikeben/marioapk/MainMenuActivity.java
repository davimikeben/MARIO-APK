package com.davimikeben.marioapk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Menu principal próprio do aplicativo (não é a interface do emulador).
 * Opções: PLAY, teclado, desempenho, gráficos, áudio, tela, monitoramento,
 * salvar e restaurar configurações.
 */
public class MainMenuActivity extends AppCompatActivity implements View.OnClickListener {
    private RomInfo romInfo;
    private SettingsManager settings;

    private CardView cardPlay, cardKeyboard, cardPerformance, cardGraphics,
                     cardAudio, cardDisplay, cardMonitor, cardSave, cardRestore;
    private TextView tvRomInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        romInfo = (RomInfo) getIntent().getSerializableExtra(MainActivity.EXTRA_ROM_INFO);
        if (romInfo == null) romInfo = new RomInfo("desconhecido",".bin",0,"","","","");
        settings = new SettingsManager(this);

        cardPlay = findViewById(R.id.card_play);
        cardKeyboard = findViewById(R.id.card_keyboard);
        cardPerformance = findViewById(R.id.card_performance);
        cardGraphics = findViewById(R.id.card_graphics);
        cardAudio = findViewById(R.id.card_audio);
        cardDisplay = findViewById(R.id.card_display);
        cardMonitor = findViewById(R.id.card_monitor);
        cardSave = findViewById(R.id.card_save);
        cardRestore = findViewById(R.id.card_restore);
        tvRomInfo = findViewById(R.id.tv_rom_info);

        cardPlay.setOnClickListener(this);
        cardKeyboard.setOnClickListener(this);
        cardPerformance.setOnClickListener(this);
        cardGraphics.setOnClickListener(this);
        cardAudio.setOnClickListener(this);
        cardDisplay.setOnClickListener(this);
        cardMonitor.setOnClickListener(this);
        cardSave.setOnClickListener(this);
        cardRestore.setOnClickListener(this);

        tvRomInfo.setText("ROM: " + romInfo.name + "\n" + romInfo.platform + " • " +
                romInfo.emulatorCore + "\nTamanho: " + romInfo.sizeBytes + " bytes");
    }

    @Override
    public void onClick(View v) {
        if (v == cardPlay) {
            if (romInfo.name == null || romInfo.name.isEmpty()) {
                Toast.makeText(this, R.string.rom_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(MainActivity.EXTRA_ROM_INFO, romInfo);
            startActivity(i);
        } else if (v == cardKeyboard) {
            startActivity(new Intent(this, KeyboardSettingsActivity.class));
        } else if (v == cardPerformance) {
            startActivity(new Intent(this, PerformanceSettingsActivity.class));
        } else if (v == cardGraphics) {
            startActivity(new Intent(this, GraphicsSettingsActivity.class));
        } else if (v == cardAudio) {
            startActivity(new Intent(this, AudioSettingsActivity.class));
        } else if (v == cardDisplay) {
            startActivity(new Intent(this, DisplaySettingsActivity.class));
        } else if (v == cardMonitor) {
            startActivity(new Intent(this, MonitorSettingsActivity.class));
        } else if (v == cardSave) {
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        } else if (v == cardRestore) {
            settings.restoreDefaults();
            Toast.makeText(this, R.string.settings_restored, Toast.LENGTH_SHORT).show();
        }
    }
}
