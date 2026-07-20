const { execFileSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const projectDir = path.resolve(__dirname, "..");
const baseJar = process.argv[2] || path.resolve(projectDir, "..", "stardewcraft-0.5.1fix4.jar");
const modelPath = "assets/stardewcraft/models/block/bush/small_bush.json";
const resourceRoot = path.resolve(projectDir, "src/main/resources/assets/stardewcraftsve");
const postOutputPath = path.resolve(resourceRoot, "models/block/hedge_fence_post.json");
const sideOutputPath = path.resolve(resourceRoot, "models/block/hedge_fence_side.json");
const blockstateOutputPath = path.resolve(resourceRoot, "blockstates/hedge_fence.json");
const itemOutputPath = path.resolve(resourceRoot, "models/item/hedge_fence.json");
const legacySeamTextureOutputPath = path.resolve(
  resourceRoot,
  "textures/block/fence/hedge_fence_seam_fill.png",
);

const source = execFileSync("tar", ["-xOf", baseJar, modelPath], { encoding: "utf8" });
const sourceModel = JSON.parse(source);

function round(value) {
  return Math.round(value * 100000) / 100000;
}

function transformModel(transform, credit) {
  const model = JSON.parse(JSON.stringify(sourceModel));
  for (const [elementIndex, element] of (model.elements || []).entries()) {
    element.from = element.from.map(
      (value, axis) => round(transform(value, axis, elementIndex)),
    );
    element.to = element.to.map(
      (value, axis) => round(transform(value, axis, elementIndex)),
    );
    if (element.rotation?.origin) {
      element.rotation.origin = element.rotation.origin.map(
        (value, axis) => round(transform(value, axis, elementIndex)),
      );
    }
  }
  model.credit = credit;
  return model;
}

const postModel = transformModel(
  (value, axis) => axis === 1 ? value : 8 + (value - 8) * 0.82,
  "Derived from StardewCraft's small bush as the compact hedge-fence core",
);

const sideModel = transformModel(
  (value, axis, elementIndex) => {
    if (axis === 0) return 8 + (value - 8) * 0.6;
    if (axis === 2) {
      if (elementIndex === 0) return (value - 2.5) * (8 / 11);
      return value * 0.5;
    }
    return value;
  },
  "Derived from StardewCraft's small bush as the north hedge-fence connection",
);

// Use the complete half-bush for every direction, matching the horizontal
// connection instead of trying to patch the top with separate cards.
sideModel.elements[0].to[1] = 15.85;

if (fs.existsSync(legacySeamTextureOutputPath)) {
  fs.unlinkSync(legacySeamTextureOutputPath);
}

const blockstate = {
  multipart: [
    { apply: { model: "stardewcraftsve:block/hedge_fence_post" } },
    { when: { north: "true" }, apply: { model: "stardewcraftsve:block/hedge_fence_side", uvlock: true } },
    { when: { east: "true" }, apply: { model: "stardewcraftsve:block/hedge_fence_side", y: 90, uvlock: true } },
    { when: { south: "true" }, apply: { model: "stardewcraftsve:block/hedge_fence_side", y: 180, uvlock: true } },
    { when: { west: "true" }, apply: { model: "stardewcraftsve:block/hedge_fence_side", y: 270, uvlock: true } },
  ],
};

const itemModel = {
  parent: "minecraft:item/generated",
  textures: { layer0: "stardewcraftsve:item/fence/hedge_fence" },
};

for (const [outputPath, value] of [
  [postOutputPath, postModel],
  [sideOutputPath, sideModel],
  [blockstateOutputPath, blockstate],
  [itemOutputPath, itemModel],
]) {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, JSON.stringify(value, null, 2) + "\n", "utf8");
}

console.log(JSON.stringify({
  source: modelPath,
  postElements: postModel.elements?.length || 0,
  sideElements: sideModel.elements?.length || 0,
  directions: 4,
}, null, 2));
