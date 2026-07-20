package com.stardew.craft.sve.mixin;

import com.stardew.craft.farm.FarmInstanceInitializer;
import com.stardew.craft.sve.tree.wild.SveFarmWildTreeGeneration;
import com.stardew.craft.tree.WildTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmInstanceInitializer.class)
public abstract class FarmInstanceInitializerMixin {
    @Redirect(
            method = "spawnNaturalDebris",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/stardew/craft/tree/prefab/PrefabTreeManager;tryPlaceRandomVariant(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lcom/stardew/craft/tree/WildTrees$Def;)Z"
            ),
            require = 1
    )
    private static boolean stardewcraftsve$placeFarmTree(ServerLevel level, BlockPos pos, WildTrees.Def type) {
        return SveFarmWildTreeGeneration.placeMatureTree(level, pos, type);
    }

    @Redirect(
            method = "spawnNaturalDebris",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            require = 1
    )
    private static boolean stardewcraftsve$placeFarmDebris(ServerLevel level, BlockPos pos,
                                                            BlockState state, int flags) {
        return SveFarmWildTreeGeneration.placeDebrisBlock(level, pos, state, flags);
    }
}
