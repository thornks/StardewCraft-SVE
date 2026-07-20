package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.service.AnimalEntitySyncService;
import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.sve.animal.SveAnimalCompatibility;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimalEntitySyncService.class)
public abstract class AnimalEntitySyncServiceMixin {
    @Inject(method = "resolveEntityType", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveEntityType(
            String animalTypeId,
            CallbackInfoReturnable<EntityType<? extends BaseCoopAnimalEntity>> cir
    ) {
        EntityType<? extends BaseCoopAnimalEntity> entityType =
                SveAnimalCompatibility.entityType(animalTypeId);
        if (entityType != null) cir.setReturnValue(entityType);
    }
}
