#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
RUN="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RUN"
mkdir -p plugins

sed_inplace() {
  case $(uname -s) in
  Darwin) sed -i '' "$@" ;;
  *) sed -i "$@" ;;
  esac
}

fetch() {
  test -f "$2" || curl -fsSL "$1" -o "$2"
}

fetch 'https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar' paper.jar
fetch 'https://cdn.modrinth.com/data/P1OZGk5p/versions/4JQUNqJk/ViaVersion-5.10.1-SNAPSHOT.jar' plugins/ViaVersion.jar
fetch 'https://cdn.modrinth.com/data/NpvuJQoq/versions/gsSGwSIA/ViaBackwards-5.10.1-SNAPSHOT.jar' plugins/ViaBackwards.jar
fetch 'https://cdn.modrinth.com/data/FIlZB9L0/versions/Ufl71nST/Terra-bukkit-6.6.6-BETA%2B451683aff-shaded.jar' plugins/Terra.jar
fetch 'https://cdn.modrinth.com/data/Vebnzrzj/versions/MBSY8toc/LuckPerms-Bukkit-5.5.53.jar' plugins/LuckPerms.jar
fetch 'https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot' plugins/Geyser-Spigot.jar
fetch 'https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot' plugins/floodgate-spigot.jar

sh "$ROOT/scripts/build.sh" -q clean package
version=$(sh "$ROOT/scripts/build.sh" -q help:evaluate -Dexpression=project.version -DforceStdout)
cp "$ROOT/target/Blume-${version}.jar" plugins/Blume.jar

mkdir -p plugins/Blume
CFG="plugins/Blume/config.yml"
DEFAULT_CFG="$ROOT/target/classes/config.yml"
if [ ! -f "$CFG" ]; then
  cp "$DEFAULT_CFG" "$CFG"
fi
if ! grep -q '^  builtin-host:' "$CFG"; then
  sed_inplace "/^  enabled:/a\\
  builtin-host: true\\
  host: \"127.0.0.1\"
" "$CFG"
fi
sed_inplace 's/^  builtin-host:.*/  builtin-host: true/' "$CFG"
sed_inplace 's/^  host:.*/  host: "127.0.0.1"/' "$CFG"

GEYSER_CFG="plugins/Geyser-Spigot/config.yml"
patch_geyser_config() {
  test -f "$GEYSER_CFG" || return 0
  sed_inplace 's/^  auth-type:.*/  auth-type: floodgate/' "$GEYSER_CFG"
  sed_inplace 's/^  enable-custom-content:.*/  enable-custom-content: true/' "$GEYSER_CFG"
  sed_inplace 's/^add-non-bedrock-items:.*/add-non-bedrock-items: true/' "$GEYSER_CFG"
}
patch_geyser_config

printf '%s\n' \
  '#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).' \
  'eula=true' \
  > eula.txt

exec java -Xms1G -Xmx2G -jar paper.jar --nogui "$@"
