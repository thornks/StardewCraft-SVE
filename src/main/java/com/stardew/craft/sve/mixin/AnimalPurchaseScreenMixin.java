package com.stardew.craft.sve.mixin;

import com.stardew.craft.client.gui.AnimalPurchaseScreen;
import com.stardew.craft.client.gui.common.SdvTexture;
import com.stardew.craft.sve.StardewcraftsveMod;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(value = AnimalPurchaseScreen.class, remap = false)
public abstract class AnimalPurchaseScreenMixin {
    @Shadow(remap = false) @Final @Mutable
    private static Map<String, SdvTexture> ANIMAL_TEXTURES;

    @Inject(method = "<clinit>", at = @At("TAIL"), require = 1)
    private static void stardewcraftsve$installAnimalTextures(CallbackInfo ci) {
        LinkedHashMap<String, SdvTexture> textures = new LinkedHashMap<>(ANIMAL_TEXTURES);
        textures.put(SveAnimalRules.GOOSE_ID, texture("goose"));
        textures.put(SveAnimalRules.CAMEL_ID, texture("camel"));
        ANIMAL_TEXTURES = Map.copyOf(textures);
    }

    private static SdvTexture texture(String animalTypeId) {
        return SdvTexture.full(
                ResourceLocation.fromNamespaceAndPath(
                        StardewcraftsveMod.MODID,
                        "textures/gui/animal_query/icon_" + animalTypeId + ".png"
                ),
                32,
                16
        );
    }
}
