package com.stardew.craft.sve.mixin;

import com.stardew.craft.manager.FarmCaveDailyService;
import com.stardew.craft.sve.SveFarmCaveFruits;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FarmCaveDailyService.class)
public abstract class FarmCaveDailyServiceMixin {
    @Inject(method = "pickFruitBlock", at = @At("RETURN"), cancellable = true, require = 1)
    private static void stardewcraftsve$extendFruitTreePool(
            RandomSource random, CallbackInfoReturnable<Block> callback) {
        callback.setReturnValue(SveFarmCaveFruits.extendOrchardFruit(callback.getReturnValue(), random));
    }
}
