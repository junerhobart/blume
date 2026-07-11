# Contributing

## Plugin

```bash
mvn package
```

For tagged releases, pass the revision:

```bash
mvn -Drevision=$(git describe --tags --always | sed 's/^v//') package
```

## Website

```bash
cd docs
python3 -m http.server 8765
```

# Local testing

Start the server:

```bash
run/scripts/start.sh
```

Remove server data:

```bash
run/scripts/purge.sh
```

# Releasing

Merge to `main`, then tag:

```bash
git tag v0.5.0
git push origin v0.5.0
```

Beta:

```bash
git tag v0.5.0-beta.1
git push origin v0.5.0-beta.1
```

`release.yml` builds the jar, publishes the GitHub Release (jar + packs), and uploads the jar to Modrinth.
