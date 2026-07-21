"""Generate smoked fish textures + model JSONs for SVE smoked fish."""
import os
import json
import sys
from PIL import Image, ImageEnhance, ImageFilter

PROJECT = r"C:\Users\mfx\Desktop\blockbench-mcp\stardewcraftsve"
SRC = os.path.join(PROJECT, "src", "main", "resources")

# All SVE objects that are both category -4 fish and valid smoker inputs.
FISH = [
    "alligator", "arrowhead_shark", "baby_lunaloo", "barred_knifejaw",
    "blue_tang", "bonefish", "bull_trout", "butterfish", "clownfish",
    "daggerfish", "diamond_carp", "fiber_goby", "frog", "gar", "gemfish",
    "goldenfish", "goldfish", "grass_carp", "highlands_bass", "king_salmon",
    "kittyfish", "lunaloo", "meteor_carp", "minnow", "ocean_sunfish",
    "puppyfish", "radioactive_bass", "razor_trout", "sea_sponge", "seahorse", "shark",
    "shiny_lunaloo", "snatcher_worm", "starfish", "swamp_crab", "tadpole", "torpedo_trout",
    "turretfish", "undeadfish", "viper_eel", "void_eel", "water_grub",
    "wolf_snapper",
]

# Fish whose source texture is NOT in item/fish/
SPECIAL_TEXTURE_DIRS = {
    "baby_lunaloo": "animal_product",
    "lunaloo": "animal_product",
    "shiny_lunaloo": "animal_product",
    "sea_sponge": "forage",
    "starfish": "forage",
    "swamp_crab": "forage",
}

def source_path(name):
    """Path to the original SVE fish texture."""
    subdir = SPECIAL_TEXTURE_DIRS.get(name, "fish")
    return os.path.join(SRC, "assets", "stardewcraftsve", "textures", "item", subdir, f"{name}.png")

def dest_path(name):
    """Path where the smoked texture will be written (stardewcraft namespace)."""
    d = os.path.join(SRC, "assets", "stardewcraft", "textures", "item", "fish", "smoked")
    os.makedirs(d, exist_ok=True)
    return os.path.join(d, f"{name}.png")


# ── 1. Generate smoked textures ────────────────────────────────────────

def make_smoked_texture(src, dst):
    """Read source fish texture, apply smoked filter, write to dst."""
    img = Image.open(src).convert("RGBA")

    # Split alpha
    r, g, b, a = img.split()

    # Desaturate slightly
    gray = Image.merge("RGB", (r, g, b)).convert("L")

    # Convert back to RGB and apply sepia/brown tint
    rgb = gray.convert("RGB")

    # Brownish tint matrix (sepia-like but darker)
    # Multiply each pixel by a brownish color matrix
    pixels = rgb.load()
    for y in range(rgb.height):
        for x in range(rgb.width):
            l = pixels[x, y][0]  # gray value
            # Smoked fish color: brownish-golden
            nr = int(l * 1.0)
            ng = int(l * 0.70)
            nb = int(l * 0.25)
            pixels[x, y] = (min(nr, 255), min(ng, 255), min(nb, 255))

    # Slightly darken
    enhancer = ImageEnhance.Brightness(rgb)
    rgb = enhancer.enhance(0.75)

    # Restore alpha
    rgb.putalpha(a)
    rgb.save(dst, "PNG")
    print(f"  Generated smoked texture: {os.path.basename(dst)}")


# ── 2. Generate model JSONs ───────────────────────────────────────────

MODELS_DIRS = [
    os.path.join(SRC, "assets", "stardewcraft", "models", "item"),
    os.path.join(SRC, "assets", "stardewcraftsve", "models", "item"),
]
for models_dir in MODELS_DIRS:
    os.makedirs(models_dir, exist_ok=True)

def gen_models(name):
    """Generate the 8 model JSON files for one smoked fish."""
    tex = f"stardewcraft:item/fish/smoked/{name}"

    # a) Main display model (builtin/entity)
    display = {
        "parent": "minecraft:builtin/entity",
        "textures": {"particle": tex},
        "display": {
            "thirdperson_righthand": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 1],
                "scale": [0.55, 0.55, 0.55]
            },
            "thirdperson_lefthand": {
                "rotation": [0, 0, 0],
                "translation": [0, 3, 1],
                "scale": [0.55, 0.55, 0.55]
            },
            "firstperson_righthand": {
                "rotation": [0, -90, 25],
                "translation": [1.13, 3.2, 1.13],
                "scale": [0.68, 0.68, 0.68]
            },
            "firstperson_lefthand": {
                "rotation": [0, -90, 25],
                "translation": [1.13, 3.2, 1.13],
                "scale": [0.68, 0.68, 0.68]
            },
            "ground": {
                "rotation": [0, 0, 0],
                "translation": [0, 2, 0],
                "scale": [0.5, 0.5, 0.5]
            }
        }
    }

    # b) Base item model (generated, layer0, overrides for quality)
    base = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": tex,
            "particle": tex
        },
        "overrides": [
            {"predicate": {"custom_model_data": 1}, "model": f"stardewcraft:item/smoked_{name}_base_silver"},
            {"predicate": {"custom_model_data": 2}, "model": f"stardewcraft:item/smoked_{name}_base_gold"},
            {"predicate": {"custom_model_data": 3}, "model": f"stardewcraft:item/smoked_{name}_base_iridium"},
        ]
    }

    # c) Quality variants (silver, gold, iridium) — same content for _base_* and standalone
    def quality_model(star):
        return {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": tex,
                "layer1": f"stardewcraft:item/quality/{star}_star",
                "particle": tex
            }
        }

    files = {
        f"smoked_{name}.json": display,
        f"smoked_{name}_base.json": base,
        f"smoked_{name}_base_silver.json": quality_model("silver"),
        f"smoked_{name}_base_gold.json": quality_model("gold"),
        f"smoked_{name}_base_iridium.json": quality_model("iridium"),
        f"smoked_{name}_silver.json": quality_model("silver"),
        f"smoked_{name}_gold.json": quality_model("gold"),
        f"smoked_{name}_iridium.json": quality_model("iridium"),
    }

    for models_dir in MODELS_DIRS:
        for fname, content in files.items():
            path = os.path.join(models_dir, fname)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(content, f, indent=2)
    print(f"  Generated 8 models in both namespaces for: {name}")


# ── Main ──────────────────────────────────────────────────────────────

print("=== Generating smoked textures ===")
selected_fish = sys.argv[1:] or FISH
unknown = sorted(set(selected_fish) - set(FISH))
if unknown:
    raise SystemExit(f"Unknown fish IDs: {', '.join(unknown)}")
missing = []
for name in selected_fish:
    src = source_path(name)
    if not os.path.exists(src):
        missing.append((name, src))
        continue
    dst = dest_path(name)
    if not os.path.exists(dst):
        make_smoked_texture(src, dst)

if missing:
    print("WARNING: Missing source textures:")
    for n, p in missing:
        print(f"  {n}: {p}")

print("\n=== Generating model JSONs ===")
for name in selected_fish:
    gen_models(name)

print(f"\nDone! Generated textures + models for {len(selected_fish)} fish.")
print(f"Texture location: assets/stardewcraft/textures/item/fish/smoked/")
print("Model locations: assets/stardewcraft/models/item/ and assets/stardewcraftsve/models/item/")
