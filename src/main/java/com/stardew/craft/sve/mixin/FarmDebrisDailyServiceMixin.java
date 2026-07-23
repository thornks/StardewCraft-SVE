package com.stardew.craft.sve.mixin;

import com.stardew.craft.farm.FarmDebrisDailyService;
import com.stardew.craft.sve.tree.wild.SveFarmWildTreeGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Adds SVE species when the host's daily farm-debris pass chooses a tree sapling. */
@Mixin(FarmDebrisDailyService.class)
public abstract class FarmDebrisDailyServiceMixin {
    @Redirect(
            method = "spawnRandomDebris",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            require = 2,
            allow = 2
    )
    private static boolean stardewcraftsve$placeDailyFarmDebris(ServerLevel level, BlockPos pos,
                                                                 BlockState state, int flags) {
        return SveFarmWildTreeGeneration.placeDebrisBlock(level, pos, state, flags);
    }
}
