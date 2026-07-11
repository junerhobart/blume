# Building

## Plugin

```bash
mvn package
```

## Website

```bash
cd docs
python3 -m http.server 8765
```

# Local testing

To start the server: 

<sup>optionally add --pinned tag for my servers version.</sup>
```bash
run/scripts/start.sh
```

To remove files run:
```bash
run/scripts/purge.sh
```