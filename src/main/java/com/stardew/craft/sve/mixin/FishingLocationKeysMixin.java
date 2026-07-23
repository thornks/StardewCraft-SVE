package com.stardew.craft.sve.mixin;

import com.stardew.craft.fishing.data.FishingDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = FishingDataManager.class, remap = false)
public abstract class FishingLocationKeysMixin {
    private static final String SVE_FISHING_POOL = "stardewcraftsve:sve_fish";
    private static final String MOONLIGHT_JELLIES_POOL = "stardewcraftsve:moonlight_jellies_festival";

    @Inject(
            method = "resolveVanillaAlignedLocationKeysStatic(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$includeFishingPool(
            ServerLevel level,
            Holder<Biome> biome,
            BlockPos pos,
            CallbackInfoReturnable<List<String>> cir
    ) {
        List<String> resolved = cir.getReturnValue();
        String dimensionId = level.dimension().location().toString();
        if (!dimensionId.equals("stardewcraft:stardew_valley")
                && !dimensionId.equals("stardewcraft:stardew_mining")) {
            return;
        }

        boolean addSvePool = !resolved.contains(SVE_FISHING_POOL);
        boolean addFestivalPool = !resolved.contains(MOONLIGHT_JELLIES_POOL);
        if (!addSvePool && !addFestivalPool) return;

        List<String> withSvePool = new ArrayList<>(resolved.size() + 2);
        withSvePool.addAll(resolved);
        if (addSvePool) withSvePool.add(SVE_FISHING_POOL);
        if (addFestivalPool) withSvePool.add(MOONLIGHT_JELLIES_POOL);
        cir.setReturnValue(withSvePool);
    }
}
