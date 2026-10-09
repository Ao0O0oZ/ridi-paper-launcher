#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 DEVICE_SERIAL" >&2
  exit 2
fi

SERIAL=$1
ADB_BIN=${ADB_BIN:-adb}
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
REMOTE='/sdcard/Documents/E-Ink Launcher/icon'

"$ADB_BIN" -s "$SERIAL" get-state
"$ADB_BIN" -s "$SERIAL" shell "mkdir -p '$REMOTE'"
for icon in "$ROOT"/assets/desktop-icons/*.png; do
  "$ADB_BIN" -s "$SERIAL" push "$icon" "$REMOTE/"
done
"$ADB_BIN" -s "$SERIAL" shell am force-stop cn.modificator.launcher.clean
"$ADB_BIN" -s "$SERIAL" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
