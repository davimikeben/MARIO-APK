package com.davimikeben.marioapk;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

/**
 * Monitoramento: contador de FPS e métricas de desempenho.
 */
public class MonitorSettingsActivity extends AppCompatActivity {
    private SettingsManager settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xFF0B0E14);

        TextView title = new TextView(this);
        title.setText("📊 Monitoramento");
        title.setTextSize(22f);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        Switch swFps = new Switch(this);
        swFps.setText("Mostrar contador de FPS (padrão: somente FPS)");
        swFps.setChecked(settings.isFpsShown());
        swFps.setOnCheckedChangeListener((cb, b) -> settings.setFpsShown(b));
        root.addView(swFps);

        TextView info = new TextView(this);
        info.setText("Por padrão, apenas o FPS aparece durante a partida. "
                + "No menu flutuante → Desempenho estão CPU/RAM/GPU.");
        info.setTextSize(14f);
        info.setTextColor(0xFFB0B8C8);
        root.addView(info);

        TextView perf = new TextView(this);
        perf.setText(new PerformanceManager(this).capabilitiesReport());
        perf.setTextSize(12f);
        perf.setTextColor(0xFF00E5FF);
        root.addView(perf);

        Toast.makeText(this, "Monitoramento", Toast.LENGTH_SHORT).show();
        setContentView(root);
    }
}
