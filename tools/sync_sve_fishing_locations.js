const fs = require("fs");

const path = "src/main/resources/data/stardewcraftsve/fishing/locations/sve_fish.json";
const fishTagPath = "src/main/resources/data/stardewcraft/tags/item/fishes.json";
const data = JSON.parse(fs.readFileSync(path, "utf8"));

const custom = (biomes, biomeTags = []) => ({ biomes, biomeTags });
const vanilla = (...biomeTags) => ({ biomes: [], biomeTags });
const mixed = (biomes, ...biomeTags) => ({ biomes, biomeTags });

// Direct translation of SVE 1.15.11 Data/Locations. Placeholder SVE biome IDs
// deliberately cannot match until those maps are ported, but remain visible in JEI.
const targets = {
  alligator: custom(["stardewcraftsve:forbidden_maze"]),
  arrowhead_shark: custom(["stardewcraftsve:fable_reef"]),
  baby_lunaloo: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  barred_knifejaw: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  blue_tang: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  bonefish: custom(["stardewcraftsve:crimson_badlands"]),
  bull_trout: mixed(
    ["stardewcraftsve:adventurer_summit", "stardewcraftsve:forest_west", "stardewcraftsve:highlands"],
    "#stardewcraft:is_forest_river", "#stardewcraft:is_mountain_lake"
  ),
  butterfish: mixed([
    "stardewcraftsve:forest_west", "stardewcraftsve:morris_property",
    "stardewcraftsve:shearwater_bridge"
  ], "#stardewcraft:is_forest_river"),
  clownfish: vanilla("#stardewcraft:is_ginger_island_ocean"),
  daggerfish: custom(["stardewcraftsve:fable_reef"]),
  diamond_carp: custom(["stardewcraftsve:diamond_cavern"]),
  fiber_goby: custom(["stardewcraftsve:highlands"]),
  frog: vanilla("#stardewcraft:is_mountain_lake"),
  gar: mixed(["stardewcraftsve:forest_west"], "#stardewcraft:is_forest_river"),
  gemfish: custom(["stardewcraftsve:highlands_cavern"]),
  goldenfish: custom(["stardewcraftsve:junimo_woods", "stardewcraftsve:sprite_spring"]),
  goldfish: mixed(["stardewcraftsve:blue_moon_vineyard"], "#stardewcraft:is_town_river"),
  grass_carp: vanilla("#stardewcraft:is_secret_woods"),
  highlands_bass: custom(["stardewcraftsve:highlands"]),
  king_salmon: mixed(["stardewcraftsve:forest_west"], "#stardewcraft:is_forest_river"),
  kittyfish: custom(["stardewcraftsve:shearwater_bridge"]),
  lunaloo: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  meteor_carp: custom(["stardewcraftsve:junimo_woods", "stardewcraftsve:sprite_spring"]),
  minnow: mixed(
    ["stardewcraftsve:adventurer_summit", "stardewcraftsve:blue_moon_vineyard",
      "stardewcraftsve:morris_property", "stardewcraftsve:shearwater_bridge"],
    "#stardewcraft:is_freshwater", "#stardewcraft:is_forest_river",
    "#stardewcraft:is_mountain_lake", "#stardewcraft:is_town_river"
  ),
  ocean_sunfish: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  puppyfish: mixed(
    ["stardewcraftsve:forest_west", "stardewcraftsve:shearwater_bridge"],
    "#stardewcraft:is_forest_river"
  ),
  radioactive_bass: vanilla("#stardewcraft:is_sewers"),
  // Both original Razor Trout locations require Joja event 1056732, which is not ported.
  razor_trout: custom([
    "stardewcraftsve:blue_moon_vineyard", "stardewcraftsve:joja_town_after_event"
  ]),
  sea_sponge: mixed(
    ["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"
  ),
  seahorse: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  shark: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  shiny_lunaloo: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  snatcher_worm: vanilla("#stardewcraft:is_mutant_bug_lair"),
  starfish: mixed(
    ["stardewcraftsve:blue_moon_vineyard"],
    "#stardewcraft:is_beach", "#stardewcraft:is_ginger_island_ocean"
  ),
  swamp_crab: custom(["stardewcraftsve:forbidden_maze"]),
  tadpole: vanilla("#stardewcraft:is_mountain_lake"),
  torpedo_trout: custom(["stardewcraftsve:fable_reef"]),
  turretfish: custom(["stardewcraftsve:fable_reef"]),
  undeadfish: custom(["stardewcraftsve:crimson_badlands"]),
  viper_eel: mixed(["stardewcraftsve:fable_reef"], "#stardewcraft:is_ginger_island_ocean"),
  void_eel: vanilla("#stardewcraft:is_witch_swamp"),
  water_grub: vanilla("#stardewcraft:is_mutant_bug_lair"),
  wolf_snapper: custom(["stardewcraftsve:shearwater_bridge"])
};

const actualIds = new Set(data.fish.filter(rule => rule.id).map(rule => rule.id));
const expectedIds = new Set(Object.keys(targets));
const missing = [...actualIds].filter(id => !expectedIds.has(id));
const extra = [...expectedIds].filter(id => !actualIds.has(id));
if (missing.length || extra.length) {
  throw new Error(`Location mapping mismatch; missing=${missing.join(",")} extra=${extra.join(",")}`);
}

for (const rule of data.fish) {
  if (!rule.id) continue;
  // StardewCraft tries larger precedence values first. SVE fish are regular
  // location fish, so they must share precedence 0 with the base location pool.
  rule.precedence = 0;
  rule.biomes = targets[rule.id].biomes;
  rule.biomeTags = targets[rule.id].biomeTags;
  if (rule.biomeTags.length === 0) rule.displayOnly = true;
  else delete rule.displayOnly;
}

fs.writeFileSync(path, `${JSON.stringify(data, null, 2)}\n`);
const fishTag = {
  replace: false,
  values: [...actualIds].map(id => `stardewcraftsve:${id}`)
};
fs.writeFileSync(fishTagPath, `${JSON.stringify(fishTag, null, 2)}\n`);
console.log(`Synchronized ${actualIds.size} location mappings and fish tag entries.`);
