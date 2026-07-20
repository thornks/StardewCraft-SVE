package com.stardew.craft.sve.client;

import com.stardew.craft.client.gui.common.SdvTexture;
import com.stardew.craft.sve.StardewcraftsveMod;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Client-only animal texture bridge for StardewCraft screens. */
public final class SveAnimalClientCompatibility {
    private SveAnimalClientCompatibility() {
    }

    public static Map<String, SdvTexture> appendPurchaseTextures(Map<String, SdvTexture> base) {
        LinkedHashMap<String, SdvTexture> textures = new LinkedHashMap<>(base);
        for (SveAnimalRules.Definition definition : SveAnimalRules.definitions()) {
            textures.put(definition.id(), texture(definition.id()));
        }
        return Map.copyOf(textures);
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
