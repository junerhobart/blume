#!/usr/bin/env bash

set -euo pipefail

function die() { echo "\x1b[1;31merror:\x1b[m $*" >&2; exit 1; }

DOCS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SITE_DIR="$DOCS_DIR/site"
STATIC_DIR="$DOCS_DIR/assets"

[[ -f "$DOCS_DIR/content.toml" ]] \
	|| die "Missing docs/content.toml"

[[ -d "$DOCS_DIR/content" ]] \
	|| die "Missing docs/content/ - edit feature markdown there"

echo "Building site into docs/site/..."
rm -rf "$SITE_DIR"
mkdir -p "$SITE_DIR/assets" "$SITE_DIR/css"

if [[ -d "$STATIC_DIR" ]]; then
	cp -R "$STATIC_DIR"/. "$SITE_DIR/assets/"
fi

[[ -f "$DOCS_DIR/favicon.ico" ]] && cp "$DOCS_DIR/favicon.ico" "$SITE_DIR/"

for pack in blume-pack.zip blume-bedrock-pack.zip blume-geyser-mappings.json; do
	[[ -f "$DOCS_DIR/$pack" ]] && cp "$DOCS_DIR/$pack" "$SITE_DIR/"
done

echo "Generating pages from docs/content/ + docs/content.toml..."
if [[ -e minecraft-client.jar ]]; then
	.venv/bin/python ./generate.py \
		--client-jar minecraft-client.jar \
		--content-dir "$DOCS_DIR/content" \
		--content-toml "$DOCS_DIR/content.toml" \
		-o "$SITE_DIR"
else
	.venv/bin/python ./generate.py \
		--content-dir "$DOCS_DIR/content" \
		--content-toml "$DOCS_DIR/content.toml" \
		-o "$SITE_DIR"
fi

echo "Generating css..."
npx tailwindcss -i templates/style.css -o "$SITE_DIR/css/style.css" --minify

if [[ -f "$STATIC_DIR/fonts/Minecraftia.woff" ]]; then
	cp "$STATIC_DIR/fonts/Minecraftia.woff" "$SITE_DIR/css/Minecraftia.woff"
fi

echo "Done: $SITE_DIR"
