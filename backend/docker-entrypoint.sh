#!/bin/sh
set -eu
if [ ! -f gradle/wrapper/gradle-wrapper.jar ] || [ ! -f gradlew ]; then
  echo "backend/gradlew and backend/gradle/wrapper are part of the repository; restore them with git before starting the container" >&2
  exit 1
fi
chmod +x gradlew
exec ./gradlew "$@"
