package com.stardew.craft.sve.mixin;

import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.entity.animal.CoopAnimalVariant;
import com.stardew.craft.sve.animal.SveAnimalRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseCoopAnimalEntity.class)
public abstract class BaseCoopAnimalEntityMixin {
    @Redirect(
            method = "lambda$openAnimalQueryMenu$3",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/stardew/craft/entity/animal/CoopAnimalVariant;ordinal()I"
            ),
            require = 1
    )
    private int stardewcraftsve$useAddonVariantIndex(CoopAnimalVariant variant) {
        String animalTypeId = ((BaseCoopAnimalEntity) (Object) this).getManagedAnimalType();
        if (SveAnimalRules.GOOSE_ID.equals(animalTypeId)) {
            return SveAnimalRules.GOOSE_VARIANT_INDEX;
        }
        if (SveAnimalRules.CAMEL_ID.equals(animalTypeId)) {
            return SveAnimalRules.CAMEL_VARIANT_INDEX;
        }
        return variant.ordinal();
    }
}
