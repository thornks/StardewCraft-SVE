package com.stardew.craft.sve;

import com.stardew.craft.item.cooking.CookingDishItem;
import com.stardew.craft.item.cooking.CookingDishItem.BuffType;
import com.stardew.craft.item.cooking.CookingDishItem.DishBuff;
import net.minecraft.world.item.Item;

import java.util.List;

/** Authoritative cooking rules sourced from SVE's Objects and CookingRecipes data. */
public final class SveCookingData {
    private static final List<Definition> ALL = List.of(
            dish("baked_berry_oatmeal_supreme", 400, 80,
                    shop("stardewcraftsve:bear_vendor"),
                    buffs(buff(BuffType.FARMING, 4, 96_000), buff(BuffType.FORAGING, 4, 96_000),
                            buff(BuffType.SPEED, 2, 96_000)),
                    item("stardewcraft:salmonberry", 15), item("stardewcraft:blackberry", 15),
                    item("stardewcraft:sugar"), item("stardewcraft:wheat_flour")),
            dish("big_bark_burger", 400, 85,
                    shop("stardewcraft:saloon"),
                    buffs(buff(BuffType.SPEED, 1, 28_800), buff(BuffType.ATTACK, 3, 28_800)),
                    item("stardewcraftsve:puppyfish"), item("stardewcraft:bread"), item("stardewcraft:oil")),
            dish("flower_cookie", 185, 75,
                    shop("stardewcraftsve:bear_vendor"),
                    buffs(buff(BuffType.LUCK, 2, 48_000), buff(BuffType.FORAGING, 2, 48_000),
                            buff(BuffType.SPEED, 2, 48_000)),
                    item("stardewcraftsve:ferngill_primrose"), item("stardewcraftsve:goldenrod"),
                    item("stardewcraftsve:winter_star_rose"), item("stardewcraft:wheat_flour"),
                    item("stardewcraft:sugar"), tag("c:eggs", "stardewcraft:egg_white")),
            dish("frog_legs", 400, 30,
                    shop("stardewcraft:adventure_shop"),
                    buffs(buff(BuffType.SPEED, 2, 28_800), buff(BuffType.DEFENSE, 2, 28_800)),
                    item("stardewcraftsve:frog"), item("stardewcraft:oil"), item("stardewcraft:wheat_flour")),
            dish("glazed_butterfish", 800, 80,
                    shop("stardewcraft:saloon"),
                    buffs(buff(BuffType.FISHING, 2, 28_800), buff(BuffType.LUCK, 2, 28_800)),
                    item("stardewcraftsve:butterfish"), item("stardewcraft:wheat_flour", 2),
                    item("stardewcraft:oil", 2), item("stardewcraft:sugar")),
            dish("mixed_berry_pie", 250, 75,
                    shop("stardewcraft:saloon"),
                    buffs(buff(BuffType.FARMING, 3, 96_000), buff(BuffType.MAX_ENERGY, 50, 96_000)),
                    item("stardewcraft:strawberry"), item("stardewcraftsve:salal_berry"),
                    item("stardewcraft:blackberry"), item("stardewcraftsve:bearberrys"),
                    item("stardewcraft:sugar"), item("stardewcraft:wheat_flour")),
            dish("mushroom_berry_rice", 115, 1,
                    shop("stardewcraft:adventure_shop"),
                    buffs(buff(BuffType.MINING, 3, 48_000), buff(BuffType.MAX_ENERGY, -50, 48_000),
                            buff(BuffType.MAGNETIC_RADIUS, 32, 48_000), buff(BuffType.DEFENSE, 3, 48_000),
                            buff(BuffType.ATTACK, 3, 48_000)),
                    item("stardewcraftsve:poison_mushroom", 3), item("stardewcraftsve:red_baneberry", 10),
                    item("stardewcraft:rice"), item("stardewcraft:sugar", 2)),
            dish("seaweed_salad", 200, 70,
                    shop("stardewcraft:fish_shop"),
                    buffs(buff(BuffType.FISHING, 1, 54_000), buff(BuffType.MAX_ENERGY, 30, 54_000)),
                    item("stardewcraftsve:dulse_seaweed", 2), item("stardewcraft:seaweed", 2),
                    item("stardewcraft:oil")),
            dish("void_delight", 800, 1,
                    shop("stardewcraft:shadow_shop"),
                    buffs(buff(BuffType.MINING, 2, 36_000), buff(BuffType.LUCK, 2, 36_000),
                            buff(BuffType.MAX_ENERGY, 20, 36_000), buff(BuffType.MAGNETIC_RADIUS, 2, 36_000),
                            buff(BuffType.SPEED, 2, 36_000), buff(BuffType.ATTACK, 3, 36_000)),
                    item("stardewcraftsve:void_eel"), item("stardewcraft:void_essence", 50),
                    item("stardewcraft:solar_essence", 20)),
            dish("void_salmon_sushi", 800, 1,
                    shop("stardewcraft:shadow_shop"),
                    buffs(buff(BuffType.FISHING, 3, 72_000), buff(BuffType.LUCK, 3, 72_000),
                            buff(BuffType.MAX_ENERGY, 80, 72_000), buff(BuffType.DEFENSE, 5, 72_000)),
                    item("stardewcraft:void_salmon"), item("stardewcraft:void_mayonnaise"),
                    item("stardewcraft:seaweed", 3)),
            drink("birch_syrup", 500, 35,
                    shop("stardewcraft:seed_shop"), List.of(),
                    item("stardewcraftsve:birch_water", 3), item("stardewcraft:sap", 25)),
            dish("candy", 2_000, 50,
                    shop("stardewcraft:saloon"),
                    buffs(buff(BuffType.LUCK, 1, 30_000), buff(BuffType.MAX_ENERGY, 70, 30_000),
                            buff(BuffType.SPEED, 1, 30_000)),
                    item("stardewcraftsve:birch_syrup"), item("stardewcraftsve:birch_water"),
                    item("stardewcraft:sugar")),
            dish("chocolate_truffle_bar", 4_500, 75,
                    shop("stardewcraft:festival_festival_of_ice_traveling_merchant"), List.of(),
                    item("stardewcraft:hazelnut"), item("stardewcraft:truffle"),
                    tag("c:milk", "stardewcraft:milk"), item("stardewcraft:sugar")),
            dish("vegan_cone", 350, 50,
                    shop("stardewcraft:oasis_shop"), List.of(),
                    item("stardewcraft:coconut"), item("stardewcraft:sugar")),
            dish("ice_cream_sundae", 400, 80,
                    shop("stardewcraft:festival_luau_pierre"),
                    buffs(buff(BuffType.MAX_ENERGY, 75, 36_000), buff(BuffType.SPEED, 1, 36_000)),
                    item("stardewcraft:hazelnut"), tag("c:milk", "stardewcraft:milk"),
                    item("stardewcraft:sugar"), item("stardewcraft:cherry")),
            dish("prismatic_pop", 2_500, 100,
                    shop("stardewcraft:festival_stardew_valley_fair_star_tokens"),
                    buffs(buff(BuffType.MINING, 1, 43_200), buff(BuffType.LUCK, 3, 43_200),
                            buff(BuffType.SPEED, 1, 43_200), buff(BuffType.DEFENSE, 3, 43_200),
                            buff(BuffType.ATTACK, 3, 43_200)),
                    item("stardewcraft:prismatic_shard"), tag("c:milk", "stardewcraft:milk"),
                    item("stardewcraft:sugar")),
            dish("fish_dumpling", 100, 45,
                    friendship("olivia", 750), buffs(buff(BuffType.FISHING, 1, 24_000)),
                    categories("stardewcraft:sardine", "stardewcraft:fish", "stardewcraft:legendary_fish"),
                    item("stardewcraft:wheat_flour"), item("stardewcraft:oil")),
            dish("gingerbread_man", 125, 50,
                    friendship("susan", 750), List.of(),
                    item("stardewcraft:ginger"), item("stardewcraft:wheat_flour"),
                    item("stardewcraft:sugar")),
            dish("ramen", 250, 75,
                    friendship("victor", 750), List.of(),
                    item("stardewcraft:wheat_flour"), item("stardewcraft:seaweed"),
                    tag("c:eggs", "stardewcraft:egg_white"), item("stardewcraft:oil")),
            dish("baked_potato", 225, 70,
                    mail("gunther", 750), List.of(),
                    item("stardewcraft:potato"), item("stardewcraftsve:butter")),
            dish("grilled_cheese_sandwich", 100, 85,
                    friendship("martin", 750), List.of(),
                    item("stardewcraft:bread"), item("stardewcraft:cheese"), item("stardewcraftsve:butter")),
            dish("pineapple_custard_crepe", 300, 90,
                    friendship("lance", 750),
                    buffs(buff(BuffType.LUCK, 3, 30_000), buff(BuffType.SPEED, 1, 30_000)),
                    item("stardewcraft:pineapple"), item("stardewcraft:sugar"),
                    item("stardewcraft:wheat_flour"), item("stardewcraftsve:butter")),
            dish("nectarine_fruit_bread", 150, 60,
                    friendship("claire", 750), List.of(),
                    item("stardewcraftsve:nectarine"), item("stardewcraft:wheat_flour"),
                    item("stardewcraft:sugar")),
            dish("glazed_pear", 400, 100,
                    friendship("morgan", 750),
                    buffs(buff(BuffType.MINING, 2, 42_000), buff(BuffType.MAGNETIC_RADIUS, 32, 42_000),
                            buff(BuffType.SPEED, 1, 42_000), buff(BuffType.DEFENSE, 6, 42_000)),
                    item("stardewcraftsve:pear"), item("stardewcraft:sugar"),
                    item("stardewcraft:wheat_flour"), item("stardewcraftsve:butter")),
            dish("stuffed_persimmon", 500, 110,
                    friendship("andy", 750), buffs(buff(BuffType.FORAGING, 5, 36_000)),
                    item("stardewcraftsve:persimmon"), item("stardewcraft:stuffing"),
                    item("stardewcraft:cheese"), item("stardewcraftsve:butter")),
            dish("cheese_charcuterie", 750, 120,
                    friendship("scarlett", 750), buffs(buff(BuffType.FARMING, 4, 24_000)),
                    item("stardewcraft:cheese"), item("stardewcraft:goat_cheese"),
                    item("stardewcraft:wheat_flour"), item("stardewcraftsve:butter"))
    );

    private SveCookingData() {
    }

    public static List<Definition> all() {
        return ALL;
    }

    public static Definition byPath(String path) {
        return ALL.stream()
                .filter(definition -> definition.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SVE cooking recipe: " + path));
    }

    private static Definition dish(
            String path, int price, int edibility, Unlock unlock,
            List<Buff> buffs, Ingredient... ingredients
    ) {
        return new Definition(path, price, edibility, false, buffs, List.of(ingredients), unlock);
    }

    private static Definition drink(
            String path, int price, int edibility, Unlock unlock,
            List<Buff> buffs, Ingredient... ingredients
    ) {
        return new Definition(path, price, edibility, true, buffs, List.of(ingredients), unlock);
    }

    private static List<Buff> buffs(Buff... buffs) {
        return List.of(buffs);
    }

    private static Buff buff(BuffType type, int level, int durationTicks) {
        return new Buff(type, level, durationTicks);
    }

    private static Ingredient item(String id) {
        return item(id, 1);
    }

    private static Ingredient item(String id, int count) {
        return new Ingredient(IngredientKind.ITEM, List.of(id), null, count);
    }

    private static Ingredient tag(String id, String displayItem) {
        return new Ingredient(IngredientKind.TAG, List.of(id), displayItem, 1);
    }

    private static Ingredient categories(String displayItem, String... ids) {
        return new Ingredient(IngredientKind.CATEGORIES, List.of(ids), displayItem, 1);
    }

    private static Unlock shop(String source) {
        return new Unlock(UnlockType.SHOP, source, 0);
    }

    private static Unlock mail(String source, int points) {
        return new Unlock(UnlockType.MAIL, source, points);
    }

    private static Unlock friendship(String source, int points) {
        return new Unlock(UnlockType.FRIENDSHIP, source, points);
    }

    public enum IngredientKind {
        ITEM,
        TAG,
        CATEGORIES
    }

    public enum UnlockType {
        SHOP,
        MAIL,
        FRIENDSHIP
    }

    public record Buff(BuffType type, int level, int durationTicks) {
        public Buff {
            if (type == null) throw new IllegalArgumentException("type");
            if (durationTicks <= 0) throw new IllegalArgumentException("durationTicks");
        }

        public DishBuff toDishBuff() {
            return new DishBuff(type, level, durationTicks);
        }
    }

    public record Ingredient(
            IngredientKind kind,
            List<String> ids,
            String displayItem,
            int count
    ) {
        public Ingredient {
            if (kind == null) throw new IllegalArgumentException("kind");
            ids = List.copyOf(ids);
            if (ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id.isBlank())) {
                throw new IllegalArgumentException("ids");
            }
            if (count <= 0) throw new IllegalArgumentException("count");
            if (kind == IngredientKind.ITEM && ids.size() != 1) {
                throw new IllegalArgumentException("Item ingredient must have exactly one id");
            }
            if (kind != IngredientKind.ITEM && (displayItem == null || displayItem.isBlank())) {
                throw new IllegalArgumentException("Grouped ingredient requires a display item");
            }
        }
    }

    public record Unlock(UnlockType type, String source, int friendshipPoints) {
        public Unlock {
            if (type == null) throw new IllegalArgumentException("type");
            if (source == null || source.isBlank()) throw new IllegalArgumentException("source");
            if ((type == UnlockType.FRIENDSHIP || type == UnlockType.MAIL) && friendshipPoints <= 0) {
                throw new IllegalArgumentException("friendshipPoints");
            }
        }
    }

    public record Definition(
            String path,
            int price,
            int edibility,
            boolean drink,
            List<Buff> buffs,
            List<Ingredient> ingredients,
            Unlock unlock
    ) {
        public Definition {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("path");
            if (price < 0) throw new IllegalArgumentException("price");
            buffs = List.copyOf(buffs);
            ingredients = List.copyOf(ingredients);
            if (ingredients.isEmpty()) throw new IllegalArgumentException("ingredients");
            if (unlock == null) throw new IllegalArgumentException("unlock");
        }

        public CookingDishItem createItem(Item.Properties properties) {
            return new CookingDishItem(
                    price, edibility, buffs.stream().map(Buff::toDishBuff).toList(), properties, drink);
        }
    }
}
