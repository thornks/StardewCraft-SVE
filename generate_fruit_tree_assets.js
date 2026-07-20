const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { PNG } = require("../node_modules/pngjs");

const ROOT = path.join(__dirname, "src", "main", "resources");
const JAR = path.join(__dirname, "libs", "stardewcraft-0.4.12.jar");
const NAMESPACE = "stardewcraftsve";

const TREES = {
  pear: {
    template: "apple",
    leafPalette: ["#052908", "#08470c", "#0a6810", "#128c16", "#25ad22", "#55c83c"],
    barkPalette: ["#2d1708", "#4b280b", "#71410f", "#99601b", "#c08632"],
    fruitPalette: ["#435b0b", "#688314", "#91ab24", "#bfd247", "#e2eb79"],
  },
  nectarine: {
    template: "peach",
    leafPalette: ["#062b09", "#0b4b0e", "#0e7012", "#15961a", "#2bb42a", "#6acd3f"],
    barkPalette: ["#321806", "#542b09", "#7c450e", "#a7651b", "#ce8c35"],
    fruitPalette: ["#7d180b", "#aa2c0d", "#d84b12", "#ef7720", "#ffc151"],
  },
  persimmon: {
    template: "pomegranate",
    leafPalette: ["#461407", "#702008", "#96300a", "#bd470e", "#dc6b18", "#f09a2d"],
    barkPalette: ["#2f1508", "#4d2109", "#71300b", "#984412", "#c36a25"],
    fruitPalette: ["#7d1807", "#a92a08", "#d64a0b", "#f07412", "#ffad2c"],
  },
};

function ensureDir(file) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
}

function writeJson(relativePath, value) {
  const target = path.join(ROOT, relativePath);
  ensureDir(target);
  fs.writeFileSync(target, `${JSON.stringify(value, null, 2)}\n`);
}

function readJarEntry(entry) {
  return execFileSync("tar", ["-xOf", JAR, entry], { maxBuffer: 32 * 1024 * 1024 });
}

function hexToRgba(hex) {
  const value = parseInt(hex.slice(1), 16);
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255, 255];
}

function setPixel(png, x, y, rgba) {
  if (x < 0 || y < 0 || x >= png.width || y >= png.height) return;
  const index = (png.width * y + x) << 2;
  png.data[index] = rgba[0];
  png.data[index + 1] = rgba[1];
  png.data[index + 2] = rgba[2];
  png.data[index + 3] = rgba[3];
}

function fillRect(png, x, y, width, height, color) {
  const rgba = Array.isArray(color) ? color : hexToRgba(color);
  for (let py = y; py < y + height; py++) {
    for (let px = x; px < x + width; px++) setPixel(png, px, py, rgba);
  }
}

function seededRandom(seed) {
  let value = seed >>> 0;
  return () => {
    value = (value * 1664525 + 1013904223) >>> 0;
    return value / 0x100000000;
  };
}

function stableHash(value) {
  let hash = 2166136261;
  for (const char of value) {
    hash ^= char.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function isLeafPlane(cube) {
  return cube.size.filter((value) => value === 0).length === 1 && Math.max(...cube.size) >= 16;
}

function markUv(mask, width, height, uvData) {
  if (!uvData || !Array.isArray(uvData.uv) || !Array.isArray(uvData.uv_size)) return;
  const [u, v] = uvData.uv;
  const [uw, vh] = uvData.uv_size;
  const minX = Math.max(0, Math.floor(Math.min(u, u + uw)));
  const maxX = Math.min(width, Math.ceil(Math.max(u, u + uw)));
  const minY = Math.max(0, Math.floor(Math.min(v, v + vh)));
  const maxY = Math.min(height, Math.ceil(Math.max(v, v + vh)));
  for (let y = minY; y < maxY; y++) {
    for (let x = minX; x < maxX; x++) mask[y * width + x] = 1;
  }
}

function markCubeUvs(mask, width, height, cube) {
  if (!cube.uv || Array.isArray(cube.uv)) return;
  for (const face of Object.values(cube.uv)) markUv(mask, width, height, face);
}

function createTemplateModel(type, spec) {
  const source = `assets/stardewcraft/geo/block/tree/fruit/${spec.template}_tree.geo.json`;
  const model = JSON.parse(readJarEntry(source).toString("utf8"));
  const geometry = model["minecraft:geometry"][0];
  geometry.description.identifier = `geometry.${NAMESPACE}.${type}_tree`;

  if (type === "persimmon") {
    for (const bone of geometry.bones) {
      if (!bone.cubes || bone.parent === "fruit") continue;
      bone.cubes = bone.cubes.filter((cube, index) => {
        if (!isLeafPlane(cube)) return true;
        const key = `${bone.name}:${index}:${cube.origin.join(",")}`;
        return stableHash(key) % 100 >= 42;
      });
      if (bone.cubes.length === 0) delete bone.cubes;
    }
  }

  if (type === "nectarine") {
    for (const bone of geometry.bones) {
      if (Array.isArray(bone.pivot) && bone.pivot[1] > 35) {
        bone.pivot[0] *= 0.94;
        bone.pivot[1] = 35 + (bone.pivot[1] - 35) * 1.05;
      }
      for (const cube of bone.cubes || []) {
        if (cube.origin[1] <= 35) continue;
        cube.origin[0] *= 0.94;
        cube.origin[1] = 35 + (cube.origin[1] - 35) * 1.05;
        cube.size[0] *= 0.94;
      }
    }
    geometry.description.visible_bounds_height *= 1.05;
    geometry.description.visible_bounds_offset[1] *= 1.05;
  }

  return model;
}

function createTemplateTexture(type, spec, model) {
  const source = `assets/stardewcraft/textures/block/tree/fruit/${spec.template}_tree.png`;
  const png = PNG.sync.read(readJarEntry(source));
  const geometry = model["minecraft:geometry"][0];
  const leafMask = new Uint8Array(png.width * png.height);
  const fruitMask = new Uint8Array(png.width * png.height);

  for (const bone of geometry.bones) {
    for (const cube of bone.cubes || []) {
      if (bone.parent === "fruit") markCubeUvs(fruitMask, png.width, png.height, cube);
      else if (isLeafPlane(cube)) markCubeUvs(leafMask, png.width, png.height, cube);
    }
  }

  const categories = [
    { mask: leafMask, palette: spec.leafPalette.map(hexToRgba) },
    { mask: fruitMask, palette: spec.fruitPalette.map(hexToRgba) },
  ];
  const stats = categories.map(() => ({ min: 1, max: 0 }));

  for (let pixel = 0; pixel < png.width * png.height; pixel++) {
    const offset = pixel << 2;
    if (png.data[offset + 3] === 0) continue;
    const luminance = (png.data[offset] * 0.2126 + png.data[offset + 1] * 0.7152 + png.data[offset + 2] * 0.0722) / 255;
    categories.forEach((category, index) => {
      if (!category.mask[pixel]) return;
      stats[index].min = Math.min(stats[index].min, luminance);
      stats[index].max = Math.max(stats[index].max, luminance);
    });
  }

  for (let pixel = 0; pixel < png.width * png.height; pixel++) {
    const offset = pixel << 2;
    if (png.data[offset + 3] === 0) continue;
    const luminance = (png.data[offset] * 0.2126 + png.data[offset + 1] * 0.7152 + png.data[offset + 2] * 0.0722) / 255;
    let categoryIndex = fruitMask[pixel] ? 1 : leafMask[pixel] ? 0 : -1;
    const palette = categoryIndex >= 0 ? categories[categoryIndex].palette : spec.barkPalette.map(hexToRgba);
    const range = categoryIndex >= 0 ? stats[categoryIndex] : { min: 0.04, max: 0.82 };
    const normalized = Math.max(0, Math.min(1, (luminance - range.min) / Math.max(0.01, range.max - range.min)));
    const color = palette[Math.round(normalized * (palette.length - 1))];
    png.data[offset] = color[0];
    png.data[offset + 1] = color[1];
    png.data[offset + 2] = color[2];
  }

  const target = path.join(ROOT, "assets", NAMESPACE, "textures", "block", "tree", "fruit", `${type}_tree.png`);
  ensureDir(target);
  fs.writeFileSync(target, PNG.sync.write(png));
}

function drawCircle(png, cx, cy, radius, palette, seed) {
  const random = seededRandom(seed);
  const colors = palette.map(hexToRgba);
  for (let y = cy - radius; y <= cy + radius; y++) {
    for (let x = cx - radius; x <= cx + radius; x++) {
      const dx = x - cx;
      const dy = y - cy;
      if (dx * dx + dy * dy > radius * radius + random() * 2) continue;
      setPixel(png, x, y, colors[Math.floor(random() * colors.length)]);
    }
  }
}

function createSaplingTexture(type, spec, stage, half) {
  const png = new PNG({ width: 16, height: 16, colorType: 6 });
  fillRect(png, 0, 0, 16, 16, [0, 0, 0, 0]);
  const bark = spec.barkPalette.map(hexToRgba);

  if (half === "bottom") {
    const trunkTop = [10, 6, 1, 0][stage];
    const trunkWidth = stage >= 2 ? 2 : 1;
    for (let y = trunkTop; y < 16; y++) {
      for (let x = 8 - trunkWidth + 1; x <= 8; x++) setPixel(png, x, y, bark[(x + y) % bark.length]);
    }
    if (stage === 0) drawCircle(png, 8, 9, 3, spec.leafPalette, 10);
    if (stage === 1) {
      drawCircle(png, 6, 7, 3, spec.leafPalette, 11);
      drawCircle(png, 10, 6, 3, spec.leafPalette, 12);
    }
    if (stage === 2) {
      drawCircle(png, 5, 4, 3, spec.leafPalette, 13);
      drawCircle(png, 11, 4, 3, spec.leafPalette, 14);
      drawCircle(png, 8, 2, 3, spec.leafPalette, 15);
    }
    if (stage === 3) {
      drawCircle(png, 4, 3, 4, spec.leafPalette, 16);
      drawCircle(png, 11, 3, 4, spec.leafPalette, 17);
    }
  } else if (stage >= 1) {
    const trunkStart = stage === 1 ? 13 : stage === 2 ? 8 : 6;
    for (let y = trunkStart; y < 16; y++) setPixel(png, 8, y, bark[y % bark.length]);
    if (stage === 1) drawCircle(png, 8, 13, 2, spec.leafPalette, 21);
    if (stage === 2) {
      drawCircle(png, 6, 10, 3, spec.leafPalette, 22);
      drawCircle(png, 10, 9, 3, spec.leafPalette, 23);
    }
    if (stage === 3) {
      drawCircle(png, 4, 11, 4, spec.leafPalette, 24);
      drawCircle(png, 9, 9, 5, spec.leafPalette, 25);
      drawCircle(png, 12, 12, 3, spec.leafPalette, 26);
    }
  }

  const target = path.join(
    ROOT,
    "assets",
    NAMESPACE,
    "textures",
    "block",
    "tree",
    "fruit",
    "sapling",
    `${type}_sapling_stage${stage}_${half}.png`,
  );
  ensureDir(target);
  fs.writeFileSync(target, PNG.sync.write(png));
}

function createBlockResources(type) {
  const variants = {};
  for (let stage = 0; stage < 4; stage++) {
    for (const [halfValue, halfName] of [["lower", "bottom"], ["upper", "top"]]) {
      const modelName = `${type}_sapling_stage${stage}_${halfName}`;
      variants[`age=${stage},half=${halfValue}`] = { model: `${NAMESPACE}:block/tree/fruit/${modelName}` };
      writeJson(path.join("assets", NAMESPACE, "models", "block", "tree", "fruit", `${modelName}.json`), {
        render_type: "minecraft:cutout",
        parent: "minecraft:block/cross",
        textures: {
          cross: `${NAMESPACE}:block/tree/fruit/sapling/${modelName}`,
          particle: `${NAMESPACE}:block/tree/fruit/sapling/${modelName}`,
        },
      });
    }
  }

  writeJson(path.join("assets", NAMESPACE, "blockstates", `${type}_sapling.json`), { variants });
  writeJson(path.join("assets", NAMESPACE, "models", "block", "tree", "fruit", `${type}_tree_particle.json`), {
    parent: "minecraft:block/cube_all",
    textures: {
      all: `${NAMESPACE}:block/tree/fruit/${type}_tree`,
      particle: `${NAMESPACE}:block/tree/fruit/${type}_tree`,
    },
  });
  const particleState = { variants: { "": { model: `${NAMESPACE}:block/tree/fruit/${type}_tree_particle` } } };
  writeJson(path.join("assets", NAMESPACE, "blockstates", `${type}_tree.json`), particleState);
  writeJson(path.join("assets", NAMESPACE, "blockstates", `${type}_tree_extension.json`), particleState);

  writeJson(path.join("data", NAMESPACE, "loot_table", "blocks", `${type}_sapling.json`), {
    type: "minecraft:block",
    pools: [{
      rolls: 1,
      entries: [{ type: "minecraft:item", name: `${NAMESPACE}:${type}_sapling` }],
      conditions: [{ condition: "minecraft:survives_explosion" }],
    }],
  });
  writeJson(path.join("data", NAMESPACE, "loot_table", "blocks", `${type}_tree.json`), { type: "minecraft:block", pools: [] });
  writeJson(path.join("data", NAMESPACE, "loot_table", "blocks", `${type}_tree_extension.json`), { type: "minecraft:block", pools: [] });
}

for (const [type, spec] of Object.entries(TREES)) {
  const model = createTemplateModel(type, spec);
  createTemplateTexture(type, spec, model);
  writeJson(path.join("assets", NAMESPACE, "geo", "block", "tree", "fruit", `${type}_tree.geo.json`), model);
  for (let stage = 0; stage < 4; stage++) {
    createSaplingTexture(type, spec, stage, "bottom");
    createSaplingTexture(type, spec, stage, "top");
  }
  createBlockResources(type);
}

console.log("Generated SVE fruit tree assets from StardewCraft fruit tree geometry templates.");
