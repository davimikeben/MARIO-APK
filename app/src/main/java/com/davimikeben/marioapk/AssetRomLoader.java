package com.davimikeben.marioapk;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Localiza e carrega a ROM integrada ao APK.
 *
 * Fluxo:
 * 1. Tenta abrir a ROM diretamente dos assets via AssetManager.
 * 2. Se o núcleo de emulação exigir um caminho físico (arquivo no disco),
 *    extrai automaticamente para context.getFilesDir()/rom/ sem interação do usuário.
 * 3. Fornece o SHA-256 para verificação de integridade.
 */
public class AssetRomLoader {
    private static final String TAG = "AssetRomLoader";
    private static final int BUFFER_SIZE = 8192;

    private final Context context;
    private final RomInfo romInfo;

    public AssetRomLoader(Context context, RomInfo romInfo) {
        this.context = context.getApplicationContext();
        this.romInfo = romInfo;
    }

    public RomInfo getRomInfo() {
        return romInfo;
    }

    /** Retorna um InputStream lendo diretamente dos recursos internos (assets). */
    public InputStream openAssetStream() throws IOException {
        AssetManager am = context.getAssets();
        return am.open(romInfo.assetPath);
    }

    /** Verifica se a ROM existe nos assets. */
    public boolean assetExists() {
        AssetManager am = context.getAssets();
        try {
            String[] list = am.list("rom");
            if (list == null) return false;
            for (String f : list) {
                if (f.equals(romInfo.name)) return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao listar assets/rom", e);
        }
        return false;
    }

    /**
     * Extrai a ROM dos assets para o armazenamento privado do aplicativo:
     * context.getFilesDir()/rom/<nome>
     * Retorna o File físico extraído (ou já existente).
     */
    public File extractToPrivateStorage() throws IOException {
        File romDir = new File(context.getFilesDir(), "rom");
        if (!romDir.exists()) {
            if (!romDir.mkdirs()) {
                throw new IOException("Não foi possível criar " + romDir.getAbsolutePath());
            }
        }

        File dest = new File(romDir, romInfo.name);

        // Se já existe e o SHA-256 confere, reutiliza (não reextrai desnecessariamente)
        if (dest.exists() && sha256Of(dest).equals(romInfo.sha256)) {
            Log.d(TAG, "ROM já extraída e íntegra: " + dest.getAbsolutePath());
            return dest;
        }

        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = openAssetStream();
            out = new FileOutputStream(dest);
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
            Log.d(TAG, "ROM extraída para: " + dest.getAbsolutePath());
            return dest;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    /** Calcula SHA-256 de um arquivo. */
    public static String sha256Of(File file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível", e);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = fis.read(buf)) != -1) {
                digest.update(buf, 0, read);
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao calcular SHA-256", e);
            return "";
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Calcula SHA-256 de um InputStream (útil para validar os assets antes de empacotar). */
    public static String sha256Of(InputStream is) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível", e);
        }
        byte[] buf = new byte[BUFFER_SIZE];
        int read;
        while ((read = is.read(buf)) != -1) {
            digest.update(buf, 0, read);
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
