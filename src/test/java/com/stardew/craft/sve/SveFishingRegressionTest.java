package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
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
    private static final Path ITEMS = Path.of(
            "src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final Path TREASURE = Path.of(
            "src/main/resources/data/stardewcraft/fishing/fishing_treasure.json");
    private static final Path LOCATION_MIXIN = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin/FishingLocationKeysMixin.java");
    private static final Path INFO_MIXIN = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin/FishingInfoCategoryMixin.java");

    private static final Map<String, Integer> MOTION_TYPES = Map.of(
            "mixed", 0, "dart", 1, "smooth", 2, "sinker", 3, "floater", 4);

    private static final Set<String> LOCATION_ONLY = Set.of(
            "alligator", "arrowhead_shark", "bonefish", "butterfish", "daggerfish",
            "diamond_carp", "fiber_goby", "gemfish", "goldenfish", "highlands_bass",
            "king_salmon", "kittyfish", "meteor_carp", "puppyfish", "razor_trout",
            "torpedo_trout", "turretfish", "undeadfish", "wolf_snapper");

    private static final Map<String, Set<String>> VANILLA_TAGS = Map.ofEntries(
            tags("baby_lunaloo", "is_ginger_island_ocean"),
            tags("barred_knifejaw", "is_ginger_island_ocean"),
            tags("blue_tang", "is_ginger_island_ocean"),
            tags("bull_trout", "is_forest_river", "is_mountain_lake"),
            tags("clownfish", "is_ginger_island_ocean"),
            tags("frog", "is_mountain_lake"),
            tags("gar", "is_forest_river"),
            tags("goldfish", "is_town_river"),
            tags("grass_carp", "is_secret_woods"),
            tags("lunaloo", "is_ginger_island_ocean"),
            tags("minnow", "is_freshwater", "is_forest_river", "is_mountain_lake", "is_town_river"),
            tags("ocean_sunfish", "is_ginger_island_ocean"),
            tags("radioactive_bass", "is_sewers"),
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

    public static void main(String[] args) throws IOException {
        JsonObject reference = JsonParser.parseString(Files.readString(REFERENCE)).getAsJsonObject();
        Map<String, JsonObject> rules = readRules();
        validateRuleSet(reference, rules);
        validateFishItems(reference);
        validateLocations(rules);
        validateTreasure(reference);
        System.out.println("SVE fishing regression suite passed: original=44, catchRules=41, "
                + "locationOnly=" + LOCATION_ONLY.size());
    }

    private static void validateRuleSet(JsonObject reference, Map<String, JsonObject> rules) {
        Set<String> expectedRules = new LinkedHashSet<>(reference.keySet());
        expectedRules.removeAll(Set.of("dulse_seaweed", "sea_sponge", "swamp_crab"));
        expect(rules.keySet().equals(expectedRules), "Catch rule set differs: " + rules.keySet());

        for (String id : expectedRules) {
            JsonObject expected = reference.getAsJsonObject(id);
            JsonObject actual = rules.get(id);
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

        for (Set<String> tags : VANILLA_TAGS.values()) {
            for (String tag : tags) {
                expect(locationMixin.contains("\"stardewcraft:" + tag + "\""),
                        "Fishing pool mixin does not support " + tag);
            }
        }
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

    private static void validateTreasure(JsonObject reference) throws IOException {
        JsonObject treasure = JsonParser.parseString(Files.readString(TREASURE)).getAsJsonObject();
        Set<String> fishItems = new LinkedHashSet<>();
        for (String id : reference.keySet()) {
            if (!id.equals("dulse_seaweed")) fishItems.add("stardewcraftsve:" + id);
        }
        for (String pool : List.of("commonLoot", "rareLoot", "goldenLoot")) {
            for (JsonElement entry : treasure.getAsJsonArray(pool)) {
                String item = string(entry.getAsJsonObject(), "item");
                expect(!fishItems.contains(item), "Non-original SVE fish treasure entry: " + item);
            }
        }
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
