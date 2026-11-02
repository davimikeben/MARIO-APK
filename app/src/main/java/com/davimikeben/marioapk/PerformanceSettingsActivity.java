package com.davimikeben.marioapk;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.view.View;

/**
 * Modos de desempenho otimizados para Mali-G52: Desempenho, Equilibrado, Qualidade.
 */
public class PerformanceSettingsActivity extends AppCompatActivity {
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
        title.setText("⚡ Desempenho (Mali-G52)");
        title.setTextSize(22f);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        TextView caps = new TextView(this);
        caps.setText(perf.capabilitiesReport());
        caps.setTextSize(12f);
        caps.setTextColor(0xFF00E5FF);
        root.addView(caps);

        TextView lbl = new TextView(this);
        lbl.setText("Modo de desempenho:");
        lbl.setTextSize(16f);
        lbl.setTextColor(0xFFB0B8C8);
        root.addView(lbl);

        Spinner spinner = new Spinner(this);
        String[] modes = {"Desempenho (fluidez)", "Equilibrado", "Qualidade (imagem)"};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        spinner.setSelection(settings.getPerfMode());
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                perf.setMode(pos);
                settings.setPerfMode(pos);
            }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        root.addView(spinner);

        TextView res = new TextView(this);
        res.setText("Resolução recomendada: " + perf.getRecommendedResolution() +
                " • FPS alvo: " + perf.getTargetFps());
        res.setTextSize(14f);
        res.setTextColor(0xFFB0B8C8);
        root.addView(res);

        Toast.makeText(this, "Modo: " + perf.getModeName(), Toast.LENGTH_SHORT).show();
        setContentView(root);
    }
}
