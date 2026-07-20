package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.model.AnimalTypeCatalog;
import com.stardew.craft.sve.animal.SveAnimalRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(AnimalTypeCatalog.class)
public abstract class AnimalTypeCatalogMixin {
    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveAnimalType(
            String animalTypeId,
            CallbackInfoReturnable<AnimalTypeCatalog.AnimalTypeSpec> cir
    ) {
        if (SveAnimalRules.GOOSE_ID.equalsIgnoreCase(animalTypeId)) {
            cir.setReturnValue(new AnimalTypeCatalog.AnimalTypeSpec(
                    SveAnimalRules.GOOSE_ID,
                    "coop",
                    SveAnimalRules.GOOSE_DAYS_TO_MATURE
            ));
        } else if (SveAnimalRules.CAMEL_ID.equalsIgnoreCase(animalTypeId)) {
            cir.setReturnValue(new AnimalTypeCatalog.AnimalTypeSpec(
                    SveAnimalRules.CAMEL_ID,
                    "barn",
                    SveAnimalRules.CAMEL_DAYS_TO_MATURE
            ));
        }
    }

    @Inject(method = "knownTypeIds", at = @At("RETURN"), cancellable = true, require = 1)
    private static void stardewcraftsve$includeAnimalTypeIds(CallbackInfoReturnable<Set<String>> cir) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(cir.getReturnValue());
        ids.add(SveAnimalRules.GOOSE_ID);
        ids.add(SveAnimalRules.CAMEL_ID);
        cir.setReturnValue(Set.copyOf(ids));
    }
}
