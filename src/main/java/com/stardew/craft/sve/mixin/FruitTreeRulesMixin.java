package com.stardew.craft.sve.mixin;

import com.stardew.craft.sve.tree.SveFruitTreeBlock;
import com.stardew.craft.sve.tree.SveFruitTreeSaplingBlock;
import com.stardew.craft.tree.fruit.FruitTreeRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FruitTreeRules.class)
public abstract class FruitTreeRulesMixin {
    @Inject(method = "isTreeAt", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$recognizeSveFruitTrees(
            LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SveFruitTreeBlock
                || state.getBlock() instanceof SveFruitTreeSaplingBlock) {
            callback.setReturnValue(true);
        }
    }
}
