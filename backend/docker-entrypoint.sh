#!/bin/sh
set -eu
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  cp /opt/wrapper/gradlew /opt/wrapper/gradlew.bat .
  mkdir -p gradle/wrapper
  cp /opt/wrapper/gradle/wrapper/* gradle/wrapper/
fi
chmod +x gradlew
exec ./gradlew "$@"
