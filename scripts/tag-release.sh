#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
version=${1:-}
if [ -z "$version" ]; then
  echo "usage: $0 <version>" >&2
  exit 1
fi

pinned=$(sh "$ROOT/scripts/build.sh" -q help:evaluate -Dexpression=minecraft.version.pinned -DforceStdout)
latest_tag="v${version}"
pinned_tag="v${version}-${pinned}"

git -C "$ROOT" tag "$latest_tag"
git -C "$ROOT" tag "$pinned_tag"

printf '%s\n' "Created ${latest_tag} and ${pinned_tag}"
printf '%s\n' "git push origin ${latest_tag} ${pinned_tag}"
