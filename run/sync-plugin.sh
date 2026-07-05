#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/run/plugins"
(cd "$ROOT" && mvn -q package)
cp "$ROOT/target/Blume-0.1-SNAPSHOT.jar" "$ROOT/run/plugins/Blume.jar"
