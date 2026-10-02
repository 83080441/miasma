"""Turn sprites-temp into loose files in sprites/. Maps with a JSON are cut by name."""

import json
import re
import shutil
from pathlib import Path

from PIL import Image, ImageSequence

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "sprites-temp"
OUT = ROOT / "sprites"

SKIP_DIRS = {"composite files"}
SHEET_JSON = SRC / "sprites-item" / "spritesheet.json"
SHEET_PNG = SRC / "sprites-item" / "spritesheet.png"
COLLECT = SRC / "sprites-item3" / "CollectablesSheet.png"
COIN = SRC / "sprites-item4" / "Coin.png"


def dim_label(width, height):
    return str(width) if width == height else f"{width}x{height}"


def clean(name):
    name = name.replace("\\", "/").replace("/", "_")
    name = re.sub(r"\s+", "_", name.strip())
    name = re.sub(r"[^0-9A-Za-z._+-]+", "_", name)
    name = re.sub(r"_+", "_", name).strip("._")
    return name or "sprite"


class Names:
    def __init__(self):
        self.used = set()

    def take(self, label, name):
        base = f"{label} {clean(name)}"
        candidate = base
        n = 2
        while candidate.lower() in self.used:
            candidate = f"{base}_{n}"
            n += 1
        self.used.add(candidate.lower())
        return OUT / f"{candidate}.png"


def save_png(image, path):
    image.save(path, "PNG")


def cut_json(names):
    sheet = Image.open(SHEET_PNG).convert("RGBA")
    frames = json.loads(SHEET_JSON.read_text())
    count = 0
    for key, spec in frames.items():
        frame = spec["frame"]
        x, y = frame["x"], frame["y"]
        w, h = frame["width"], frame["height"]
        tile = sheet.crop((x, y, x + w, y + h))
        if spec.get("rotated"):
            tile = tile.transpose(Image.ROTATE_90)
            w, h = h, w
        save_png(tile, names.take(dim_label(w, h), key))
        count += 1
    print(f"json sheet {count}")


def cut_grid(names, path, cell, prefix):
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    cw, ch = cell
    n = 0
    for row in range(height // ch):
        for col in range(width // cw):
            tile = image.crop((col * cw, row * ch, (col + 1) * cw, (row + 1) * ch))
            if tile.getextrema()[3][1] <= 8:
                continue
            n += 1
            save_png(tile, names.take(dim_label(cw, ch), f"{prefix}_{n:03d}"))
    print(f"grid {path.name} {n}")


def loose_sources():
    found = []
    for path in SRC.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix.lower() == ".png" and path in (SHEET_PNG, COLLECT, COIN):
            continue
        if path.suffix.lower() not in {".png", ".gif"}:
            continue
        if path.name.startswith("."):
            continue
        found.append(path)
    return found


def export_loose(names):
    groups = {}
    for path in loose_sources():
        if path.suffix.lower() == ".png":
            with Image.open(path) as image:
                label = dim_label(*image.size)
            key = (label, clean(path.stem))
            groups.setdefault(key, []).append(path)
        else:
            with Image.open(path) as image:
                width, height = image.size
            label = dim_label(width, height)
            key = (label, clean(path.stem))
            groups.setdefault(key, []).append(path)

    pngs = 0
    frames = 0
    for (label, stem), paths in groups.items():
        for path in paths:
            if len(paths) == 1:
                name = stem
            else:
                parent = clean(path.parent.name)
                name = f"{parent}_{stem}"
            if path.suffix.lower() == ".png":
                dest = names.take(label, name)
                shutil.copy2(path, dest)
                pngs += 1
            else:
                with Image.open(path) as image:
                    for index, frame in enumerate(ImageSequence.Iterator(image)):
                        tile = frame.convert("RGBA")
                        frame_label = dim_label(*tile.size)
                        frame_name = name if image.n_frames == 1 else f"{name}_frame{index:02d}"
                        save_png(tile, names.take(frame_label, frame_name))
                        frames += 1
    print(f"loose png {pngs} gif frames {frames}")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir()
    names = Names()
    cut_json(names)
    cut_grid(names, COLLECT, (16, 16), "collectable")
    cut_grid(names, COIN, (16, 16), "coin")
    export_loose(names)
    print("total", len(names.used))


if __name__ == "__main__":
    main()
