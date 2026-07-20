package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.model.AnimalTypeCatalog;
import com.stardew.craft.sve.animal.SveAnimalCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(AnimalTypeCatalog.class)
public abstract class AnimalTypeCatalogMixin {
    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveAnimalType(
            String animalTypeId,
            CallbackInfoReturnable<AnimalTypeCatalog.AnimalTypeSpec> cir
    ) {
        AnimalTypeCatalog.AnimalTypeSpec spec = SveAnimalCompatibility.typeSpec(animalTypeId);
        if (spec != null) cir.setReturnValue(spec);
    }

    @Inject(method = "knownTypeIds", at = @At("RETURN"), cancellable = true, require = 1)
    private static void stardewcraftsve$includeAnimalTypeIds(CallbackInfoReturnable<Set<String>> cir) {
        cir.setReturnValue(SveAnimalCompatibility.appendKnownTypeIds(cir.getReturnValue()));
    }
}
