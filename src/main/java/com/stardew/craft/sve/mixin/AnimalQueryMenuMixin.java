package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.menu.AnimalQueryMenu;
import com.stardew.craft.sve.animal.SveAnimalCompatibility;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimalQueryMenu.class)
public abstract class AnimalQueryMenuMixin {
    @Shadow private Player player;
    @Shadow private long animalId;
    @Shadow private int friendship;
    @Shadow private int variantIndex;

    @Inject(method = "getBaseAnimalSellPrice", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$getAnimalSellPrice(CallbackInfoReturnable<Integer> cir) {
        FarmAnimalRecord animal = stardewcraftsve$animalRecord();
        String animalTypeId = SveAnimalCompatibility.menuAnimalType(animal, variantIndex);
        if (animalTypeId != null) {
            cir.setReturnValue(SveAnimalRules.sellPrice(animalTypeId, friendship));
        }
    }

    @Inject(method = "canToggleReproduction", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$disableCamelReproductionToggle(CallbackInfoReturnable<Boolean> cir) {
        if (SveAnimalCompatibility.hasReproductionLocked(
                stardewcraftsve$animalRecord(), variantIndex)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleToggleReproduction", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$rejectCamelReproductionChange(boolean enabled, CallbackInfo ci) {
        FarmAnimalRecord animal = stardewcraftsve$animalRecord();
        if (SveAnimalCompatibility.enforceReproductionLock(
                player, animalId, animal, variantIndex)) ci.cancel();
    }

    private FarmAnimalRecord stardewcraftsve$animalRecord() {
        return SveAnimalCompatibility.findRecord(player, animalId);
    }
}
