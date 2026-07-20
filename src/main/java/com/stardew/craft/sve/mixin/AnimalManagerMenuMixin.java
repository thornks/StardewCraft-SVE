package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.menu.BarnManagerMenu;
import com.stardew.craft.menu.CoopManagerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {CoopManagerMenu.class, BarnManagerMenu.class}, remap = false)
public abstract class AnimalManagerMenuMixin {
    @Shadow(remap = false) @Final private Player player;
    @Shadow(remap = false) @Final private BlockPos managerPos;
    @Shadow(remap = false) private int boundAnimalCount;

    @Inject(method = "refreshState", at = @At("RETURN"), require = 1)
    private void stardewcraftsve$refreshRecordBackedAnimalCount(CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }

        String family = (Object) this instanceof CoopManagerMenu ? "coop" : "barn";
        AnimalWorldData worldData = AnimalWorldData.get(level);
        AnimalBuildingRecord building = worldData.findBuildingByManagerAnyOwner(
                level.dimension().location().toString(),
                family,
                managerPos
        ).orElse(null);
        if (building == null) {
            return;
        }

        long recordCount = worldData.getAnimals().stream()
                .filter(record -> building.buildingId().equals(record.buildingId()))
                .count();
        boundAnimalCount = Math.max(building.memberAnimalIds().size(), (int) recordCount);
    }
}
