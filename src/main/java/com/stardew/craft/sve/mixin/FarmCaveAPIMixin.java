package com.stardew.craft.sve.mixin;

import com.stardew.craft.farm.FarmCaveAPI;
import com.stardew.craft.sve.SveFarmCaveFruits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmCaveAPI.class)
public abstract class FarmCaveAPIMixin {
    @Inject(method = "clearCaveFruits", at = @At("TAIL"), require = 1)
    private static void stardewcraftsve$clearExtendedFruitPool(
            ServerLevel level, BlockPos caveOrigin, CallbackInfo callback) {
        SveFarmCaveFruits.clear(level, caveOrigin);
    }
}
