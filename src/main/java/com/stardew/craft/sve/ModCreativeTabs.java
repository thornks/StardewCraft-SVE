package com.stardew.craft.sve;

import com.stardew.craft.item.quality.QualityHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StardewcraftsveMod.MODID);

    @FunctionalInterface
    private interface TabFiller {
        void fill(CreativeModeTab.Output output);
    }

    // ===== Tabs =====

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CROPS =
        tab("crops", () -> new ItemStack(ModItems.GOLD_CARROT.get()), output -> {
            quality(output, ModItems.BUTTERNUT_SQUASH, ModItems.CUCUMBER, ModItems.GOLD_CARROT,
                ModItems.JOJA_BERRY, ModItems.JOJA_VEGGIE, ModItems.MONSTER_FRUIT, ModItems.SWEET_POTATO,
                ModItems.ANCIENT_FIBER, ModItems.BEARBERRYS,
                ModItems.MONSTER_MUSHROOM,
                ModItems.NECTARINE, ModItems.PEAR, ModItems.PERSIMMON,
                ModItems.SALAL_BERRY, ModItems.SLIME_BERRY, ModItems.VOID_ROOT);
            output.accept(ModItems.DEWDROP_BERRY.get());
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEEDS =
        tab("seeds", () -> new ItemStack(ModItems.GOLD_CARROT_SEED.get()), output -> {
            output.accept(ModItems.ANCIENT_FERNS_SEED.get());
            output.accept(ModItems.BIRCH_SEED.get());
            output.accept(ModItems.BUTTERNUT_SQUASH_SEED.get());
            output.accept(ModItems.CUCUMBER_SEED.get());
            output.accept(ModItems.FUNGUS_SEED.get());
            output.accept(ModItems.FIR_CONE.get());
            output.accept(ModItems.GOLD_CARROT_SEED.get());
            output.accept(ModItems.JOJA_BERRY_STARTER.get());
            output.accept(ModItems.JOJA_VEGGIE_SEEDS.get());
            output.accept(ModItems.NECTARINE_SAPLING.get());
            output.accept(ModItems.PEAR_SAPLING.get());
            output.accept(ModItems.PERSIMMON_SAPLING.get());
            output.accept(ModItems.SLIME_SEED.get());
            output.accept(ModItems.STALK_SEED.get());
            output.accept(ModItems.SWEET_POTATO_SEED.get());
            output.accept(ModItems.SALAL_BERRY_SEED.get());
            output.accept(ModItems.VOID_SEED.get());
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FORAGE =
        tab("forage", () -> new ItemStack(ModItems.GOLDEN_OCEAN_FLOWER.get()), output -> {
            quality(output, ModItems.GREEN_MUSHROOM, ModItems.MEGA_PURPLE_MUSHROOM,
                ModItems.MUSHROOM_COLONY, ModItems.POISON_MUSHROOM,
                ModItems.DIAMOND_FLOWER, ModItems.FERNGILL_PRIMROSE, ModItems.GOLDENROD,
                ModItems.GOLDEN_OCEAN_FLOWER, ModItems.SMELLY_RAFFLESIA, ModItems.SWAMP_FLOWER,
                ModItems.THISTLE, ModItems.WINTER_STAR_ROSE,
                ModItems.BIG_CONCH, ModItems.DRIED_SAND_DOLLAR, ModItems.DULSE_SEAWEED,
                ModItems.SEA_SPONGE, ModItems.SLUDGE, ModItems.STARFISH,
                ModItems.SUPERNATURAL_GOO, ModItems.SWAMP_CRAB, ModItems.SWAMP_ESSENCE,
                ModItems.VOID_PEBBLE, ModItems.VOID_SHARD, ModItems.VOID_SOUL,
                ModItems.SHARK_TOOTH, ModItems.SWIRL_STONE,
                ModItems.RED_BANEBERRY);
            // Non-quality forage
            output.accept(ModItems.LUCKY_FOUR_LEAF_CLOVER.get());
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FISH =
        tab("fish", () -> new ItemStack(ModItems.SHARK.get()), output -> {
            quality(output, ModItems.ALLIGATOR, ModItems.ARROWHEAD_SHARK, ModItems.BARRED_KNIFEJAW,
                ModItems.BLUE_TANG, ModItems.BONEFISH, ModItems.BULL_TROUT, ModItems.BUTTERFISH,
                ModItems.CLOWNFISH, ModItems.DAGGERFISH, ModItems.DIAMOND_CARP, ModItems.FIBER_GOBY,
                ModItems.FROG, ModItems.GAR, ModItems.GEMFISH, ModItems.GOLDFISH, ModItems.GOLDENFISH,
                ModItems.GRASS_CARP, ModItems.HIGHLANDS_BASS, ModItems.KING_SALMON, ModItems.KITTYFISH,
                ModItems.METEOR_CARP, ModItems.MINNOW, ModItems.OCEAN_SUNFISH, ModItems.PUPPYFISH,
                ModItems.RADIOACTIVE_BASS, ModItems.RAZOR_TROUT, ModItems.SEAHORSE, ModItems.SHARK,
                ModItems.SNATCHER_WORM, ModItems.TADPOLE, ModItems.TORPEDO_TROUT, ModItems.TURRETFISH,
                ModItems.UNDEADFISH, ModItems.VIPER_EEL, ModItems.VOID_EEL, ModItems.WATER_GRUB,
                ModItems.WOLF_SNAPPER, ModItems.BABY_LUNALOO, ModItems.LUNALOO, ModItems.SHINY_LUNALOO);
            // Smoked fish (4 quality variants each)
            for (var holder : ModItems.SMOKED_FISH) {
                for (int q : new int[]{QualityHelper.NORMAL, QualityHelper.SILVER, QualityHelper.GOLD, QualityHelper.IRIDIUM}) {
                    output.accept(QualityHelper.createWithQuality(new ItemStack(holder.get()), q));
                }
            }
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD_ARTISAN =
        tab("food_artisan", () -> new ItemStack(ModItems.GRAMPLETON_ORANGE_CHICKEN.get()), output -> {
            // Cooking dishes
            output.accept(ModItems.BAKED_BERRY_OATMEAL_SUPREME.get());
            output.accept(ModItems.BAKED_POTATO.get());
            output.accept(ModItems.BIG_BARK_BURGER.get());
            output.accept(ModItems.CANDY.get());
            output.accept(ModItems.CHEESE_CHARCUTERIE.get());
            output.accept(ModItems.CHOCOLATE_TRUFFLE_BAR.get());
            output.accept(ModItems.FISH_DUMPLING.get());
            output.accept(ModItems.FLOWER_COOKIE.get());
            output.accept(ModItems.FROG_LEGS.get());
            output.accept(ModItems.GINGERBREAD_MAN.get());
            output.accept(ModItems.GLAZED_BUTTERFISH.get());
            output.accept(ModItems.GLAZED_PEAR.get());
            output.accept(ModItems.GRAMPLETON_ORANGE_CHICKEN.get());
            output.accept(ModItems.GRILLED_CHEESE_SANDWICH.get());
            output.accept(ModItems.ICE_CREAM_SUNDAE.get());
            output.accept(ModItems.MIXED_BERRY_PIE.get());
            output.accept(ModItems.MUSHROOM_BERRY_RICE.get());
            output.accept(ModItems.NECTARINE_FRUIT_BREAD.get());
            output.accept(ModItems.PINEAPPLE_CUSTARD_CREPE.get());
            output.accept(ModItems.RAMEN.get());
            output.accept(ModItems.SEAWEED_SALAD.get());
            output.accept(ModItems.SEED_COOKIE.get());
            output.accept(ModItems.STUFFED_PERSIMMON.get());
            output.accept(ModItems.VEGAN_CONE.get());
            output.accept(ModItems.VOID_DELIGHT.get());
            output.accept(ModItems.VOID_SALMON_SUSHI.get());
            output.accept(ModItems.VOID_MAYO_SANDWICH.get());
            // Artisan goods
            output.accept(ModItems.AGED_BLUE_MOON_WINE.get());
            output.accept(ModItems.BLUE_MOON_WINE.get());
            output.accept(ModItems.BUTTER.get());
            output.accept(ModItems.GOOSE_MAYONNAISE.get());
            output.accept(ModItems.HONEY_JAR.get());
            quality(output, ModItems.YARN);
            // Machines
            output.accept(ModItems.BUTTER_CHURNER.get());
            output.accept(ModItems.YARN_SPOOLER.get());
            // Artisan display items
            for (var holder : List.of(
                ModItems.JOJA_BERRY_JELLY, ModItems.MONSTER_FRUIT_JELLY,
                ModItems.CUCUMBER_PICKLES, ModItems.BUTTERNUT_SQUASH_PICKLES,
                ModItems.GOLD_CARROT_PICKLES, ModItems.SWEET_POTATO_PICKLES,
                ModItems.JOJA_VEGGIE_PICKLES,
                ModItems.JOJA_BERRY_DRIED_FRUIT, ModItems.MONSTER_FRUIT_DRIED_FRUIT,
                ModItems.GREEN_MUSHROOM_DRIED_MUSHROOMS,
                ModItems.MEGA_PURPLE_MUSHROOM_DRIED_MUSHROOMS,
                ModItems.MONSTER_MUSHROOM_DRIED_MUSHROOMS,
                ModItems.MUSHROOM_COLONY_DRIED_MUSHROOMS,
                ModItems.POISON_MUSHROOM_DRIED_MUSHROOMS,
                ModItems.SVE_ROE, ModItems.SVE_AGED_ROE
            )) {
                output.accept(new ItemStack(holder.get()));
            }
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RESOURCES_ELIXIRS =
        tab("resources_elixirs", () -> new ItemStack(ModItems.GALDORAN_GEM.get()), output -> {
            // Artifacts
            output.accept(ModItems.AMBER.get());
            output.accept(ModItems.FOSSILIZED_APPLE.get());
            output.accept(ModItems.GALDORAN_GEM.get());
            output.accept(ModItems.MAGIC_LAMP.get());
            output.accept(ModItems.MONEY_BAG.get());
            output.accept(ModItems.ORNATE_TREASURE_CHEST.get());
            output.accept(ModItems.BOOMERANG.get());
            output.accept(ModItems.FADED_BUTTON.get());
            output.accept(ModItems.OLD_COIN.get());
            output.accept(ModItems.RUSTY_SHIELD.get());
            output.accept(ModItems.STONE_OF_YOBA.get());
            output.accept(ModItems.GOLD_SLIME_EGG.get());
            output.accept(ModItems.BLUE_SLIME_EGG.get());
            output.accept(ModItems.GREEN_SLIME_EGG.get());
            output.accept(ModItems.RED_SLIME_EGG.get());
            output.accept(ModItems.PURPLE_SLIME_EGG.get());
            output.accept(ModItems.FIREWORKS_RED.get());
            output.accept(ModItems.FIREWORKS_PURPLE.get());
            output.accept(ModItems.FIREWORKS_GREEN.get());
            output.accept(ModItems.RUSTY_BLADE.get());
            // Animal products
            quality(output, ModItems.CAMEL_WOOL,
                ModItems.GOLDEN_GOOSE_EGG, ModItems.GOOSE_EGG);
            output.accept(ModItems.GOOSE_SPAWN_EGG.get());
            output.accept(ModItems.CAMEL_SPAWN_EGG.get());
            // Elixirs & drinks
            output.accept(ModItems.AEGIS_ELIXIR.get());
            output.accept(ModItems.ARMOR_ELIXIR.get());
            output.accept(ModItems.BARBARIAN_ELIXIR.get());
            output.accept(ModItems.BOMBARDIER_ELIXIR.get());
            output.accept(ModItems.GRAVITY_ELIXIR.get());
            output.accept(ModItems.HASTE_ELIXIR.get());
            output.accept(ModItems.HERO_ELIXIR.get());
            output.accept(ModItems.LIGHTNING_ELIXIR.get());
            output.accept(ModItems.MARSH_TONIC.get());
            output.accept(ModItems.SPORTS_DRINK.get());
            output.accept(ModItems.STAMINA_CAPSULE.get());
            output.accept(ModItems.SUPER_JOJA_COLA.get());
            output.accept(ModItems.SUPER_STARFRUIT.get());
            output.accept(ModItems.BIRCH_WATER.get());
            output.accept(ModItems.FIR_WAX.get());
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMBAT_SPECIAL =
        tab("combat_special", () -> new ItemStack(ModItems.TEMPERED_GALAXY_SWORD.get()), output -> {
            // Weapons
            output.accept(ModItems.DIAMOND_WAND.get());
            output.accept(ModItems.HEAVY_SHIELD.get());
            output.accept(ModItems.MONSTER_SPLITTER.get());
            output.accept(ModItems.TEMPERED_GALAXY_DAGGER.get());
            output.accept(ModItems.TEMPERED_GALAXY_HAMMER.get());
            output.accept(ModItems.TEMPERED_GALAXY_SWORD.get());
            // Trinkets
            output.accept(ModItems.GOLDEN_KEY.get());
            output.accept(ModItems.MERMAID_BRACELET.get());
            output.accept(ModItems.PRISMATIC_POP.get());
            output.accept(ModItems.SUN_TOTEM.get());
            output.accept(ModItems.TREE_COIN.get());
            output.accept(ModItems.WIND_TOTEM.get());
            // Powers
            output.accept(ModItems.ANIMAL_MASTERY.get());
            output.accept(ModItems.BREWING_MASTERY.get());
            output.accept(ModItems.CHEESE_MASTERY.get());
            output.accept(ModItems.CRAFTING_MASTERY.get());
            output.accept(ModItems.GRAPE_MASTERY.get());
            output.accept(ModItems.STARFRUIT_MASTERY.get());
            output.accept(ModItems.STRAWBERRY_MASTERY.get());
            output.accept(ModItems.WARP_MAGIC.get());
            // Deeds & fences
            output.accept(ModItems.AURORA_VINEYARD_PROPERTY_DEED.get());
            output.accept(ModItems.PROPERTY_DEED.get());
            output.accept(ModItems.HEDGE_FENCE.get());
            output.accept(ModItems.SMALL_HARDWOOD_FENCE.get());
        });

    // ===== Helpers =====

    @SafeVarargs
    private static void quality(CreativeModeTab.Output output, DeferredHolder<? extends Item, ?>... items) {
        for (var item : items) {
            for (int q : new int[]{QualityHelper.NORMAL, QualityHelper.SILVER, QualityHelper.GOLD, QualityHelper.IRIDIUM}) {
                output.accept(QualityHelper.createWithQuality(new ItemStack(item.get()), q));
            }
        }
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(
            String name, Supplier<ItemStack> icon, TabFiller filler) {
        return TABS.register(name, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.stardewcraftsve." + name))
            .icon(icon)
            .displayItems((params, output) -> filler.fill(output))
            .build());
    }

    private ModCreativeTabs() {}
}
