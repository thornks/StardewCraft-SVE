package com.stardew.craft.sve;

import com.stardew.craft.combat.WeaponType;
import com.stardew.craft.item.StardewBlockItem;
import com.stardew.craft.item.cooking.CookingDishItem;
import com.stardew.craft.item.SimpleStardewItem;
import com.stardew.craft.item.EdibleSimpleStardewItem;
import com.stardew.craft.item.StardewQualityItem;
import com.stardew.craft.item.artisan.ArtisanDrinkItem;
import com.stardew.craft.item.artisan.PreserveType;
import com.stardew.craft.item.artisan.PreservesItem;
import com.stardew.craft.item.artisan.SmokedFishItem;
import com.stardew.craft.item.fish.FishItem;
import com.stardew.craft.item.weapon.StardewClubItem;
import com.stardew.craft.item.weapon.StardewDaggerItem;
import com.stardew.craft.item.weapon.StardewWeaponItem;
import com.stardew.craft.sve.tree.SveFruitTreeSaplingItem;
import com.stardew.craft.sve.tree.SveFruitTreeType;
import com.stardew.craft.sve.tree.wild.FirConeItem;
import com.stardew.craft.sve.tree.wild.BirchSeedItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import com.stardew.craft.sve.animal.SveAnimalEntities;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Registry for SVE-ported items. Most items are plain {@link Item} (textures only). Items with typed behavior
 * (elixirs, foods, weapons, ...) are registered as their proper stardewcraft
 * subclass and carry SVE's original numeric values.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(StardewcraftsveMod.MODID);

    // Cooking-pot dishes use the SVE namespace so JEI attributes them to this
    // addon. Stardewcraft 0.5.1 resolves namespaced cooking definitions directly.
    public static final DeferredRegister.Items STARDEWCRAFT_ITEMS =
        DeferredRegister.createItems("stardewcraftsve");

    // Smoked-fish items are registered under `stardewcraftsve` namespace so
    // JEI attributes them to the correct mod (searchable via @SVE).
    public static final DeferredRegister.Items SMOKED_FISH_ITEMS =
        DeferredRegister.createItems("stardewcraftsve");

    // Flavored artisan products (jelly, pickles, dried fruit, dried mushrooms)
    // registered under `stardewcraftsve` so JEI shows them with the SVE mod tag
    // instead of attributing them to the base mod's items.
    public static final DeferredRegister.Items ARTISAN_ITEMS =
        DeferredRegister.createItems("stardewcraftsve");

    static {
        ITEMS.addAlias(id("cherry"), baseId("cherry"));
        ITEMS.addAlias(id("salmonberry"), baseId("salmonberry"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }

    private static ResourceLocation baseId(String path) {
        return ResourceLocation.fromNamespaceAndPath("stardewcraft", path);
    }

    private static DeferredHolder<Item, Item> reg(String id) {
        return ITEMS.register(id, () -> new Item(stackableProperties()));
    }

    private static Item.Properties stackableProperties() {
        return new Item.Properties().stacksTo(999);
    }

    private static Item.Properties unstackableProperties() {
        return new Item.Properties().stacksTo(1);
    }

    private static CookingDishItem cooking(String path) {
        return SveCookingData.byPath(path).createItem(stackableProperties());
    }

    private static CookingDishItem consumable(String path) {
        return SveConsumableData.byPath(path).createItem(stackableProperties());
    }

    private static StardewQualityItem cropItem(SveCropData.Definition definition) {
        return new StardewQualityItem(
                "stardewcraft.type.crop",
                definition.produceSellPrice(),
                definition.edibility(),
                true,
                stackableProperties());
    }

    private static SveSeedItem seedItem(
            java.util.function.Supplier<net.minecraft.world.level.block.Block> cropBlock,
            SveCropData.Definition definition
    ) {
        return new SveSeedItem(
                stackableProperties(),
                cropBlock,
                definition.seasonIdsArray(),
                definition.seedSellPrice());
    }

    // Creative-only animal test items. Farm-managed animals still come from Marnie's shop.
    public static final DeferredHolder<Item, SpawnEggItem> GOOSE_SPAWN_EGG = ITEMS.register(
        "goose_spawn_egg",
        () -> new SpawnEggItem(SveAnimalEntities.GOOSE.get(), 0xF2E7C4, 0xD99A2B, new Item.Properties()));
    public static final DeferredHolder<Item, SpawnEggItem> CAMEL_SPAWN_EGG = ITEMS.register(
        "camel_spawn_egg",
        () -> new SpawnEggItem(SveAnimalEntities.CAMEL.get(), 0xA9653A, 0xD9A56C, new Item.Properties()));

    // ===== fish =====
    public static final DeferredHolder<Item, FishItem> ALLIGATOR = ITEMS.register("alligator",
        () -> new FishItem(
            new int[]{400, 500, 600, 800},
            new int[]{63, 88, 113, 163},
            new int[]{28, 39, 50, 72},
            75, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> ARROWHEAD_SHARK = ITEMS.register("arrowhead_shark",
        () -> new FishItem(
            new int[]{700, 875, 1050, 1400},
            new int[]{70, 98, 126, 182},
            new int[]{31, 43, 55, 80},
            95, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> BARRED_KNIFEJAW = ITEMS.register("barred_knifejaw",
        () -> new FishItem(
            new int[]{250, 312, 375, 500},
            new int[]{33, 46, 59, 85},
            new int[]{14, 19, 25, 36},
            65, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> BLUE_TANG = ITEMS.register("blue_tang",
        () -> new FishItem(
            new int[]{130, 162, 195, 260},
            new int[]{25, 35, 45, 65},
            new int[]{11, 15, 19, 28},
            30, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> BONEFISH = ITEMS.register("bonefish",
        () -> new FishItem(
            new int[]{450, 562, 675, 900},
            new int[]{-75, -75, -75, -75},
            new int[]{-14, -14, -14, -14},
            70, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> BULL_TROUT = ITEMS.register("bull_trout",
        () -> new FishItem(
            new int[]{185, 231, 277, 370},
            new int[]{25, 35, 45, 65},
            new int[]{11, 15, 19, 28},
            45, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> BUTTERFISH = ITEMS.register("butterfish",
        () -> new FishItem(
            new int[]{200, 250, 300, 400},
            new int[]{50, 70, 90, 130},
            new int[]{22, 30, 39, 57},
            80, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> CLOWNFISH = ITEMS.register("clownfish",
        () -> new FishItem(
            new int[]{70, 87, 105, 140},
            new int[]{50, 70, 90, 130},
            new int[]{22, 30, 39, 57},
            45, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> DAGGERFISH = ITEMS.register("daggerfish",
        () -> new FishItem(
            new int[]{200, 250, 300, 400},
            new int[]{-150, -150, -150, -150},
            new int[]{-27, -27, -27, -27},
            50, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> DIAMOND_CARP = ITEMS.register("diamond_carp",
        () -> new FishItem(
            new int[]{575, 718, 862, 1150},
            new int[]{-25, -25, -25, -25},
            new int[]{-5, -5, -5, -5},
            70, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> FIBER_GOBY = ITEMS.register("fiber_goby",
        () -> new FishItem(
            new int[]{250, 312, 375, 500},
            new int[]{55, 77, 99, 143},
            new int[]{24, 33, 43, 62},
            60, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> FROG = ITEMS.register("frog",
        () -> new FishItem(
            new int[]{100, 125, 150, 200},
            new int[]{-75, -75, -75, -75},
            new int[]{-14, -14, -14, -14},
            70, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> GAR = ITEMS.register("gar",
        () -> new FishItem(
            new int[]{175, 218, 262, 350},
            new int[]{50, 70, 90, 130},
            new int[]{22, 30, 39, 57},
            85, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> GEMFISH = ITEMS.register("gemfish",
        () -> new FishItem(
            new int[]{800, 1000, 1200, 1600},
            new int[]{-30, -30, -30, -30},
            new int[]{-6, -6, -6, -6},
            100, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> GOLDFISH = ITEMS.register("goldfish",
        () -> new FishItem(
            new int[]{80, 100, 120, 160},
            new int[]{23, 32, 41, 59},
            new int[]{10, 14, 18, 26},
            25, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> GOLDENFISH = ITEMS.register("goldenfish",
        () -> new FishItem(
            new int[]{150, 187, 225, 300},
            new int[]{13, 18, 23, 33},
            new int[]{5, 7, 9, 13},
            60, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> GRASS_CARP = ITEMS.register("grass_carp",
        () -> new FishItem(
            new int[]{150, 187, 225, 300},
            new int[]{38, 53, 68, 98},
            new int[]{17, 23, 30, 44},
            85, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> HIGHLANDS_BASS = ITEMS.register("highlands_bass",
        () -> new FishItem(
            new int[]{200, 250, 300, 400},
            new int[]{38, 53, 68, 98},
            new int[]{17, 23, 30, 44},
            45, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> KING_SALMON = ITEMS.register("king_salmon",
        () -> new FishItem(
            new int[]{320, 400, 480, 640},
            new int[]{130, 182, 234, 338},
            new int[]{58, 81, 104, 150},
            80, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> KITTYFISH = ITEMS.register("kittyfish",
        () -> new FishItem(
            new int[]{250, 312, 375, 500},
            new int[]{100, 140, 180, 260},
            new int[]{45, 62, 81, 117},
            85, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> METEOR_CARP = ITEMS.register("meteor_carp",
        () -> new FishItem(
            new int[]{175, 218, 262, 350},
            new int[]{-50, -50, -50, -50},
            new int[]{-9, -9, -9, -9},
            80, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> MINNOW = ITEMS.register("minnow",
        () -> new FishItem(
            new int[]{20, 25, 30, 40},
            new int[]{3, 4, 5, 7},
            new int[]{1, 1, 1, 2},
            1, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> OCEAN_SUNFISH = ITEMS.register("ocean_sunfish",
        () -> new FishItem(
            new int[]{650, 812, 975, 1300},
            new int[]{50, 70, 90, 130},
            new int[]{22, 30, 39, 57},
            90, "floater",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> PUPPYFISH = ITEMS.register("puppyfish",
        () -> new FishItem(
            new int[]{250, 312, 375, 500},
            new int[]{100, 140, 180, 260},
            new int[]{45, 62, 81, 117},
            90, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> RADIOACTIVE_BASS = ITEMS.register("radioactive_bass",
        () -> new FishItem(
            new int[]{500, 625, 750, 1000},
            new int[]{-125, -125, -125, -125},
            new int[]{-23, -23, -23, -23},
            90, "floater",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> RAZOR_TROUT = ITEMS.register("razor_trout",
        () -> new FishItem(
            new int[]{400, 500, 600, 800},
            new int[]{-87, -87, -87, -87},
            new int[]{-16, -16, -16, -16},
            85, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> SEAHORSE = ITEMS.register("seahorse",
        () -> new FishItem(
            new int[]{60, 75, 90, 120},
            new int[]{13, 18, 23, 33},
            new int[]{5, 7, 9, 13},
            25, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> SHARK = ITEMS.register("shark",
        () -> new FishItem(
            new int[]{800, 1000, 1200, 1600},
            new int[]{88, 123, 158, 228},
            new int[]{39, 54, 70, 101},
            110, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> SNATCHER_WORM = ITEMS.register("snatcher_worm",
        () -> new FishItem(
            new int[]{280, 350, 420, 560},
            new int[]{33, 46, 59, 85},
            new int[]{14, 19, 25, 36},
            75, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> TADPOLE = ITEMS.register("tadpole",
        () -> new FishItem(
            new int[]{15, 18, 22, 30},
            new int[]{5, 7, 9, 13},
            new int[]{2, 2, 3, 5},
            3, "floater",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> TORPEDO_TROUT = ITEMS.register("torpedo_trout",
        () -> new FishItem(
            new int[]{300, 375, 450, 600},
            new int[]{-25, -25, -25, -25},
            new int[]{-5, -5, -5, -5},
            70, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> TURRETFISH = ITEMS.register("turretfish",
        () -> new FishItem(
            new int[]{10000, 12500, 15000, 20000},
            new int[]{-125, -125, -125, -125},
            new int[]{-23, -23, -23, -23},
            150, "dart",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> UNDEADFISH = ITEMS.register("undeadfish",
        () -> new FishItem(
            new int[]{600, 750, 900, 1200},
            new int[]{-150, -150, -150, -150},
            new int[]{-27, -27, -27, -27},
            80, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> VIPER_EEL = ITEMS.register("viper_eel",
        () -> new FishItem(
            new int[]{600, 750, 900, 1200},
            new int[]{-325, -325, -325, -325},
            new int[]{-59, -59, -59, -59},
            80, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> VOID_EEL = ITEMS.register("void_eel",
        () -> new FishItem(
            new int[]{450, 562, 675, 900},
            new int[]{-500, -500, -500, -500},
            new int[]{-90, -90, -90, -90},
            100, "mixed",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> WATER_GRUB = ITEMS.register("water_grub",
        () -> new FishItem(
            new int[]{170, 212, 255, 340},
            new int[]{100, 140, 180, 260},
            new int[]{45, 62, 81, 117},
            60, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> WOLF_SNAPPER = ITEMS.register("wolf_snapper",
        () -> new FishItem(
            new int[]{4000, 5000, 6000, 8000},
            new int[]{125, 175, 225, 325},
            new int[]{56, 78, 100, 145},
            110, "dart",
            stackableProperties()));

    // ===== crops =====
    public static final DeferredHolder<Item, StardewQualityItem> BUTTERNUT_SQUASH = ITEMS.register("butternut_squash",
        () -> cropItem(SveCropData.BUTTERNUT_SQUASH));
    public static final DeferredHolder<Item, StardewQualityItem> CUCUMBER = ITEMS.register("cucumber",
        () -> cropItem(SveCropData.CUCUMBER));
    public static final DeferredHolder<Item, StardewQualityItem> GOLD_CARROT = ITEMS.register("gold_carrot",
        () -> cropItem(SveCropData.GOLD_CARROT));
    public static final DeferredHolder<Item, StardewQualityItem> JOJA_BERRY = ITEMS.register("joja_berry",
        () -> cropItem(SveCropData.JOJA_BERRY));
    public static final DeferredHolder<Item, StardewQualityItem> JOJA_VEGGIE = ITEMS.register("joja_veggie",
        () -> cropItem(SveCropData.JOJA_VEGGIE));
    public static final DeferredHolder<Item, StardewQualityItem> MONSTER_FRUIT = ITEMS.register("monster_fruit",
        () -> cropItem(SveCropData.MONSTER_FRUIT));
    public static final DeferredHolder<Item, StardewQualityItem> SWEET_POTATO = ITEMS.register("sweet_potato",
        () -> cropItem(SveCropData.SWEET_POTATO));
    public static final DeferredHolder<Item, StardewQualityItem> SALAL_BERRY = ITEMS.register("salal_berry",
        () -> cropItem(SveCropData.SALAL_BERRY));
    public static final DeferredHolder<Item, StardewQualityItem> VOID_ROOT = ITEMS.register("void_root",
        () -> cropItem(SveCropData.VOID_ROOT));
    public static final DeferredHolder<Item, StardewQualityItem> ANCIENT_FIBER = ITEMS.register("ancient_fiber",
        () -> cropItem(SveCropData.ANCIENT_FIBER));

    // ===== seeds =====
    public static final DeferredHolder<Item, SveSeedItem> ANCIENT_FERNS_SEED = ITEMS.register("ancient_ferns_seed",
        () -> seedItem(ModBlocks.ANCIENT_FIBER_CROP, SveCropData.ANCIENT_FIBER));
    public static final DeferredHolder<Item, BirchSeedItem> BIRCH_SEED = ITEMS.register("birch_seed",
        () -> new BirchSeedItem(stackableProperties()));
    public static final DeferredHolder<Item, SveSeedItem> BUTTERNUT_SQUASH_SEED = ITEMS.register("butternut_squash_seed",
        () -> seedItem(ModBlocks.BUTTERNUT_SQUASH_CROP, SveCropData.BUTTERNUT_SQUASH));
    public static final DeferredHolder<Item, SveSeedItem> CUCUMBER_SEED = ITEMS.register("cucumber_seed",
        () -> seedItem(ModBlocks.CUCUMBER_CROP, SveCropData.CUCUMBER));
    public static final DeferredHolder<Item, SveSeedItem> FUNGUS_SEED = ITEMS.register("fungus_seed",
        () -> seedItem(ModBlocks.MONSTER_MUSHROOM_CROP, SveCropData.MONSTER_MUSHROOM));
    public static final DeferredHolder<Item, SveSeedItem> GOLD_CARROT_SEED = ITEMS.register("gold_carrot_seed",
        () -> seedItem(ModBlocks.GOLD_CARROT_CROP, SveCropData.GOLD_CARROT));
    public static final DeferredHolder<Item, SveSeedItem> JOJA_BERRY_STARTER = ITEMS.register("joja_berry_starter",
        () -> seedItem(ModBlocks.JOJA_BERRY_CROP, SveCropData.JOJA_BERRY));
    public static final DeferredHolder<Item, SveSeedItem> JOJA_VEGGIE_SEEDS = ITEMS.register("joja_veggie_seeds",
        () -> seedItem(ModBlocks.JOJA_VEGGIE_CROP, SveCropData.JOJA_VEGGIE));
    public static final DeferredHolder<Item, SveFruitTreeSaplingItem> NECTARINE_SAPLING = ITEMS.register("nectarine_sapling",
        () -> new SveFruitTreeSaplingItem(SveFruitTreeType.NECTARINE, ModBlocks.NECTARINE_SAPLING, stackableProperties()));
    public static final DeferredHolder<Item, SveFruitTreeSaplingItem> PEAR_SAPLING = ITEMS.register("pear_sapling",
        () -> new SveFruitTreeSaplingItem(SveFruitTreeType.PEAR, ModBlocks.PEAR_SAPLING, stackableProperties()));
    public static final DeferredHolder<Item, SveFruitTreeSaplingItem> PERSIMMON_SAPLING = ITEMS.register("persimmon_sapling",
        () -> new SveFruitTreeSaplingItem(SveFruitTreeType.PERSIMMON, ModBlocks.PERSIMMON_SAPLING, stackableProperties()));
    public static final DeferredHolder<Item, SveSeedItem> SLIME_SEED = ITEMS.register("slime_seed",
        () -> seedItem(ModBlocks.SLIME_BERRY_CROP, SveCropData.SLIME_BERRY));
    public static final DeferredHolder<Item, SveSeedItem> STALK_SEED = ITEMS.register("stalk_seed",
        () -> seedItem(ModBlocks.MONSTER_FRUIT_CROP, SveCropData.MONSTER_FRUIT));
    public static final DeferredHolder<Item, SveSeedItem> SWEET_POTATO_SEED = ITEMS.register("sweet_potato_seed",
        () -> seedItem(ModBlocks.SWEET_POTATO_CROP, SveCropData.SWEET_POTATO));
    public static final DeferredHolder<Item, SveSeedItem> SALAL_BERRY_SEED = ITEMS.register("salal_berry_seed",
        () -> seedItem(ModBlocks.SALAL_BERRY_CROP, SveCropData.SALAL_BERRY));
    public static final DeferredHolder<Item, SveSeedItem> VOID_SEED = ITEMS.register("void_seed",
        () -> seedItem(ModBlocks.VOID_ROOT_CROP, SveCropData.VOID_ROOT));

    // ===== fruit =====
    public static final DeferredHolder<Item, StardewQualityItem> BEARBERRYS = ITEMS.register("bearberrys",
        () -> new StardewQualityItem("stardewcraft.type.fruit", 55, 15, true, stackableProperties()));
    public static final DeferredHolder<Item, CookingDishItem> DEWDROP_BERRY = ITEMS.register("dewdrop_berry",
        () -> consumable("dewdrop_berry"));
    public static final DeferredHolder<Item, StardewQualityItem> NECTARINE = ITEMS.register("nectarine",
        () -> new StardewQualityItem("stardewcraft.type.fruit", 150, 28, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> PEAR = ITEMS.register("pear",
        () -> new StardewQualityItem("stardewcraft.type.fruit", 90, 20, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> PERSIMMON = ITEMS.register("persimmon",
        () -> new StardewQualityItem("stardewcraft.type.fruit", 200, 28, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SLIME_BERRY = ITEMS.register("slime_berry",
        () -> cropItem(SveCropData.SLIME_BERRY));

    // ===== mushroom =====
    public static final DeferredHolder<Item, StardewQualityItem> GREEN_MUSHROOM = ITEMS.register("green_mushroom",
        () -> SveConsumableData.byPath("green_mushroom")
            .createQualityItem("stardewcraft.type.forage", true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> MEGA_PURPLE_MUSHROOM = ITEMS.register("mega_purple_mushroom",
        () -> new StardewQualityItem("stardewcraft.type.forage", 8000, 999, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> MONSTER_MUSHROOM = ITEMS.register("monster_mushroom",
        () -> cropItem(SveCropData.MONSTER_MUSHROOM));
    public static final DeferredHolder<Item, StardewQualityItem> MUSHROOM_COLONY = ITEMS.register("mushroom_colony",
        () -> new StardewQualityItem("stardewcraft.type.forage", 105, 18, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> POISON_MUSHROOM = ITEMS.register("poison_mushroom",
        () -> new StardewQualityItem("stardewcraft.type.forage", 60, -100, true, stackableProperties()));

    // ===== flower =====
    public static final DeferredHolder<Item, StardewQualityItem> DIAMOND_FLOWER = ITEMS.register("diamond_flower",
        () -> new StardewQualityItem("stardewcraft.type.forage", 850, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> FERNGILL_PRIMROSE = ITEMS.register("ferngill_primrose",
        () -> new StardewQualityItem("stardewcraft.type.forage", 120, 18, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> GOLDENROD = ITEMS.register("goldenrod",
        () -> new StardewQualityItem("stardewcraft.type.forage", 160, 23, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> GOLDEN_OCEAN_FLOWER = ITEMS.register("golden_ocean_flower",
        () -> new StardewQualityItem("stardewcraft.type.forage", 1000, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SMELLY_RAFFLESIA = ITEMS.register("smelly_rafflesia",
        () -> new StardewQualityItem("stardewcraft.type.forage", 375, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SWAMP_FLOWER = ITEMS.register("swamp_flower",
        () -> new StardewQualityItem("stardewcraft.type.forage", 125, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> THISTLE = ITEMS.register("thistle",
        () -> new StardewQualityItem("stardewcraft.type.forage", 120, 10, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> WINTER_STAR_ROSE = ITEMS.register("winter_star_rose",
        () -> new StardewQualityItem("stardewcraft.type.forage", 200, 28, true, stackableProperties()));

    // ===== forage =====
    public static final DeferredHolder<Item, StardewQualityItem> RED_BANEBERRY = ITEMS.register("red_baneberry",
        () -> new StardewQualityItem("stardewcraft.type.forage", 20, -50, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> BIG_CONCH = ITEMS.register("big_conch",
        () -> new StardewQualityItem("stardewcraft.type.forage", 150, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> DRIED_SAND_DOLLAR = ITEMS.register("dried_sand_dollar",
        () -> new StardewQualityItem("stardewcraft.type.forage", 80, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> DULSE_SEAWEED = ITEMS.register("dulse_seaweed",
        () -> new StardewQualityItem("stardewcraft.type.forage", 35, 15, true, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> FADED_BUTTON = ITEMS.register("faded_button",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 35, stackableProperties()));
    public static final DeferredHolder<Item, FirConeItem> FIR_CONE = ITEMS.register("fir_cone",
        () -> new FirConeItem(stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> OLD_COIN = ITEMS.register("old_coin",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 100, stackableProperties()));
    public static final DeferredHolder<Item, FishItem> SEA_SPONGE = ITEMS.register("sea_sponge",
        () -> new FishItem(
            new int[]{75, 93, 112, 150},
            new int[]{-62, -62, -62, -62},
            new int[]{-11, -11, -11, -11},
            40, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SLUDGE = ITEMS.register("sludge",
        () -> new StardewQualityItem("stardewcraft.type.forage", 60, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, FishItem> STARFISH = ITEMS.register("starfish",
        () -> new FishItem(
            new int[]{150, 187, 225, 300},
            new int[]{-50, -50, -50, -50},
            new int[]{-9, -9, -9, -9},
            75, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SUPERNATURAL_GOO = ITEMS.register("supernatural_goo",
        () -> new StardewQualityItem("stardewcraft.type.forage", 200, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, FishItem> SWAMP_CRAB = ITEMS.register("swamp_crab",
        () -> new FishItem(
            new int[]{150, 187, 225, 300},
            new int[]{50, 70, 90, 130},
            new int[]{22, 30, 39, 57},
            35, "sinker",
            stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SWAMP_ESSENCE = ITEMS.register("swamp_essence",
        () -> new StardewQualityItem("stardewcraft.type.forage", 80, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> VOID_PEBBLE = ITEMS.register("void_pebble",
        () -> new StardewQualityItem("stardewcraft.type.forage", 300, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> VOID_SHARD = ITEMS.register("void_shard",
        () -> new StardewQualityItem("stardewcraft.type.forage", 10000, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> VOID_SOUL = ITEMS.register("void_soul",
        () -> new StardewQualityItem("stardewcraft.type.forage", 150, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> YARN = ITEMS.register("yarn",
        () -> new StardewQualityItem("stardewcraft.type.artisan_goods", 900, -300, true, stackableProperties()));

    // ===== artifact =====
    public static final DeferredHolder<Item, SimpleStardewItem> AMBER = ITEMS.register("amber",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 750, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> BOOMERANG = ITEMS.register("boomerang",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 125, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> FOSSILIZED_APPLE = ITEMS.register("fossilized_apple",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 80, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> GALDORAN_GEM = ITEMS.register("galdoran_gem",
        () -> new SimpleStardewItem("stardewcraft.type.gem", 600, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> MAGIC_LAMP = ITEMS.register("magic_lamp",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 15000, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> MONEY_BAG = ITEMS.register("money_bag",
        () -> new SimpleStardewItem("stardewcraft.type.misc", 1000, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> ORNATE_TREASURE_CHEST = ITEMS.register("ornate_treasure_chest",
        () -> new SimpleStardewItem("stardewcraft.type.misc", 10000, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> RUSTY_SHIELD = ITEMS.register("rusty_shield",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 250, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SHARK_TOOTH = ITEMS.register("shark_tooth",
        () -> new StardewQualityItem("stardewcraft.type.forage", 175, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> STONE_OF_YOBA = ITEMS.register("stone_of_yoba",
        () -> new SimpleStardewItem("stardewcraft.type.artifact", 1500, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> SWIRL_STONE = ITEMS.register("swirl_stone",
        () -> new StardewQualityItem("stardewcraft.type.forage", 8500, -300, true, stackableProperties()));

    // ===== animal_product =====
    public static final DeferredHolder<Item, FishItem> BABY_LUNALOO = ITEMS.register("baby_lunaloo",
        () -> new FishItem(
            new int[]{35, 43, 52, 70},
            new int[]{-25, -25, -25, -25},
            new int[]{-5, -5, -5, -5},
            15, "floater",
            stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> CAMEL_WOOL = ITEMS.register("camel_wool",
        () -> new StardewQualityItem("stardewcraft.type.animal_product", 450, -300, true, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> FIR_WAX = ITEMS.register("fir_wax",
        () -> new SimpleStardewItem("stardewcraft.type.artisan_goods", 250, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> GOLD_SLIME_EGG = ITEMS.register("gold_slime_egg",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 12000, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> BLUE_SLIME_EGG = ITEMS.register("blue_slime_egg",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 1750, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> GREEN_SLIME_EGG = ITEMS.register("green_slime_egg",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 1000, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> RED_SLIME_EGG = ITEMS.register("red_slime_egg",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 2500, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> PURPLE_SLIME_EGG = ITEMS.register("purple_slime_egg",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 5000, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> FIREWORKS_RED = ITEMS.register("fireworks_red",
        () -> new SimpleStardewItem("stardewcraft.type.craftable", 50, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> FIREWORKS_PURPLE = ITEMS.register("fireworks_purple",
        () -> new SimpleStardewItem("stardewcraft.type.craftable", 50, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> FIREWORKS_GREEN = ITEMS.register("fireworks_green",
        () -> new SimpleStardewItem("stardewcraft.type.craftable", 50, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> GOLDEN_GOOSE_EGG = ITEMS.register("golden_goose_egg",
        () -> new StardewQualityItem("stardewcraft.type.animal_product", 6000, 0, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> GOOSE_EGG = ITEMS.register("goose_egg",
        () -> new StardewQualityItem("stardewcraft.type.animal_product", 300, 15, true, stackableProperties()));
    public static final DeferredHolder<Item, FishItem> LUNALOO = ITEMS.register("lunaloo",
        () -> new FishItem(
            new int[]{100, 125, 150, 200},
            new int[]{-62, -62, -62, -62},
            new int[]{-12, -12, -12, -12},
            70, "floater",
            stackableProperties()));
    public static final DeferredHolder<Item, FishItem> SHINY_LUNALOO = ITEMS.register("shiny_lunaloo",
        () -> new FishItem(
            new int[]{400, 500, 600, 800},
            new int[]{-125, -125, -125, -125},
            new int[]{-23, -23, -23, -23},
            110, "floater",
            stackableProperties()));

    // ===== artisan =====
    public static final DeferredHolder<Item, CookingDishItem> AGED_BLUE_MOON_WINE = ITEMS.register("aged_blue_moon_wine",
        () -> consumable("aged_blue_moon_wine"));
    public static final DeferredHolder<Item, CookingDishItem> BIRCH_SYRUP = STARDEWCRAFT_ITEMS.register("birch_syrup",
        () -> cooking("birch_syrup"));
    public static final DeferredHolder<Item, CookingDishItem> BLUE_MOON_WINE = ITEMS.register("blue_moon_wine",
        () -> consumable("blue_moon_wine"));
    public static final DeferredHolder<Item, StardewQualityItem> BUTTER = ITEMS.register("butter",
        () -> new StardewQualityItem(
            "stardewcraft.type.artisan_goods", 215, 31, true, stackableProperties()));
    public static final DeferredHolder<Item, StardewQualityItem> GOOSE_MAYONNAISE = ITEMS.register("goose_mayonnaise",
        () -> new StardewQualityItem(
            "stardewcraft.type.artisan_goods", 700, 45, false, stackableProperties()));
    public static final DeferredHolder<Item, CookingDishItem> HONEY_JAR = ITEMS.register("honey_jar",
        () -> new CookingDishItem(1000, 50,
            List.of(),
            stackableProperties(),
            false));

    public static final DeferredHolder<Item, ArtisanDrinkItem> JOJA_BERRY_WINE = kegProduct("joja_berry");
    public static final DeferredHolder<Item, ArtisanDrinkItem> MONSTER_FRUIT_WINE = kegProduct("monster_fruit");
    public static final DeferredHolder<Item, ArtisanDrinkItem> SALAL_BERRY_WINE = kegProduct("salal_berry");
    public static final DeferredHolder<Item, ArtisanDrinkItem> SLIME_BERRY_WINE = kegProduct("slime_berry");
    public static final DeferredHolder<Item, ArtisanDrinkItem> CUCUMBER_JUICE = kegProduct("cucumber");
    public static final DeferredHolder<Item, ArtisanDrinkItem> BUTTERNUT_SQUASH_JUICE = kegProduct("butternut_squash");
    public static final DeferredHolder<Item, ArtisanDrinkItem> GOLD_CARROT_JUICE = kegProduct("gold_carrot");
    public static final DeferredHolder<Item, ArtisanDrinkItem> SWEET_POTATO_JUICE = kegProduct("sweet_potato");
    public static final DeferredHolder<Item, ArtisanDrinkItem> JOJA_VEGGIE_JUICE = kegProduct("joja_veggie");
    public static final DeferredHolder<Item, ArtisanDrinkItem> ANCIENT_FIBER_JUICE = kegProduct("ancient_fiber");
    public static final DeferredHolder<Item, ArtisanDrinkItem> MONSTER_MUSHROOM_JUICE = kegProduct("monster_mushroom");
    public static final DeferredHolder<Item, ArtisanDrinkItem> VOID_ROOT_JUICE = kegProduct("void_root");

    // ===== cooking =====
    // ===== Block Items =====
    public static final DeferredHolder<Item, BlockItem> BUTTER_CHURNER = ITEMS.register("butter_churner",
        () -> new StardewBlockItem(ModBlocks.BUTTER_CHURNER.get(), "stardewcraft.type.utility", 0, stackableProperties()));
    public static final DeferredHolder<Item, BlockItem> YARN_SPOOLER = ITEMS.register("yarn_spooler",
        () -> new StardewBlockItem(ModBlocks.YARN_SPOOLER.get(), "stardewcraft.type.utility", 0,
            "item.stardewcraftsve.yarn_spooler.desc", stackableProperties()));

    // ===== cooking =====
    public static final DeferredHolder<Item, CookingDishItem> BAKED_BERRY_OATMEAL_SUPREME = STARDEWCRAFT_ITEMS.register("baked_berry_oatmeal_supreme",
        () -> cooking("baked_berry_oatmeal_supreme"));
    public static final DeferredHolder<Item, CookingDishItem> BAKED_POTATO = STARDEWCRAFT_ITEMS.register("baked_potato",
        () -> cooking("baked_potato"));
    public static final DeferredHolder<Item, CookingDishItem> BIG_BARK_BURGER = STARDEWCRAFT_ITEMS.register("big_bark_burger",
        () -> cooking("big_bark_burger"));
    public static final DeferredHolder<Item, CookingDishItem> CANDY = STARDEWCRAFT_ITEMS.register("candy",
        () -> cooking("candy"));
    public static final DeferredHolder<Item, CookingDishItem> CHEESE_CHARCUTERIE = STARDEWCRAFT_ITEMS.register("cheese_charcuterie",
        () -> cooking("cheese_charcuterie"));
    public static final DeferredHolder<Item, CookingDishItem> CHOCOLATE_TRUFFLE_BAR = STARDEWCRAFT_ITEMS.register("chocolate_truffle_bar",
        () -> cooking("chocolate_truffle_bar"));
    public static final DeferredHolder<Item, CookingDishItem> FISH_DUMPLING = STARDEWCRAFT_ITEMS.register("fish_dumpling",
        () -> cooking("fish_dumpling"));
    public static final DeferredHolder<Item, CookingDishItem> FLOWER_COOKIE = STARDEWCRAFT_ITEMS.register("flower_cookie",
        () -> cooking("flower_cookie"));
    public static final DeferredHolder<Item, CookingDishItem> FROG_LEGS = STARDEWCRAFT_ITEMS.register("frog_legs",
        () -> cooking("frog_legs"));
    public static final DeferredHolder<Item, CookingDishItem> GINGERBREAD_MAN = STARDEWCRAFT_ITEMS.register("gingerbread_man",
        () -> cooking("gingerbread_man"));
    public static final DeferredHolder<Item, CookingDishItem> GLAZED_BUTTERFISH = STARDEWCRAFT_ITEMS.register("glazed_butterfish",
        () -> cooking("glazed_butterfish"));
    public static final DeferredHolder<Item, CookingDishItem> GLAZED_PEAR = STARDEWCRAFT_ITEMS.register("glazed_pear",
        () -> cooking("glazed_pear"));
    public static final DeferredHolder<Item, CookingDishItem> GRAMPLETON_ORANGE_CHICKEN = STARDEWCRAFT_ITEMS.register("grampleton_orange_chicken",
        () -> consumable("grampleton_orange_chicken"));
    public static final DeferredHolder<Item, CookingDishItem> GRILLED_CHEESE_SANDWICH = STARDEWCRAFT_ITEMS.register("grilled_cheese_sandwich",
        () -> cooking("grilled_cheese_sandwich"));
    public static final DeferredHolder<Item, CookingDishItem> ICE_CREAM_SUNDAE = STARDEWCRAFT_ITEMS.register("ice_cream_sundae",
        () -> cooking("ice_cream_sundae"));
    public static final DeferredHolder<Item, CookingDishItem> MIXED_BERRY_PIE = STARDEWCRAFT_ITEMS.register("mixed_berry_pie",
        () -> cooking("mixed_berry_pie"));
    public static final DeferredHolder<Item, CookingDishItem> MUSHROOM_BERRY_RICE = STARDEWCRAFT_ITEMS.register("mushroom_berry_rice",
        () -> cooking("mushroom_berry_rice"));
    public static final DeferredHolder<Item, CookingDishItem> NECTARINE_FRUIT_BREAD = STARDEWCRAFT_ITEMS.register("nectarine_fruit_bread",
        () -> cooking("nectarine_fruit_bread"));
    public static final DeferredHolder<Item, CookingDishItem> PINEAPPLE_CUSTARD_CREPE = STARDEWCRAFT_ITEMS.register("pineapple_custard_crepe",
        () -> cooking("pineapple_custard_crepe"));
    public static final DeferredHolder<Item, CookingDishItem> RAMEN = STARDEWCRAFT_ITEMS.register("ramen",
        () -> cooking("ramen"));
    public static final DeferredHolder<Item, CookingDishItem> SEAWEED_SALAD = STARDEWCRAFT_ITEMS.register("seaweed_salad",
        () -> cooking("seaweed_salad"));
    public static final DeferredHolder<Item, CookingDishItem> SEED_COOKIE = STARDEWCRAFT_ITEMS.register("seed_cookie",
        () -> consumable("seed_cookie"));
    public static final DeferredHolder<Item, CookingDishItem> STUFFED_PERSIMMON = STARDEWCRAFT_ITEMS.register("stuffed_persimmon",
        () -> cooking("stuffed_persimmon"));
    public static final DeferredHolder<Item, CookingDishItem> VEGAN_CONE = STARDEWCRAFT_ITEMS.register("vegan_cone",
        () -> cooking("vegan_cone"));
    public static final DeferredHolder<Item, CookingDishItem> VOID_DELIGHT = STARDEWCRAFT_ITEMS.register("void_delight",
        () -> cooking("void_delight"));
    public static final DeferredHolder<Item, CookingDishItem> VOID_SALMON_SUSHI = STARDEWCRAFT_ITEMS.register("void_salmon_sushi",
        () -> cooking("void_salmon_sushi"));
    public static final DeferredHolder<Item, CookingDishItem> VOID_MAYO_SANDWICH = STARDEWCRAFT_ITEMS.register("void_mayo_sandwich",
        () -> new VoidMayoSandwichItem(stackableProperties()));

    // ===== elixir =====
    public static final DeferredHolder<Item, CookingDishItem> AEGIS_ELIXIR = ITEMS.register("aegis_elixir",
        () -> consumable("aegis_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> ARMOR_ELIXIR = ITEMS.register("armor_elixir",
        () -> consumable("armor_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> BARBARIAN_ELIXIR = ITEMS.register("barbarian_elixir",
        () -> consumable("barbarian_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> BOMBARDIER_ELIXIR = ITEMS.register("bombardier_elixir",
        () -> consumable("bombardier_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> GRAVITY_ELIXIR = ITEMS.register("gravity_elixir",
        () -> consumable("gravity_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> HASTE_ELIXIR = ITEMS.register("haste_elixir",
        () -> consumable("haste_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> HERO_ELIXIR = ITEMS.register("hero_elixir",
        () -> consumable("hero_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> LIGHTNING_ELIXIR = ITEMS.register("lightning_elixir",
        () -> consumable("lightning_elixir"));
    public static final DeferredHolder<Item, CookingDishItem> MARSH_TONIC = ITEMS.register("marsh_tonic",
        () -> consumable("marsh_tonic"));
    public static final DeferredHolder<Item, CookingDishItem> SPORTS_DRINK = ITEMS.register("sports_drink",
        () -> consumable("sports_drink"));
    public static final DeferredHolder<Item, CookingDishItem> STAMINA_CAPSULE = ITEMS.register("stamina_capsule",
        () -> consumable("stamina_capsule"));
    public static final DeferredHolder<Item, CookingDishItem> SUPER_JOJA_COLA = ITEMS.register("super_joja_cola",
        () -> consumable("super_joja_cola"));
    public static final DeferredHolder<Item, CookingDishItem> SUPER_STARFRUIT = ITEMS.register("super_starfruit",
        () -> new CookingDishItem(4500, -300,
            List.of(),
            stackableProperties(),
            false));

    // ===== trinket =====
    public static final DeferredHolder<Item, SimpleStardewItem> GOLDEN_KEY = ITEMS.register("golden_key",
        () -> new SimpleStardewItem("stardewcraft.type.trinket", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> LUCKY_FOUR_LEAF_CLOVER = ITEMS.register("lucky_four_leaf_clover",
        () -> new SimpleStardewItem("stardewcraft.type.forage", 400, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> MERMAID_BRACELET = ITEMS.register("mermaid_bracelet",
        () -> new SimpleStardewItem("stardewcraft.type.trinket", 0, stackableProperties()));
    public static final DeferredHolder<Item, CookingDishItem> PRISMATIC_POP = STARDEWCRAFT_ITEMS.register("prismatic_pop",
        () -> cooking("prismatic_pop"));
    public static final DeferredHolder<Item, SunTotemItem> SUN_TOTEM = ITEMS.register("sun_totem",
        () -> new SunTotemItem(30, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> TREE_COIN = ITEMS.register("tree_coin",
        () -> new SimpleStardewItem("stardewcraft.type.trinket", 10000, stackableProperties()));
    public static final DeferredHolder<Item, WindTotemItem> WIND_TOTEM = ITEMS.register("wind_totem",
        () -> new WindTotemItem(30, stackableProperties()));

    // ===== deed =====
    public static final DeferredHolder<Item, SimpleStardewItem> AURORA_VINEYARD_PROPERTY_DEED = ITEMS.register("aurora_vineyard_property_deed",
        () -> new SimpleStardewItem("stardewcraft.type.deed", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> PROPERTY_DEED = ITEMS.register("property_deed",
        () -> new SimpleStardewItem("stardewcraft.type.deed", 0, stackableProperties()));

    // ===== furniture =====
    public static final DeferredHolder<Item, BlockItem> HEDGE_FENCE = ITEMS.register("hedge_fence",
        () -> new StardewBlockItem(ModBlocks.HEDGE_FENCE.get(), "stardewcraft.type.furniture", 8,
            "item.stardewcraftsve.hedge_fence.desc", stackableProperties()));
    public static final DeferredHolder<Item, BlockItem> SMALL_HARDWOOD_FENCE = ITEMS.register("small_hardwood_fence",
        () -> new StardewBlockItem(ModBlocks.SMALL_HARDWOOD_FENCE.get(), "stardewcraft.type.furniture", 8,
            "item.stardewcraftsve.small_hardwood_fence.desc", stackableProperties()));

    // ===== power =====
    public static final DeferredHolder<Item, SimpleStardewItem> ANIMAL_MASTERY = ITEMS.register("animal_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> BREWING_MASTERY = ITEMS.register("brewing_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> CHEESE_MASTERY = ITEMS.register("cheese_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> CRAFTING_MASTERY = ITEMS.register("crafting_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> GRAPE_MASTERY = ITEMS.register("grape_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> STARFRUIT_MASTERY = ITEMS.register("starfruit_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> STRAWBERRY_MASTERY = ITEMS.register("strawberry_mastery",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> WARP_MAGIC = ITEMS.register("warp_magic",
        () -> new SimpleStardewItem("stardewcraft.type.power", 0, stackableProperties()));

    // ===== weapon =====
    public static final DeferredHolder<Item, StardewWeaponItem> DIAMOND_WAND = ITEMS.register("diamond_wand",
        () -> new SveSwordItem("diamond_wand", unstackableProperties()));
    public static final DeferredHolder<Item, StardewWeaponItem> HEAVY_SHIELD = ITEMS.register("heavy_shield",
        () -> new SveSwordItem("heavy_shield", unstackableProperties()));
    public static final DeferredHolder<Item, StardewClubItem> MONSTER_SPLITTER = ITEMS.register("monster_splitter",
        () -> new SveClubItem("monster_splitter", unstackableProperties()));
    public static final DeferredHolder<Item, SimpleStardewItem> RUSTY_BLADE = ITEMS.register("rusty_blade",
        () -> new SimpleStardewItem("stardewcraft.type.monster_loot", 200, stackableProperties()));
    public static final DeferredHolder<Item, StardewDaggerItem> TEMPERED_GALAXY_DAGGER = ITEMS.register("tempered_galaxy_dagger",
        () -> new SveDaggerItem("tempered_galaxy_dagger", unstackableProperties()));
    public static final DeferredHolder<Item, StardewClubItem> TEMPERED_GALAXY_HAMMER = ITEMS.register("tempered_galaxy_hammer",
        () -> new SveClubItem("tempered_galaxy_hammer", unstackableProperties()));
    public static final DeferredHolder<Item, StardewWeaponItem> TEMPERED_GALAXY_SWORD = ITEMS.register("tempered_galaxy_sword",
        () -> new SveSwordItem("tempered_galaxy_sword", unstackableProperties()));

    // ===== Recipe-only intermediates =====

    public static final DeferredHolder<Item, EdibleSimpleStardewItem> BIRCH_WATER =
        ITEMS.register("birch_water",
            () -> new EdibleSimpleStardewItem(
                    "stardewcraft.type.artisan_goods", 50, 38, 17, true, stackableProperties()));

    // ===== smoked fish (registered under stardewcraftsve namespace so JEI shows @SVE tag) =====

    public static final DeferredHolder<Item, Item> SMOKED_ALLIGATOR = SMOKED_FISH_ITEMS.register("smoked_alligator",
        () -> new SmokedFishItem(() -> ModItems.ALLIGATOR.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_ARROWHEAD_SHARK = SMOKED_FISH_ITEMS.register("smoked_arrowhead_shark",
        () -> new SmokedFishItem(() -> ModItems.ARROWHEAD_SHARK.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_BABY_LUNALOO = SMOKED_FISH_ITEMS.register("smoked_baby_lunaloo",
        () -> new SmokedFishItem(() -> ModItems.BABY_LUNALOO.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_BARRED_KNIFEJAW = SMOKED_FISH_ITEMS.register("smoked_barred_knifejaw",
        () -> new SmokedFishItem(() -> ModItems.BARRED_KNIFEJAW.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_BLUE_TANG = SMOKED_FISH_ITEMS.register("smoked_blue_tang",
        () -> new SmokedFishItem(() -> ModItems.BLUE_TANG.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_BONEFISH = SMOKED_FISH_ITEMS.register("smoked_bonefish",
        () -> new SmokedFishItem(() -> ModItems.BONEFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_BULL_TROUT = SMOKED_FISH_ITEMS.register("smoked_bull_trout",
        () -> new SmokedFishItem(() -> ModItems.BULL_TROUT.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_BUTTERFISH = SMOKED_FISH_ITEMS.register("smoked_butterfish",
        () -> new SmokedFishItem(() -> ModItems.BUTTERFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_CLOWNFISH = SMOKED_FISH_ITEMS.register("smoked_clownfish",
        () -> new SmokedFishItem(() -> ModItems.CLOWNFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_DAGGERFISH = SMOKED_FISH_ITEMS.register("smoked_daggerfish",
        () -> new SmokedFishItem(() -> ModItems.DAGGERFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_DIAMOND_CARP = SMOKED_FISH_ITEMS.register("smoked_diamond_carp",
        () -> new SmokedFishItem(() -> ModItems.DIAMOND_CARP.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_FIBER_GOBY = SMOKED_FISH_ITEMS.register("smoked_fiber_goby",
        () -> new SmokedFishItem(() -> ModItems.FIBER_GOBY.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_FROG = SMOKED_FISH_ITEMS.register("smoked_frog",
        () -> new SmokedFishItem(() -> ModItems.FROG.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_GAR = SMOKED_FISH_ITEMS.register("smoked_gar",
        () -> new SmokedFishItem(() -> ModItems.GAR.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_GEMFISH = SMOKED_FISH_ITEMS.register("smoked_gemfish",
        () -> new SmokedFishItem(() -> ModItems.GEMFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_GOLDENFISH = SMOKED_FISH_ITEMS.register("smoked_goldenfish",
        () -> new SmokedFishItem(() -> ModItems.GOLDENFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_GOLDFISH = SMOKED_FISH_ITEMS.register("smoked_goldfish",
        () -> new SmokedFishItem(() -> ModItems.GOLDFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_GRASS_CARP = SMOKED_FISH_ITEMS.register("smoked_grass_carp",
        () -> new SmokedFishItem(() -> ModItems.GRASS_CARP.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_HIGHLANDS_BASS = SMOKED_FISH_ITEMS.register("smoked_highlands_bass",
        () -> new SmokedFishItem(() -> ModItems.HIGHLANDS_BASS.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_KING_SALMON = SMOKED_FISH_ITEMS.register("smoked_king_salmon",
        () -> new SmokedFishItem(() -> ModItems.KING_SALMON.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_KITTYFISH = SMOKED_FISH_ITEMS.register("smoked_kittyfish",
        () -> new SmokedFishItem(() -> ModItems.KITTYFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_LUNALOO = SMOKED_FISH_ITEMS.register("smoked_lunaloo",
        () -> new SmokedFishItem(() -> ModItems.LUNALOO.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_METEOR_CARP = SMOKED_FISH_ITEMS.register("smoked_meteor_carp",
        () -> new SmokedFishItem(() -> ModItems.METEOR_CARP.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_MINNOW = SMOKED_FISH_ITEMS.register("smoked_minnow",
        () -> new SmokedFishItem(() -> ModItems.MINNOW.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_OCEAN_SUNFISH = SMOKED_FISH_ITEMS.register("smoked_ocean_sunfish",
        () -> new SmokedFishItem(() -> ModItems.OCEAN_SUNFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_PUPPYFISH = SMOKED_FISH_ITEMS.register("smoked_puppyfish",
        () -> new SmokedFishItem(() -> ModItems.PUPPYFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_RADIOACTIVE_BASS = SMOKED_FISH_ITEMS.register("smoked_radioactive_bass",
        () -> new SmokedFishItem(() -> ModItems.RADIOACTIVE_BASS.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_RAZOR_TROUT = SMOKED_FISH_ITEMS.register("smoked_razor_trout",
        () -> new SmokedFishItem(() -> ModItems.RAZOR_TROUT.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_SEA_SPONGE = SMOKED_FISH_ITEMS.register("smoked_sea_sponge",
        () -> new SmokedFishItem(() -> ModItems.SEA_SPONGE.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_SEAHORSE = SMOKED_FISH_ITEMS.register("smoked_seahorse",
        () -> new SmokedFishItem(() -> ModItems.SEAHORSE.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_SHARK = SMOKED_FISH_ITEMS.register("smoked_shark",
        () -> new SmokedFishItem(() -> ModItems.SHARK.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_SHINY_LUNALOO = SMOKED_FISH_ITEMS.register("smoked_shiny_lunaloo",
        () -> new SmokedFishItem(() -> ModItems.SHINY_LUNALOO.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_SNATCHER_WORM = SMOKED_FISH_ITEMS.register("smoked_snatcher_worm",
        () -> new SmokedFishItem(() -> ModItems.SNATCHER_WORM.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_STARFISH = SMOKED_FISH_ITEMS.register("smoked_starfish",
        () -> new SmokedFishItem(() -> ModItems.STARFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_SWAMP_CRAB = SMOKED_FISH_ITEMS.register("smoked_swamp_crab",
        () -> new SmokedFishItem(() -> ModItems.SWAMP_CRAB.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_TADPOLE = SMOKED_FISH_ITEMS.register("smoked_tadpole",
        () -> new SmokedFishItem(() -> ModItems.TADPOLE.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_TORPEDO_TROUT = SMOKED_FISH_ITEMS.register("smoked_torpedo_trout",
        () -> new SmokedFishItem(() -> ModItems.TORPEDO_TROUT.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_TURRETFISH = SMOKED_FISH_ITEMS.register("smoked_turretfish",
        () -> new SmokedFishItem(() -> ModItems.TURRETFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_UNDEADFISH = SMOKED_FISH_ITEMS.register("smoked_undeadfish",
        () -> new SmokedFishItem(() -> ModItems.UNDEADFISH.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_VIPER_EEL = SMOKED_FISH_ITEMS.register("smoked_viper_eel",
        () -> new SmokedFishItem(() -> ModItems.VIPER_EEL.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_VOID_EEL = SMOKED_FISH_ITEMS.register("smoked_void_eel",
        () -> new SmokedFishItem(() -> ModItems.VOID_EEL.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_WATER_GRUB = SMOKED_FISH_ITEMS.register("smoked_water_grub",
        () -> new SmokedFishItem(() -> ModItems.WATER_GRUB.get(), stackableProperties()));
    public static final DeferredHolder<Item, Item> SMOKED_WOLF_SNAPPER = SMOKED_FISH_ITEMS.register("smoked_wolf_snapper",
        () -> new SmokedFishItem(() -> ModItems.WOLF_SNAPPER.get(), stackableProperties()));

    // Convenience list for iteration (creative tab, etc.)
    public static final List<DeferredHolder<Item, ? extends Item>> SMOKED_FISH = List.of(
        SMOKED_ALLIGATOR, SMOKED_ARROWHEAD_SHARK, SMOKED_BABY_LUNALOO, SMOKED_BARRED_KNIFEJAW,
        SMOKED_BLUE_TANG, SMOKED_BONEFISH, SMOKED_BULL_TROUT, SMOKED_BUTTERFISH,
        SMOKED_CLOWNFISH, SMOKED_DAGGERFISH, SMOKED_DIAMOND_CARP, SMOKED_FIBER_GOBY,
        SMOKED_FROG, SMOKED_GAR, SMOKED_GEMFISH, SMOKED_GOLDENFISH, SMOKED_GOLDFISH,
        SMOKED_GRASS_CARP, SMOKED_HIGHLANDS_BASS, SMOKED_KING_SALMON, SMOKED_KITTYFISH,
        SMOKED_LUNALOO, SMOKED_METEOR_CARP, SMOKED_MINNOW, SMOKED_OCEAN_SUNFISH,
        SMOKED_PUPPYFISH, SMOKED_RADIOACTIVE_BASS, SMOKED_RAZOR_TROUT, SMOKED_SEA_SPONGE,
        SMOKED_SEAHORSE, SMOKED_SHARK, SMOKED_SHINY_LUNALOO, SMOKED_SNATCHER_WORM,
        SMOKED_STARFISH, SMOKED_SWAMP_CRAB, SMOKED_TADPOLE,
        SMOKED_TORPEDO_TROUT, SMOKED_TURRETFISH, SMOKED_UNDEADFISH, SMOKED_VIPER_EEL,
        SMOKED_VOID_EEL, SMOKED_WATER_GRUB, SMOKED_WOLF_SNAPPER
    );

    private static DeferredHolder<Item, Item> artisan(String id, PreserveType type) {
        return ARTISAN_ITEMS.register(id, () -> new PreservesItem(type, stackableProperties()));
    }

    private static DeferredHolder<Item, Item> cropArtisan(String inputPath, PreserveType type) {
        SvePreservesData.Product product = SvePreservesData.displayProduct(inputPath, type);
        return artisan(product.displayOutputPath(), type);
    }

    private static DeferredHolder<Item, ArtisanDrinkItem> kegProduct(String inputPath) {
        SveKegData.Product product = SveKegData.byInputPath(inputPath);
        return ARTISAN_ITEMS.register(product.outputPath(), () -> new ArtisanDrinkItem(
                product.sellPrice(), product.energy(), product.health(), 0, 0,
                product.supportsQuality(), stackableProperties()));
    }

    // ===== flavored artisan display items (registered under stardewcraftsve for JEI mod tag) =====

    // Jelly flavors
    public static final DeferredHolder<Item, Item> JOJA_BERRY_JELLY = cropArtisan("joja_berry", PreserveType.JELLY);
    public static final DeferredHolder<Item, Item> MONSTER_FRUIT_JELLY = cropArtisan("monster_fruit", PreserveType.JELLY);
    public static final DeferredHolder<Item, Item> SALAL_BERRY_JELLY = cropArtisan("salal_berry", PreserveType.JELLY);
    public static final DeferredHolder<Item, Item> SLIME_BERRY_JELLY = cropArtisan("slime_berry", PreserveType.JELLY);

    // Pickles flavors
    public static final DeferredHolder<Item, Item> CUCUMBER_PICKLES = cropArtisan("cucumber", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> BUTTERNUT_SQUASH_PICKLES = cropArtisan("butternut_squash", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> GOLD_CARROT_PICKLES = cropArtisan("gold_carrot", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> SWEET_POTATO_PICKLES = cropArtisan("sweet_potato", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> JOJA_VEGGIE_PICKLES = cropArtisan("joja_veggie", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> ANCIENT_FIBER_PICKLES = cropArtisan("ancient_fiber", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> MONSTER_MUSHROOM_PICKLES = cropArtisan("monster_mushroom", PreserveType.PICKLES);
    public static final DeferredHolder<Item, Item> VOID_ROOT_PICKLES = cropArtisan("void_root", PreserveType.PICKLES);

    // Dried fruit flavors
    public static final DeferredHolder<Item, Item> JOJA_BERRY_DRIED_FRUIT = cropArtisan("joja_berry", PreserveType.DRIED_FRUIT);
    public static final DeferredHolder<Item, Item> MONSTER_FRUIT_DRIED_FRUIT = cropArtisan("monster_fruit", PreserveType.DRIED_FRUIT);
    public static final DeferredHolder<Item, Item> SALAL_BERRY_DRIED_FRUIT = cropArtisan("salal_berry", PreserveType.DRIED_FRUIT);
    public static final DeferredHolder<Item, Item> SLIME_BERRY_DRIED_FRUIT = cropArtisan("slime_berry", PreserveType.DRIED_FRUIT);

    // Dried mushroom flavors
    public static final DeferredHolder<Item, Item> GREEN_MUSHROOM_DRIED_MUSHROOMS = artisan("green_mushroom_dried_mushrooms", PreserveType.DRIED_MUSHROOMS);
    public static final DeferredHolder<Item, Item> MEGA_PURPLE_MUSHROOM_DRIED_MUSHROOMS = artisan("mega_purple_mushroom_dried_mushrooms", PreserveType.DRIED_MUSHROOMS);
    public static final DeferredHolder<Item, Item> MONSTER_MUSHROOM_DRIED_MUSHROOMS = cropArtisan("monster_mushroom", PreserveType.DRIED_MUSHROOMS);
    public static final DeferredHolder<Item, Item> MUSHROOM_COLONY_DRIED_MUSHROOMS = artisan("mushroom_colony_dried_mushrooms", PreserveType.DRIED_MUSHROOMS);
    public static final DeferredHolder<Item, Item> POISON_MUSHROOM_DRIED_MUSHROOMS = artisan("poison_mushroom_dried_mushrooms", PreserveType.DRIED_MUSHROOMS);

    // SVE-namespaced roe and aged roe display items so they appear under @SVE mod tag in JEI
    public static final DeferredHolder<Item, Item> SVE_ROE = artisan("sve_roe", PreserveType.ROE);
    public static final DeferredHolder<Item, Item> SVE_AGED_ROE = artisan("sve_aged_roe", PreserveType.AGED_ROE);

    // ===== Debug items =====
    public static final DeferredHolder<Item, SveDebugWandItem> DEBUG_WAND = ITEMS.register("debug_wand",
        () -> new SveDebugWandItem(unstackableProperties()));

    private ModItems() {}
}
