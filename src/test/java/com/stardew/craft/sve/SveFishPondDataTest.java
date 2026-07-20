package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Structural checks for all SVE fish pond data files; no game world is required. */
public final class SveFishPondDataTest {
    private static final Path DATA_DIRECTORY = Path.of(
            "src/main/resources/data/stardewcraftsve/fishpond/pond_data");
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> FIXED_SINGLE_FISH_PONDS = Set.of("turretfish", "wolf_snapper");

    private SveFishPondDataTest() {
    }

    public static void main(String[] args) throws IOException {
        Set<String> files = new HashSet<>();
        try (var paths = Files.list(DATA_DIRECTORY)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                String fishId = path.getFileName().toString().replaceFirst("\\.json$", "");
                files.add(fishId);
                validateDefinition(fishId, JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            }
        }

        expectEquals(new HashSet<>(SveFishData.FISH_POND_ITEMS), files, "fish pond file set");
        expect(new HashSet<>(SveFishData.FISH_POND_ITEMS).containsAll(SveFishData.SVE_FISH),
                "every SVE fish must have fish pond data");
        expectEquals(43, files.size(), "fish pond definition count");
        System.out.println("SVE fish pond data regression suite passed");
    }

    private static void validateDefinition(String fishId, JsonObject data) {
        expectEquals("stardewcraftsve:" + fishId, string(data, "fish"), fishId + " fish id");
        int maxPopulation = integer(data, "max_population");
        if (FIXED_SINGLE_FISH_PONDS.contains(fishId)) {
            expectEquals(1, maxPopulation, fishId + " fixed population");
            expect(!data.has("population_gates") || data.get("population_gates").isJsonNull(),
                    fishId + " must not have population gates");
        } else {
            expect(maxPopulation <= 0, fishId + " must use dynamic population gates");
        }
        expect(integer(data, "spawn_time") >= -1, fishId + " spawn time");

        double minimumChance = decimal(data, "base_min_produce_chance");
        double maximumChance = decimal(data, "base_max_produce_chance");
        expect(validChance(minimumChance) && validChance(maximumChance) && minimumChance <= maximumChance,
                fishId + " base chance range");

        JsonArray productions = data.getAsJsonArray("produced_items");
        expect(productions != null && !productions.isEmpty(), fishId + " productions");
        for (JsonElement element : productions) {
            JsonObject production = element.getAsJsonObject();
            expectResourceId(string(production, "item"), fishId + " production item");
            int requiredPopulation = integer(production, "required_population");
            expect(requiredPopulation >= 0 && requiredPopulation <= 10,
                    fishId + " production population");
            expect(validChance(decimal(production, "chance")), fishId + " production chance");
            int minimumCount = integer(production, "min_count");
            int maximumCount = integer(production, "max_count");
            expect(minimumCount >= 1 && maximumCount >= minimumCount,
                    fishId + " production count range");
        }

        if (data.has("population_gates") && !data.get("population_gates").isJsonNull()) {
            JsonObject gates = data.getAsJsonObject("population_gates");
            expect(!gates.entrySet().isEmpty(), fishId + " population gates");
            for (var gate : gates.entrySet()) {
                int population = Integer.parseInt(gate.getKey());
                expect(population >= 2 && population <= 10, fishId + " gate population");
                JsonArray requests = gate.getValue().getAsJsonArray();
                expect(!requests.isEmpty(), fishId + " gate requests");
                for (JsonElement requestElement : requests) {
                    JsonObject request = requestElement.getAsJsonObject();
                    expectResourceId(string(request, "item"), fishId + " gate item");
                    int minimumCount = integer(request, "min_count");
                    int maximumCount = integer(request, "max_count");
                    expect(minimumCount >= 1 && maximumCount >= minimumCount,
                            fishId + " gate count range");
                }
            }
        }
    }

    private static void expectResourceId(String value, String label) {
        expect(RESOURCE_ID.matcher(value).matches(), label + ": " + value);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) throw new AssertionError("Missing string " + key);
        return value.getAsString();
    }

    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) throw new AssertionError("Missing integer " + key);
        return value.getAsInt();
    }

    private static double decimal(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) throw new AssertionError("Missing number " + key);
        return value.getAsDouble();
    }

    private static boolean validChance(double chance) {
        return Double.isFinite(chance) && chance >= 0.0D && chance <= 1.0D;
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
