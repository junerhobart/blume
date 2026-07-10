#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
pinned=false
if [ "${1:-}" = --pinned ]; then
  pinned=true
  shift
fi

if $pinned; then
  mc=$(sh "$ROOT/scripts/build.sh" -q help:evaluate -Dexpression=minecraft.version.pinned -DforceStdout)
else
  mc=$(sh "$ROOT/scripts/build.sh" -q help:evaluate -Dexpression=minecraft.version.latest -DforceStdout)
fi

sh "$ROOT/scripts/build.sh" -q package -Dminecraft.version="$mc"
version=$(sh "$ROOT/scripts/build.sh" -q help:evaluate -Dexpression=project.version -DforceStdout)
cp "$ROOT/target/Blume-${version}.jar" "$ROOT/run/plugins/Blume.jar"
