#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
mkdir -p "$ROOT/run/plugins"
sh "$ROOT/scripts/build.sh" -q package
version=$(sh "$ROOT/scripts/build.sh" -q help:evaluate -Dexpression=project.version -DforceStdout)
cp "$ROOT/target/Blume-${version}.jar" "$ROOT/run/plugins/Blume.jar"
