package com.davimikeben.marioapk;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.AdapterView;

/**
 * Configurações de tela (orientação, fullscreen).
 */
public class DisplaySettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xFF0B0E14);

        TextView title = new TextView(this);
        title.setText("📱 Tela");
        title.setTextSize(22f);
        title.setTextColor(0xFFFFFFFF);
        root.addView(title);

        TextView lbl = new TextView(this);
        lbl.setText("Orientação: Paisagem (fixa para emulação)");
        lbl.setTextSize(16f);
        lbl.setTextColor(0xFFB0B8C8);
        root.addView(lbl);

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Paisagem", "Retrato", "Sensor"}));
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {}
            public void onNothingSelected(AdapterView<?> p) {}
        });
        root.addView(spinner);

        TextView info = new TextView(this);
        info.setText("Imersão: tela cheia sem barra de status.");
        info.setTextSize(13f);
        info.setTextColor(0xFF00E5FF);
        root.addView(info);

        Toast.makeText(this, "Configurações de tela", Toast.LENGTH_SHORT).show();
        setContentView(root);
    }
}
