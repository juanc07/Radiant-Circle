#!/usr/bin/env bash
set -euo pipefail

APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"
APK_TARGET_DIR="release/submission"
APK_TARGET="$APK_TARGET_DIR/RadiantRush-debug.apk"

if [ ! -f "$APK_SOURCE" ]; then
  echo "Debug APK not found at $APK_SOURCE"
  echo "Run: ./gradlew :app:assembleDebug"
  exit 1
fi

mkdir -p "$APK_TARGET_DIR"
cp "$APK_SOURCE" "$APK_TARGET"

echo "Copied APK to $APK_TARGET"
ls -lh "$APK_TARGET"
echo "Do not commit APK binaries unless the submission rules require it."
