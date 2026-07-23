package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Shared item paths used by the SVE collection pages and creative tabs. */
public final class SveItemCatalog {
    private static final String ARTIFACT_TAG_RESOURCE =
            "/data/stardewcraft/tags/item/artifacts.json";

    private static final Set<String> NON_SHIPPING_CROPS = Set.of("joja_berry", "joja_veggie");

    private static final List<String> SHIPPING_EXTRAS = List.of(
            "bearberrys", "big_conch", "birch_water", "butter", "camel_wool",
            "diamond_flower", "dried_sand_dollar", "ferngill_primrose", "fir_wax",
            "gold_slime_egg", "golden_goose_egg", "golden_ocean_flower", "goldenrod",
            "goose_egg", "goose_mayonnaise", "green_mushroom", "honey_jar",
            "lucky_four_leaf_clover", "magic_lamp", "mega_purple_mushroom",
            "mushroom_colony", "nectarine", "pear", "persimmon", "poison_mushroom",
            "red_baneberry", "rusty_blade", "shark_tooth", "sludge", "smelly_rafflesia",
            "supernatural_goo", "swamp_essence", "swamp_flower", "swirl_stone", "thistle",
            "void_pebble", "void_shard", "void_soul", "winter_star_rose", "yarn");

    private static final List<String> CREATIVE_CROP_EXTRAS = List.of(
            "bearberrys", "nectarine", "pear", "persimmon", "dewdrop_berry");
    private static final List<String> CREATIVE_SEED_EXTRAS = List.of(
            "birch_seed", "fir_cone", "nectarine_sapling", "pear_sapling", "persimmon_sapling");
    private static final List<String> CREATIVE_COOKING_EXTRAS = List.of(
            "grampleton_orange_chicken", "seed_cookie", "void_mayo_sandwich");
    private static final List<String> CREATIVE_ARTISAN_EXTRAS = List.of(
            "aged_blue_moon_wine", "blue_moon_wine", "butter", "goose_mayonnaise", "honey_jar",
            "butter_churner", "yarn_spooler", "green_mushroom_dried_mushrooms",
            "mega_purple_mushroom_dried_mushrooms", "mushroom_colony_dried_mushrooms",
            "poison_mushroom_dried_mushrooms", "sve_roe", "sve_aged_roe");

    public static final List<String> SHIPPING = sortedUnique(Stream.concat(
            SveCropData.all().stream()
                    .map(SveCropData.Definition::producePath)
                    .filter(path -> !NON_SHIPPING_CROPS.contains(path)),
            SHIPPING_EXTRAS.stream()));
    public static final List<String> FISH = SveFishData.FISH_COLLECTION_ITEMS;
    public static final List<String> ARTIFACTS = loadSveArtifactPaths();
    public static final List<String> MINERALS = List.of("galdoran_gem");
    public static final List<String> COOKING = SveCookingData.all().stream()
            .map(SveCookingData.Definition::path)
            .toList();

    public static final List<String> CREATIVE_CROPS = orderedUnique(Stream.concat(
            SveCropData.all().stream().map(SveCropData.Definition::producePath),
            CREATIVE_CROP_EXTRAS.stream()));
    public static final List<String> CREATIVE_SEEDS = orderedUnique(Stream.concat(
            SveCropData.all().stream().map(SveCropData.Definition::seedPath),
            CREATIVE_SEED_EXTRAS.stream()));
    public static final List<String> CREATIVE_COOKING = orderedUnique(Stream.concat(
            COOKING.stream().filter(path -> !path.equals("prismatic_pop")),
            CREATIVE_COOKING_EXTRAS.stream()));
    public static final List<String> CREATIVE_ARTISAN_NORMAL = orderedUnique(Stream.of(
            CREATIVE_ARTISAN_EXTRAS.stream(),
            SveKegData.all().stream()
                    .filter(product -> !product.supportsQuality())
                    .map(SveKegData.Product::outputPath),
            SvePreservesData.preservesJar().stream().map(SvePreservesData.Product::displayOutputPath),
            SvePreservesData.dehydratorCrops().stream().map(SvePreservesData.Product::displayOutputPath)
    ).flatMap(stream -> stream));
    public static final List<String> CREATIVE_ARTISAN_QUALITY = orderedUnique(Stream.concat(
            Stream.of("yarn"),
            SveKegData.all().stream()
                    .filter(SveKegData.Product::supportsQuality)
                    .map(SveKegData.Product::outputPath)));

    private SveItemCatalog() {
    }

    private static List<String> loadSveArtifactPaths() {
        try (InputStream input = SveItemCatalog.class.getResourceAsStream(ARTIFACT_TAG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing artifact tag " + ARTIFACT_TAG_RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("values") || !root.get("values").isJsonArray()) {
                throw new IllegalStateException("Artifact tag has no values array");
            }
            List<String> paths = new ArrayList<>();
            for (JsonElement value : root.getAsJsonArray("values")) {
                String id = value.getAsString();
                String prefix = StardewcraftsveMod.MODID + ":";
                if (id.startsWith(prefix)) {
                    paths.add(id.substring(prefix.length()));
                }
            }
            return sortedUnique(paths.stream());
        } catch (IOException | RuntimeException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static List<String> orderedUnique(Stream<String> paths) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        paths.forEach(path -> {
            if (path == null || path.isBlank()) {
                throw new IllegalStateException("Blank item path in SVE item catalog");
            }
            if (!unique.add(path)) {
                throw new IllegalStateException("Duplicate item path in SVE item catalog: " + path);
            }
        });
        return List.copyOf(unique);
    }

    private static List<String> sortedUnique(Stream<String> paths) {
        return orderedUnique(paths.sorted());
    }
}
