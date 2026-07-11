#!/bin/bash
# SessionStart hook pour Claude Code sur le web : installe le SDK Android
# (platform 35, build-tools 35) et Gradle, puis expose ANDROID_HOME/PATH.
#
# Gradle vient de conda-forge car services.gradle.org redirige les
# distributions vers github.com, bloqué par la politique réseau de
# l'environnement. Utiliser `gradle assembleDebug` (pas ./gradlew).
set -euo pipefail

# Uniquement dans les sessions distantes (Claude Code sur le web)
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
GRADLE_VERSION="8.11.1"
GRADLE_HOME="/opt/gradle-$GRADLE_VERSION"
GRADLE_CONDA_URL="https://conda.anaconda.org/conda-forge/noarch/gradle-$GRADLE_VERSION-h707e725_0.conda"

command -v unzip >/dev/null || apt-get install -y unzip || { apt-get update && apt-get install -y unzip; }
command -v unzstd >/dev/null || apt-get install -y zstd || { apt-get update && apt-get install -y zstd; }

# --- SDK Android ---
if [ ! -x "$SDKMANAGER" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/cmdtools.zip" "$CMDTOOLS_URL"
  unzip -q "$tmp/cmdtools.zip" -d "$tmp"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi

if [ ! -d "$ANDROID_HOME/platforms/android-35" ] || [ ! -d "$ANDROID_HOME/build-tools/35.0.0" ]; then
  yes | "$SDKMANAGER" --licenses > /dev/null 2>&1 || true
  "$SDKMANAGER" "platforms;android-35" "build-tools;35.0.0" "platform-tools" > /dev/null
fi

# --- Gradle (conda-forge) ---
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/gradle.conda" "$GRADLE_CONDA_URL"
  unzip -q "$tmp/gradle.conda" -d "$tmp"
  tar --use-compress-program=unzstd -xf "$tmp"/pkg-gradle-*.tar.zst -C "$tmp"
  rm -rf "$GRADLE_HOME"
  mv "$tmp/share/gradle-$GRADLE_VERSION" "$GRADLE_HOME"
  chmod +x "$GRADLE_HOME/bin/gradle"
  rm -rf "$tmp"
fi
ln -sf "$GRADLE_HOME/bin/gradle" /usr/local/bin/gradle

# --- Environnement de session ---
if [ -n "${CLAUDE_ENV_FILE:-}" ] && ! grep -qs "ANDROID_HOME=\"$ANDROID_HOME\"" "$CLAUDE_ENV_FILE"; then
  {
    echo "export ANDROID_HOME=\"$ANDROID_HOME\""
    echo "export ANDROID_SDK_ROOT=\"$ANDROID_HOME\""
    echo "export PATH=\"$GRADLE_HOME/bin:\$PATH\""
  } >> "$CLAUDE_ENV_FILE"
fi

# local.properties (gitignoré) pour que Gradle trouve le SDK sans variable d'env
if [ -n "${CLAUDE_PROJECT_DIR:-}" ] && [ ! -f "$CLAUDE_PROJECT_DIR/local.properties" ]; then
  echo "sdk.dir=$ANDROID_HOME" > "$CLAUDE_PROJECT_DIR/local.properties"
fi

echo "SDK Android (platform 35) + Gradle $GRADLE_VERSION prêts. Builder avec 'gradle assembleDebug' (pas ./gradlew : téléchargement des distributions Gradle bloqué par la politique réseau)."
