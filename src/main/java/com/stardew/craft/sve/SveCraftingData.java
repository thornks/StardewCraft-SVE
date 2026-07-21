package com.stardew.craft.sve;

import java.util.List;

/** Authoritative crafting rules sourced from SVE 1.15.11's CraftingRecipes data. */
public final class SveCraftingData {
    private static final List<Definition> ALL = List.of(
            recipe("armor_elixir", false, "s Combat 7",
                    item("stardewcraft:solar_essence", 50), item("stardewcraftsve:void_soul", 5),
                    item("stardewcraft:bone_fragment", 30), item("stardewcraft:vinegar")),
            recipe("haste_elixir", false, "s Combat 8",
                    item("stardewcraft:void_essence", 50), item("stardewcraftsve:void_soul", 5),
                    item("stardewcraft:spicy_eel", 3), item("stardewcraft:sugar")),
            recipe("hero_elixir", false, "s Combat 9",
                    item("stardewcraft:slime_item", 50), item("stardewcraftsve:void_soul", 5),
                    item("stardewcraftsve:void_pebble", 10), item("stardewcraft:oil")),
            recipe("seed_cookie", false, "s Foraging 3",
                    item("stardewcraftsve:fir_cone"), item("stardewcraftsve:birch_seed"),
                    item("stardewcraft:acorn"), item("stardewcraft:maple_seed"),
                    item("stardewcraft:pine_cone")),
            recipe("small_hardwood_fence", false, null,
                    item("stardewcraft:wood_hard")),
            recipe("hedge_fence", false, "s Farming 6",
                    item("stardewcraft:fiber", 3), item("stardewcraft:wood_normal")),
            recipe("bombardier_elixir", false, "s Combat 9",
                    item("stardewcraft:solar_essence", 30), item("stardewcraft:void_essence", 30),
                    item("stardewcraftsve:void_soul", 10), item("stardewcraftsve:void_pebble", 20),
                    item("stardewcraft:cherry_bomb", 5)),
            recipe("butter_churner", true, "s Farming 3",
                    item("stardewcraft:wood_normal", 25), item("stardewcraft:stone", 25),
                    item("stardewcraft:frozen_tear"), item("stardewcraft:iron_bar")),
            recipe("yarn_spooler", true, "s Farming 9",
                    item("stardewcraft:wood_hard", 25), item("stardewcraft:battery_pack"),
                    item("stardewcraftsve:fir_wax"), item("stardewcraft:pine_tar")),
            recipe("sun_totem", false, "s Foraging 9",
                    item("stardewcraft:wood_hard"), item("stardewcraft:solar_essence", 10),
                    item("stardewcraftsve:birch_water")),
            recipe("wind_totem", false, "s Foraging 9",
                    item("stardewcraft:wood_hard"), item("stardewcraft:bat_wing", 10),
                    item("stardewcraftsve:fir_wax")),
            // The original unlock is Henchman's four-heart event; Combat 8 remains the temporary fallback.
            recipe("marsh_tonic", false, "s Combat 8",
                    item("stardewcraft:slime_item", 30), item("stardewcraftsve:swamp_essence", 15),
                    item("stardewcraftsve:swamp_flower", 10), item("stardewcraft:sugar"))
    );

    private SveCraftingData() {
    }

    public static List<Definition> all() {
        return ALL;
    }

    private static Definition recipe(
            String path,
            boolean bigCraftable,
            String legacyUnlockCondition,
            Ingredient... ingredients
    ) {
        return new Definition(
                path,
                "stardewcraftsve:" + path,
                1,
                bigCraftable,
                legacyUnlockCondition,
                List.of(ingredients));
    }

    private static Ingredient item(String id) {
        return item(id, 1);
    }

    private static Ingredient item(String id, int count) {
        return new Ingredient(id, count);
    }

    public record Ingredient(String itemId, int count) {
        public Ingredient {
            if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId");
            if (count <= 0) throw new IllegalArgumentException("count");
        }
    }

    public record Definition(
            String path,
            String outputId,
            int outputCount,
            boolean bigCraftable,
            String legacyUnlockCondition,
            List<Ingredient> ingredients
    ) {
        public Definition {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("path");
            if (outputId == null || outputId.isBlank()) throw new IllegalArgumentException("outputId");
            if (outputCount <= 0) throw new IllegalArgumentException("outputCount");
            ingredients = List.copyOf(ingredients);
            if (ingredients.isEmpty()) throw new IllegalArgumentException("ingredients");
        }
    }
}
