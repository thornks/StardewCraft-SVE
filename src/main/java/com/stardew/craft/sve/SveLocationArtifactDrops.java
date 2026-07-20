package com.stardew.craft.sve;

import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.desert.DesertConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Rolls SVE artifacts whose dig-spot chances depend on the exact world position. */
public final class SveLocationArtifactDrops {
    private static final ResourceLocation FOREST_POND =
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "forest_pond");

    private static final List<DropRule> SECRET_WOODS_DROPS = List.of(
            drop(ModItems.SALAL_BERRY_SEED, 0.05D),
            drop(ModItems.ANCIENT_FERNS_SEED, 0.07D));
    private static final List<DropRule> COMMUNITY_CENTER_DROPS = List.of(
            drop(ModItems.FADED_BUTTON, 0.10D),
            drop(ModItems.OLD_COIN, 0.10D));
    private static final List<DropRule> BUS_STOP_DROPS = List.of(
            drop(ModItems.OLD_COIN, 0.10D));
    private static final List<DropRule> FOREST_POND_DROPS = List.of(
            drop(ModItems.FOSSILIZED_APPLE, 0.03D));
    private static final List<DropRule> DESERT_DROPS = List.of(
            drop(ModItems.STONE_OF_YOBA, 0.05D));
    private static final List<DropRule> RAILROAD_DROPS = List.of(
            drop(ModItems.STONE_OF_YOBA, 0.05D),
            drop(ModItems.AMBER, 0.07D),
            drop(ModItems.BOOMERANG, 0.05D));
    private static final List<DropRule> BACKWOODS_DROPS = List.of(
            drop(ModItems.RUSTY_SHIELD, 0.05D),
            drop(ModItems.AMBER, 0.04D),
            drop(ModItems.BOOMERANG, 0.05D));
    private static final List<DropRule> MOUNTAIN_DROPS = List.of(
            drop(ModItems.RUSTY_SHIELD, 0.04D),
            drop(ModItems.AMBER, 0.10D),
            drop(ModItems.BOOMERANG, 0.05D));
    private static final List<DropRule> FOREST_DROPS = List.of(
            drop(ModItems.RUSTY_SHIELD, 0.03D),
            drop(ModItems.AMBER, 0.06D),
            drop(ModItems.BOOMERANG, 0.05D));

    private SveLocationArtifactDrops() {}

    public static ItemStack tryRoll(ServerLevel level, BlockPos pos) {
        if (level.dimension() != ModDimensions.STARDEW_VALLEY) {
            return ItemStack.EMPTY;
        }

        List<DropRule> drops = resolveDrops(level, pos);
        return drops.isEmpty() ? ItemStack.EMPTY : roll(drops, level.getRandom());
    }

    private static List<DropRule> resolveDrops(ServerLevel level, BlockPos pos) {
        if (SveWorldRegions.SECRET_WOODS.containsXZ(pos)) {
            return SECRET_WOODS_DROPS;
        }
        if (SveWorldRegions.COMMUNITY_CENTER.contains(pos)) {
            return COMMUNITY_CENTER_DROPS;
        }
        if (SveWorldRegions.BUS_STOP.contains(pos)) {
            return BUS_STOP_DROPS;
        }
        if (isForestPond(level, pos)) {
            return FOREST_POND_DROPS;
        }
        if (DesertConstants.isInDesertRegion(pos)) {
            return DESERT_DROPS;
        }
        if (SveWorldRegions.RAILROAD.contains(pos)) {
            return RAILROAD_DROPS;
        }
        if (SveWorldRegions.BACKWOODS.containsXZ(pos)) {
            return BACKWOODS_DROPS;
        }
        if (SveWorldRegions.isMountain(pos)) {
            return MOUNTAIN_DROPS;
        }
        if (SveWorldRegions.FOREST.containsXZ(pos)) {
            return FOREST_DROPS;
        }
        return List.of();
    }

    private static ItemStack roll(List<DropRule> drops, RandomSource random) {
        for (DropRule drop : drops) {
            if (random.nextDouble() < drop.chance()) {
                return new ItemStack(drop.item().get());
            }
        }
        return ItemStack.EMPTY;
    }

    private static DropRule drop(Supplier<? extends Item> item, double chance) {
        return new DropRule(item, chance);
    }

    private static boolean isForestPond(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .map(key -> FOREST_POND.equals(key.location()))
                .orElse(false);
    }

    private record DropRule(Supplier<? extends Item> item, double chance) {
        private DropRule {
            item = Objects.requireNonNull(item, "item");
            if (chance <= 0.0D || chance > 1.0D) {
                throw new IllegalArgumentException("Artifact drop chance must be in (0, 1]");
            }
        }
    }
}
