package com.stardew.craft.sve.mixin;

import com.mojang.logging.LogUtils;
import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.AnimalAcquisitionSource;
import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.animal.service.AnimalAcquireService;
import com.stardew.craft.animal.service.AnimalEntitySyncService;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;

@Mixin(value = AnimalAcquireService.class, remap = false)
public abstract class AnimalAcquireServiceMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

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
        if (!SveAnimalRules.GOOSE_ID.equals(animalTypeId)) {
            return;
        }

        AnimalWorldData worldData = AnimalWorldData.get(level);
        AnimalBuildingRecord building = worldData.getBuilding(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown animal building: " + buildingId));
        if (!"coop".equalsIgnoreCase(building.buildingType().family())) {
            throw new IllegalStateException("Goose incubation requires a coop: " + buildingId);
        }
        if (!building.hasCapacity()) {
            throw new IllegalStateException("Animal building is full: " + buildingId);
        }

        String resolvedName = customName == null || customName.isBlank()
                ? SveAnimalRules.GOOSE_ID
                : customName;
        FarmAnimalRecord record = worldData.createAnimal(
                SveAnimalRules.GOOSE_ID,
                resolvedName,
                buildingId,
                AnimalAcquisitionSource.INCUBATION
        );

        // The saved record is authoritative. A temporary entity-spawn failure must not
        // leave the incubator permanently ready or create another animal every tick.
        try {
            AnimalEntitySyncService.ensurePresentNow(level, record);
        } catch (RuntimeException exception) {
            worldData.markChanged();
            LOGGER.warn(
                    "Created incubated SVE goose record {} but could not spawn its entity immediately; "
                            + "the managed-animal recovery pass will retry",
                    record.animalId(),
                    exception
            );
        }
        cir.setReturnValue(record);
    }

    @Inject(method = "validateBuilding", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$acceptSveAnimalBuilding(
            ServerLevel level,
            String animalTypeId,
            AnimalBuildingRecord building,
            CallbackInfo ci
    ) {
        String requiredFamily;
        if (SveAnimalRules.GOOSE_ID.equals(animalTypeId)) {
            requiredFamily = "coop";
        } else if (SveAnimalRules.CAMEL_ID.equals(animalTypeId)) {
            requiredFamily = "barn";
        } else {
            return;
        }

        String actualFamily = building.buildingType().family();
        if (!requiredFamily.equals(actualFamily)) {
            throw new IllegalStateException(
                    "SVE animal " + animalTypeId + " requires " + requiredFamily
                            + " but building " + building.buildingId() + " is " + actualFamily
            );
        }
        ci.cancel();
    }
}
