package com.stardew.craft.sve.mixin;

import com.stardew.craft.api.v1.agriculture.StardewAnimalData;
import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.manager.AnimalGrowthManager;
import com.stardew.craft.sve.animal.SveAnimalData;
import com.stardew.craft.sve.animal.SveAnimalProduction;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimalGrowthManager.class)
public abstract class AnimalGrowthManagerMixin {
    @Inject(
            method = "applyDayUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/stardew/craft/animal/model/FarmAnimalRecord;incrementDaysSinceLastProduce()V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void stardewcraftsve$produceSveAnimalItems(
            ServerLevel level,
            AnimalWorldData worldData,
            FarmAnimalRecord record,
            int absoluteDaysPlayed,
            boolean offlineCatchUp,
            CallbackInfoReturnable<Boolean> cir
    ) {
        SveAnimalProduction.beforeDailyUpdate(level, worldData, record, absoluteDaysPlayed, offlineCatchUp);
    }

    @Redirect(
            method = "applyDayUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/stardew/craft/api/v1/agriculture/StardewAgricultureDataApi;animal(Lnet/minecraft/world/entity/Entity;)Lcom/stardew/craft/api/v1/agriculture/StardewAnimalData;"
            ),
            require = 1
    )
    private StardewAnimalData stardewcraftsve$suppressGenericProduction(
            Entity entity
    ) {
        StardewAnimalData resolved = StardewAgricultureDataApi.animal(entity);
        if (entity instanceof BaseCoopAnimalEntity animal) {
            return SveAnimalData.suppressBaseProduction(animal.getManagedAnimalType(), resolved);
        }
        return resolved;
    }

    @Redirect(
            method = "tryReproduction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/stardew/craft/animal/model/FarmAnimalRecord;allowReproduction()Z"
            ),
            require = 1
    )
    private boolean stardewcraftsve$excludeCamelsFromReproduction(FarmAnimalRecord record) {
        return SveAnimalRules.canReproduce(record.animalTypeId()) && record.allowReproduction();
    }
}
