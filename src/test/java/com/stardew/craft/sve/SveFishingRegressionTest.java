package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.stardew.craft.api.v1.fishing.StardewFishingTreasureEntry;
import com.stardew.craft.api.v1.fishing.StardewFishingTreasurePoolDefinition;
import com.stardew.craft.api.v1.internal.BuiltinApiTypes;
import com.stardew.craft.fishing.data.FishingDataManager;
import com.stardew.craft.fishing.data.SpawnFishRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline parity checks against the SVE 1.15.11 Data/Fish reference snapshot. */
public final class SveFishingRegressionTest {
    private static final Path REFERENCE = Path.of(
            "src/test/resources/sve_reference/fish_metadata.json");
    private static final Path RULES = Path.of(
            "src/main/resources/data/stardewcraftsve/fishing/locations/sve_fish.json");
    private static final Path MOONLIGHT_JELLIES_RULES = Path.of(
            "src/main/resources/data/stardewcraftsve/fishing/locations/"
                    + "moonlight_jellies_festival.json");
    private static final Path ITEMS = Path.of(
            "src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final Path TREASURE = Path.of(
            "src/main/resources/data/stardewcraftsve/fishing/treasure_pools/"
                    + "sve_rare_treasure.json");
    private static final Path LEGACY_TREASURE = Path.of(
            "src/main/resources/data/stardewcraft/fishing/fishing_treasure.json");
    private static final Path LOCATION_MIXIN = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin/FishingLocationKeysMixin.java");
    private static final Path INFO_MIXIN = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin/FishingInfoCategoryMixin.java");
    private static final Path FESTIVAL_MIXIN = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin/MoonlightJelliesFishingMixin.java");

    private static final Map<String, Integer> MOTION_TYPES = Map.of(
            "mixed", 0, "dart", 1, "smooth", 2, "sinker", 3, "floater", 4);

    private static final Set<String> LOCATION_ONLY = Set.of(
            "alligator", "arrowhead_shark", "bonefish", "daggerfish",
            "diamond_carp", "fiber_goby", "gemfish", "goldenfish", "highlands_bass",
            "kittyfish", "meteor_carp", "razor_trout", "swamp_crab",
            "torpedo_trout", "turretfish", "undeadfish", "wolf_snapper");

    private static final Map<String, Set<String>> VANILLA_TAGS = Map.ofEntries(
            tags("baby_lunaloo", "is_ginger_island_ocean"),
            tags("barred_knifejaw", "is_ginger_island_ocean"),
            tags("blue_tang", "is_ginger_island_ocean"),
            tags("bull_trout", "is_forest_river", "is_mountain_lake"),
            tags("butterfish", "is_forest_river"),
            tags("clownfish", "is_ginger_island_ocean"),
            tags("frog", "is_mountain_lake"),
            tags("gar", "is_forest_river"),
            tags("goldfish", "is_town_river"),
            tags("grass_carp", "is_secret_woods"),
            tags("king_salmon", "is_forest_river"),
            tags("lunaloo", "is_ginger_island_ocean"),
            tags("minnow", "is_freshwater", "is_forest_river", "is_mountain_lake", "is_town_river"),
            tags("ocean_sunfish", "is_ginger_island_ocean"),
            tags("puppyfish", "is_forest_river"),
            tags("radioactive_bass", "is_sewers"),
            tags("sea_sponge", "is_ginger_island_ocean"),
            tags("seahorse", "is_ginger_island_ocean"),
            tags("shark", "is_ginger_island_ocean"),
            tags("shiny_lunaloo", "is_ginger_island_ocean"),
            tags("snatcher_worm", "is_mutant_bug_lair"),
            tags("starfish", "is_beach", "is_ginger_island_ocean"),
            tags("tadpole", "is_mountain_lake"),
            tags("viper_eel", "is_ginger_island_ocean"),
            tags("void_eel", "is_witch_swamp"),
            tags("water_grub", "is_mutant_bug_lair")
    );

    private static final Map<String, String> PRECISE_JEI_TAGS = Map.of(
            "is_forest_river", "forest",
            "is_freshwater", "freshwater",
            "is_ginger_island_ocean", "ginger_island",
            "is_mutant_bug_lair", "mutant_bug_lair",
            "is_town_river", "town"
    );

    private SveFishingRegressionTest() {
    }

    public static void main(String[] args) throws Exception {
        JsonObject reference = JsonParser.parseString(Files.readString(REFERENCE)).getAsJsonObject();
        Map<String, JsonObject> rules = readRules();
        validateRuleSet(reference, rules);
        validateFishItems(reference);
        validateLocations(rules);
        validateHost052Contract(rules);
        validateMoonlightJelliesFestival();
        validateTreasure();
        System.out.println("SVE fishing regression suite passed: original=44, catchRules=43, "
                + "locationOnly=" + LOCATION_ONLY.size());
    }

    private static void validateRuleSet(JsonObject reference, Map<String, JsonObject> rules) {
        Set<String> expectedRules = new LinkedHashSet<>(reference.keySet());
        expectedRules.remove("dulse_seaweed");
        expect(rules.keySet().equals(expectedRules), "Catch rule set differs: " + rules.keySet());

        for (String id : expectedRules) {
            JsonObject expected = reference.getAsJsonObject(id);
            JsonObject actual = rules.get(id);
            equal(integer(actual, "precedence"), 0,
                    id + " must share the base location-fish precedence");
            expect(Math.abs(decimal(actual, "chance") - 1.0) < 0.000001,
                    id + " must not duplicate the original spawn-rate roll");
            equal(integer(actual, "difficulty"), integer(expected, "difficulty"), id + " difficulty");
            equal(integer(actual, "motionType"), MOTION_TYPES.get(string(expected, "behavior")),
                    id + " motion type");
            for (String field : List.of("minFishSize", "maxFishSize", "minFishingLevel", "maxDepth")) {
                equal(integer(actual, field), integer(expected, field), id + " " + field);
            }
            for (String field : List.of("spawnRate", "depthMultiplier")) {
                expect(Math.abs(decimal(actual, field) - decimal(expected, field)) < 0.000001,
                        id + " " + field);
            }
            equal(string(actual, "weather"), string(expected, "weather"), id + " weather");
            equal(strings(actual.getAsJsonArray("seasons")), strings(expected.getAsJsonArray("seasons")),
                    id + " seasons");
            equal(ranges(actual.getAsJsonArray("timeRanges")), ranges(expected.getAsJsonArray("timeRanges")),
                    id + " time ranges");
        }
    }

    private static void validateFishItems(JsonObject reference) throws IOException {
        String source = Files.readString(ITEMS);
        Pattern pattern = Pattern.compile(
                "DeferredHolder<Item,\\s*FishItem>\\s+\\w+\\s*=\\s*ITEMS\\.register\\(\"(?<id>[^\"]+)\""
                        + ".*?new FishItem\\(.*?\\},\\s*(?<difficulty>\\d+),\\s*\"(?<behavior>[^\"]+)\"",
                Pattern.DOTALL);
        Set<String> actualIds = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String id = matcher.group("id");
            if (!reference.has(id)) continue;
            actualIds.add(id);
            JsonObject expected = reference.getAsJsonObject(id);
            equal(Integer.parseInt(matcher.group("difficulty")), integer(expected, "difficulty"),
                    id + " item difficulty");
            equal(matcher.group("behavior"), string(expected, "behavior"), id + " item behavior");
        }
        Set<String> expectedIds = new LinkedHashSet<>(reference.keySet());
        expectedIds.remove("dulse_seaweed");
        expect(actualIds.equals(expectedIds), "FishItem registry differs: " + actualIds);
    }

    private static void validateLocations(Map<String, JsonObject> rules) throws IOException {
        String locationMixin = Files.readString(LOCATION_MIXIN);
        String infoMixin = Files.readString(INFO_MIXIN);
        Set<String> customLocations = new LinkedHashSet<>();

        for (Map.Entry<String, JsonObject> entry : rules.entrySet()) {
            String id = entry.getKey();
            JsonObject rule = entry.getValue();
            Set<String> actualTags = new LinkedHashSet<>();
            for (String tag : strings(rule.getAsJsonArray("biomeTags"))) {
                actualTags.add(tag.replace("#stardewcraft:", ""));
            }
            if (LOCATION_ONLY.contains(id)) {
                expect(actualTags.isEmpty(), id + " must not enter a ported water biome");
                expect(!rule.getAsJsonArray("biomes").isEmpty(), id + " needs a JEI-only SVE location");
                expect(rule.has("displayOnly") && rule.get("displayOnly").getAsBoolean(),
                        id + " must be excluded from acquisition scanning");
            } else {
                equal(actualTags, VANILLA_TAGS.get(id), id + " vanilla location tags");
                expect(!rule.has("displayOnly"), id + " must remain catchable");
            }
            expect(rule.get("canBeInherited").getAsBoolean(), id + " should inherit its pool rule");
            for (String biome : strings(rule.getAsJsonArray("biomes"))) {
                if (biome.startsWith("stardewcraftsve:")) customLocations.add(biome.substring(16));
            }
        }

        expect(locationMixin.contains("withSvePool.add(SVE_FISHING_POOL)"),
                "SVE cross-location pool is not appended");
        expect(locationMixin.contains("withSvePool.add(MOONLIGHT_JELLIES_POOL)"),
                "Moonlight Jellies pool is not appended");
        for (String location : customLocations) {
            expect(infoMixin.contains("location(\"" + location + "\")"),
                    "JEI has no name mapping for " + location);
        }
        for (Map.Entry<String, String> entry : PRECISE_JEI_TAGS.entrySet()) {
            expect(infoMixin.contains("tag(\"" + entry.getKey() + "\", \""
                            + entry.getValue() + "\")"),
                    "JEI does not preserve the precise name for " + entry.getKey());
        }
        expect(!locationMixin.contains("\"stardewcraft:is_ocean\""),
                "Broad ocean injection would leak SVE fish into normal oceans");
        expect(!locationMixin.contains("\"stardewcraft:is_river\""),
                "Broad river injection would leak SVE fish into normal rivers");
    }

    private static void validateHost052Contract(Map<String, JsonObject> rules) throws Exception {
        Method locationResolver = FishingDataManager.class.getDeclaredMethod(
                "resolveVanillaAlignedLocationKeysStatic",
                ServerLevel.class, Holder.class, BlockPos.class);
        equal(locationResolver.getReturnType(), List.class,
                "0.5.2 fishing location resolver return type");

        Method conditionMatcher = FishingDataManager.class.getDeclaredMethod(
                "matchesVanillaCondition",
                ServerPlayer.class, ServerLevel.class, BlockPos.class, Holder.class,
                SpawnFishRule.class, boolean.class);
        equal(conditionMatcher.getReturnType(), boolean.class,
                "0.5.2 fishing condition matcher return type");

        Method lookupResolver = FishingDataManager.class.getDeclaredMethod(
                "resolveFishingLookupKeys", boolean.class, boolean.class, List.class);
        lookupResolver.setAccessible(true);
        List<String> ordinaryKeys = List.of("Default", "stardewcraftsve:sve_fish");
        equal(lookupResolver.invoke(null, true, false, ordinaryKeys), List.of("fishingGame"),
                "Fair fishing must replace ordinary and SVE pools");
        equal(lookupResolver.invoke(null, false, true, ordinaryKeys), List.of("Temp"),
                "Ice Festival fishing must replace ordinary and SVE pools");

        String jeiClass = classFileText(
                "/com/stardew/craft/integration/jei/FishingInfoCategory.class");
        expect(jeiClass.contains("location")
                        && jeiClass.contains(
                        "(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;"),
                "0.5.2 FishingInfoCategory location Mixin target changed");

        expect(resourceExists("/com/stardew/craft/world/MutantBugLairService.class"),
                "0.5.2 mutant bug lair runtime is absent");
        expect(resourceExists("/com/stardew/craft/world/WitchAreaService.class"),
                "0.5.2 witch swamp runtime is absent");
        validateHostFishingPool("/data/stardewcraft/fishing/locations/bugland.json",
                "BugLand", "#stardewcraft:is_mutant_bug_lair");
        validateHostFishingPool("/data/stardewcraft/fishing/locations/witchswamp.json",
                "WitchSwamp", "#stardewcraft:is_witch_swamp");

        equal(new LinkedHashSet<>(strings(rules.get("snatcher_worm")
                        .getAsJsonArray("biomeTags"))),
                Set.of("#stardewcraft:is_mutant_bug_lair"),
                "snatcher worm 0.5.2 location");
        equal(new LinkedHashSet<>(strings(rules.get("water_grub")
                        .getAsJsonArray("biomeTags"))),
                Set.of("#stardewcraft:is_mutant_bug_lair"),
                "water grub 0.5.2 location");
        equal(new LinkedHashSet<>(strings(rules.get("void_eel")
                        .getAsJsonArray("biomeTags"))),
                Set.of("#stardewcraft:is_witch_swamp"),
                "void eel 0.5.2 location");
    }

    private static void validateHostFishingPool(
            String resourcePath,
            String location,
            String biomeTag
    ) throws IOException {
        JsonObject data = JsonParser.parseString(resourceText(resourcePath)).getAsJsonObject();
        equal(string(data, "location"), location, location + " host pool id");
        boolean hasTag = false;
        for (JsonElement element : data.getAsJsonArray("fish")) {
            if (strings(element.getAsJsonObject().getAsJsonArray("biomeTags"))
                    .contains(biomeTag)) {
                hasTag = true;
                break;
            }
        }
        expect(hasTag, location + " host pool lacks " + biomeTag);
    }

    private static boolean resourceExists(String resourcePath) throws IOException {
        try (InputStream stream = SveFishingRegressionTest.class.getResourceAsStream(resourcePath)) {
            return stream != null;
        }
    }

    private static String classFileText(String resourcePath) throws IOException {
        return new String(resourceBytes(resourcePath), StandardCharsets.ISO_8859_1);
    }

    private static String resourceText(String resourcePath) throws IOException {
        return new String(resourceBytes(resourcePath), StandardCharsets.UTF_8);
    }

    private static byte[] resourceBytes(String resourcePath) throws IOException {
        try (InputStream stream = SveFishingRegressionTest.class.getResourceAsStream(resourcePath)) {
            if (stream == null) throw new AssertionError("Missing host resource " + resourcePath);
            return stream.readAllBytes();
        }
    }

    private static void validateTreasure() throws IOException {
        expect(Files.notExists(LEGACY_TREASURE),
                "Legacy whole-table fishing treasure override still exists");
        JsonObject treasure = JsonParser.parseString(Files.readString(TREASURE)).getAsJsonObject();
        for (String legacyField : List.of(
                "commonLoot", "rareLoot", "goldenLoot", "fallbackLoot",
                "rollChanceStart", "rollChanceDecayNormal", "rollChanceDecayGolden",
                "rareChance", "goldenPoolChance")) {
            expect(!treasure.has(legacyField),
                    "Modern additive treasure pool contains legacy field " + legacyField);
        }

        BuiltinApiTypes.bootstrap();
        StardewFishingTreasurePoolDefinition definition =
                StardewFishingTreasurePoolDefinition.CODEC
                        .parse(JsonOps.INSTANCE, treasure).result()
                        .orElseThrow(() -> new AssertionError(
                                "Modern SVE fishing treasure pool failed its host Codec"));
        equal(definition.chest(), "any", "treasure chest kind");
        expect(Math.abs(definition.chance() - 0.0015f) < 0.0000001f,
                "SVE treasure chance changed: " + definition.chance());
        equal(definition.rolls(), 1, "treasure rolls");
        expect(definition.availableWhen().isEmpty(),
                "SVE treasure pool unexpectedly gained conditions");

        JsonArray entries = treasure.getAsJsonArray("entries");
        equal(entries.size(), 3, "SVE treasure entry count");
        Set<String> expected = Set.of(
                "stardewcraftsve:ornate_treasure_chest",
                "stardewcraftsve:money_bag",
                "stardewcraftsve:magic_lamp");
        Set<String> actual = new LinkedHashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            JsonObject raw = entries.get(index).getAsJsonObject();
            JsonObject query = raw.getAsJsonObject("query");
            equal(string(query, "type"), "stardewcraft:item",
                    "treasure query type " + index);
            JsonObject data = query.getAsJsonObject("data");
            actual.add(string(data, "item"));
            equal(integer(data, "count"), 1, "treasure item count " + index);

            StardewFishingTreasureEntry decoded = definition.entries().get(index);
            equal(decoded.query().type().toString(), "stardewcraft:item",
                    "decoded treasure query type " + index);
            equal(decoded.weight(), 2, "treasure weight " + index);
            equal(decoded.minFishingLevel(), 2, "treasure minimum level " + index);
            equal(decoded.maxFishingLevel(), 100, "treasure maximum level " + index);
            equal(decoded.minWaterDistance(), 0, "treasure minimum water distance " + index);
            equal(decoded.maxWaterDistance(), 5, "treasure maximum water distance " + index);
        }
        equal(actual, expected, "SVE treasure outputs");

        List<String> adapterProblems = new java.util.ArrayList<>();
        List<SveAcquisitionEntrypoints.Route> liveRoutes =
                StardewCraftAcquisitionSnapshotAdapter.fishingTreasureRoutes(
                        Map.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "stardewcraftsve", "sve_rare_treasure"), definition),
                        adapterProblems);
        equal(liveRoutes.stream().map(SveAcquisitionEntrypoints.Route::output)
                        .collect(java.util.stream.Collectors.toSet()),
                expected, "accepted live treasure outputs");
        expect(adapterProblems.isEmpty(),
                "Accepted direct treasure queries produced adapter problems: " + adapterProblems);

        StardewFishingTreasureEntry zeroWeight = new StardewFishingTreasureEntry(
                definition.entries().getFirst().query(), 0, 2, 100, 0, 5);
        StardewFishingTreasureEntry unreachable = new StardewFishingTreasureEntry(
                definition.entries().get(1).query(), 2, 100, 100, 0, 5);
        StardewFishingTreasurePoolDefinition ineffective =
                new StardewFishingTreasurePoolDefinition(
                        "any", 1.0f, 1, List.of(), List.of(zeroWeight, unreachable));
        expect(StardewCraftAcquisitionSnapshotAdapter.fishingTreasureRoutes(
                        Map.of(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "stardewcraftsve", "ineffective"), ineffective),
                        new java.util.ArrayList<>()).isEmpty(),
                "Ineffective accepted treasure entries became acquisition sources");

        JsonObject empty = treasure.deepCopy();
        empty.add("entries", new JsonArray());
        expect(treasureCodecRejects(empty),
                "Host Codec accepted an empty treasure pool");
        JsonObject invalidChance = treasure.deepCopy();
        invalidChance.addProperty("chance", 1.1f);
        expect(treasureCodecRejects(invalidChance),
                "Host Codec accepted an out-of-range treasure chance");
    }

    private static boolean treasureCodecRejects(JsonObject candidate) {
        try {
            return StardewFishingTreasurePoolDefinition.CODEC
                    .parse(JsonOps.INSTANCE, candidate).result().isEmpty();
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    private static void validateMoonlightJelliesFestival() throws IOException {
        JsonObject data = JsonParser.parseString(Files.readString(MOONLIGHT_JELLIES_RULES))
                .getAsJsonObject();
        equal(string(data, "location"), "stardewcraftsve:moonlight_jellies_festival",
                "moonlight-jellies pool id");

        JsonArray fish = data.getAsJsonArray("fish");
        equal(fish.size(), 2, "moonlight-jellies fish count");
        Map<String, JsonObject> byItem = new LinkedHashMap<>();
        for (JsonElement element : fish) {
            JsonObject rule = element.getAsJsonObject();
            byItem.put(string(rule, "item"), rule);
        }
        equal(byItem.keySet(), Set.of(
                "stardewcraftsve:lunaloo", "stardewcraftsve:shiny_lunaloo"),
                "moonlight-jellies catches");

        for (Map.Entry<String, JsonObject> entry : byItem.entrySet()) {
            JsonObject rule = entry.getValue();
            equal(integer(rule, "precedence"), 0, entry.getKey() + " festival precedence");
            equal(new LinkedHashSet<>(strings(rule.getAsJsonArray("biomeTags"))), Set.of(
                            "#stardewcraft:is_beach", "#stardewcraft:is_ocean"),
                    entry.getKey() + " festival biome");
            equal(strings(rule.getAsJsonArray("biomes")),
                    List.of("stardewcraftsve:moonlight_jellies_festival"),
                    entry.getKey() + " JEI festival location");
            equal(strings(rule.getAsJsonArray("seasons")), List.of("summer"),
                    entry.getKey() + " festival season");
            equal(ranges(rule.getAsJsonArray("timeRanges")), List.of("2200-2400"),
                    entry.getKey() + " festival time");
            equal(string(rule, "condition"), "SEASON_DAY summer 28",
                    entry.getKey() + " festival date");
        }

        String locationMixin = Files.readString(LOCATION_MIXIN);
        String infoMixin = Files.readString(INFO_MIXIN);
        String festivalMixin = Files.readString(FESTIVAL_MIXIN);
        expect(locationMixin.contains("stardewcraftsve:moonlight_jellies_festival"),
                "Beach location resolver does not include the festival pool");
        expect(locationMixin.contains("level.dimension().location().toString()"),
                "Fishing pool injection must compare stable dimension ids");
        expect(!locationMixin.contains("isStardewFishingDimensionPublic(level)"),
                "Fishing pool injection must not inherit the host's identity-based dimension check");
        expect(!locationMixin.contains("SUPPORTED_WATER_TAGS"),
                "Cross-location pools must rely on each fish rule's own biome filter");
        expect(infoMixin.contains("location(\"moonlight_jellies_festival\")"),
                "JEI has no Moonlight Jellies festival location label");
        expect(festivalMixin.contains("MoonlightJelliesFestivalService.isParticipant(player)"),
                "Festival catches are not gated by active player participation");
    }

    private static Map<String, JsonObject> readRules() throws IOException {
        JsonArray fish = JsonParser.parseString(Files.readString(RULES))
                .getAsJsonObject().getAsJsonArray("fish");
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement element : fish) {
            JsonObject rule = element.getAsJsonObject();
            if (!rule.has("id")) continue;
            String id = string(rule, "id");
            expect(result.put(id, rule) == null, "Duplicate catch rule " + id);
        }
        return result;
    }

    private static Map.Entry<String, Set<String>> tags(String fish, String... tags) {
        return Map.entry(fish, Set.of(tags));
    }

    private static int integer(JsonObject object, String field) {
        return object.get(field).getAsInt();
    }

    private static double decimal(JsonObject object, String field) {
        return object.get(field).getAsDouble();
    }

    private static String string(JsonObject object, String field) {
        return object.get(field).getAsString();
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static List<String> ranges(JsonArray array) {
        return array.asList().stream()
                .map(JsonElement::getAsJsonArray)
                .map(range -> range.get(0).getAsInt() + "-" + range.get(1).getAsInt())
                .toList();
    }

    private static void equal(Object actual, Object expected, String label) {
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
