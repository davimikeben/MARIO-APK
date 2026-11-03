#!/usr/bin/env bash
# build_apk.sh — Executa Gradle para compilar o APK release.
# Assume que setup_android_sdk.sh já foi executado.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

# Garante que o core libretro está presente
if [[ ! -f app/src/main/jniLibs/arm64-v8a/libretro_mupen64plus_next.so ]]; then
    echo "[!] Core libretro não encontrado. Executando fetch_libretro_core.sh..."
    bash scripts/fetch_libretro_core.sh
fi

# Path do SDK
if [[ -z "${ANDROID_HOME:-}" ]]; then
    export ANDROID_HOME="$HOME/android-sdk"
fi
echo "ANDROID_HOME=$ANDROID_HOME"

# Escreve local.properties
cat > local.properties <<EOF
sdk.dir=$ANDROID_HOME
EOF

# Gradle wrapper (download do gradle 8.7 se faltar)
if [[ ! -x ./gradlew ]]; then
    echo "[fetch] Gradle wrapper"
    if [[ ! -f gradle/wrapper/gradle-wrapper.jar ]]; then
        curl -fL -o gradle/wrapper/gradle-wrapper.jar \
            "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
    fi
    chmod +x gradlew 2>/dev/null || true
    # Caso gradlew script não esteja presente, cria um mínimo
    if [[ ! -f gradlew ]]; then
        cat > gradlew <<'W'
#!/usr/bin/env sh
exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
W
        chmod +x gradlew
    fi
fi

# Compila release
./gradlew :app:assembleRelease --no-daemon --stacktrace

APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
if [[ ! -f "$APK" ]]; then
    echo "[ERROR] APK não gerado em $APK" >&2
    exit 1
fi

# Copia para download/
mkdir -p "$PROJECT_DIR/../download"
cp "$APK" "$PROJECT_DIR/../download/sm64_emulator-release.apk"
echo "[ok] APK gerado: $PROJECT_DIR/../download/sm64_emulator-release.apk"
ls -la "$PROJECT_DIR/../download/sm64_emulator-release.apk"
