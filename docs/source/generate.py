#!/usr/bin/env python3

import argparse
import json
import markdown
import os
import re
import shutil
import subprocess
import tempfile
import toml
import yaml
import zipfile
from dataclasses import dataclass, field
from functools import reduce
from glob import glob
from pathlib import Path
from typing import Any

markdown_item_pattern = re.compile(r'{{ item:(\S*) }}')

@dataclass
class Feature:
    loaded_from: str
    metadata: dict[str, Any]
    html_content: str

@dataclass
class Context:
    build_path: Path = Path("build")
    assets_path: Path = Path("build/assets")
    content_dir: Path = Path("content")
    plugins_dir: Path = Path("plugins")
    client_jar: zipfile.ZipFile = None # type: ignore
    loaded_minecraft_asset_icons: dict[str, str] = field(default_factory=dict)
    content_settings: dict[str, Any] = field(default_factory=dict)
    features: dict[str, list[Feature]] = field(default_factory=dict)
    categories: dict[str, Any] = field(default_factory=dict)
    templates: dict[str, str] = field(default_factory=dict)
    required_minecraft_assets: set[str] = field(default_factory=set)
    required_project_assets: set[tuple[str, str]] = field(default_factory=set)

context = Context()

def load_templates() -> dict[str, str]:
    templates = {}
    for i in glob("templates/*.html"):
        with open(i, "r") as f:
            templates[os.path.basename(i).removesuffix(".html")] = f.read()
    return templates

def item_to_inline_icon(resource_key: str) -> str:
    html = context.templates["inline-icon"].strip()
    html = html.replace("{{ icon }}", item_to_icon(resource_key))
    html = html.replace("{{ name }}", resource_key)
    return html

def load_feature_markdown(markdown_file: Path, default_slug: str) -> Feature:
    with open(markdown_file, "r") as f:
        raw = f.read()

    metadata = None
    content = raw
    if raw.startswith("---"):
        parts = raw.split("---", 2)
        if len(parts) >= 3:
            metadata = yaml.safe_load(parts[1])
            content = parts[2].strip()
    else:
        try:
            metadata_text, content = raw.split("```\n---\n", maxsplit=1)
            metadata = toml.loads(metadata_text.removeprefix("```toml"))
            content = content.strip()
        except ValueError:
            print(f"Missing or invalid metadata section in {markdown_file}.")
            exit(1)

    if not metadata:
        print(f"Missing or invalid metadata section in {markdown_file}.")
        exit(1)
    if "slug" not in metadata:
        metadata["slug"] = default_slug
    metadata["module"] = markdown_file.parent.name.removeprefix("vane-")

    if "icon" in metadata:
        if ":" in metadata["icon"] and metadata["icon"].endswith(".png"):
            namespace, key = metadata["icon"].split(":", maxsplit=1)
            if namespace == "minecraft":
                metadata["icon"] = "assets/minecraft/" + key
                context.required_minecraft_assets.add(metadata["icon"])
            elif namespace.startswith("vane-"):
                metadata["icon"] = f"assets/{namespace}/{key}"
                context.required_project_assets.add((namespace, key))
                print("a", (namespace, key))
        else:
            metadata["icon"] = item_to_icon(metadata["icon"])
    else:
        if "itemlike" not in metadata:
            raise ValueError("metadata contains no icon definition. This is only possible if 'itemlike' is set to determine the icon from the recipe.")

    content = markdown_item_pattern.sub(lambda match: item_to_inline_icon(match.group(1)), content)
    if "icon" not in metadata and metadata.get("itemlike", "").startswith("vane-enchantments:enchantment_"):
        metadata["icon"] = item_to_icon("minecraft:enchanted_book")
    return Feature(loaded_from=str(markdown_file),
                   metadata=metadata,
                   html_content=markdown.markdown(content, extensions=['tables']))

def replace_category_variables(s: str, category: dict[str, Any]):
    s = s.replace("{{ category.id }}", category["id"])
    s = s.replace("{{ category.title }}", category["title"])
    return s

def deep_get(dictionary, keys):
    return reduce(lambda d, key: d[key], keys.split("."), dictionary)

def get_from_config(resource_key: str, default=None) -> dict[str, Any]:
    try:
        namespace, key = resource_key.split(":", maxsplit=1)
        with open(context.plugins_dir / namespace / "config.yml") as f:
            config = yaml.safe_load(f)
        return deep_get(config, key)
    except KeyError:
        if default is not None:
            return default
        print(f"Error while trying to get {resource_key} from config")
        raise
    except:
        print(f"Error while trying to get {resource_key} from config")
        raise

def collect_jar_asset(asset: str) -> None:
    out = context.assets_path / asset.removeprefix("assets/")
    try:
        data = context.client_jar.read(asset)
    except KeyError:
        print(f"\x1b[1;33mwarning:\x1b[m missing asset: {asset}")
        out.unlink(missing_ok=True)
        return
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)

def _render_block(texture_front: str, texture_side: str, texture_top: str, output: Path):
    size = 128
    if not output.exists():
        subprocess.run(["convert", "-size", f"{size}x{size}", "xc:transparent",
            "(", texture_top, "-interpolate", "Nearest", "-filter", "point", "-resize", "3200%",
                "-alpha", "set", "-virtual-pixel", "transparent", "+distort", "Perspective",
                    f"{int(0 * 512)},{int(0 * 512)} {int(150/300 * size)},{int(  0/300 * size)} \
                    {int(1 * 512)},{int(0 * 512)} {int(284/300 * size)},{int( 68/300 * size)} \
                    {int(0 * 512)},{int(1 * 512)} {int( 16/300 * size)},{int( 68/300 * size)} \
                    {int(1 * 512)},{int(1 * 512)} {int(150/300 * size)},{int(135/300 * size)}", ")",
            "(", texture_side, "-interpolate", "Nearest", "-filter", "point", "-resize", "3200%",
                "-alpha", "set", "-virtual-pixel", "transparent", "+distort", "Perspective",
                    f"{int(0 * 512)},{int(0 * 512)} {int( 16/300 * size)},{int( 68/300 * size)} \
                    {int(1 * 512)},{int(0 * 512)} {int(150/300 * size)},{int(135/300 * size)} \
                    {int(0 * 512)},{int(1 * 512)} {int( 16/300 * size)},{int(232/300 * size)} \
                    {int(1 * 512)},{int(1 * 512)} {int(150/300 * size)},{int(300/300 * size)}", ")",
            "(", texture_front, "-interpolate", "Nearest", "-filter", "point", "-resize", "3200%",
                "-alpha", "set", "-virtual-pixel", "transparent", "+distort", "Perspective",
                    f"{int(0 * 512)},{int(0 * 512)} {int(150/300 * size)},{int(135/300 * size)} \
                    {int(1 * 512)},{int(0 * 512)} {int(284/300 * size)},{int( 68/300 * size)} \
                    {int(0 * 512)},{int(1 * 512)} {int(150/300 * size)},{int(300/300 * size)} \
                    {int(1 * 512)},{int(1 * 512)} {int(284/300 * size)},{int(232/300 * size)}", ")",
            "-background", "transparent", "-compose", "plus", "-layers", "flatten", "+repage", str(output)
        ], check=True)

def render_cube_all(key: str, model: dict[str, Any]) -> str:
    print(f"Rendering cube_all {key}...")
    texture = model['textures']['all'].removeprefix('minecraft:')

    with tempfile.NamedTemporaryFile() as ftmp:
        asset = f"assets/minecraft/textures/{texture}.png"
        ftmp.write(context.client_jar.read(asset))
        ftmp.flush()

        icon = f"assets/minecraft/blocks/{key}.png"
        out = context.assets_path / icon.removeprefix("assets/")
        out.parent.mkdir(parents=True, exist_ok=True)
        _render_block(ftmp.name, ftmp.name, ftmp.name, out)

    return icon

def render_orientable(key: str, model: dict[str, Any]) -> str:
    print(f"Rendering orientable {key}...")
    texture_front = model['textures']['front'].removeprefix('minecraft:')
    texture_side = model['textures']['side'].removeprefix('minecraft:')
    texture_top = model['textures']['top'].removeprefix('minecraft:')

    with tempfile.NamedTemporaryFile() as tmp_front, tempfile.NamedTemporaryFile() as tmp_side, tempfile.NamedTemporaryFile() as tmp_top:
        tmp_front.write(context.client_jar.read(f"assets/minecraft/textures/{texture_front}.png"))
        tmp_front.flush()
        tmp_side.write(context.client_jar.read(f"assets/minecraft/textures/{texture_side}.png"))
        tmp_side.flush()
        tmp_top.write(context.client_jar.read(f"assets/minecraft/textures/{texture_top}.png"))
        tmp_top.flush()

        icon = f"assets/minecraft/blocks/{key}.png"
        out = context.assets_path / icon.removeprefix("assets/")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.parent.mkdir(parents=True, exist_ok=True)
        _render_block(tmp_front.name, tmp_side.name, tmp_top.name, out)

    return icon

SPAWN_EGG_COLORS: dict[str, tuple[int, int, int, int, int, int]] = {
    "bee_spawn_egg": (0xED, 0xC3, 0x43, 0x43, 0x24, 0x1B),
    "cow_spawn_egg": (0x44, 0x36, 0x26, 0xA1, 0xA1, 0xA1),
}

def _tint_grayscale(rows: list[list[tuple[int, int, int, int]]], rgb: tuple[int, int, int]) -> list[list[tuple[int, int, int, int]]]:
    red, green, blue = rgb
    return [
        [
            (red * pixel[0] // 255, green * pixel[0] // 255, blue * pixel[0] // 255, pixel[3])
            if pixel[3] else (0, 0, 0, 0)
            for pixel in row
        ]
        for row in rows
    ]

def _alpha_over(
    bottom: list[list[tuple[int, int, int, int]]],
    top: list[list[tuple[int, int, int, int]]],
) -> list[list[tuple[int, int, int, int]]]:
    height = len(bottom)
    width = len(bottom[0])
    out: list[list[tuple[int, int, int, int]]] = []
    for y in range(height):
        row: list[tuple[int, int, int, int]] = []
        for x in range(width):
            br, bg, bb, ba = bottom[y][x]
            tr, tg, tb, ta = top[y][x]
            if ta == 0:
                row.append((br, bg, bb, ba))
                continue
            alpha = ta / 255
            row.append((
                int(tr * alpha + br * (1 - alpha)),
                int(tg * alpha + bg * (1 - alpha)),
                int(tb * alpha + bb * (1 - alpha)),
                max(ba, ta),
            ))
        out.append(row)
    return out

def render_spawn_egg(key: str, base_rgb: tuple[int, int, int], spot_rgb: tuple[int, int, int]) -> str:
    icon = f"assets/minecraft/items/{key}.png"
    out = context.assets_path / icon.removeprefix("assets/")
    if out.exists():
        return icon

    collect_jar_asset("assets/minecraft/textures/item/spawn_egg.png")
    collect_jar_asset("assets/minecraft/textures/item/spawn_egg_overlay.png")
    base_path = context.assets_path / "minecraft/textures/item/spawn_egg.png"
    overlay_path = context.assets_path / "minecraft/textures/item/spawn_egg_overlay.png"
    base_w, base_h, base_rows = _read_png_rgba(base_path)
    overlay_w, overlay_h, overlay_rows = _read_png_rgba(overlay_path)
    if base_w != overlay_w or base_h != overlay_h:
        raise ValueError("spawn egg textures size mismatch")

    composite = _alpha_over(_tint_grayscale(base_rows, base_rgb), _tint_grayscale(overlay_rows, spot_rgb))
    out.parent.mkdir(parents=True, exist_ok=True)
    _write_png_rgba(out, base_w, base_h, composite)
    return icon

def _committed_minecraft_icon(key: str) -> str | None:
    for candidate in (
        f"assets/minecraft/textures/item/{key}.png",
        f"assets/minecraft/blocks/{key}.png",
        f"assets/minecraft/textures/block/{key}.png",
        f"assets/minecraft_special/{key}.png",
    ):
        if (context.assets_path / candidate.removeprefix("assets/")).exists():
            return candidate
    return None

def minecraft_asset_icon(key: str) -> str:
    if key in context.loaded_minecraft_asset_icons:
        return context.loaded_minecraft_asset_icons[key]

    if context.client_jar is None:
        icon = _committed_minecraft_icon(key)
        if icon is None:
            icon = f"assets/minecraft/textures/item/barrier.png"
            print(f"\x1b[1;33mwarning:\x1b[m missing committed icon for minecraft:{key}")
        context.loaded_minecraft_asset_icons[key] = icon
        return icon

    icon = None
    item_model = None
    msg = ""
    try:
        item_model = json.loads(context.client_jar.read(f"assets/minecraft/models/item/{key}.json"))
    except KeyError:
        msg = f"unknown item minecraft:{key}"

    if item_model is not None:
        item_parent = item_model["parent"].removeprefix("minecraft:")
        if item_parent in ["item/generated", "item/handheld"]:
            texture = item_model["textures"]["layer0"].removeprefix('minecraft:')
            icon = f"assets/minecraft/textures/{texture}.png"
            collect_jar_asset(icon)
        elif item_parent == "item/template_spawn_egg":
            colors = SPAWN_EGG_COLORS.get(key)
            if colors is not None:
                icon = render_spawn_egg(key, colors[0:3], colors[3:6])
            else:
                msg = f"unknown spawn egg colors for minecraft:{key}"
        elif item_parent.startswith("block/"):
            block = item_parent.removeprefix("block/")
            block_model = json.loads(context.client_jar.read(f"assets/minecraft/models/block/{block}.json"))
            block_parent = block_model['parent'].removeprefix("minecraft:")
            if block_parent == "block/cube_all":
                icon = render_cube_all(key, block_model)
            elif block_parent == "block/orientable":
                icon = render_orientable(key, block_model)
            else:
                msg = f"unknown block model type {block_parent} in item minecraft:{key}"
        else:
            msg = f"unknown item model type {item_parent} in item minecraft:{key}"

    if icon is None:
        icon = f"assets/minecraft_special/{key}.png"
        out = context.assets_path / icon.removeprefix("assets/")
        print(f"using {icon} for {msg}")
        if not out.exists():
            print(f"[1;33mwarning:[m missing asset: {icon}")

    context.loaded_minecraft_asset_icons[key] = icon
    return icon

def item_to_icon(item: str) -> str:
    resource_key = item.split("{")[0]
    if not resource_key.startswith("#"):
        resource_key = resource_key.split("#")[0]
    namespace, key = resource_key.split(":", maxsplit=1)

    # FIXME: contains hardcoded overrides
    if resource_key == "minecraft:leather_chestplate":
        return f"assets/minecraft_special/leather_chestplate.png"
    elif namespace == "minecraft":
        return minecraft_asset_icon(key)
    elif namespace == "blume":
        return f"assets/blume/textures/item/{key}.png"
    elif namespace.startswith("vane"):
        if resource_key == "vane_trifles:north_compass":
            context.required_project_assets.add(("vane-trifles", "items/north_compass_16.png"))
            return f"assets/vane-trifles/items/north_compass_16.png"
        else:
            namespace = namespace.replace("_", "-")
            key = f"items/{key}.png"
            icon = f"{namespace}/{key}"
            context.required_project_assets.add((namespace, key))
            return "assets/" + icon
    elif resource_key == "#minecraft:beds":
        return f"assets/minecraft_special/bed.png"
    elif resource_key == "#minecraft:shulker_boxes":
        return f"assets/minecraft_special/shulker_box.png"
    elif resource_key == "#minecraft:saplings":
        return minecraft_asset_icon("oak_sapling")
    else:
        print(f"[1;33mwarning:[m unknown icon for item: {item}")
    return f"assets/minecraft/textures/item/barrier.png"

def remove_lines_containing(where: str, what: str):
    return "".join(line for line in where.splitlines(keepends=True) if what not in line)

def render_recipe(feature: Feature, recipe: dict[str, Any]) -> str:
    if recipe["type"] == "shaped":
        html = context.templates["shaped-recipe"]

        shape = "".join([row.ljust(3) for row in (recipe["shape"] + 3 * [""])[:3]])
        ingredients = {str(k):v for k,v in recipe["ingredients"].items()}
        assert len(shape) == 9
        for i,c in enumerate(shape):
            tag = f"{{{{ recipe.ingredients.{i} }}}}"
            if c == " ":
                html = remove_lines_containing(html, tag)
            else:
                ingredient = ingredients[c]
                html = html.replace(tag, item_to_icon(ingredient))
                html = html.replace(f"{{{{ recipe.ingredients.{i}.name }}}}", ingredient)
    elif recipe["type"] == "shapeless":
        html = context.templates["shaped-recipe"] # Abuse the template for shapeless recipes

        for i,ingredient in enumerate(recipe["ingredients"] + (9 - len(recipe["ingredients"])) * [None]):
            tag = f"{{{{ recipe.ingredients.{i} }}}}"
            if ingredient is None:
                html = remove_lines_containing(html, tag)
            else:
                html = html.replace(tag, item_to_icon(ingredient))
                html = html.replace(f"{{{{ recipe.ingredients.{i}.name }}}}", ingredient)
    elif recipe["type"] == "smithing":
        html = context.templates["smithing-recipe"]

        html = html.replace("{{ recipe.base }}", item_to_icon(recipe["base"]))
        html = html.replace("{{ recipe.base.name }}", recipe["base"])

        html = html.replace("{{ recipe.addition }}", item_to_icon(recipe["addition"]))
        html = html.replace("{{ recipe.addition.name }}", recipe["addition"])
    else:
        raise ValueError(f"cannot render recipe of unknown type {recipe['type']}")

    result_icon = item_to_icon(recipe["result"])
    html = html.replace("{{ recipe.result }}", result_icon)
    html = html.replace("{{ recipe.result.name }}", recipe["result"])
    if "icon" not in feature.metadata:
        feature.metadata["icon"] = result_icon
    return html

def render_enchantment_sources(feature: Feature) -> str:
    """Render acquisition sources for enchantments as plain text from the live config."""
    try:
        namespace, key = feature.metadata["itemlike"].split(":", maxsplit=1)
        get_from_config(f"{namespace}:{key}.enabled")
    except Exception:
        return ""

    source_names = [
        ("enchanting_table", "Enchanting Table"),
        ("villager_trades", "Villager Trades"),
        ("fishing_loot", "Fishing Loot"),
        ("structure_loot", "Structure Loot"),
    ]
    sources = []
    for source, label in source_names:
        try:
            active = get_from_config(f"{namespace}:{key}.{source}")
        except Exception:
            active = False
        if active:
            sources.append(label)
    if not sources:
        return ""
    return f'<p class="font-minecraft text-sm mc-text-gold">Available from: {", ".join(sources)}.</p>'

def render_loot_table(loot: dict[str, Any]) -> str:
    html = context.templates["loot-table"]
    loot_badge = context.templates["badge"]

    table_rows = []
    for table in loot.values():
        where_rows = []
        for where in table["tables"]:
            where = where.removeprefix("minecraft:chests/")
            badge_html = '<div class="inline-flex m-1">'
            badge_html = badge_html + loot_badge.replace("{{ text }}", where)
            badge_html = badge_html + '</div>'
            where_rows.append(badge_html)
        where_rows = "\n".join(where_rows)

        col_html = context.templates["loot-table-col-where"]
        col_html = col_html.replace("{{ loot_table.n_rows }}", str(len(table["items"])))
        col_html = col_html.replace("{{ loot_table.where.rows }}", where_rows)

        for i,item in enumerate(table["items"]):
            row_html = context.templates["loot-table-row"]
            row_html = row_html.replace("{{ loot_table.row.item.name }}", item["item"])
            row_html = row_html.replace("{{ loot_table.row.item.icon }}", item_to_icon(item["item"]))
            row_html = row_html.replace("{{ loot_table.row.chance }}", f"{item['chance'] * 100.0:.2f}%")
            if item['amount_min'] == item['amount_max']:
                amount = str(item['amount_min'])
            else:
                amount = f"{item['amount_min']} - {item['amount_max']}"
            row_html = row_html.replace("{{ loot_table.row.amount }}", amount)
            row_html = row_html.replace("{{ loot_table.col.where }}", col_html if i == 0 else "")
            table_rows.append(row_html)

    html = html.replace("{{ loot_table.rows }}", "\n".join(table_rows))
    return html

def video_for_feature(markdown_path: Path) -> Path | None:
    stem = markdown_path.stem
    video = context.content_dir / "media" / f"{stem}.mp4"
    return video if video.exists() else None

def render_feature_video(video_path: Path) -> str:
    src = f"media/{video_path.name}"
    title = video_path.stem.replace("-", " ")
    return (
        f'<figure class="feature-video">'
        f'<video class="feature-video-player w-full mc-border" controls playsinline preload="metadata">'
        f'<source src="{src}" type="video/mp4">'
        f'</video>'
        f'<figcaption class="sr-only">Demo: {title}</figcaption>'
        f'</figure>'
    )

def render_feature(feature: Feature, index: int, count: int) -> str:
    html = context.templates["feature"]

    html = html.replace("{{ accordion.heading }}", "")
    html = html.replace("{{ accordion.body }}", "")

    if "itemlike" in feature.metadata:
        recipes = [render_recipe(feature, r) for r in get_from_config(feature.metadata["itemlike"] + ".recipes", default={}).values()]
        if len(recipes) > 0:
            html = html.replace("{{ feature.recipes }}", "\n".join(recipes))
        else:
            html = remove_lines_containing(html, "{{ feature.recipes }}")

        loot = get_from_config(feature.metadata["itemlike"] + ".loot", default={})
        if len(loot) > 0:
            html = html.replace("{{ feature.loot }}", render_loot_table(loot))
        else:
            html = remove_lines_containing(html, "{{ feature.loot }}")
    else:
        html = remove_lines_containing(html, "{{ feature.recipes }}")
        html = remove_lines_containing(html, "{{ feature.loot }}")

    if "icon" not in feature.metadata:
        print(f"[1;33mwarning:[m could not infer icon")

    if "icon_overlay" in feature.metadata:
        feature.metadata["icon_overlay"] = item_to_icon(feature.metadata["icon_overlay"])
    else:
        html = remove_lines_containing(html, "{{ feature.metadata.icon_overlay }}")

    for k,v in feature.metadata.items():
        html = html.replace(f"{{{{ feature.metadata.{k} }}}}", str(v) if v is not None else "")

    html = html.replace("{{ feature.html_content }}", feature.html_content)

    video_path = video_for_feature(Path(feature.loaded_from))
    if video_path is not None:
        html = html.replace("{{ feature.video }}", render_feature_video(video_path))
    else:
        html = remove_lines_containing(html, "{{ feature.video }}")

    enchantment_sources = ""
    if "itemlike" in feature.metadata and feature.metadata["itemlike"].startswith("vane-enchantments:enchantment_"):
        enchantment_sources = render_enchantment_sources(feature)
    html = html.replace("{{ feature.sources }}", enchantment_sources)

    enabled_default = feature.metadata.get("enabled_by_default", True)
    enabled = str(enabled_default).lower() == "true"
    status_class = "mc-text-gold" if enabled else "mc-text-gray"
    status_text = "Enabled By Default" if enabled else "Not Enabled By Default"
    status_footer = f'<div class="mt-4 pt-3 border-t border-[#2b2b2c] text-sm"><span class="font-minecraft text-xl {status_class}">{status_text}</span></div>'
    html = html.replace("{{ feature.status_footer }}", status_footer)
    return html

def render_category_content(category: dict[str, Any]) -> str:
    # Pre-render features
    features_html = []
    fs = context.features[category["id"]]
    for i,f in enumerate(fs):
        print(f"Rendering feature {f.loaded_from}")
        features_html.append(render_feature(f, i, len(fs)))

    html = context.templates["category"]
    html = replace_category_variables(html, category)
    html = html.replace("{{ features }}", "\n".join(features_html))
    return html

def generate_docs() -> None:
    # Load features
    for c in context.content_settings["categories"]:
        fs = []
        for i in c["content"]:
            print(f"Processing {i}")
            fs.append(load_feature_markdown(context.content_dir / i,
            default_slug="feature-" + i.removesuffix(".md").replace("/", "--")))
        context.features[c["id"]] = fs

    index_content = context.templates["index"]
    index_content = index_content.replace("{{ each_category_content }}", "\n".join(
        render_category_content(c) for c in context.content_settings["categories"]
    ))

    print(f"Writing index.html")
    with open(context.build_path / "index.html", "w") as f:
        f.write(index_content)

# Block palette #75501 - https://www.blockpalettes.com/palette/75501
BLOCK_TEXTURES = [
    "assets/minecraft/textures/block/moss_block.png",
    "assets/minecraft/textures/block/stripped_spruce_log.png",
    "assets/minecraft/textures/block/polished_andesite.png",
    "assets/minecraft/textures/block/cyan_terracotta.png",
    "assets/minecraft/textures/block/spruce_planks.png",
    "assets/minecraft/textures/block/deepslate_bricks.png",
]
MC_TILE_PX = 128

TRAPDOOR_SOUNDS = [
    "assets/minecraft/sounds/block/wooden_trapdoor/open1.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/open2.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/open3.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/open4.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/open5.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/close1.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/close2.ogg",
    "assets/minecraft/sounds/block/wooden_trapdoor/close3.ogg",
]

def collect_trapdoor_sounds() -> None:
    bundled = Path(__file__).resolve().parent / "assets"
    for asset in TRAPDOOR_SOUNDS:
        out = context.assets_path / asset.removeprefix("assets/")
        out.parent.mkdir(parents=True, exist_ok=True)
        if context.client_jar is not None:
            try:
                out.write_bytes(context.client_jar.read(asset))
                continue
            except KeyError:
                pass
        src = bundled / asset.removeprefix("assets/")
        if src.exists():
            shutil.copy2(src, out)
        else:
            print(f"\x1b[1;33mwarning:\x1b[m missing trapdoor sound: {asset}")

def collect_static_assets() -> None:
    collect_trapdoor_sounds()

def upscaled_tile_path(asset: str) -> Path:
    name = Path(asset).name
    return context.assets_path / "minecraft/textures/block/tiles" / name

def _png_chunk(tag: bytes, body: bytes) -> bytes:
    import struct
    import zlib
    crc = zlib.crc32(tag + body) & 0xFFFFFFFF
    return struct.pack(">I", len(body)) + tag + body + struct.pack(">I", crc)

def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c

def _row_bytes(width: int, bit_depth: int, color_type: int) -> int:
    if color_type == 6:
        return width * 4
    if color_type == 2:
        return width * 3
    return (width * bit_depth + 7) // 8

def _filter_bpp(bit_depth: int, color_type: int) -> int:
    if color_type == 6:
        return 4
    if color_type == 2:
        return 3
    return 1

def _unfilter_scanlines(
    raw: bytes,
    width: int,
    height: int,
    bit_depth: int,
    color_type: int,
) -> bytes:
    bpp = _filter_bpp(bit_depth, color_type)
    row_bytes = _row_bytes(width, bit_depth, color_type)
    stride = row_bytes + 1
    out = bytearray(height * row_bytes)
    prev = bytearray(row_bytes)
    for y in range(height):
        start = y * stride
        filter_type = raw[start]
        row = bytearray(raw[start + 1:start + stride])
        if filter_type == 1:
            for i in range(row_bytes):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + left) & 0xFF
        elif filter_type == 2:
            for i in range(row_bytes):
                row[i] = (row[i] + prev[i]) & 0xFF
        elif filter_type == 3:
            for i in range(row_bytes):
                left = row[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + ((left + prev[i]) // 2)) & 0xFF
        elif filter_type == 4:
            for i in range(row_bytes):
                left = row[i - bpp] if i >= bpp else 0
                up = prev[i]
                up_left = prev[i - bpp] if i >= bpp else 0
                row[i] = (row[i] + _paeth(left, up, up_left)) & 0xFF
        out[y * row_bytes:(y + 1) * row_bytes] = row
        prev = row
    return bytes(out)

def _read_png_rgba(path: Path) -> tuple[int, int, list[list[tuple[int, int, int, int]]]]:
    import struct
    import zlib

    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"not a png: {path}")

    width = height = bit_depth = color_type = 0
    palette: list[tuple[int, int, int]] = []
    alpha: dict[int, int] = {}
    idat = bytearray()
    pos = 8
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        pos += 4
        chunk_type = data[pos:pos + 4]
        pos += 4
        chunk = data[pos:pos + length]
        pos += length + 4
        if chunk_type == b"IHDR":
            width, height, bit_depth, color_type = struct.unpack(">IIBB", chunk[:10])
        elif chunk_type == b"PLTE":
            palette = [(chunk[i], chunk[i + 1], chunk[i + 2]) for i in range(0, len(chunk), 3)]
        elif chunk_type == b"tRNS":
            for i, value in enumerate(chunk):
                alpha[i] = value
        elif chunk_type == b"IDAT":
            idat.extend(chunk)
        elif chunk_type == b"IEND":
            break

    raw = _unfilter_scanlines(
        zlib.decompress(bytes(idat)),
        width,
        height,
        bit_depth,
        color_type,
    )
    row_bytes = _row_bytes(width, bit_depth, color_type)
    rows: list[list[tuple[int, int, int, int]]] = []

    for y in range(height):
        row = raw[y * row_bytes:(y + 1) * row_bytes]
        pixels: list[tuple[int, int, int, int]] = []
        if color_type == 6:
            for x in range(width):
                i = x * 4
                pixels.append((row[i], row[i + 1], row[i + 2], row[i + 3]))
        elif color_type == 3 and bit_depth == 4:
            for x in range(width):
                byte = row[x // 2]
                index = (byte >> 4) if x % 2 == 0 else (byte & 0x0F)
                r, g, b = palette[index]
                pixels.append((r, g, b, alpha.get(index, 255)))
        elif color_type == 3 and bit_depth == 8:
            for x in range(width):
                index = row[x]
                r, g, b = palette[index]
                pixels.append((r, g, b, alpha.get(index, 255)))
        else:
            raise ValueError(f"unsupported png format in {path}: type={color_type} depth={bit_depth}")
        rows.append(pixels)

    return width, height, rows

def _write_png_rgba(path: Path, width: int, height: int, rows: list[list[tuple[int, int, int, int]]]) -> None:
    import struct
    import zlib

    raw = bytearray()
    for row in rows:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend((r, g, b, a))

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(_png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)))
    png.extend(_png_chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(_png_chunk(b"IEND", b""))
    path.write_bytes(png)

def upscale_png_nearest(src: Path, dst: Path, out_size: int) -> None:
    width, height, rows = _read_png_rgba(src)
    if width != height:
        raise ValueError(f"expected square texture: {src}")
    scale = out_size // width
    if scale * width != out_size:
        raise ValueError(f"cannot scale {width}px texture to {out_size}px")

    out_rows: list[list[tuple[int, int, int, int]]] = []
    for y in range(height):
        scaled_row: list[tuple[int, int, int, int]] = []
        for x in range(width):
            scaled_row.extend([rows[y][x]] * scale)
        for _ in range(scale):
            out_rows.append(scaled_row)

    dst.parent.mkdir(parents=True, exist_ok=True)
    _write_png_rgba(dst, out_size, out_size, out_rows)

def write_upscaled_tile(src: Path, dst: Path) -> None:
    convert = shutil.which("magick") or shutil.which("convert")
    if convert is not None:
        dst.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([
            convert, str(src),
            "-interpolate", "Nearest", "-filter", "point",
            "-resize", f"{MC_TILE_PX}x{MC_TILE_PX}!",
            str(dst),
        ], check=True)
        return
    upscale_png_nearest(src, dst, MC_TILE_PX)

def collect_block_textures() -> None:
    print(f"Collecting {len(BLOCK_TEXTURES)} Minecraft block textures from client jar...")
    for asset in BLOCK_TEXTURES:
        collect_jar_asset(asset)
        src = context.assets_path / asset.removeprefix("assets/")
        if not src.exists():
            continue
        write_upscaled_tile(src, upscaled_tile_path(asset))

def collect_assets() -> None:
    print(f"Collecting {len(context.required_minecraft_assets)} required assets from client jar...")
    for asset in context.required_minecraft_assets:
        collect_jar_asset(asset)

    collect_block_textures()

    print(f"Collecting {len(context.required_project_assets)} required assets from this project...")
    project_root = Path(__file__).resolve().parent.parent.parent
    for namespace, key in context.required_project_assets:
        out = context.assets_path / namespace / key
        out.parent.mkdir(parents=True, exist_ok=True)
        module_dir = namespace.removeprefix("vane-")
        asset_name = Path(key).name
        candidates = [
            project_root / "modules" / module_dir / "src/main/resources" / key,
            project_root / "modules" / module_dir / "src/main/resources/resource_pack/assets" / namespace.replace("-", "_") / "textures/item" / asset_name,
        ]
        src = next((c for c in candidates if c.exists()), None)
        if src is None:
            print(f"\x1b[1;33mwarning:\x1b[m missing project asset: {namespace}/{key}")
            continue
        shutil.copy2(src, out)

def collect_media() -> None:
    media_dir = context.content_dir / "media"
    if not media_dir.is_dir():
        return
    out_dir = context.build_path / "media"
    videos = sorted(media_dir.glob("*.mp4"))
    if not videos:
        return
    out_dir.mkdir(parents=True, exist_ok=True)
    print(f"Copying {len(videos)} demo videos to {out_dir}...")
    for video in videos:
        shutil.copy2(video, out_dir / video.name)

def main():
    parser = argparse.ArgumentParser(description="Generates the documentation page.")
    parser.add_argument('--client-jar', type=str,
            help="Specifies a minecraft client jar file from which required assets will be extracted.")
    parser.add_argument('--plugins-dir', default="plugins", type=str,
            help="Specifies a plugins/ directory from where vane's generated config can be read (for recipes and loot).")
    parser.add_argument('--content-dir', default="content", type=str,
            help="Directory containing feature markdown and media. (default: 'content')")
    parser.add_argument('--content-toml', default="content.toml", type=str,
            help="Path to content.toml listing categories and features. (default: 'content.toml')")
    parser.add_argument('-o', '--output-dir', dest='output_dir', default="build", type=str,
            help="Specifies the output directory for the documentation. (default: 'build')")
    args = parser.parse_args()

    context.build_path = Path(args.output_dir)
    context.build_path.mkdir(parents=True, exist_ok=True)

    context.assets_path = context.build_path / "assets"
    context.assets_path.mkdir(parents=True, exist_ok=True)

    context.content_dir = Path(args.content_dir)
    context.plugins_dir = Path(args.plugins_dir)

    context.content_settings = toml.load(args.content_toml)
    assert "categories" in context.content_settings
    context.categories = { c["id"]: c for c in context.content_settings["categories"] }
    context.templates = load_templates()

    if args.client_jar:
        with zipfile.ZipFile(args.client_jar) as client_jar:
            context.client_jar = client_jar
            generate_docs()
            collect_assets()
            collect_media()
            collect_static_assets()
    else:
        generate_docs()
        collect_media()
        collect_static_assets()

    # Ensure that all content documents are included as a safety check
    used = set(f for cat in context.content_settings["categories"] for f in cat["content"])
    content_glob = str(context.content_dir / "**" / "*.md")
    available = set(Path(p).relative_to(context.content_dir).as_posix() for p in glob(content_glob, recursive=True))
    missing = available - used
    if len(missing) > 0:
        print(f"[1;33mwarning:[m unused content templates: {', '.join(missing)}")

if __name__ == "__main__":
    main()