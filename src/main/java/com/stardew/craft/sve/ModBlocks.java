package com.stardew.craft.sve;

import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.block.nature.ForageBlock;
import com.stardew.craft.sve.tree.SveFruitTreeBlock;
import com.stardew.craft.sve.tree.SveFruitTreeExtensionBlock;
import com.stardew.craft.sve.tree.SveFruitTreeSaplingBlock;
import com.stardew.craft.sve.tree.SveFruitTreeType;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeSaplingBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry for SVE crop blocks.
 * Each crop block is a parameterized SveCropBlock instance.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(StardewcraftsveMod.MODID);

    private static BlockBehaviour.Properties cropProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .pushReaction(PushReaction.DESTROY)
                .noOcclusion()
                .instabreak()
                .sound(net.minecraft.world.level.block.SoundType.CROP);
    }

    private static final BlockBehaviour.Properties FORAGE_PROPS = BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .pushReaction(PushReaction.DESTROY)
            .sound(SoundType.GRASS)
            .noCollission()
            .noOcclusion()
            .instabreak();

    private static BlockBehaviour.Properties forageProps(boolean seasonal) {
        return seasonal ? FORAGE_PROPS.randomTicks() : FORAGE_PROPS;
    }

    private static DeferredBlock<Block> forage(String name, int... seasons) {
        return BLOCKS.register("forage_" + name, () -> {
            ForageBlock block = new ForageBlock(forageProps(seasons.length > 0));
            return block
                .setDrop(() -> new ItemStack(
                    BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("stardewcraftsve", name)
                    )
                ))
                .setAllowedSeasons(seasons);
        });
    }

    private static BlockBehaviour.Properties fruitSaplingProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .pushReaction(PushReaction.DESTROY)
                .sound(SoundType.GRASS)
                .noCollission()
                .noOcclusion()
                .instabreak();
    }

    private static BlockBehaviour.Properties fruitTreeProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .strength(2.0F, 3.0F);
    }

    // ===== Fruit trees =====
    public static final DeferredBlock<Block> PEAR_SAPLING = BLOCKS.register("pear_sapling",
            () -> new SveFruitTreeSaplingBlock(SveFruitTreeType.PEAR, fruitSaplingProps()));
    public static final DeferredBlock<Block> PEAR_TREE = BLOCKS.register("pear_tree",
            () -> new SveFruitTreeBlock(SveFruitTreeType.PEAR, fruitTreeProps()));
    public static final DeferredBlock<Block> PEAR_TREE_EXTENSION = BLOCKS.register("pear_tree_extension",
            () -> new SveFruitTreeExtensionBlock(SveFruitTreeType.PEAR, fruitTreeProps()));
    public static final DeferredBlock<Block> NECTARINE_SAPLING = BLOCKS.register("nectarine_sapling",
            () -> new SveFruitTreeSaplingBlock(SveFruitTreeType.NECTARINE, fruitSaplingProps()));
    public static final DeferredBlock<Block> NECTARINE_TREE = BLOCKS.register("nectarine_tree",
            () -> new SveFruitTreeBlock(SveFruitTreeType.NECTARINE, fruitTreeProps()));
    public static final DeferredBlock<Block> NECTARINE_TREE_EXTENSION = BLOCKS.register("nectarine_tree_extension",
            () -> new SveFruitTreeExtensionBlock(SveFruitTreeType.NECTARINE, fruitTreeProps()));
    public static final DeferredBlock<Block> PERSIMMON_SAPLING = BLOCKS.register("persimmon_sapling",
            () -> new SveFruitTreeSaplingBlock(SveFruitTreeType.PERSIMMON, fruitSaplingProps()));
    public static final DeferredBlock<Block> PERSIMMON_TREE = BLOCKS.register("persimmon_tree",
            () -> new SveFruitTreeBlock(SveFruitTreeType.PERSIMMON, fruitTreeProps()));
    public static final DeferredBlock<Block> PERSIMMON_TREE_EXTENSION = BLOCKS.register("persimmon_tree_extension",
            () -> new SveFruitTreeExtensionBlock(SveFruitTreeType.PERSIMMON, fruitTreeProps()));

    // ===== Wild trees =====
    public static final DeferredBlock<Block> FIR_SAPLING = BLOCKS.register("fir_sapling",
            () -> new SveWildTreeSaplingBlock(SveWildTreeType.FIR, fruitSaplingProps()));
    public static final DeferredBlock<Block> FIR_TREE = BLOCKS.register("fir_tree",
            () -> new SveWildTreeBlock(SveWildTreeType.FIR, fruitTreeProps()));
    public static final DeferredBlock<Block> FIR_TREE_EXTENSION = BLOCKS.register("fir_tree_extension",
            () -> new SveWildTreeExtensionBlock(SveWildTreeType.FIR, fruitTreeProps()));
    public static final DeferredBlock<Block> BIRCH_SAPLING = BLOCKS.register("birch_sapling",
            () -> new SveWildTreeSaplingBlock(SveWildTreeType.BIRCH, fruitSaplingProps()));
    public static final DeferredBlock<Block> BIRCH_TREE = BLOCKS.register("birch_tree",
            () -> new SveWildTreeBlock(SveWildTreeType.BIRCH, fruitTreeProps()));
    public static final DeferredBlock<Block> BIRCH_TREE_EXTENSION = BLOCKS.register("birch_tree_extension",
            () -> new SveWildTreeExtensionBlock(SveWildTreeType.BIRCH, fruitTreeProps()));

    // ===== Fences =====
    public static final DeferredBlock<FenceBlock> HEDGE_FENCE = BLOCKS.register("hedge_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(2.5F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()));
    public static final DeferredBlock<FenceBlock> SMALL_HARDWOOD_FENCE = BLOCKS.register("small_hardwood_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.0F, 4.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()));

    // ===== Crops =====
    // Cucumber: Spring, 12 days, regrow every 2d
    public static final DeferredBlock<Block> CUCUMBER_CROP = BLOCKS.register("cucumber_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.CUCUMBER_SEED.get(),
                    () -> ModItems.CUCUMBER.get(),
                    SveCropData.CUCUMBER));

    // Butternut Squash: Summer, 12 days, no regrow
    public static final DeferredBlock<Block> BUTTERNUT_SQUASH_CROP = BLOCKS.register("butternut_squash_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.BUTTERNUT_SQUASH_SEED.get(),
                    () -> ModItems.BUTTERNUT_SQUASH.get(),
                    SveCropData.BUTTERNUT_SQUASH));

    // Gold Carrot: Spring/Summer/Fall, 6 days, no regrow
    public static final DeferredBlock<Block> GOLD_CARROT_CROP = BLOCKS.register("gold_carrot_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.GOLD_CARROT_SEED.get(),
                    () -> ModItems.GOLD_CARROT.get(),
                    SveCropData.GOLD_CARROT));

    // Sweet Potato: Fall, 12 days, no regrow
    public static final DeferredBlock<Block> SWEET_POTATO_CROP = BLOCKS.register("sweet_potato_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.SWEET_POTATO_SEED.get(),
                    () -> ModItems.SWEET_POTATO.get(),
                    SveCropData.SWEET_POTATO));

    // Joja Berry: Spring/Summer/Fall, 25 days, regrow every 4d
    public static final DeferredBlock<Block> JOJA_BERRY_CROP = BLOCKS.register("joja_berry_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.JOJA_BERRY_STARTER.get(),
                    () -> ModItems.JOJA_BERRY.get(),
                    SveCropData.JOJA_BERRY));

    // Joja Veggie: Spring/Summer/Fall, 13 days, no regrow
    public static final DeferredBlock<Block> JOJA_VEGGIE_CROP = BLOCKS.register("joja_veggie_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.JOJA_VEGGIE_SEEDS.get(),
                    () -> ModItems.JOJA_VEGGIE.get(),
                    SveCropData.JOJA_VEGGIE));

    // Monster Fruit (from Stalk Seed): Summer, 25 days, no regrow
    public static final DeferredBlock<Block> MONSTER_FRUIT_CROP = BLOCKS.register("monster_fruit_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.STALK_SEED.get(),
                    () -> ModItems.MONSTER_FRUIT.get(),
                    SveCropData.MONSTER_FRUIT));

    // Salal Berry: Spring/Summer, 13 days, regrow every 4d, 2-4 harvest
    public static final DeferredBlock<Block> SALAL_BERRY_CROP = BLOCKS.register("salal_berry_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.SALAL_BERRY_SEED.get(),
                    () -> ModItems.SALAL_BERRY.get(),
                    SveCropData.SALAL_BERRY));

    // Slime Berry: Spring, 13 days, regrow every 4d
    public static final DeferredBlock<Block> SLIME_BERRY_CROP = BLOCKS.register("slime_berry_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.SLIME_SEED.get(),
                    () -> ModItems.SLIME_BERRY.get(),
                    SveCropData.SLIME_BERRY));

    // Ancient Fiber: Summer, 12 days, no regrow, 2-4 harvest
    public static final DeferredBlock<Block> ANCIENT_FIBER_CROP = BLOCKS.register("ancient_fiber_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.ANCIENT_FERNS_SEED.get(),
                    () -> ModItems.ANCIENT_FIBER.get(),
                    SveCropData.ANCIENT_FIBER));

    // Monster Mushroom: Fall, 13 days, no regrow
    public static final DeferredBlock<Block> MONSTER_MUSHROOM_CROP = BLOCKS.register("monster_mushroom_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.FUNGUS_SEED.get(),
                    () -> ModItems.MONSTER_MUSHROOM.get(),
                    SveCropData.MONSTER_MUSHROOM));

    // Void Root: Winter, 8 days, no regrow
    public static final DeferredBlock<Block> VOID_ROOT_CROP = BLOCKS.register("void_root_crop",
            () -> new SveCropBlock(cropProps(),
                    () -> ModItems.VOID_SEED.get(),
                    () -> ModItems.VOID_ROOT.get(),
                    SveCropData.VOID_ROOT));

    // ===== Forage: Mushrooms =====
    public static final DeferredBlock<Block> FORAGE_GREEN_MUSHROOM = forage("green_mushroom", 1, 2);
    public static final DeferredBlock<Block> FORAGE_MEGA_PURPLE_MUSHROOM = forage("mega_purple_mushroom", 3);
    public static final DeferredBlock<Block> FORAGE_MUSHROOM_COLONY = forage("mushroom_colony", 2);
    public static final DeferredBlock<Block> FORAGE_POISON_MUSHROOM = forage("poison_mushroom", 1, 2);

    // ===== Forage: Flowers =====
    public static final DeferredBlock<Block> FORAGE_DIAMOND_FLOWER = forage("diamond_flower", 3);
    public static final DeferredBlock<Block> FORAGE_FERNGILL_PRIMROSE = forage("ferngill_primrose", 0);
    public static final DeferredBlock<Block> FORAGE_GOLDENROD = forage("goldenrod", 1, 2);
    public static final DeferredBlock<Block> FORAGE_GOLDEN_OCEAN_FLOWER = forage("golden_ocean_flower");
    public static final DeferredBlock<Block> FORAGE_SMELLY_RAFFLESIA = forage("smelly_rafflesia", 0, 1, 2);
    public static final DeferredBlock<Block> FORAGE_SWAMP_FLOWER = forage("swamp_flower", 0, 1, 2);
    public static final DeferredBlock<Block> FORAGE_THISTLE = forage("thistle");
    public static final DeferredBlock<Block> FORAGE_WINTER_STAR_ROSE = forage("winter_star_rose", 3);

    // ===== Forage: Beach =====
    public static final DeferredBlock<Block> FORAGE_BIG_CONCH = forage("big_conch");
    public static final DeferredBlock<Block> FORAGE_DRIED_SAND_DOLLAR = forage("dried_sand_dollar");
    public static final DeferredBlock<Block> FORAGE_DULSE_SEAWEED = forage("dulse_seaweed");
    public static final DeferredBlock<Block> FORAGE_SEA_SPONGE = forage("sea_sponge");
    public static final DeferredBlock<Block> FORAGE_STARFISH = forage("starfish");

    // ===== Forage: Swamp =====
    public static final DeferredBlock<Block> FORAGE_SLUDGE = forage("sludge");
    public static final DeferredBlock<Block> FORAGE_SUPERNATURAL_GOO = forage("supernatural_goo");
    public static final DeferredBlock<Block> FORAGE_SWAMP_CRAB = forage("swamp_crab");
    public static final DeferredBlock<Block> FORAGE_SWAMP_ESSENCE = forage("swamp_essence");

    // ===== Forage: General =====
    public static final DeferredBlock<Block> FORAGE_FIR_CONE = forage("fir_cone");
    public static final DeferredBlock<Block> FORAGE_VOID_PEBBLE = forage("void_pebble");
    public static final DeferredBlock<Block> FORAGE_VOID_ROOT = forage("void_root");
    public static final DeferredBlock<Block> FORAGE_VOID_SHARD = forage("void_shard");
    public static final DeferredBlock<Block> FORAGE_VOID_SOUL = forage("void_soul");
    public static final DeferredBlock<Block> FORAGE_YARN = forage("yarn");
    public static final DeferredBlock<Block> FORAGE_SHARK_TOOTH = forage("shark_tooth");
    public static final DeferredBlock<Block> FORAGE_SWIRL_STONE = forage("swirl_stone");

    // ===== Forage: Special (biome-based spawning) =====
    public static final DeferredBlock<Block> FORAGE_BEARBERRYS = forage("bearberrys", 3);
    public static final DeferredBlock<Block> FORAGE_LUCKY_FOUR_LEAF_CLOVER = forage("lucky_four_leaf_clover", 0, 1);
    public static final DeferredBlock<Block> FORAGE_RED_BANEBERRY = forage("red_baneberry", 1);

    // ===== Utility =====
    public static final DeferredBlock<Block> BUTTER_CHURNER = BLOCKS.register("butter_churner",
        () -> new ButterChurnerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .noOcclusion()
            .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> YARN_SPOOLER = BLOCKS.register("yarn_spooler",
        () -> new YarnSpoolerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .noOcclusion()
            .sound(SoundType.WOOD)));

    private ModBlocks() {}
}
