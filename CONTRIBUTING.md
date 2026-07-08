# Building

## Plugin

```bash
git clone https://github.com/junerhobart/blume.git
cd blume
mvn package
```

The plugin jar is written to `target/Blume-<version>.jar`. Resource packs are built to `target/` and embedded in the jar. Releases attach packs to Modrinth.

Custom item textures: `docs/assets/textures/`

## Website

Static files in `docs/`. No build step.

```bash
cd docs
python3 -m http.server 8765
```

- Feature pages: `docs/features/{category}/*.md`
- Demo videos: `docs/assets/videos/*.mp4`
- Images: `docs/assets/`

# Local testing

1. Run `'/Users/junehobart/Projects/blume/run/scripts/start.sh'`
2. Join `localhost` and test.

Note: you can also run `'/Users/junehobart/Projects/blume/run/scripts/purge.sh'` to remove server data (keeps important bits), `'/Users/junehobart/Projects/blume/run/scripts/sync-plugin.sh'`, and then in the server `/blume reload`.

# Code guidelines

- Keep names clear and diffs small.
- Avoid unrelated refactors in the same PR.
- Do not commit generated files unless the project already tracks them.
- 'AI assisted' or 'vibecoded' commits are permitted, but I better not see a +200k line commit.

# License

By contributing, you agree that your contributions will be licensed under the same license as the project ([GPL-3.0](./LICENSE)).

You can dm me on discord `@junehobart` if you have any more questions.
