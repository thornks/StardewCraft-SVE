package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.tree.WildTrees;
import net.minecraft.world.level.block.Blocks;

public final class SveWildTreeCompat {
    public static final WildTrees.Def FIR_DEF = create(SveWildTreeType.FIR);
    public static final WildTrees.Def BIRCH_DEF = create(SveWildTreeType.BIRCH);

    public static WildTrees.Def def(SveWildTreeType type) {
        return type == SveWildTreeType.BIRCH ? BIRCH_DEF : FIR_DEF;
    }

    public static WildTrees.Def byId(String id) {
        return "birch".equals(id) ? BIRCH_DEF : "fir".equals(id) ? FIR_DEF : null;
    }

    private static WildTrees.Def create(SveWildTreeType type) {
        return new WildTrees.Def(
                type.id(), type::matureBlock, type::matureBlock,
                () -> Blocks.AIR, () -> Blocks.AIR, () -> Blocks.AIR,
                type::saplingBlock, type::saplingBlock,
                () -> Blocks.AIR, () -> Blocks.AIR, () -> Blocks.AIR, () -> Blocks.AIR,
                SveWildTreeType.GROWTH_CHANCE,
                SveWildTreeType.FERTILIZED_GROWTH_CHANCE,
                SveWildTreeType.SEED_SPREAD_CHANCE,
                SveWildTreeType.SEED_ON_SHAKE_CHANCE,
                SveWildTreeType.SEED_ON_CHOP_CHANCE,
                false);
    }

    private SveWildTreeCompat() {
    }
}
