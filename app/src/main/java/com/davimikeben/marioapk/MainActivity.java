package com.davimikeben.marioapk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Tela de inicialização: verifica se a ROM integrada está presente no APK
 * e é carregável. Em seguida, abre o menu principal personalizado.
 */
public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_ROM_INFO = "rom_info";

    private AssetRomLoader romLoader;
    private RomInfo romInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configura a ROM esperada. ATUALIZAR com os dados reais do arquivo (ver ROM_INTEGRATION.md).
        romInfo = new RomInfo(
                "Super Mario 64.z64", ".z64", 0,
                "", "assets/rom/Super Mario 64.z64",
                "Nintendo 64", "N64 (mupen64plus/ARES)"
        );
        romLoader = new AssetRomLoader(this, romInfo);

        TextView status = findViewById(R.id.status);
        status.setText("Verificando ROM integrada…");

        new Handler(Looper.getMainLooper()).postDelayed(this::verifyRom, 300);
    }

    private void verifyRom() {
        if (!romLoader.assetExists()) {
            Toast.makeText(this, R.string.rom_not_found, Toast.LENGTH_LONG).show();
            ((TextView)findViewById(R.id.status)).setText(R.string.rom_not_found);
            return;
        }
        try {
            // Calcula SHA-256 dos assets para integridade
            java.io.InputStream is = romLoader.openAssetStream();
            String sha = AssetRomLoader.sha256Of(is);
            is.close();
            RomInfo updated = new RomInfo(
                    romInfo.name, romInfo.extension, romInfo.sizeBytes,
                    sha, romInfo.assetPath, romInfo.platform, romInfo.emulatorCore
            );
            this.romInfo = updated;
            openMenu();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao ler ROM: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openMenu() {
        Intent i = new Intent(this, MainMenuActivity.class);
        i.putExtra(EXTRA_ROM_INFO, romInfo);
        startActivity(i);
    }
}
