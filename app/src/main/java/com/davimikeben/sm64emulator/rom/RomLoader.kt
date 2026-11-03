package com.davimikeben.sm64emulator.rom

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Responsável por localizar a ROM integrada dentro do APK (assets/rom/) e,
 * se necessário, extraí-la para o armazenamento privado do aplicativo
 * (context.getFilesDir()/rom/) para que o núcleo de emulação possa carregá-la
 * por caminho físico.
 *
 * A ROM NÃO é baixada — ela é parte do APK. Apenas é copiada dos assets
 * para filesDir na primeira execução, e depois reaproveitada.
 */
object RomLoader {
    private const val TAG = "RomLoader"

    /** Caminho dos assets dentro do APK. */
    const val ASSET_ROM_PATH = "rom/super_mario_64.z64"
    const val ASSET_ROM_SHA256 = "17ce077343c6133f8c9f2d6d6d9a4ab62c8cd2aa57c40aea1f490b4c8bb21d91"

    /** Nome do arquivo extraído em filesDir/rom. */
    const val EXTRACTED_NAME = "super_mario_64.z64"

    /**
     * Garante que a ROM exista em filesDir/rom/super_mario_64.z64,
     * com hash SHA-256 idêntico ao original empacotado.
     * Retorna o caminho absoluto para o arquivo extraído.
     */
    @Throws(Exception::class)
    fun ensureExtracted(context: Context): File {
        val romDir = File(context.filesDir, "rom").apply { mkdirs() }
        val romFile = File(romDir, EXTRACTED_NAME)

        // Verificação rápida: se o arquivo já existe e o hash bate, reutiliza.
        if (romFile.exists() && romFile.length() > 0) {
            val currentHash = sha256(romFile)
            if (currentHash.equals(ASSET_ROM_SHA256, ignoreCase = true)) {
                Log.i(TAG, "ROM já extraída e íntegra: ${romFile.absolutePath}")
                return romFile
            }
            Log.w(TAG, "ROM extraída está corrompida — re-extraindo...")
            romFile.delete()
        }

        // Extrai dos assets para filesDir.
        context.assets.open(ASSET_ROM_PATH).use { input ->
            romFile.outputStream().use { output -> input.copyTo(output) }
        }

        // Verificação de integridade.
        val extractedHash = sha256(romFile)
        if (!extractedHash.equals(ASSET_ROM_SHA256, ignoreCase = true)) {
            romFile.delete()
            throw IllegalStateException(
                "Falha de integridade da ROM extraída. " +
                "Esperado=$ASSET_ROM_SHA256 Obtido=$extractedHash"
            )
        }

        Log.i(TAG, "ROM extraída com sucesso: ${romFile.absolutePath} (${romFile.length()} bytes)")
        return romFile
    }

    /** Hash SHA-256 em lowercase hex. */
    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
