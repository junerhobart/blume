#!/bin/sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
version=${1:-}
if [ -z "$version" ]; then
  echo "usage: $0 <version>" >&2
  exit 1
fi

tag="v${version}"
git -C "$ROOT" tag "$tag"

printf '%s\n' "Created ${tag}"
printf '%s\n' "git push origin ${tag}"
