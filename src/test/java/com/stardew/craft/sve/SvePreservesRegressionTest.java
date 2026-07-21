package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stardew.craft.item.artisan.PreserveType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Regression checks for SVE preserves-jar and dehydrator crop coverage. */
public final class SvePreservesRegressionTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path JAR_RECIPES = RESOURCES.resolve("data/stardewcraft/artisan/sve_preserves_jar.json");
    private static final Path DEHYDRATOR_RECIPES = RESOURCES.resolve("data/stardewcraft/artisan/sve_dehydrator.json");
    private static final Path INGREDIENTS = RESOURCES.resolve(
            "data/stardewcraftsve/preserves/ingredients.json");
    private static final Path LEGACY_DUPLICATE = RESOURCES.resolve(
            "data/stardewcraft/preserves/sve_ingredients.json");
    private static final Path MODELS = RESOURCES.resolve("assets/stardewcraftsve/models/item");

    private SvePreservesRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        expectEquals(12, SvePreservesData.preservesJar().size(), "preserves-jar crop count");
        expectEquals(5, SvePreservesData.dehydratorCrops().size(), "dehydrator crop count");
        expectEquals(4L, SvePreservesData.preservesJar().stream()
                .filter(product -> product.type() == PreserveType.JELLY).count(), "jelly count");
        expectEquals(8L, SvePreservesData.preservesJar().stream()
                .filter(product -> product.type() == PreserveType.PICKLES).count(), "pickles count");
        expectEquals(4L, SvePreservesData.dehydratorCrops().stream()
                .filter(product -> product.type() == PreserveType.DRIED_FRUIT).count(), "dried fruit count");

        validateRecipes(JAR_RECIPES, SvePreservesData.preservesJar(), true);
        validateRecipes(DEHYDRATOR_RECIPES, SvePreservesData.dehydratorCrops(), false);
        validateIngredientMetadata();
        validateClientAssets();
        System.out.println("SVE preserves regression suite passed: 12 jar crops, 5 dehydrator crops");
    }

    private static void validateRecipes(
            Path file,
            List<SvePreservesData.Product> products,
            boolean exactSet
    ) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        Map<String, JsonObject> recipes = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            String inputId = recipe.get("input").getAsString();
            if (!inputId.startsWith("stardewcraftsve:")) continue;
            String inputPath = inputId.substring(inputId.indexOf(':') + 1);
            expect(recipes.put(inputPath, recipe) == null, "duplicate machine input " + inputId);
        }

        Set<String> expectedInputs = products.stream()
                .map(SvePreservesData.Product::inputPath)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (exactSet) expectEquals(expectedInputs, recipes.keySet(), file + " input set");
        else expect(recipes.keySet().containsAll(expectedInputs), file + " missing crop recipes");

        for (SvePreservesData.Product product : products) {
            JsonObject recipe = recipes.get(product.inputPath());
            expect(recipe != null, "missing recipe for " + product.inputPath());
            expectEquals(product.machineOutputId(), recipe.get("output").getAsString(),
                    product.inputPath() + " output");
            expectEquals(product.processingMinutes(), recipe.get("minutes").getAsInt(),
                    product.inputPath() + " minutes");
            expectEquals(product.consume(), recipe.has("consume") ? recipe.get("consume").getAsInt() : 1,
                    product.inputPath() + " consume");
            expectEquals(product.type().name(), recipe.get("preserveType").getAsString(),
                    product.inputPath() + " preserve type");
            expectEquals(product.keepInputQuality(), recipe.get("keepInputQuality").getAsBoolean(),
                    product.inputPath() + " quality behavior");
        }
    }

    private static void validateIngredientMetadata() throws IOException {
        expect(!Files.exists(LEGACY_DUPLICATE),
                "duplicate stardewcraft-namespaced SVE preserves metadata must stay removed");
        JsonObject ingredients = JsonParser.parseString(Files.readString(INGREDIENTS)).getAsJsonObject();
        for (SveCropData.Definition crop : SveCropData.all()) {
            JsonObject data = ingredients.getAsJsonObject(crop.producePath());
            expect(data != null, "missing preserves metadata for " + crop.producePath());
            expectEquals(crop.produceSellPrice(), data.get("price").getAsInt(), crop.producePath() + " price");
            expectEquals(crop.edibility(), data.get("edibility").getAsInt(), crop.producePath() + " edibility");
            expectEquals(SvePreservesData.color(crop.producePath()),
                    data.get("color").getAsString().toUpperCase(), crop.producePath() + " color");
        }
    }

    private static void validateClientAssets() throws IOException {
        Set<String> displayOutputs = new LinkedHashSet<>();
        Stream.concat(SvePreservesData.preservesJar().stream(), SvePreservesData.dehydratorCrops().stream())
                .forEach(product -> {
                    expect(displayOutputs.add(product.displayOutputPath()),
                            "duplicate display output " + product.displayOutputPath());
                    expect(Files.isRegularFile(MODELS.resolve(product.displayOutputPath() + ".json")),
                            "missing model " + product.displayOutputPath());
                });
        expectEquals(17, displayOutputs.size(), "display output count");

        for (String locale : List.of("en_us", "zh_cn")) {
            Path languageFile = RESOURCES.resolve("assets/stardewcraftsve/lang/" + locale + ".json");
            JsonObject language = JsonParser.parseString(Files.readString(languageFile)).getAsJsonObject();
            for (String output : displayOutputs) {
                String key = "item.stardewcraftsve." + output;
                expect(language.has(key), locale + " missing " + key);
                expect(language.has(key + ".desc"), locale + " missing " + key + ".desc");
            }
        }
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
