package com.stardew.craft.sve.mixin;

import com.stardew.craft.festival.MoonlightJelliesFestivalService;
import com.stardew.craft.fishing.data.FishingDataManager;
import com.stardew.craft.fishing.data.SpawnFishRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the festival pool from being used before the player enters the event. */
@Mixin(value = FishingDataManager.class, remap = false)
public abstract class MoonlightJelliesFishingMixin {
    private static final String FESTIVAL_LOCATION =
            "stardewcraftsve:moonlight_jellies_festival";

    @Inject(
            method = "matchesVanillaCondition(Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/core/Holder;Lcom/stardew/craft/fishing/data/SpawnFishRule;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$requireMoonlightJelliesFestival(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos,
            Holder<Biome> biome,
            SpawnFishRule rule,
            boolean magicBait,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (rule.biomes().contains(FESTIVAL_LOCATION)
                && !MoonlightJelliesFestivalService.isParticipant(player)) {
            cir.setReturnValue(false);
        }
    }
}
