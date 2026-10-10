#!/bin/sh
set -eu
P=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ -n "${JAVA_HOME:-}" ]; then PATH="$JAVA_HOME/bin:$PATH"; export PATH; fi
mkdir -p "$P/build/test-classes"
javac -encoding UTF-8 -classpath "$P/libs/lunar-1.7.7.jar" -d "$P/build/test-classes" "$P/src/com/ridi/calendar/CalendarEngine.java" "$P/tests/CalendarEngineTest.java"
java -classpath "$P/build/test-classes:$P/libs/lunar-1.7.7.jar" CalendarEngineTest
if [ -n "${ANDROID_HOME:-}" ]; then
  javac -encoding UTF-8 -classpath "$P/build/test-classes:$P/libs/lunar-1.7.7.jar:$ANDROID_HOME/platforms/android-19/android.jar" -d "$P/build/test-classes" "$P/src/com/ridi/calendar/ChineseCities.java" "$P/src/com/ridi/calendar/Weather.java" "$P/tests/WeatherCodeTest.java"
  java -classpath "$P/build/test-classes:$P/libs/lunar-1.7.7.jar:$ANDROID_HOME/platforms/android-19/android.jar" WeatherCodeTest
fi
