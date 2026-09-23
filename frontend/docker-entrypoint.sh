#!/bin/sh
set -eu
# First bootstrap writes the genuine lockfile to the source mount; subsequent runs use npm ci.
if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; else npm install --no-audit --no-fund; fi
exec "$@"
