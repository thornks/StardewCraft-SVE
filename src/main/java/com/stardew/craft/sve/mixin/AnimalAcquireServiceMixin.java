package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.animal.service.AnimalAcquireService;
import com.stardew.craft.sve.animal.SveAnimalCompatibility;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AnimalAcquireService.class, remap = false)
public abstract class AnimalAcquireServiceMixin {
    @Inject(
            method = "incubation(Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lcom/stardew/craft/animal/model/FarmAnimalRecord;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$incubateGooseWithoutLosingTheHatch(
            ServerLevel level,
            String animalTypeId,
            String customName,
            String buildingId,
            CallbackInfoReturnable<FarmAnimalRecord> cir
    ) {
        FarmAnimalRecord record = SveAnimalCompatibility.incubate(
                level, animalTypeId, customName, buildingId);
        if (record != null) cir.setReturnValue(record);
    }

    @Inject(method = "validateBuilding", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$acceptSveAnimalBuilding(
            ServerLevel level,
            String animalTypeId,
            AnimalBuildingRecord building,
            CallbackInfo ci
    ) {
        if (SveAnimalCompatibility.validateBuilding(animalTypeId, building)) ci.cancel();
    }
}
