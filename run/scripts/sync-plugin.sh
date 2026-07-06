#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
mkdir -p "$ROOT/run/plugins"
(cd "$ROOT" && mvn -q package)
cp "$ROOT/target/Blume-0.3.jar" "$ROOT/run/plugins/Blume.jar"
