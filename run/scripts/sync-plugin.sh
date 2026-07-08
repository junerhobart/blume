#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
mkdir -p "$ROOT/run/plugins"
revision_arg=
if tag=$(git -C "$ROOT" describe --tags --exact-match 2>/dev/null); then
  revision_arg="-Drevision=${tag#v}"
fi
(cd "$ROOT" && mvn -q package $revision_arg)
version=$(cd "$ROOT" && mvn -q help:evaluate -Dexpression=project.version -DforceStdout $revision_arg)
cp "$ROOT/target/Blume-${version}.jar" "$ROOT/run/plugins/Blume.jar"
