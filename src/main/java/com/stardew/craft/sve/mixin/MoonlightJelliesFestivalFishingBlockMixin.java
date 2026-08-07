package com.stardew.craft.sve.mixin;

import com.stardew.craft.festival.ActiveFestivalHandlers;
import com.stardew.craft.festival.MoonlightJelliesFestivalService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows the SVE moonlight-jellies fishing pool during the active festival. */
@Mixin(value = ActiveFestivalHandlers.class, remap = false)
public abstract class MoonlightJelliesFestivalFishingBlockMixin {
    @Inject(
            method = "blocksFishingDuringActiveFestival(Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$allowMoonlightJelliesFishing(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ() && MoonlightJelliesFestivalService.isParticipant(player)) {
            cir.setReturnValue(false);
        }
    }
}
