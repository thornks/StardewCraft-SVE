package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stardew.craft.sve.collection.SveCollectionCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Regression checks for SVE animal products and their original machine routes. */
public final class SveAnimalProductRegressionTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path ARTISAN = RESOURCES.resolve("data/stardewcraft/artisan");
    private static final Path ITEM_SOURCE = Path.of(
            "src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final Path BLOCK_SOURCE = Path.of(
            "src/main/java/com/stardew/craft/sve/ModBlocks.java");
    private static final Path TIMED_MACHINE_SOURCE = Path.of(
            "src/main/java/com/stardew/craft/sve/SveTimedMachineBlockEntity.java");
    private static final Path ITEM_MODELS = RESOURCES.resolve(
            "assets/stardewcraftsve/models/item");

    private SveAnimalProductRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        validateButterChurner();
        validateYarnSpooler();
        validateGooseMayonnaise();
        validateIncubation();
        validateNoInventedLoomRoutes();
        validateItemMetadataAndCollections();
        System.out.println("SVE animal-product regression suite passed: "
                + "quality butter and yarn, goose mayonnaise, and incubation");
    }

    private static void validateButterChurner() throws IOException {
        JsonObject root = readJson(ARTISAN.resolve("butter_churner.json"));
        expectEquals("butter_churner", root.get("machine").getAsString(),
                "butter churner machine id");

        Set<String> expectedInputs = Set.of(
                "stardewcraft:milk", "stardewcraft:large_milk",
                "stardewcraft:goat_milk", "stardewcraft:large_goat_milk");
        JsonArray recipes = root.getAsJsonArray("recipes");
        expectEquals(expectedInputs.size(), recipes.size(), "butter recipe count");
        Set<String> actualInputs = new java.util.LinkedHashSet<>();
        for (var element : recipes) {
            JsonObject recipe = element.getAsJsonObject();
            String input = recipe.get("input").getAsString();
            expect(actualInputs.add(input), "duplicate butter input " + input);
            expectEquals("stardewcraftsve:butter", recipe.get("output").getAsString(),
                    input + " butter output");
            expectEquals(60, recipe.get("minutes").getAsInt(), input + " processing time");
            expectEquals("keep", recipe.get("quality").getAsString(),
                    input + " quality inheritance");
        }
        expectEquals(expectedInputs, actualInputs, "butter input set");

        String source = Files.readString(TIMED_MACHINE_SOURCE);
        expect(source.contains("recipe.minutes() > 0 ? recipe.minutes() : fallbackProcessingMinutes"),
                "butter churner runtime must use recipe processing time");
        expectEquals(1, countOccurrences(source,
                        "readyAtAbsMinute = getCurrentAbsMinute()"),
                "butter churner must have one shared recipe-start path");
    }

    private static void validateYarnSpooler() throws IOException {
        JsonObject root = readJson(ARTISAN.resolve("yarn_spooler.json"));
        expectEquals("yarn_spooler", root.get("machine").getAsString(),
                "yarn spooler machine id");
        JsonObject recipe = onlyRecipe(root, "yarn spooler");
        expectEquals("stardewcraftsve:camel_wool", recipe.get("input").getAsString(),
                "yarn input");
        expectEquals("stardewcraftsve:yarn", recipe.get("output").getAsString(),
                "yarn output");
        expectEquals(120, recipe.get("minutes").getAsInt(), "yarn processing time");
        expectEquals("keep", recipe.get("quality").getAsString(),
                "yarn quality inheritance");
        String source = Files.readString(TIMED_MACHINE_SOURCE);
        expect(source.contains("QualityHelper.setQuality(output, QualityHelper.getQuality(source))"),
                "yarn spooler runtime must copy input quality");
    }

    private static void validateGooseMayonnaise() throws IOException {
        JsonObject root = readJson(ARTISAN.resolve("sve_mayonnaise_machine.json"));
        expectEquals("mayonnaise_machine", root.get("machine").getAsString(),
                "mayonnaise machine id");
        JsonObject recipe = onlyRecipe(root, "goose mayonnaise");
        expectEquals("stardewcraftsve:goose_egg", recipe.get("input").getAsString(),
                "goose mayonnaise input");
        expectEquals("stardewcraftsve:goose_mayonnaise", recipe.get("output").getAsString(),
                "goose mayonnaise output");
        expectEquals(180, recipe.get("minutes").getAsInt(),
                "goose mayonnaise processing time");
        expect(!recipe.has("quality"), "goose mayonnaise must always be normal quality");

        String source = Files.readString(ITEM_SOURCE);
        expect(source.contains("DeferredHolder<Item, StardewQualityItem> GOOSE_MAYONNAISE"),
                "goose mayonnaise must use the standard artisan item implementation");
        expect(source.contains("\"stardewcraft.type.artisan_goods\", 700, 45, false"),
                "goose mayonnaise metadata must match SVE");

        JsonObject model = readJson(ITEM_MODELS.resolve("goose_mayonnaise.json"));
        expect(!model.has("overrides"), "goose mayonnaise must not have quality variants");
    }

    private static void validateIncubation() throws IOException {
        JsonObject root = readJson(ARTISAN.resolve("sve_incubator.json"));
        expectEquals("incubator", root.get("machine").getAsString(), "incubator machine id");
        JsonObject recipe = onlyRecipe(root, "goose incubation");
        expectEquals("stardewcraftsve:goose_egg", recipe.get("input").getAsString(),
                "incubator input");
        expectEquals("copy_input", recipe.get("outputMode").getAsString(),
                "incubator output mode");
        expectEquals(18_000, recipe.get("minutes").getAsInt(), "goose incubation time");
    }

    private static void validateNoInventedLoomRoutes() {
        expect(!Files.exists(ARTISAN.resolve("sve_loom.json")),
                "SVE does not define camel wool or ancient fiber loom recipes");
    }

    private static void validateItemMetadataAndCollections() throws IOException {
        String source = Files.readString(ITEM_SOURCE);
        String blocks = Files.readString(BLOCK_SOURCE);
        Map<String, String> registrations = Map.of(
                "butter", "\"stardewcraft.type.artisan_goods\", 215, 31, true",
                "camel_wool", "\"stardewcraft.type.animal_product\", 450, -300, true",
                "yarn", "\"stardewcraft.type.artisan_goods\", 900, -300, true",
                "goose_egg", "\"stardewcraft.type.animal_product\", 300, 15, true",
                "golden_goose_egg", "\"stardewcraft.type.animal_product\", 6000, 0, true"
        );
        for (var entry : registrations.entrySet()) {
            expect(source.contains(entry.getValue()), entry.getKey() + " metadata must match SVE");
            validateQualityModels(entry.getKey());
        }

        Set<String> shipping = Set.copyOf(SveCollectionCatalog.configuredItemIdsForTab(0));
        for (String path : Set.of(
                "butter", "camel_wool", "yarn", "goose_egg",
                "golden_goose_egg", "goose_mayonnaise")) {
            expect(shipping.contains("stardewcraftsve:" + path),
                    path + " must appear in the shipping collection");
        }
        expect(!blocks.contains("FORAGE_YARN"),
                "obsolete yarn forage block must not remain registered");
        expect(!Files.exists(RESOURCES.resolve(
                        "assets/stardewcraftsve/blockstates/forage_yarn.json")),
                "obsolete yarn forage blockstate must be removed");
    }

    private static void validateQualityModels(String path) throws IOException {
        JsonObject model = readJson(ITEM_MODELS.resolve(path + ".json"));
        expectEquals(3, model.getAsJsonArray("overrides").size(),
                path + " quality override count");
        for (String quality : Set.of("silver", "gold", "iridium")) {
            expect(Files.isRegularFile(ITEM_MODELS.resolve(path + "_" + quality + ".json")),
                    path + " missing " + quality + " model");
        }
    }

    private static JsonObject onlyRecipe(JsonObject root, String label) {
        JsonArray recipes = root.getAsJsonArray("recipes");
        expectEquals(1, recipes.size(), label + " recipe count");
        return recipes.get(0).getAsJsonObject();
    }

    private static JsonObject readJson(Path path) throws IOException {
        expect(Files.isRegularFile(path), "missing JSON " + path);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
