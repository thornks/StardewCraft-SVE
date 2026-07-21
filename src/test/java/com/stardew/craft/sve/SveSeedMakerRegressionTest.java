package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Regression checks for SVE seed-maker eligibility and recipes. */
public final class SveSeedMakerRegressionTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path RECIPES = RESOURCES.resolve("data/stardewcraft/artisan/sve_seed_maker.json");
    private static final Path BANNED_TAG = RESOURCES.resolve(
            "data/stardewcraft/tags/item/seedmaker_banned.json");

    private SveSeedMakerRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        Set<String> allowed = producePaths(SveSeedMakerData.allowed());
        Set<String> banned = producePaths(SveSeedMakerData.banned());

        expectEquals(11, allowed.size(), "allowed crop count");
        expectEquals(Set.of("gold_carrot"), banned, "banned crop set");
        expect(!allowed.contains("gold_carrot"), "gold carrot must not be seed-maker eligible");

        validateRecipes(allowed);
        validateBannedTag(banned);
        System.out.println("SVE seed-maker regression suite passed: 11 allowed crops, 1 banned crop");
    }

    private static void validateRecipes(Set<String> allowed) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(RECIPES)).getAsJsonObject();
        expectEquals("seed_maker", root.get("machine").getAsString(), "machine id");

        Set<String> inputs = new LinkedHashSet<>();
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            String input = recipe.get("input").getAsString();
            expect(input.startsWith("stardewcraftsve:"), "SVE recipe namespace: " + input);
            String path = input.substring(input.indexOf(':') + 1);
            expect(inputs.add(path), "duplicate seed-maker input " + input);
            expectEquals(SveSeedMakerData.PROCESSING_MINUTES,
                    recipe.get("minutes").getAsInt(), path + " processing time");
            expectEquals("seedmaker", recipe.get("outputMode").getAsString(), path + " output mode");
        }
        expectEquals(allowed, inputs, "seed-maker recipe set");
    }

    private static void validateBannedTag(Set<String> banned) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(BANNED_TAG)).getAsJsonObject();
        expect(!root.get("replace").getAsBoolean(), "banned tag must extend the base tag");

        Set<String> values = new LinkedHashSet<>();
        for (JsonElement element : root.getAsJsonArray("values")) {
            String id = element.getAsString();
            expect(id.startsWith("stardewcraftsve:"), "SVE banned tag namespace: " + id);
            expect(values.add(id.substring(id.indexOf(':') + 1)), "duplicate banned tag entry " + id);
        }
        expectEquals(banned, values, "seed-maker banned tag set");
    }

    private static Set<String> producePaths(java.util.List<SveCropData.Definition> crops) {
        return crops.stream()
                .map(SveCropData.Definition::producePath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
