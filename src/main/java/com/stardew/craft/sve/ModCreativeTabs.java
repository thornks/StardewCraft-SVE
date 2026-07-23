package com.stardew.craft.sve;

import com.stardew.craft.item.quality.QualityHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
            for (String path : SveItemCatalog.CREATIVE_CROPS) {
                if (path.equals("dewdrop_berry")) {
                    accept(output, path);
                } else {
                    quality(output, path);
                }
            }
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEEDS =
        tab("seeds", () -> new ItemStack(ModItems.GOLD_CARROT_SEED.get()), output -> {
            SveItemCatalog.CREATIVE_SEEDS.forEach(path -> accept(output, path));
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FORAGE =
        tab("forage", () -> new ItemStack(ModItems.GOLDEN_OCEAN_FLOWER.get()), output -> {
            quality(output, ModItems.GREEN_MUSHROOM, ModItems.MEGA_PURPLE_MUSHROOM,
                ModItems.MUSHROOM_COLONY, ModItems.POISON_MUSHROOM,
                ModItems.DIAMOND_FLOWER, ModItems.FERNGILL_PRIMROSE, ModItems.GOLDENROD,
                ModItems.GOLDEN_OCEAN_FLOWER, ModItems.SMELLY_RAFFLESIA, ModItems.SWAMP_FLOWER,
                ModItems.THISTLE, ModItems.WINTER_STAR_ROSE,
                ModItems.BIG_CONCH, ModItems.DRIED_SAND_DOLLAR, ModItems.DULSE_SEAWEED,
                ModItems.SHARK_TOOTH,
                ModItems.RED_BANEBERRY);
            // Non-quality forage
            output.accept(ModItems.LUCKY_FOUR_LEAF_CLOVER.get());
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FISH =
        tab("fish", () -> new ItemStack(ModItems.SHARK.get()), output -> {
            for (String path : SveFishData.SVE_FISH) {
                quality(output, registeredItem(path));
            }
            // Smoked fish (4 quality variants each)
            for (var holder : ModItems.SMOKED_FISH) {
                for (int q : new int[]{QualityHelper.NORMAL, QualityHelper.SILVER, QualityHelper.GOLD, QualityHelper.IRIDIUM}) {
                    output.accept(QualityHelper.createWithQuality(new ItemStack(holder.get()), q));
                }
            }
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD_ARTISAN =
        tab("food_artisan", () -> new ItemStack(ModItems.GRAMPLETON_ORANGE_CHICKEN.get()), output -> {
            SveItemCatalog.CREATIVE_COOKING.forEach(path -> accept(output, path));
            SveItemCatalog.CREATIVE_ARTISAN_NORMAL.forEach(path -> accept(output, path));
            SveItemCatalog.CREATIVE_ARTISAN_QUALITY.forEach(path -> quality(output, path));
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RESOURCES_ELIXIRS =
        tab("resources_elixirs", () -> new ItemStack(ModItems.GALDORAN_GEM.get()), output -> {
            // Artifacts
            SveItemCatalog.ARTIFACTS.forEach(path -> accept(output, path));
            output.accept(ModItems.GALDORAN_GEM.get());
            output.accept(ModItems.MAGIC_LAMP.get());
            output.accept(ModItems.MONEY_BAG.get());
            output.accept(ModItems.ORNATE_TREASURE_CHEST.get());
            output.accept(ModItems.SLUDGE.get());
            output.accept(ModItems.SUPERNATURAL_GOO.get());
            output.accept(ModItems.SWAMP_ESSENCE.get());
            output.accept(ModItems.VOID_PEBBLE.get());
            output.accept(ModItems.VOID_SHARD.get());
            output.accept(ModItems.VOID_SOUL.get());
            output.accept(ModItems.SWIRL_STONE.get());
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

    private static void quality(CreativeModeTab.Output output, Item item) {
        for (int q : new int[]{QualityHelper.NORMAL, QualityHelper.SILVER, QualityHelper.GOLD, QualityHelper.IRIDIUM}) {
            output.accept(QualityHelper.createWithQuality(new ItemStack(item), q));
        }
    }

    private static void quality(CreativeModeTab.Output output, String path) {
        quality(output, registeredItem(path));
    }

    private static void accept(CreativeModeTab.Output output, String path) {
        output.accept(registeredItem(path));
    }

    private static Item registeredItem(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalStateException("Missing catalog item " + id);
        }
        return BuiltInRegistries.ITEM.get(id);
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
