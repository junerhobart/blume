#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
revision=$(git -C "$ROOT" describe --tags --always 2>/dev/null | sed 's/^v//')
revision=${revision:-0.0.0-dev}
exec mvn -f "$ROOT/pom.xml" -Drevision="$revision" "$@"
