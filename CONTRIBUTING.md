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

Squash merge PRs to `main`. The squash commit title sets the release type and changelog section:

| PR / squash title prefix | Release label | Changelog section |
|--------------------------|---------------|-------------------|
| `Release:` | RELEASE | Features |
| `Patch:` | PATCH | Improvements |
| `Hotfix:` | HOTFIX | Fixes |
| `feature:` / `feat:` | (from HEAD at tag time) | Features |
| `fix:` | (from HEAD at tag time) | Fixes |
| `improve:` / `perf:` / `tweak:` | (from HEAD at tag time) | Improvements |

Put player-facing bullets in the PR description; squash merge copies them into the commit body.

`chore:`, `refactor:`, `ci:`, `docs:`, `test:`, `build:`, `style:`, and `merge:` commits are omitted from release notes.

Tag `main` after merging:

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
