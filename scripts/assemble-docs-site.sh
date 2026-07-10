#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
site="$root/target/docs-site"

rm -rf "$site"
mkdir -p "$site"
cp -R "$root/docs/." "$site/"
cp -R "$root/resourcepack" "$site/resourcepack"
