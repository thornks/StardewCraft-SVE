package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.menu.AnimalQueryMenu;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.server.level.ServerLevel;
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

        String animalTypeId;
        if ((animal != null && SveAnimalRules.GOOSE_ID.equals(animal.animalTypeId()))
                || variantIndex == SveAnimalRules.GOOSE_VARIANT_INDEX) {
            animalTypeId = SveAnimalRules.GOOSE_ID;
        } else if ((animal != null && SveAnimalRules.CAMEL_ID.equals(animal.animalTypeId()))
                || variantIndex == SveAnimalRules.CAMEL_VARIANT_INDEX) {
            animalTypeId = SveAnimalRules.CAMEL_ID;
        } else {
            return;
        }
        cir.setReturnValue(SveAnimalRules.sellPrice(animalTypeId, friendship));
    }

    @Inject(method = "canToggleReproduction", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$disableCamelReproductionToggle(CallbackInfoReturnable<Boolean> cir) {
        if (stardewcraftsve$isCamel()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleToggleReproduction", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$rejectCamelReproductionChange(boolean enabled, CallbackInfo ci) {
        FarmAnimalRecord animal = stardewcraftsve$animalRecord();
        if ((animal != null && SveAnimalRules.CAMEL_ID.equals(animal.animalTypeId()))
                || variantIndex == SveAnimalRules.CAMEL_VARIANT_INDEX) {
            if (animal != null && animal.allowReproduction()
                    && player.level() instanceof ServerLevel serverLevel) {
                AnimalWorldData.get(serverLevel).setAllowReproduction(animalId, false);
            }
            ci.cancel();
        }
    }

    private boolean stardewcraftsve$isCamel() {
        FarmAnimalRecord animal = stardewcraftsve$animalRecord();
        return (animal != null && SveAnimalRules.CAMEL_ID.equals(animal.animalTypeId()))
                || variantIndex == SveAnimalRules.CAMEL_VARIANT_INDEX;
    }

    private FarmAnimalRecord stardewcraftsve$animalRecord() {
        return player.level() instanceof ServerLevel serverLevel
                ? AnimalWorldData.get(serverLevel).getAnimal(animalId).orElse(null)
                : null;
    }
}
