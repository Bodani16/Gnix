#!/usr/bin/env bash
# Linux/macOS with a JDK 17+ and Internet access. Android Studio is also supported.
set -euo pipefail
cd "$(dirname "$0")"
GNIX_PROJECT="$PWD"
GNIX_TOOLS="$GNIX_PROJECT/tools"
GNIX_SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$GNIX_TOOLS/android-sdk}}"
mkdir -p "$GNIX_TOOLS" "$GNIX_PROJECT/dist"
command -v java >/dev/null || { echo 'Instale um JDK 17 ou superior.' >&2; exit 1; }

# Toda ferramenta baixada é conferida por SHA-256 antes de ser executada: um
# arquivo trocado no caminho viraria código rodando na máquina de quem compila.
ANDROID_CLI_SHA256=7ec965280a073311c339e571cd5de778b9975026cfcbe79f2b1cdcb1e15317ee

# Usa sha256sum/shasum, que vêm com o sistema (e com o Git Bash no Windows).
# A versão anterior chamava python3: no Windows isso resolve para o atalho da
# Microsoft Store, que existe no PATH mas não executa — o guard passava e a
# conferência falhava depois, dando confiança falsa.
require_hasher() {
  command -v sha256sum >/dev/null 2>&1 || command -v shasum >/dev/null 2>&1 || {
    echo 'Instale sha256sum (coreutils) ou shasum para conferir os downloads.' >&2; exit 1; }
}
verify_sha256() {
  local file="$1" expected actual
  expected=$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')
  if command -v sha256sum >/dev/null 2>&1; then
    actual=$(sha256sum "$file" | cut -d' ' -f1)
  else
    actual=$(shasum -a 256 "$file" | cut -d' ' -f1)
  fi
  [ -n "$expected" ] || { echo "Checksum esperado vazio para $file" >&2; exit 1; }
  [ "$actual" = "$expected" ] || {
    printf 'Checksum invalido para %s\n  esperado: %s\n  obtido:   %s\n' "$file" "$expected" "$actual" >&2
    exit 1; }
}

if [ ! -x "$GNIX_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ] && [ ! -d "$GNIX_SDK_DIR/platforms/android-36" ]; then
  command -v curl >/dev/null
  command -v unzip >/dev/null
  require_hasher
  curl --fail --location --retry 2 --connect-timeout 15 --max-time 300 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o "$GNIX_TOOLS/android-cli.zip"
  verify_sha256 "$GNIX_TOOLS/android-cli.zip" "$ANDROID_CLI_SHA256"
  mkdir -p "$GNIX_SDK_DIR/cmdline-tools"
  unzip -q -o "$GNIX_TOOLS/android-cli.zip" -d "$GNIX_SDK_DIR/cmdline-tools"
  mv "$GNIX_SDK_DIR/cmdline-tools/cmdline-tools" "$GNIX_SDK_DIR/cmdline-tools/latest"
fi
export ANDROID_HOME="$GNIX_SDK_DIR"
if [ ! -d "$GNIX_SDK_DIR/platforms/android-36" ] || [ ! -d "$GNIX_SDK_DIR/build-tools/36.0.0" ]; then
  # Avoid pipefail interpreting yes's expected SIGPIPE as a license failure.
  set +o pipefail
  yes | "$GNIX_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$GNIX_SDK_DIR" --licenses >/dev/null
  set -o pipefail
  "$GNIX_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$GNIX_SDK_DIR" 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'
fi
if [ ! -x "$GNIX_TOOLS/gradle-8.13/bin/gradle" ]; then
  require_hasher
  curl --fail --location --retry 2 --connect-timeout 15 --max-time 300 https://services.gradle.org/distributions/gradle-8.13-bin.zip -o "$GNIX_TOOLS/gradle.zip"
  curl --fail --location --retry 2 --connect-timeout 15 --max-time 60 https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256 -o "$GNIX_TOOLS/gradle.sha256"
  verify_sha256 "$GNIX_TOOLS/gradle.zip" "$(cut -d' ' -f1 "$GNIX_TOOLS/gradle.sha256")"
  unzip -q -o "$GNIX_TOOLS/gradle.zip" -d "$GNIX_TOOLS"
fi
"$GNIX_TOOLS/gradle-8.13/bin/gradle" --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk dist/Gnix.apk
"$GNIX_SDK_DIR/build-tools/36.0.0/apksigner" verify --verbose dist/Gnix.apk
"$GNIX_SDK_DIR/build-tools/36.0.0/aapt" dump badging dist/Gnix.apk
printf '\nAPK: %s/dist/Gnix.apk\n' "$GNIX_PROJECT"
