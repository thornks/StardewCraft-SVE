package com.stardew.craft.sve.tree;

import com.stardew.craft.api.v1.agriculture.StardewTreeData;
import com.stardew.craft.farming.SeasonLocationRules;
import com.stardew.craft.time.StardewTimeManager;
import com.stardew.craft.tree.fruit.FruitTreeRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public final class SveFruitTreeRules {
    private SveFruitTreeRules() {
    }

    public static int seasonOfAbsoluteDay(int day) {
        return Math.floorMod((day - 1) / 28, 4);
    }

    public static boolean canPlantSapling(LevelReader level, BlockPos lowerPos) {
        return FruitTreeRules.canPlantSapling(level, lowerPos)
                && !isTooCloseToSveTree(level, lowerPos);
    }

    public static boolean isGrowthBlocked(LevelReader level, BlockPos lowerPos) {
        return FruitTreeRules.isGrowthBlocked(level, lowerPos);
    }

    public static boolean canFruitToday(Level level, BlockPos pos, SveFruitTreeType type) {
        return canFruitToday(level, pos, type, null);
    }

    public static boolean canFruitToday(Level level, BlockPos pos, SveFruitTreeType type,
                                        StardewTreeData data) {
        return canFruitOnSeason(level, pos, type, data, StardewTimeManager.get().getCurrentSeason());
    }

    public static boolean canFruitOnSeason(Level level, BlockPos pos, SveFruitTreeType type,
                                           StardewTreeData data, int season) {
        if (level.isClientSide()) {
            return true;
        }
        if (SeasonLocationRules.seedsIgnoreSeasonsHere(level, pos)) {
            return true;
        }
        if (data == null || data.fruitSeasons().isEmpty()) {
            return season == type.fruitSeason();
        }
        String currentSeason = switch (season) {
            case 0 -> "spring";
            case 1 -> "summer";
            case 2 -> "fall";
            case 3 -> "winter";
            default -> "";
        };
        return data.fruitSeasons().stream().anyMatch(currentSeason::equalsIgnoreCase);
    }

    private static boolean isTooCloseToSveTree(LevelReader level, BlockPos lowerPos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos check = lowerPos.offset(dx, 0, dz);
                if (isSveTreeAt(level.getBlockState(check))
                        || isSveTreeAt(level.getBlockState(check.above()))
                        || isSveTreeAt(level.getBlockState(check.below()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSveTreeAt(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof SveFruitTreeBlock
                || state.getBlock() instanceof SveFruitTreeSaplingBlock;
    }
}
