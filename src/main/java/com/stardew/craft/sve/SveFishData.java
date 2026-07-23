package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Canonical SVE fish catalog, derived from the addon's bundled fishing-rule resource. */
public final class SveFishData {
    private static final String CATALOG_RESOURCE =
            "/data/stardewcraftsve/fishing/locations/sve_fish.json";

    /** Fish with real {@code FishItem} registrations, in deterministic catalog order. */
    public static final List<String> SVE_FISH = loadFishPaths();

    /** Original fishing collection: real fish except Razor Trout, plus non-fish Dulse Seaweed. */
    public static final List<String> FISH_COLLECTION_ITEMS = collectionItems();

    private SveFishData() {
    }

    private static List<String> loadFishPaths() {
        try (InputStream input = SveFishData.class.getResourceAsStream(CATALOG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing canonical SVE fish catalog " + CATALOG_RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("fish") || !root.get("fish").isJsonArray()) {
                throw new IllegalStateException("Canonical SVE fish catalog has no fish array");
            }

            List<String> paths = new ArrayList<>();
            Set<String> unique = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray("fish")) {
                JsonObject rule = element.getAsJsonObject();
                String path = requiredString(rule, "id");
                String item = requiredString(rule, "item");
                String expectedItem = StardewcraftsveMod.MODID + ":" + path;
                if (!expectedItem.equals(item)) {
                    throw new IllegalStateException(
                            "Fish rule " + path + " points to " + item + ", expected " + expectedItem);
                }
                if (!unique.add(path)) {
                    throw new IllegalStateException("Duplicate fish rule " + path);
                }
                paths.add(path);
            }

            List<String> sorted = paths.stream().sorted().toList();
            if (!paths.equals(sorted)) {
                throw new IllegalStateException("Canonical SVE fish catalog must be sorted by id");
            }
            return List.copyOf(paths);
        } catch (IOException | RuntimeException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static List<String> collectionItems() {
        List<String> items = new ArrayList<>(SVE_FISH);
        items.remove("razor_trout");
        items.add("dulse_seaweed");
        items.sort(String::compareTo);
        return List.copyOf(items);
    }

    private static String requiredString(JsonObject object, String field) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            throw new IllegalStateException("Fish rule is missing " + field);
        }
        String value = object.get(field).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Fish rule has blank " + field);
        }
        return value;
    }
}
