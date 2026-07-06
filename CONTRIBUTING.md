# Contributing

## Building

```bash
git clone https://github.com/junerhobart/blume.git
cd blume
mvn package
```

The plugin jar is written to `target/Blume-<version>.jar`.

A build also regenerates `docs/blume-pack.zip` from `resourcepack/`.

## Local testing

1. run `'/Users/junehobart/Projects/blume/run/scripts/start.sh'`
2. Join `localhost` and test.
note: you can also run `'/Users/junehobart/Projects/blume/run/scripts/purge.sh'` remove server data (keeps important bits) and `'/Users/junehobart/Projects/blume/run/scripts/sync-plugin.sh'` and then in the server `/blume reload`

## Code guidelines

- Keep names clear and diffs small.
- Avoid unrelated refactors in the same PR.
- Do not commit generated files unless the project already tracks them (for example `docs/blume-pack.zip`).
- 'AI assisted' or 'vibecoded' commits are permitted, but I better not see a +200k line commit.

## License

By contributing, you agree that your contributions will be licensed under the same license as the project ([GPL-3.0](./LICENSE)).

You can dm me on discord `@junehobart` if you have any more questions.
