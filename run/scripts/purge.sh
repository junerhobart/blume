#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
git clean -fdX run/
git checkout -- run/bukkit.yml run/ops.json run/server.properties 2>/dev/null || true
