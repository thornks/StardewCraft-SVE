const fs = require("fs");
const path = require("path");

const projectDir = path.resolve(__dirname, "..");
const sourceFile = path.resolve(
  projectDir,
  "..",
  "temp_sve",
  "Stardew Valley Expanded",
  "[CP] Stardew Valley Expanded",
  "code",
  "Other",
  "Fish.json"
);
const outputDir = path.join(
  projectDir,
  "src",
  "main",
  "resources",
  "data",
  "stardewcraftsve",
  "fishpond",
  "pond_data"
);
const vanillaObjectsFile = path.join(
  projectDir,
  "libs",
  "_sc_inspect",
  "data",
  "stardewcraft",
  "npc",
  "vanilla",
  "data",
  "Objects.json"
);

const numericItems = {
  "70": "stardewcraft:jade",
  "72": "stardewcraft:diamond",
  "86": "stardewcraft:earth_crystal",
  "107": "stardewcraft:dinosaur_egg",
  "125": "stardewcraft:golden_relic",
  "136": "stardewcraft:largemouth_bass",
  "137": "stardewcraft:smallmouth_bass",
  "143": "stardewcraft:catfish",
  "153": "stardewcraft:green_algae",
  "167": "stardewcraft:joja_cola",
  "169": "stardewcraft:driftwood",
  "287": "stardewcraft:bomb_item",
  "288": "stardewcraft:mega_bomb",
  "297": "stardewcraft:grass_starter",
  "336": "stardewcraft:gold_bar",
  "340": "stardewcraft:honey",
  "373": "stardewcraft:golden_pumpkin",
  "384": "stardewcraft:gold_ore",
  "388": "stardewcraft:wood_normal",
  "393": "stardewcraft:coral",
  "394": "stardewcraft:rainbow_shell",
  "397": "stardewcraft:sea_urchin",
  "413": "stardewcraftsve:blue_slime_egg",
  "437": "stardewcraftsve:red_slime_egg",
  "439": "stardewcraftsve:purple_slime_egg",
  "536": "stardewcraft:frozen_geode",
  "574": "stardewcraft:mudstone",
  "580": "stardewcraft:prehistoric_tibia",
  "645": "stardewcraft:iridium_sprinkler",
  "680": "stardewcraftsve:green_slime_egg",
  "684": "stardewcraft:bug_meat",
  "706": "stardewcraft:shad",
  "709": "stardewcraft:wood_hard",
  "721": "stardewcraft:snail",
  "722": "stardewcraft:periwinkle",
  "725": "stardewcraft:oak_resin",
  "726": "stardewcraft:pine_tar",
  "749": "stardewcraft:omni_geode",
  "766": "stardewcraft:slime_item",
  "771": "stardewcraft:fiber",
  "774": "stardewcraft:wild_bait",
  "812": "stardewcraft:roe",
  "832": "stardewcraft:pineapple",
  "893": "stardewcraftsve:fireworks_red",
  "894": "stardewcraftsve:fireworks_purple",
  "895": "stardewcraftsve:fireworks_green"
};

const customItems = prefixKeys({
  Barred_Knifejaw: "stardewcraftsve:barred_knifejaw",
  Birch_Seed: "stardewcraftsve:birch_seed",
  Birch_Water: "stardewcraftsve:birch_water",
  Butter: "stardewcraftsve:butter",
  Diamond_Flower: "stardewcraftsve:diamond_flower",
  Fir_Cone: "stardewcraftsve:fir_cone",
  Fir_Wax: "stardewcraftsve:fir_wax",
  Frog: "stardewcraftsve:frog",
  Glazed_Butterfish: "stardewcraftsve:glazed_butterfish",
  Gold_Carrot_Seed: "stardewcraftsve:gold_carrot_seed",
  Lunaloo: "stardewcraftsve:lunaloo",
  Minnow: "stardewcraftsve:minnow",
  Ornate_Treasure_Chest: "stardewcraftsve:ornate_treasure_chest",
  Shark_Tooth: "stardewcraftsve:shark_tooth",
  Swirl_Stone: "stardewcraftsve:swirl_stone",
  Void_Pebble: "stardewcraftsve:void_pebble"
});
customItems.BlueGrassStarter = "stardewcraft:blue_grass_starter";

const fishIds = prefixKeys({
  Alligator: "alligator",
  Arrowhead_Shark: "arrowhead_shark",
  Baby_Lunaloo: "baby_lunaloo",
  Barred_Knifejaw: "barred_knifejaw",
  Blue_Tang: "blue_tang",
  Bonefish: "bonefish",
  Bull_Trout: "bull_trout",
  Butterfish: "butterfish",
  Clownfish: "clownfish",
  Daggerfish: "daggerfish",
  Diamond_Carp: "diamond_carp",
  Fiber_Goby: "fiber_goby",
  Frog: "frog",
  Gar: "gar",
  Gemfish: "gemfish",
  Goldenfish: "goldenfish",
  Goldfish: "goldfish",
  Grass_Carp: "grass_carp",
  Highlands_Bass: "highlands_bass",
  King_Salmon: "king_salmon",
  Kittyfish: "kittyfish",
  Lunaloo: "lunaloo",
  Meteor_Carp: "meteor_carp",
  Minnow: "minnow",
  Ocean_Sunfish: "ocean_sunfish",
  Puppyfish: "puppyfish",
  Radioactive_Bass: "radioactive_bass",
  Razor_Trout: "razor_trout",
  Seahorse: "seahorse",
  Sea_Sponge: "sea_sponge",
  Shark: "shark",
  Shiny_Lunaloo: "shiny_lunaloo",
  Snatcher_Worm: "snatcher_worm",
  Starfish: "starfish",
  Swamp_Crab: "swamp_crab",
  Tadpole: "tadpole",
  Torpedo_Trout: "torpedo_trout",
  Turretfish: "turretfish",
  Undeadfish: "undeadfish",
  Viper_Eel: "viper_eel",
  Void_Eel: "void_eel",
  Water_Grub: "water_grub",
  Wolf_Snapper: "wolf_snapper"
});

const unknownItems = new Set();
const vanillaObjects = JSON.parse(fs.readFileSync(vanillaObjectsFile, "utf8"));

function prefixKeys(values) {
  return Object.fromEntries(
    Object.entries(values).map(([key, value]) => [
      `FlashShifter.StardewValleyExpandedCP_${key}`,
      value
    ])
  );
}

function stripJsonComments(text) {
  let output = "";
  let inString = false;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;

  for (let i = 0; i < text.length; i += 1) {
    const char = text[i];
    const next = text[i + 1];
    if (lineComment) {
      if (char === "\n") {
        lineComment = false;
        output += char;
      }
      continue;
    }
    if (blockComment) {
      if (char === "*" && next === "/") {
        blockComment = false;
        i += 1;
      } else if (char === "\n") {
        output += char;
      }
      continue;
    }
    if (inString) {
      output += char;
      if (escaped) {
        escaped = false;
      } else if (char === "\\") {
        escaped = true;
      } else if (char === '"') {
        inString = false;
      }
      continue;
    }
    if (char === '"') {
      inString = true;
      output += char;
    } else if (char === "/" && next === "/") {
      lineComment = true;
      i += 1;
    } else if (char === "/" && next === "*") {
      blockComment = true;
      i += 1;
    } else {
      output += char;
    }
  }
  return output;
}

function parseJsonc(text) {
  const withoutComments = stripJsonComments(text);
  const normalizedNumbers = withoutComments.replace(
    /([:\[,]\s*)\.(\d+)/g,
    "$10.$2"
  );
  return JSON.parse(normalizedNumbers.replace(/,\s*([}\]])/g, "$1"));
}

function resolveItem(rawId) {
  const key = String(rawId);
  const objectKey = key.startsWith("(O)") ? key.slice(3) : key;
  const objectName = vanillaObjects[objectKey]?.Name;
  const resolved =
    numericItems[objectKey] ||
    customItems[key] ||
    (objectName ? `stardewcraft:${normalizeName(objectName)}` : null);
  if (!resolved) {
    unknownItems.add(key);
    return "minecraft:air";
  }
  return resolved;
}

function normalizeName(name) {
  return name
    .toLowerCase()
    .replace(/[\-():']/g, " ")
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+|_+$/g, "");
}

function convertGate(rawGate) {
  const match = String(rawGate).trim().match(/^(\S+)\s+(\d+)(?:\s+(\d+))?$/);
  if (!match) {
    throw new Error(`Invalid population gate: ${rawGate}`);
  }
  const minCount = Number(match[2]);
  return {
    item: resolveItem(match[1]),
    min_count: minCount,
    max_count: Number(match[3] || match[2])
  };
}

function convertDefinition(sourceId, source) {
  const fishId = fishIds[sourceId];
  if (!fishId) {
    throw new Error(`No addon fish mapping for ${sourceId}`);
  }

  const output = {
    fish: `stardewcraftsve:${fishId}`,
    max_population: source.MaxPopulation == null ? -1 : source.MaxPopulation,
    spawn_time: source.SpawnTime == null ? -1 : source.SpawnTime,
    base_min_produce_chance: 0.15,
    base_max_produce_chance: 0.95,
    produced_items: source.ProducedItems.map((item) => ({
      item: resolveItem(item.ItemID),
      required_population: item.RequiredPopulation || 0,
      chance: item.Chance,
      min_count: item.MinQuantity || 1,
      max_count: item.MaxQuantity || item.MinQuantity || 1
    }))
  };

  if (source.PopulationGates) {
    output.population_gates = Object.fromEntries(
      Object.entries(source.PopulationGates).map(([population, choices]) => [
        population,
        choices.map(convertGate)
      ])
    );
  }
  return [fishId, output];
}

const source = parseJsonc(fs.readFileSync(sourceFile, "utf8"));
const definitions = new Map();
for (const change of source.Changes) {
  if (change.Target !== "Data/FishPondData" || !change.Entries) {
    continue;
  }
  for (const [sourceId, pond] of Object.entries(change.Entries)) {
    const [fishId, converted] = convertDefinition(sourceId, pond);
    if (definitions.has(fishId)) {
      throw new Error(`Duplicate fish pond definition for ${fishId}`);
    }
    definitions.set(fishId, converted);
  }
}

const expected = new Set(Object.values(fishIds));
const missing = [...expected].filter((fishId) => !definitions.has(fishId));
if (unknownItems.size > 0) {
  throw new Error(
    `No StardewCraft item mapping for fish pond items: ${[...unknownItems].sort().join(", ")}`
  );
}
if (missing.length > 0 || definitions.size !== expected.size) {
  throw new Error(
    `Expected ${expected.size} fish pond definitions, found ${definitions.size}; missing: ${missing.join(", ")}`
  );
}

fs.mkdirSync(outputDir, { recursive: true });
for (const file of fs.readdirSync(outputDir)) {
  if (file.endsWith(".json")) {
    fs.unlinkSync(path.join(outputDir, file));
  }
}
for (const [fishId, definition] of [...definitions].sort(([a], [b]) => a.localeCompare(b))) {
  fs.writeFileSync(
    path.join(outputDir, `${fishId}.json`),
    `${JSON.stringify(definition, null, 2)}\n`,
    "utf8"
  );
}

console.log(`Imported ${definitions.size} SVE fish pond definitions from ${sourceFile}`);
