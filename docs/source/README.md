# Blume documentation

Static site generator copied from vane-plus. Feature pages are markdown with TOML frontmatter under `content/`.

## Build

### Dependencies

- Node.js
- Python 3 (`pip install -r requirements.txt`)
- A recent Minecraft client jar at `./minecraft-client.jar` (gitignored)

### Steps

```bash
cd docs/source
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
npm install
cp /path/to/minecraft-client.jar ./minecraft-client.jar   # once
./build.sh
```

Output is written to `docs/` (`index.html`, `css/`, `assets/`). Preview with `cd docs && python3 -m http.server 8765`.

Edit markdown in `content/blume-*/`, then rerun `./build.sh`.
