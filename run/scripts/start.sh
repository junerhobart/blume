#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
RUN="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RUN"
mkdir -p plugins

fetch() {
  test -f "$2" || curl -fsSL "$1" -o "$2"
}

fetch 'https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar' paper.jar
fetch 'https://cdn.modrinth.com/data/P1OZGk5p/versions/4JQUNqJk/ViaVersion-5.10.1-SNAPSHOT.jar' plugins/ViaVersion.jar
fetch 'https://cdn.modrinth.com/data/NpvuJQoq/versions/gsSGwSIA/ViaBackwards-5.10.1-SNAPSHOT.jar' plugins/ViaBackwards.jar
fetch 'https://cdn.modrinth.com/data/FIlZB9L0/versions/Ufl71nST/Terra-bukkit-6.6.6-BETA%2B451683aff-shaded.jar' plugins/Terra.jar
fetch 'https://cdn.modrinth.com/data/Vebnzrzj/versions/MBSY8toc/LuckPerms-Bukkit-5.5.53.jar' plugins/LuckPerms.jar

(cd "$ROOT" && mvn -q clean package)
cp "$ROOT/target/Blume-0.3.jar" plugins/Blume.jar

PACK_PORT=8765
PACK_SHA1=$(shasum -a 1 "$ROOT/docs/blume-pack.zip" | awk '{print $1}')
PACK_URL="http://127.0.0.1:${PACK_PORT}/blume-pack.zip"

# ponytail: single-process static server for local dev; production uses GitHub Pages URL in config
if lsof -ti:"$PACK_PORT" >/dev/null 2>&1; then
  kill "$(lsof -ti:"$PACK_PORT")" 2>/dev/null || true
fi
(cd "$ROOT/docs" && python3 -m http.server "$PACK_PORT" --bind 127.0.0.1 >/dev/null 2>&1 &)

mkdir -p plugins/Blume
CFG="plugins/Blume/config.yml"
if [ ! -f "$CFG" ]; then
  cp "$ROOT/target/classes/config.yml" "$CFG"
fi
sed -i '' "s|^  url:.*|  url: \"${PACK_URL}\"|" "$CFG"
sed -i '' "s|^  sha1:.*|  sha1: \"${PACK_SHA1}\"|" "$CFG"

printf '%s\n' \
  '#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).' \
  'eula=true' \
  > eula.txt

exec java -Xms1G -Xmx2G -jar paper.jar --nogui "$@"
