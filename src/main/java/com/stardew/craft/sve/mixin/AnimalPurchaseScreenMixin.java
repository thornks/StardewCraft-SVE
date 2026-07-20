package com.stardew.craft.sve.mixin;

import com.stardew.craft.client.gui.AnimalPurchaseScreen;
import com.stardew.craft.client.gui.common.SdvTexture;
import com.stardew.craft.sve.client.SveAnimalClientCompatibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = AnimalPurchaseScreen.class, remap = false)
public abstract class AnimalPurchaseScreenMixin {
    @Shadow(remap = false) @Final @Mutable
    private static Map<String, SdvTexture> ANIMAL_TEXTURES;

    @Inject(method = "<clinit>", at = @At("TAIL"), require = 1)
    private static void stardewcraftsve$installAnimalTextures(CallbackInfo ci) {
        ANIMAL_TEXTURES = SveAnimalClientCompatibility.appendPurchaseTextures(ANIMAL_TEXTURES);
    }
}
