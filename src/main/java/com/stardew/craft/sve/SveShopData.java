package com.stardew.craft.sve;

import java.util.List;

/** SVE-owned additions to StardewCraft shops, sourced from SVE 1.15.11 Data/Shops edits. */
public final class SveShopData {
    private static final List<SourceOmission> SOURCE_OMISSIONS = List.of(
            omission("stardewcraft:festival_stardew_valley_fair_star_tokens", "furniture_catalogue_2"),
            omission("stardewcraftsve:alesia_vendor", "galdoran_mage_boots"),
            omission("stardewcraftsve:alesia_vendor", "explosive_ammo"),
            omission("stardewcraftsve:isaac_vendor", "galdoran_knight_boots"),
            omission("stardewcraftsve:sophia_ledger", "pressure_nozzle"),
            omission("stardewcraftsve:zoey_vendor", "pressure_nozzle")
    );
    private static final List<String> DEFERRED_SHOP_VARIANTS = List.of(
            "ChloeVendor1", "ChloeVendor3", "ChloeVendor4");

    private static final List<Shop> ALL = List.of(
            shop("stardewcraft:adventure_shop",
                    entry("recipe:stardewcraftsve:frog_legs", 2_000, 1),
                    entry("recipe:stardewcraftsve:mushroom_berry_rice", 1_500, 1)),
            shop("stardewcraft:carpenter_shop",
                    entry("recipe:stardewcraftsve:small_hardwood_fence", 7_000, 1)),
            shop("stardewcraft:desert_trade",
                    trade("stardewcraftsve:gold_carrot_seed", 2, "stardewcraft:gold_bar", 3)),
            shop("stardewcraft:festival_festival_of_ice_traveling_merchant",
                    entry("recipe:stardewcraftsve:chocolate_truffle_bar", 12_000, 1)),
            shop("stardewcraft:festival_luau_pierre",
                    entry("recipe:stardewcraftsve:ice_cream_sundae", 2_000, 1)),
            shop("stardewcraft:festival_stardew_valley_fair_star_tokens",
                    conditioned("recipe:stardewcraftsve:prismatic_pop", 3_000, 1, 2, null)),
            shop("stardewcraft:fish_shop",
                    entry("recipe:stardewcraftsve:seaweed_salad", 1_250, 1)),
            shop("stardewcraft:hospital",
                    entry("stardewcraftsve:sports_drink", 750, 3),
                    entry("stardewcraftsve:stamina_capsule", 4_000, 1)),
            shop("stardewcraft:joja_mart",
                    conditioned("stardewcraftsve:joja_berry_starter", 6_500, 0, 0, "morris_10_hearts"),
                    conditioned("stardewcraftsve:joja_veggie_seeds", 600, 0, 0, "morris_10_hearts")),
            shop("stardewcraft:oasis_shop",
                    entry("recipe:stardewcraftsve:vegan_cone", 3_000, 1)),
            shop("stardewcraft:saloon",
                    conditioned("stardewcraftsve:grampleton_orange_chicken", 650, 0, 0, "sophia_6_hearts"),
                    entry("recipe:stardewcraftsve:mixed_berry_pie", 5_000, 1),
                    entry("recipe:stardewcraftsve:big_bark_burger", 3_000, 1),
                    entry("recipe:stardewcraftsve:glazed_butterfish", 6_000, 1),
                    entry("recipe:stardewcraftsve:candy", 10_000, 1)),
            shop("stardewcraft:seed_shop",
                    seasonal("stardewcraftsve:cucumber_seed", 75, 0),
                    seasonal("stardewcraftsve:butternut_squash_seed", 30, 1),
                    seasonal("stardewcraftsve:sweet_potato_seed", 45, 2),
                    entry("stardewcraftsve:pear_sapling", 1_600, 0),
                    entry("stardewcraftsve:nectarine_sapling", 3_000, 0),
                    entry("stardewcraftsve:persimmon_sapling", 4_000, 0),
                    entry("recipe:stardewcraftsve:birch_syrup", 2_500, 1)),
            shop("stardewcraft:shadow_shop",
                    entry("recipe:stardewcraftsve:void_salmon_sushi", 5_000, 1),
                    conditioned("recipe:stardewcraftsve:void_delight", 5_000, 1, 0, "krobus_10_hearts")),
            shop("stardewcraftsve:alesia_vendor",
                    entry("stardewcraftsve:tempered_galaxy_dagger", 350_000, 1),
                    entry("stardewcraftsve:haste_elixir", 8_000, 3),
                    entry("stardewcraftsve:armor_elixir", 5_000, 3),
                    entry("stardewcraftsve:undeadfish", 4_000, 1)),
            shop("stardewcraftsve:axel_vendor",
                    entry("stardewcraft:golden_egg", 1_000_000, 1),
                    entry("stardewcraft:galaxy_soul", 140_000, 1),
                    entry("stardewcraft:pearl", 20_000, 1),
                    entry("stardewcraft:radioactive_bar", 12_000, 1),
                    entry("stardewcraft:magic_bait", 1_000, 20),
                    entry("stardewcraftsve:turretfish", 60_000, 1),
                    entry("stardewcraft:legend", 50_000, 1),
                    entry("stardewcraftsve:wolf_snapper", 40_000, 1),
                    entry("stardewcraft:crimsonfish", 35_000, 1),
                    entry("stardewcraft:glacierfish", 30_000, 1),
                    entry("stardewcraft:mutant_carp", 20_000, 1),
                    entry("stardewcraft:angler", 15_000, 1),
                    entry("stardewcraft:slimejack", 10_000, 1),
                    entry("stardewcraft:lava_eel", 12_000, 1),
                    entry("stardewcraft:ice_pip", 6_000, 1),
                    entry("stardewcraft:stonefish", 5_000, 1)),
            shop("stardewcraftsve:bear_vendor",
                    entry("stardewcraft:honey", 1_000, 10), entry("stardewcraft:maple_syrup", 1_500, 8),
                    entry("stardewcraft:pine_tar", 2_500, 5), entry("stardewcraft:oak_resin", 3_500, 5),
                    entry("stardewcraftsve:fir_wax", 2_000, 3), entry("stardewcraftsve:birch_water", 1_000, 3),
                    entry("stardewcraft:mahogany_seed", 3_000, 3), entry("stardewcraft:cave_carrot", 400, 20),
                    entry("stardewcraft:salmon", 600, 10),
                    entry("recipe:stardewcraftsve:baked_berry_oatmeal_supreme", 12_500, 1),
                    entry("recipe:stardewcraftsve:flower_cookie", 8_750, 1)),
            shop("stardewcraftsve:camilla_vendor",
                    entry("stardewcraftsve:gravity_elixir", 4_000, 5),
                    entry("stardewcraftsve:lightning_elixir", 8_000, 3),
                    entry("stardewcraftsve:barbarian_elixir", 20_000, 1),
                    entry("stardewcraftsve:aegis_elixir", 28_000, 1)),
            shop("stardewcraftsve:chloe_vendor",
                    entry("stardewcraftsve:void_delight", 35_000, 1),
                    entry("stardewcraft:salmon_dinner", 1_400, 3), entry("stardewcraft:crispy_bass", 800, 5),
                    entry("stardewcraft:sashimi", 700, 5), entry("stardewcraft:maki_roll", 500, 10)),
            shop("stardewcraftsve:isaac_vendor",
                    entry("stardewcraftsve:bonefish", 6_000, 1), entry("stardewcraftsve:hero_elixir", 12_000, 3),
                    entry("stardewcraftsve:tempered_galaxy_hammer", 400_000, 1),
                    entry("stardewcraftsve:tempered_galaxy_sword", 600_000, 1)),
            shop("stardewcraftsve:sophia_ledger",
                    entry("stardewcraftsve:blue_moon_wine", 3_000, 10),
                    entry("stardewcraftsve:aged_blue_moon_wine", 28_000, 1),
                    entry("stardewcraft:sprinkler", 600, 5), entry("stardewcraft:quality_sprinkler", 3_500, 5),
                    conditioned("stardewcraft:iridium_sprinkler", 15_000, 3, 0, "SophiaFairyGarden")),
            shop("stardewcraftsve:zoey_vendor",
                    entry("stardewcraft:blue_grass_starter", 1_000, 0), entry("stardewcraft:iridium_sprinkler", 12_000, 3),
                    entry("stardewcraftsve:joja_veggie_seeds", 600, 0), entry("stardewcraftsve:joja_berry_starter", 6_500, 0),
                    entry("stardewcraft:rare_seed", 1_500, 15), entry("stardewcraft:spring_seeds", 65, 50),
                    entry("stardewcraft:summer_seeds", 90, 50), entry("stardewcraft:fall_seeds", 100, 50),
                    entry("stardewcraft:winter_seeds", 120, 50), entry("stardewcraft:deluxe_fertilizer", 4_000, 20),
                    entry("stardewcraft:hyper_speed_gro", 2_500, 15), entry("stardewcraft:deluxe_retaining_soil", 2_500, 15))
    );

    private SveShopData() {}

    public static List<Shop> all() { return ALL; }
    public static List<SourceOmission> sourceOmissions() { return SOURCE_OMISSIONS; }
    public static List<String> deferredShopVariants() { return DEFERRED_SHOP_VARIANTS; }

    private static Shop shop(String id, Entry... entries) { return new Shop(id, List.of(entries)); }
    private static SourceOmission omission(String shopId, String sourceItem) {
        return new SourceOmission(shopId, sourceItem, "Item is not registered by StardewCraftSVE");
    }
    private static Entry entry(String item, int price, int stock) { return new Entry(item, price, stock, null, 0, List.of(), 0, null); }
    private static Entry trade(String item, int stock, String tradeItem, int tradeCount) { return new Entry(item, 0, stock, tradeItem, tradeCount, List.of(), 0, null); }
    private static Entry seasonal(String item, int price, int season) { return new Entry(item, price, 0, null, 0, List.of(season), 0, null); }
    private static Entry conditioned(String item, int price, int stock, int minYear, String mailFlag) { return new Entry(item, price, stock, null, 0, List.of(), minYear, mailFlag); }

    public record Shop(String id, List<Entry> entries) {
        public Shop { entries = List.copyOf(entries); }
    }

    public record SourceOmission(String shopId, String sourceItem, String reason) {}

    public record Entry(String item, int price, int stock, String tradeItem, int tradeItemCount,
                        List<Integer> seasons, int minYear, String mailFlag) {
        public Entry { seasons = List.copyOf(seasons); }
    }
}
