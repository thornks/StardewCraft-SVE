package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.tree.WildTrees;
import com.stardew.craft.tree.prefab.PrefabTreeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Adds SVE tree species to the one-time debris pass used for newly created farms. */
public final class SveFarmWildTreeGeneration {
    private static final float SVE_TREE_REPLACEMENT_CHANCE = 0.20F;

    public static boolean placeMatureTree(ServerLevel level, BlockPos root, WildTrees.Def originalType) {
        if (isReplaceableBaseTree(originalType)
                && level.random.nextFloat() < SVE_TREE_REPLACEMENT_CHANCE) {
            SveWildTreeType sveType = randomType(level);
            BlockState matureTree = sveType.matureBlock().defaultBlockState();
            if (SveWildTreePlanting.isPlantableGround(level.getBlockState(root.below()))
                    && level.getBlockState(root).canBeReplaced()
                    && SveWildTreeBlock.canMature(level, root, sveType)
                    && level.setBlock(root, matureTree, Block.UPDATE_ALL)) {
                return true;
            }
        }
        return PrefabTreeManager.tryPlaceRandomVariant(level, root, originalType);
    }

    public static boolean placeDebrisBlock(ServerLevel level, BlockPos pos, BlockState originalState, int flags) {
        SaplingStage originalSapling = baseTreeSaplingStage(originalState);
        if (originalSapling != null && level.random.nextFloat() < SVE_TREE_REPLACEMENT_CHANCE) {
            SveWildTreeType sveType = randomType(level);
            BlockState sveSapling = sveType.saplingBlock().defaultBlockState()
                    .setValue(SveWildTreeSaplingBlock.STAGE, originalSapling.sveStage);
            if (sveSapling.canSurvive(level, pos)) {
                return level.setBlock(pos, sveSapling, flags);
            }
        }
        return level.setBlock(pos, originalState, flags);
    }

    private static SveWildTreeType randomType(ServerLevel level) {
        return level.random.nextBoolean() ? SveWildTreeType.FIR : SveWildTreeType.BIRCH;
    }

    private static boolean isReplaceableBaseTree(WildTrees.Def type) {
        return type == WildTrees.OAK || type == WildTrees.MAPLE || type == WildTrees.PINE;
    }

    private static SaplingStage baseTreeSaplingStage(BlockState state) {
        Block block = state.getBlock();
        for (WildTrees.Def type : new WildTrees.Def[]{WildTrees.OAK, WildTrees.MAPLE, WildTrees.PINE}) {
            if (block == type.sapling0().get()) return SaplingStage.EARLY;
            if (block == type.sapling1().get()) return SaplingStage.LATE;
        }
        return null;
    }

    private enum SaplingStage {
        EARLY(0),
        LATE(2);

        private final int sveStage;

        SaplingStage(int sveStage) {
            this.sveStage = sveStage;
        }
    }

    private SveFarmWildTreeGeneration() {
    }
}
