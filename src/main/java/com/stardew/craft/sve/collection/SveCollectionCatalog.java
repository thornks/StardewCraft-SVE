package com.stardew.craft.sve.collection;

import com.stardew.craft.cooking.service.VanillaCookingRecipeData;
import com.stardew.craft.data.VanillaObjectCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SveCollectionCatalog {
    public static final String NAMESPACE = "stardewcraftsve";

    private static final List<String> SHIPPING = List.of(
            "ancient_fiber", "bearberrys", "big_conch", "birch_water", "butter",
            "butternut_squash", "camel_wool", "cucumber", "diamond_flower",
            "dried_sand_dollar", "ferngill_primrose", "fir_wax", "gold_carrot",
            "gold_slime_egg", "golden_goose_egg", "golden_ocean_flower", "goldenrod",
            "goose_egg", "goose_mayonnaise", "green_mushroom", "honey_jar",
            "lucky_four_leaf_clover", "mega_purple_mushroom", "monster_fruit",
            "monster_mushroom", "mushroom_colony", "nectarine", "pear", "persimmon",
            "poison_mushroom", "red_baneberry", "rusty_blade", "salal_berry",
            "shark_tooth", "slime_berry", "sludge", "smelly_rafflesia",
            "supernatural_goo", "swamp_essence", "swamp_flower", "sweet_potato",
            "swirl_stone", "thistle", "void_pebble", "void_root", "void_shard",
            "void_soul", "winter_star_rose", "yarn"
    );

    private static final List<String> FISH = List.of(
            "alligator", "arrowhead_shark", "baby_lunaloo", "barred_knifejaw",
            "blue_tang", "bonefish", "bull_trout", "butterfish", "clownfish",
            "daggerfish", "diamond_carp", "dulse_seaweed", "fiber_goby", "frog",
            "gar", "gemfish", "goldenfish", "goldfish", "grass_carp", "highlands_bass",
            "king_salmon", "kittyfish", "lunaloo", "meteor_carp", "minnow",
            "ocean_sunfish", "puppyfish", "radioactive_bass", "sea_sponge", "seahorse",
            "shark", "shiny_lunaloo", "snatcher_worm", "starfish", "swamp_crab",
            "tadpole", "torpedo_trout", "turretfish", "undeadfish", "viper_eel",
            "void_eel", "water_grub", "wolf_snapper"
    );

    private static final List<String> ARTIFACTS = List.of(
            "amber", "boomerang", "faded_button", "fossilized_apple", "magic_lamp",
            "money_bag", "old_coin", "ornate_treasure_chest", "rusty_shield",
            "stone_of_yoba"
    );

    private static final List<String> MINERALS = List.of("galdoran_gem");

    private SveCollectionCatalog() {
    }

    public static List<VanillaObjectCatalog.Entry> entriesForTab(int tab) {
        return switch (tab) {
            case 0 -> staticEntries(SHIPPING, "Basic", -79);
            case 1 -> staticEntries(FISH, "Fish", -4);
            case 2 -> staticEntries(ARTIFACTS, "Arch", 0);
            case 3 -> staticEntries(MINERALS, "Minerals", -2);
            case 4 -> cookingEntries();
            default -> List.of();
        };
    }

    public static boolean isSveEntry(VanillaObjectCatalog.Entry entry) {
        return entry != null && entry.key().startsWith(NAMESPACE + ":");
    }

    /** Static collection IDs used by validation without requiring a live item registry. */
    public static List<String> configuredItemIdsForTab(int tab) {
        List<String> paths = switch (tab) {
            case 0 -> SHIPPING;
            case 1 -> FISH;
            case 2 -> ARTIFACTS;
            case 3 -> MINERALS;
            default -> List.of();
        };
        return paths.stream().map(path -> NAMESPACE + ":" + path).toList();
    }

    private static List<VanillaObjectCatalog.Entry> staticEntries(
            List<String> paths,
            String type,
            int category
    ) {
        List<VanillaObjectCatalog.Entry> entries = new ArrayList<>(paths.size());
        for (String path : paths) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                entries.add(entry(id, type, category));
            }
        }
        return List.copyOf(entries);
    }

    private static List<VanillaObjectCatalog.Entry> cookingEntries() {
        Map<ResourceLocation, VanillaObjectCatalog.Entry> entries = new LinkedHashMap<>();
        for (ResourceLocation recipeId : VanillaCookingRecipeData.getRecipeIds()) {
            if (!NAMESPACE.equals(recipeId.getNamespace())) {
                continue;
            }

            ItemStack output = VanillaCookingRecipeData.getOutputStack(recipeId, 1);
            if (output.isEmpty()) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(output.getItem());
            if (itemId != null && NAMESPACE.equals(itemId.getNamespace())) {
                entries.putIfAbsent(itemId, entry(itemId, "Cooking", -7));
            }
        }
        return List.copyOf(entries.values());
    }

    private static VanillaObjectCatalog.Entry entry(
            ResourceLocation id,
            String type,
            int category
    ) {
        return new VanillaObjectCatalog.Entry(
                id.toString(),
                id.getPath(),
                type,
                category,
                "",
                0,
                false,
                false
        );
    }
}
