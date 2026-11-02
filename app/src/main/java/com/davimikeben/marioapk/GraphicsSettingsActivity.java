package com.davimikeben.marioapk;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

/**
 * Configurações de gráficos e resolução de renderização.
 */
public class GraphicsSettingsActivity extends AppCompatActivity {
    private SettingsManager settings;
    private PerformanceManager perf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);
        perf = new PerformanceManager(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xFF0B0E14);

        TextView title = new TextView(this);
        title.setText("🎨 Gráficos");
        title.setTextSize(22f);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        TextView lbl = new TextView(this);
        lbl.setText("Resolução de renderização:");
        lbl.setTextSize(16f);
        lbl.setTextColor(0xFFB0B8C8);
        root.addView(lbl);

        Spinner spinner = new Spinner(this);
        String[] res = {"960x540 (Desempenho)", "1280x720 (Equilibrado)", "1920x1080 (Qualidade)"};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, res));
        String cur = settings.getGfxResolution();
        int sel = cur.equals("1920x1080") ? 2 : cur.equals("960x540") ? 0 : 1;
        spinner.setSelection(sel);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String r = pos == 0 ? "960x540" : pos == 2 ? "1920x1080" : "1280x720";
                settings.setGfxResolution(r);
                perf.setMode(pos == 0 ? PerformanceManager.MODE_PERFORMANCE :
                               pos == 2 ? PerformanceManager.MODE_QUALITY : PerformanceManager.MODE_BALANCED);
                settings.setPerfMode(pos == 0 ? PerformanceManager.MODE_PERFORMANCE :
                                     pos == 2 ? PerformanceManager.MODE_QUALITY : PerformanceManager.MODE_BALANCED);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        root.addView(spinner);

        TextView info = new TextView(this);
        info.setText("GPU: " + perf.gpuRenderer + " | Max textura: " + perf.maxTextureSize + "px");
        info.setTextSize(13f);
        info.setTextColor(0xFF00E5FF);
        root.addView(info);

        Toast.makeText(this, "Resolução: " + settings.getGfxResolution(), Toast.LENGTH_SHORT).show();
        setContentView(root);
    }
}
