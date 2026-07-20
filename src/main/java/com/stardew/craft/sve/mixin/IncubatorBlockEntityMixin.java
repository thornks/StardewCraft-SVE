package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.blockentity.IncubatorBlockEntity;
import com.stardew.craft.sve.animal.SveAnimalCompatibility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IncubatorBlockEntity.class, remap = false)
public abstract class IncubatorBlockEntityMixin {
    @Inject(method = "getContainingAnimalBuilding", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$preferContainingCoopForGooseEgg(
            ServerLevel level,
            CallbackInfoReturnable<AnimalBuildingRecord> cir
    ) {
        IncubatorBlockEntity incubator = (IncubatorBlockEntity) (Object) this;
        if (SveAnimalCompatibility.animalTypeForIncubatorInput(incubator.getInput()) != null) {
            AnimalBuildingRecord building = SveAnimalCompatibility.findContainingIncubatorBuilding(
                    level, incubator.getBlockPos(), incubator.getInput());
            cir.setReturnValue(building);
        }
    }

    @Inject(method = "resolveAnimalTypeId", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveGooseEgg(
            ItemStack stack,
            CallbackInfoReturnable<String> cir
    ) {
        String animalTypeId = SveAnimalCompatibility.animalTypeForIncubatorInput(stack);
        if (animalTypeId != null) cir.setReturnValue(animalTypeId);
    }
}
