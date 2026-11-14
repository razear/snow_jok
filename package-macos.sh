#!/bin/sh
set -eu

cd "$(dirname "$0")"
JAVA_HOME=$(/usr/libexec/java_home -v 25)
export JAVA_HOME

mvn clean package
mkdir -p target/jpackage-input
cp target/snow-jok.jar target/jpackage-input/

"$JAVA_HOME/bin/jpackage" \
    --type app-image \
    --name "Snow Jok" \
    --app-version 1.0.0 \
    --mac-package-identifier io.github.snowjok \
    --resource-dir src/main/macos \
    --icon src/main/macos/SnowJok.icns \
    --input target/jpackage-input \
    --main-jar snow-jok.jar \
    --add-modules java.desktop,java.prefs \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    --dest target/macos
