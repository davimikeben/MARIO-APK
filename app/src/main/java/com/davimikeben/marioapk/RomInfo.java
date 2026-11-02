package com.davimikeben.marioapk;

import java.io.Serializable;

/**
 * Informações identificadas da ROM integrada ao APK.
 * Estes dados devem ser preenchidos com os valores reais do arquivo ROM
 * colocado em app/src/main/assets/rom/ (ver ROM_INTEGRATION.md).
 */
public class RomInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String name;          // ex: "Super Mario 64.z64" / ".rom"
    public final String extension;     // ex: ".z64"
    public final long sizeBytes;       // tamanho em bytes
    public final String sha256;        // hash SHA-256 do arquivo original
    public final String assetPath;     // caminho dentro dos assets: "assets/rom/<nome>"
    public final String platform;      // plataforma original
    public final String emulatorCore;  // núcleo de emulação necessário

    public RomInfo(String name, String extension, long sizeBytes,
                   String sha256, String assetPath, String platform,
                   String emulatorCore) {
        this.name = name;
        this.extension = extension;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.assetPath = assetPath;
        this.platform = platform;
        this.emulatorCore = emulatorCore;
    }

    @Override
    public String toString() {
        return "RomInfo{name='" + name + "', ext='" + extension + "', size=" +
                sizeBytes + ", sha256='" + sha256 + "', path='" + assetPath +
                "', platform='" + platform + "', core='" + emulatorCore + "'}";
    }
}
