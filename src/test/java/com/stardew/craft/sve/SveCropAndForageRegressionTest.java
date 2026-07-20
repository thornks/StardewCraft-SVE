package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regression checks for SVE crop rules, item tags, and active forage zones. */
public final class SveCropAndForageRegressionTest {
    private static final Path ITEM_TAG_DIRECTORY = Path.of("src/main/resources/data/stardewcraft/tags/item");
    private static final Path FORAGE_DIRECTORY = Path.of("src/main/resources/data/stardewcraftsve/forage_zones");

    private SveCropAndForageRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        validateCrops();
        validateCropTags();
        validateForageZones();
        System.out.println("SVE crop and forage regression suite passed");
    }

    private static void validateCrops() {
        Map<String, ExpectedCrop> expected = expectedCrops();
        Map<String, SveCropData.Definition> actual = new HashMap<>();
        for (SveCropData.Definition definition : SveCropData.all()) {
            expect(actual.put(definition.blockPath(), definition) == null,
                    "duplicate crop " + definition.blockPath());
        }
        expectEquals(expected.keySet(), actual.keySet(), "crop definition set");

        for (var entry : expected.entrySet()) {
            String crop = entry.getKey();
            ExpectedCrop rule = entry.getValue();
            SveCropData.Definition definition = actual.get(crop);
            expectEquals(rule.seedPath(), definition.seedPath(), crop + " seed");
            expectEquals(rule.producePath(), definition.producePath(), crop + " produce");
            expectEquals(rule.produceType(), definition.produceType(), crop + " produce type");
            expectEquals(rule.seasons(), definition.seasons(), crop + " seasons");
            expectEquals(rule.phaseDays(), definition.phaseDays(), crop + " phase days");
            expectEquals(rule.regrowDays(), definition.regrowDays(), crop + " regrow days");
            expectEquals(rule.raised(), definition.raised(), crop + " raised");
            expectEquals(rule.minHarvest(), definition.minHarvest(), crop + " minimum harvest");
            expectEquals(rule.maxHarvest(), definition.maxHarvest(), crop + " maximum harvest");
            expectClose(rule.maxIncrease(), definition.harvestMaxIncreasePerFarmingLevel(), crop + " level increase");
            expectClose(rule.extraChance(), definition.extraHarvestChance(), crop + " extra harvest chance");
            expectEquals(rule.producePrice(), definition.produceSellPrice(), crop + " produce price");
            expectEquals(rule.edibility(), definition.edibility(), crop + " edibility");
            expectEquals(rule.seedPrice(), definition.seedSellPrice(), crop + " seed price");
            expectEquals(rule.farmingExperience(), definition.farmingExperience(), crop + " farming experience");
            expectEquals(rule.phaseDays().stream().mapToInt(Integer::intValue).sum(),
                    definition.totalGrowthDays(), crop + " total growth days");
        }
    }

    private static void validateCropTags() throws IOException {
        Set<String> allProduce = new HashSet<>();
        Set<String> fruit = new HashSet<>();
        Set<String> vegetables = new HashSet<>();
        Set<String> seeds = new HashSet<>();
        for (SveCropData.Definition definition : SveCropData.all()) {
            String produceId = id(definition.producePath());
            allProduce.add(produceId);
            seeds.add(id(definition.seedPath()));
            if (definition.produceType() == SveCropData.ProduceType.FRUIT) {
                fruit.add(produceId);
            } else {
                vegetables.add(produceId);
            }
        }

        expectEquals(allProduce, readTag("crops"), "crop item tag");
        expectEquals(fruit, readTag("fruit_crops"), "fruit crop item tag");
        expectEquals(vegetables, readTag("vegetable_crops"), "vegetable crop item tag");
        expect(readTag("seeds").containsAll(seeds), "seed item tag must contain all SVE crop seeds");
    }

    private static void validateForageZones() throws IOException {
        Map<String, List<String>> expected = Map.ofEntries(
                forage("beach", "big_conch"),
                forage("beach", "dried_sand_dollar"),
                forage("beach", "dulse_seaweed"),
                forage("beach", "golden_ocean_flower"),
                forage("forest", "mushroom_colony", "fall"),
                forage("mountain", "thistle"),
                forage("mountain", "ferngill_primrose", "spring"),
                forage("mountain", "goldenrod", "summer", "fall"),
                forage("mountain", "diamond_flower", "winter"),
                forage("mountain", "winter_star_rose", "winter"),
                forage("secret_woods", "mushroom_colony", "fall"),
                forage("secret_woods", "poison_mushroom", "summer", "fall"),
                forage("secret_woods", "smelly_rafflesia", "spring", "summer", "fall"),
                forage("secret_woods", "lucky_four_leaf_clover", "spring", "summer"),
                forage("secret_woods", "red_baneberry", "summer"),
                forage("secret_woods", "bearberrys", "winter"));

        Map<String, List<String>> actual = new LinkedHashMap<>();
        try (var paths = Files.list(FORAGE_DIRECTORY)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).sorted().toList()) {
                String zone = path.getFileName().toString().replaceFirst("\\.json$", "");
                JsonObject data = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                int minimum = data.get("min_daily_spawn").getAsInt();
                int maximum = data.get("max_daily_spawn").getAsInt();
                int cap = data.get("max_spawned_at_once").getAsInt();
                expect(minimum >= 0 && maximum >= minimum, zone + " daily spawn range");
                expect(cap >= maximum, zone + " simultaneous spawn cap");

                for (JsonElement element : data.getAsJsonArray("entries")) {
                    JsonObject entry = element.getAsJsonObject();
                    String block = entry.get("block").getAsString();
                    double chance = entry.get("chance").getAsDouble();
                    expect(chance > 0.0D && chance <= 1.0D, zone + " chance for " + block);
                    List<String> seasons = entry.has("seasons")
                            ? strings(entry.getAsJsonArray("seasons"))
                            : List.of();
                    String key = zone + "|" + block;
                    expect(actual.put(key, seasons) == null, "duplicate forage entry " + key);
                }
            }
        }
        expectEquals(expected, actual, "forage zone seasons");
    }

    private static Map<String, ExpectedCrop> expectedCrops() {
        return Map.ofEntries(
                expected("cucumber_crop", "cucumber_seed", "cucumber", SveCropData.ProduceType.VEGETABLE,
                        List.of(0), List.of(1, 2, 3, 3, 3), 2, false, 2, 3, 0.03, 0.20, 35, 18, 75, 8),
                expected("butternut_squash_crop", "butternut_squash_seed", "butternut_squash", SveCropData.ProduceType.VEGETABLE,
                        List.of(1), List.of(1, 2, 3, 3, 3), -1, false, 0, 0, 0.0, 0.0, 200, 20, 30, 24),
                expected("gold_carrot_crop", "gold_carrot_seed", "gold_carrot", SveCropData.ProduceType.VEGETABLE,
                        List.of(0, 1, 2), List.of(1, 1, 2, 2), -1, false, 0, 0, 0.0, 0.0, 1000, 115, 300, 47),
                expected("sweet_potato_crop", "sweet_potato_seed", "sweet_potato", SveCropData.ProduceType.VEGETABLE,
                        List.of(2), List.of(1, 2, 3, 3, 3), -1, false, 0, 0, 0.0, 0.0, 280, 20, 45, 29),
                expected("joja_berry_crop", "joja_berry_starter", "joja_berry", SveCropData.ProduceType.FRUIT,
                        List.of(0, 1, 2), List.of(5, 5, 5, 5, 5), 4, true, 1, 1, 0.05, 0.15, 650, 75, 1000, 41),
                expected("joja_veggie_crop", "joja_veggie_seeds", "joja_veggie", SveCropData.ProduceType.VEGETABLE,
                        List.of(0, 1, 2), List.of(2, 3, 4, 4), -1, true, 1, 1, 0.02, 0.10, 1140, 200, 200, 49),
                expected("monster_fruit_crop", "stalk_seed", "monster_fruit", SveCropData.ProduceType.FRUIT,
                        List.of(1), List.of(3, 6, 6, 5, 5), -1, false, 0, 0, 0.07, 0.0, 1525, 85, 0, 54),
                expected("salal_berry_crop", "salal_berry_seed", "salal_berry", SveCropData.ProduceType.FRUIT,
                        List.of(0, 1), List.of(2, 2, 3, 3, 3), 4, false, 2, 4, 0.02, 0.03, 75, 28, 0, 14),
                expected("slime_berry_crop", "slime_seed", "slime_berry", SveCropData.ProduceType.FRUIT,
                        List.of(0), List.of(2, 3, 2, 3, 3), 4, false, 1, 3, 0.03, 0.10, 65, -10, 0, 12),
                expected("ancient_fiber_crop", "ancient_ferns_seed", "ancient_fiber", SveCropData.ProduceType.VEGETABLE,
                        List.of(1), List.of(2, 2, 2, 3, 3), -1, false, 2, 4, 0.03, 0.05, 145, 35, 0, 21),
                expected("monster_mushroom_crop", "fungus_seed", "monster_mushroom", SveCropData.ProduceType.VEGETABLE,
                        List.of(2), List.of(2, 2, 3, 3, 3), -1, false, 0, 0, 0.05, 0.0, 850, 75, 0, 45),
                expected("void_root_crop", "void_seed", "void_root", SveCropData.ProduceType.VEGETABLE,
                        List.of(3), List.of(2, 2, 2, 2), -1, false, 0, 0, 0.02, 0.0, 235, -35, 0, 26));
    }

    private static Map.Entry<String, ExpectedCrop> expected(
            String blockPath, String seedPath, String producePath, SveCropData.ProduceType produceType,
            List<Integer> seasons, List<Integer> phaseDays, int regrowDays, boolean raised,
            int minHarvest, int maxHarvest, double maxIncrease, double extraChance,
            int producePrice, int edibility, int seedPrice, int farmingExperience
    ) {
        return Map.entry(blockPath, new ExpectedCrop(
                seedPath, producePath, produceType, seasons, phaseDays, regrowDays, raised,
                minHarvest, maxHarvest, maxIncrease, extraChance,
                producePrice, edibility, seedPrice, farmingExperience));
    }

    private static Map.Entry<String, List<String>> forage(String zone, String item, String... seasons) {
        return Map.entry(zone + "|" + id("forage_" + item), List.of(seasons));
    }

    private static Set<String> readTag(String name) throws IOException {
        JsonObject data = JsonParser.parseString(Files.readString(ITEM_TAG_DIRECTORY.resolve(name + ".json")))
                .getAsJsonObject();
        Set<String> values = new HashSet<>();
        for (JsonElement element : data.getAsJsonArray("values")) values.add(element.getAsString());
        return values;
    }

    private static List<String> strings(JsonArray values) {
        return values.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static String id(String path) {
        return "stardewcraftsve:" + path;
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectClose(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.000_001D) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private record ExpectedCrop(
            String seedPath,
            String producePath,
            SveCropData.ProduceType produceType,
            List<Integer> seasons,
            List<Integer> phaseDays,
            int regrowDays,
            boolean raised,
            int minHarvest,
            int maxHarvest,
            double maxIncrease,
            double extraChance,
            int producePrice,
            int edibility,
            int seedPrice,
            int farmingExperience
    ) {
    }
}
