package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Regression checks for SVE crafting ingredients, outputs, sizes, and skill unlocks. */
public final class SveCraftingRegressionTest {
    private static final Path RECIPES = Path.of(
            "src/main/resources/data/stardewcraftsve/player/crafting_recipes");
    private static final Path BIG_CRAFTABLE_TEXTURES = Path.of(
            "src/main/resources/assets/stardewcraftsve/textures/gui/crafting");

    private SveCraftingRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, SveCraftingData.Definition> definitions = definitions();
        expectEquals(12, definitions.size(), "SVE crafting definition count");
        expectEquals(Set.of("butter_churner", "yarn_spooler"), definitions.values().stream()
                .filter(SveCraftingData.Definition::bigCraftable)
                .map(SveCraftingData.Definition::path)
                .collect(Collectors.toSet()), "big craftable recipe set");
        validateRecipes(definitions);
        validateBigCraftableTextures();
        System.out.println("SVE crafting regression suite passed: 12 recipes, 2 big craftables");
    }

    private static void validateBigCraftableTextures() throws IOException {
        for (String path : List.of("butter_churner", "yarn_spooler")) {
            Path texture = BIG_CRAFTABLE_TEXTURES.resolve(path + ".png");
            expect(Files.isRegularFile(texture), path + " big craftable texture exists");
            var image = ImageIO.read(texture.toFile());
            expect(image != null, path + " big craftable texture decodes");
            expectEquals(16, image.getWidth(), path + " big craftable texture width");
            expectEquals(32, image.getHeight(), path + " big craftable texture height");
        }
    }

    private static Map<String, SveCraftingData.Definition> definitions() {
        Map<String, SveCraftingData.Definition> definitions = new LinkedHashMap<>();
        for (SveCraftingData.Definition definition : SveCraftingData.all()) {
            expect(definitions.put(definition.path(), definition) == null,
                    "duplicate crafting definition " + definition.path());
        }
        return definitions;
    }

    private static void validateRecipes(Map<String, SveCraftingData.Definition> definitions) throws IOException {
        Map<String, Path> files = new LinkedHashMap<>();
        try (var paths = Files.list(RECIPES)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                String name = file.getFileName().toString();
                files.put(name.substring(0, name.length() - ".json".length()), file);
            }
        }
        expectEquals(definitions.keySet(), files.keySet(), "crafting recipe file set");

        for (SveCraftingData.Definition definition : definitions.values()) {
            JsonObject recipe = JsonParser.parseString(Files.readString(files.get(definition.path())))
                    .getAsJsonObject();
            String label = definition.path();
            expectEquals(definition.outputId(), recipe.get("output").getAsString(), label + " output");
            expectEquals(definition.outputCount(), integer(recipe, "output_count", 1), label + " output count");
            expectEquals(definition.bigCraftable(), bool(recipe, "big_craftable", false),
                    label + " big craftable");
            if (definition.legacyUnlockCondition() == null) {
                expect(!recipe.has("legacy_unlock_condition"), label + " must have no automatic unlock condition");
            } else {
                expectEquals(definition.legacyUnlockCondition(),
                        recipe.get("legacy_unlock_condition").getAsString(), label + " unlock condition");
            }

            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            expectEquals(definition.ingredients().size(), ingredients.size(), label + " ingredient count");
            for (int index = 0; index < ingredients.size(); index++) {
                SveCraftingData.Ingredient expected = definition.ingredients().get(index);
                JsonObject actual = ingredients.get(index).getAsJsonObject();
                expectEquals(expected.itemId(), actual.get("item").getAsString(),
                        label + " ingredient " + index + " item");
                expectEquals(expected.count(), integer(actual, "count", 1),
                        label + " ingredient " + index + " count");
            }
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
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
