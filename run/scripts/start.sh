#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
RUN="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RUN"

mc=$(mvn -q -f "$ROOT/pom.xml" help:evaluate -Dexpression=minecraft.version -DforceStdout)

needs_purge=false
for jar in paper-*.jar; do
  [ -e "$jar" ] || continue
  case "$jar" in
  paper-${mc}.jar) ;;
  *) needs_purge=true; break ;;
  esac
done
if $needs_purge; then
  printf 'purging run/ (switching to %s)\n' "$mc"
  sh "$(dirname "$0")/purge.sh"
  cd "$RUN"
fi

mkdir -p plugins

sed_inplace() {
  case $(uname -s) in
  Darwin) sed -i '' "$@" ;;
  *) sed -i "$@" ;;
  esac
}

enable_plugin() {
  file="$1"
  url="$2"
  version="$3"
  rm -f "plugins/${file}.disabled"
  curl -fsSL "$url" -o "plugins/$file"
  printf 'ok: %s (%s)\n' "$file" "$version"
}

disable_plugin() {
  file="$1"
  reason="$2"
  if [ -f "plugins/$file" ]; then
    mv "plugins/$file" "plugins/${file}.disabled"
  fi
  printf 'skip: %s (%s)\n' "$file" "$reason"
}

fetch_modrinth() {
  file="$1"
  project="$2"
  versions=$(curl -fsSL -G "https://api.modrinth.com/v2/project/${project}/version" \
    --data-urlencode "game_versions=[\"${mc}\"]" \
    --data-urlencode 'loaders=["paper","bukkit","spigot"]')
  if [ "$(printf '%s' "$versions" | jq 'length')" -eq 0 ]; then
    disable_plugin "$file" "no version for ${mc}"
    return
  fi
  url=$(printf '%s' "$versions" | jq -r '
    .[0] | (.files[] | select(.primary == true) | .url) // .files[0].url
  ')
  version=$(printf '%s' "$versions" | jq -r '.[0].version_number')
  enable_plugin "$file" "$url" "$version"
}

fetch_geyser() {
  file="$1"
  project="$2"
  platform="$3"
  meta=$(curl -fsSL "https://download.geysermc.org/v2/projects/${project}/versions/latest/builds/latest")
  version=$(printf '%s' "$meta" | jq -r '.version')
  url="https://download.geysermc.org/v2/projects/${project}/versions/latest/builds/latest/downloads/${platform}"
  enable_plugin "$file" "$url" "$version"
}

paper_url=$(curl -fsSL "https://fill.papermc.io/v3/projects/paper/versions/${mc}/builds" \
  | jq -r 'sort_by(.downloads["server:default"].url | capture("-(?<n>[0-9]+)\\.jar$").n | tonumber) | last | .downloads["server:default"].url')
curl -fsSL "$paper_url" -o "paper-${mc}.jar"

fetch_modrinth ViaVersion.jar P1OZGk5p
fetch_modrinth ViaBackwards.jar NpvuJQoq
fetch_modrinth Terra.jar FIlZB9L0
fetch_modrinth LuckPerms.jar Vebnzrzj
fetch_geyser Geyser-Spigot.jar geyser spigot
fetch_geyser floodgate-spigot.jar floodgate spigot

patch_bukkit_terra() {
  sed '/^worlds:/,$d' bukkit.yml > bukkit.yml.tmp
  if [ -f plugins/Terra.jar ]; then
    cat >> bukkit.yml.tmp <<'EOF'
worlds:
  world:
    generator: Terra:OVERWORLD
EOF
  else
    cat >> bukkit.yml.tmp <<'EOF'
worlds: {}
EOF
  fi
  mv bukkit.yml.tmp bukkit.yml
}
patch_bukkit_terra

revision=$(git -C "$ROOT" describe --tags --always 2>/dev/null | sed 's/^v//')
revision=${revision:-0.0.0-dev}
mvn -q -f "$ROOT/pom.xml" -Drevision="$revision" clean package -Dminecraft.version="$mc"
version=$(mvn -q -f "$ROOT/pom.xml" -Drevision="$revision" help:evaluate -Dexpression=project.version -DforceStdout)
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

exec java -Xms1G -Xmx2G -jar "paper-${mc}.jar" --nogui "$@"
