#!/usr/bin/env bash

function die() { echo "\x1b[1;31merror:\x1b[m $*" >&2; exit 1; }

[[ -f ../content.toml ]] \
	|| die "Missing docs/content.toml"

[[ -d ../content ]] \
	|| die "Missing docs/content/ - edit feature markdown there"

JAR_ARGS=()
if [[ -e minecraft-client.jar ]]; then
	JAR_ARGS=(--client-jar minecraft-client.jar)
fi

echo "Generating site from docs/content/ + docs/content.toml..."
.venv/bin/python ./generate.py \
	"${JAR_ARGS[@]}" \
	--content-dir ../content \
	--content-toml ../content.toml \
	-o ..

echo "Generating css..."
npx tailwindcss -i templates/style.css -o ../css/style.css --minify

if [[ -f ../assets/fonts/Minecraftia.woff ]]; then
	cp ../assets/fonts/Minecraftia.woff ../css/Minecraftia.woff
fi
