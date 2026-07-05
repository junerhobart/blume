#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$(dirname "$0")"
mkdir -p plugins

fetch() {
  test -f "$2" || curl -fsSL "$1" -o "$2"
}

fetch 'https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar' paper.jar
fetch 'https://cdn.modrinth.com/data/P1OZGk5p/versions/4JQUNqJk/ViaVersion-5.10.1-SNAPSHOT.jar' plugins/ViaVersion.jar
fetch 'https://cdn.modrinth.com/data/NpvuJQoq/versions/gsSGwSIA/ViaBackwards-5.10.1-SNAPSHOT.jar' plugins/ViaBackwards.jar
fetch 'https://cdn.modrinth.com/data/FIlZB9L0/versions/Ufl71nST/Terra-bukkit-6.6.6-BETA%2B451683aff-shaded.jar' plugins/Terra.jar
fetch 'https://cdn.modrinth.com/data/Vebnzrzj/versions/MBSY8toc/LuckPerms-Bukkit-5.5.53.jar' plugins/LuckPerms.jar

(cd "$ROOT" && mvn -q package)
cp "$ROOT/target/Blume-0.2.jar" plugins/Blume.jar

printf '%s\n' \
  '#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).' \
  'eula=true' \
  > eula.txt

exec java -Xms1G -Xmx2G -jar paper.jar --nogui "$@"
