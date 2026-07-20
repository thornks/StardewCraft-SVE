package com.stardew.craft.sve.mixin;

import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeCompat;
import com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import com.stardew.craft.tree.WildTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WildTrees.class)
public abstract class WildTreesMixin {
    @Inject(method = "findTapperSupportDef", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$recognizeTapperSupport(
            LevelReader level, BlockPos supportPos, CallbackInfoReturnable<WildTrees.Def> callback) {
        SveWildTreeType type = stardewcraftsve$getType(level.getBlockState(supportPos).getBlock());
        if (type != null) {
            callback.setReturnValue(SveWildTreeCompat.def(type));
        }
    }

    @Inject(method = "findTapperTreeRoot", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$findTapperRoot(
            LevelReader level, BlockPos supportPos, CallbackInfoReturnable<BlockPos> callback) {
        SveWildTreeType type = stardewcraftsve$getType(level.getBlockState(supportPos).getBlock());
        if (type != null) {
            BlockPos root = SveWildTreeBlock.findRoot(level, supportPos, type);
            if (root != null) callback.setReturnValue(root);
        }
    }

    private static SveWildTreeType stardewcraftsve$getType(Block block) {
        if (block instanceof SveWildTreeBlock tree) return tree.getType();
        if (block instanceof SveWildTreeExtensionBlock extension) return extension.getType();
        return null;
    }
}
