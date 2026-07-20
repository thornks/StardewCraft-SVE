package com.stardew.craft.sve.mixin;

import com.stardew.craft.manager.ArtifactDropService;
import com.stardew.craft.sve.SveLocationArtifactDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ArtifactDropService.class)
public abstract class ArtifactDropServiceMixin {
    @Inject(
            method = "rollDrops(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$rollLocationArtifact(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        ItemStack drop = SveLocationArtifactDrops.tryRoll(level, pos);
        if (!drop.isEmpty()) {
            cir.setReturnValue(List.of(drop));
        }
    }
}
