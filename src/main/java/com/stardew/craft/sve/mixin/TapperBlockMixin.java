package com.stardew.craft.sve.mixin;

import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.utility.TapperBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeCompat;
import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import com.stardew.craft.tree.WildTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TapperBlock.class)
public abstract class TapperBlockMixin {
    @Inject(method = "countTappersOnTree", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$countTappersOnWholeTrunk(
            LevelReader level, BlockPos root, WildTrees.Def def,
            CallbackInfoReturnable<Integer> callback) {
        if (!(level.getBlockState(root).getBlock() instanceof SveWildTreeBlock tree)
                || SveWildTreeCompat.byId(def.id()) == null
                || !tree.getType().id().equals(def.id())) {
            return;
        }

        SveWildTreeType type = tree.getType();
        int count = 0;
        for (int y = 0; y < type.trunkHeight(); y++) {
            BlockPos trunkPos = root.above(y);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(trunkPos.relative(direction)).is(ModBlocks.TAPPER.get())) {
                    count++;
                }
            }
        }
        callback.setReturnValue(count);
    }
}
