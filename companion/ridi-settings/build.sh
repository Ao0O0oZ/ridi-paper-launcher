#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SDK_DIR=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [ -z "$SDK_DIR" ]; then
  echo "Set ANDROID_HOME (or ANDROID_SDK_ROOT) to an Android SDK with API 19." >&2
  exit 2
fi
BUILD_TOOLS="$SDK_DIR/build-tools/${RIDI_BUILD_TOOLS_VERSION:-36.1.0}"
ANDROID_JAR="$SDK_DIR/platforms/android-19/android.jar"
BUILD_DIR="$PROJECT_DIR/build"
if [ -n "${JAVA_HOME:-}" ]; then
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi
SIGNING_KEYSTORE=${RIDI_SIGNING_KEYSTORE:-"$HOME/.android/debug.keystore"}
SIGNING_ALIAS=${RIDI_SIGNING_ALIAS:-androiddebugkey}
SIGNING_STOREPASS=${RIDI_SIGNING_STOREPASS:-android}
SIGNING_KEYPASS=${RIDI_SIGNING_KEYPASS:-android}
if [ ! -f "$SIGNING_KEYSTORE" ]; then
  echo "Signing keystore not found: $SIGNING_KEYSTORE" >&2
  exit 2
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex"

"$BUILD_TOOLS/aapt" package -f -m -J "$BUILD_DIR/gen" \
  -M "$PROJECT_DIR/AndroidManifest.xml" -S "$PROJECT_DIR/res" -I "$ANDROID_JAR"

javac -encoding UTF-8 -source 7 -target 7 -classpath "$ANDROID_JAR" \
  -d "$BUILD_DIR/classes" $(find "$PROJECT_DIR/src" "$BUILD_DIR/gen" -name '*.java')

(cd "$BUILD_DIR/classes" && jar cf "$BUILD_DIR/classes.jar" .)
"$BUILD_TOOLS/d8" --min-api 14 --lib "$ANDROID_JAR" \
  --output "$BUILD_DIR/dex" "$BUILD_DIR/classes.jar"
"$BUILD_TOOLS/aapt" package -f -M "$PROJECT_DIR/AndroidManifest.xml" \
  -S "$PROJECT_DIR/res" -A "$PROJECT_DIR/assets" -I "$ANDROID_JAR" -F "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/dex"
"$BUILD_TOOLS/zipalign" -f 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/apksigner" sign \
  --ks "$SIGNING_KEYSTORE" \
  --ks-key-alias "$SIGNING_ALIAS" --ks-pass "pass:$SIGNING_STOREPASS" --key-pass "pass:$SIGNING_KEYPASS" \
  --out "$BUILD_DIR/RidiSettings.apk" "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$BUILD_DIR/RidiSettings.apk"
