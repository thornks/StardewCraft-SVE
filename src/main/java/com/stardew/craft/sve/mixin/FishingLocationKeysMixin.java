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
import java.util.Set;

@Mixin(value = FishingDataManager.class, remap = false)
public abstract class FishingLocationKeysMixin {
    private static final String SVE_FISHING_POOL = "stardewcraftsve:sve_fish";
    private static final Set<String> SUPPORTED_WATER_TAGS = Set.of(
            "stardewcraft:is_witch_swamp",
            "stardewcraft:is_beach",
            "stardewcraft:is_forest_river",
            "stardewcraft:is_freshwater",
            "stardewcraft:is_ginger_island_ocean",
            "stardewcraft:is_mountain_lake",
            "stardewcraft:is_mutant_bug_lair",
            "stardewcraft:is_secret_woods",
            "stardewcraft:is_sewers",
            "stardewcraft:is_town_river"
    );

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
        if (resolved.contains(SVE_FISHING_POOL)
                || !FishingDataManager.isStardewFishingDimensionPublic(level)
                || SUPPORTED_WATER_TAGS.stream()
                        .noneMatch(tag -> FishingDataManager.hasBiomeTagPublic(biome, tag))) {
            return;
        }

        List<String> withSvePool = new ArrayList<>(resolved.size() + 1);
        withSvePool.addAll(resolved);
        withSvePool.add(SVE_FISHING_POOL);
        cir.setReturnValue(withSvePool);
    }
}
