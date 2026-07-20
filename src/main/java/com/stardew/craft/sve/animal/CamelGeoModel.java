package com.stardew.craft.sve.animal;

import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CamelGeoModel extends GeoModel<CamelEntity> {
    @Override
    public ResourceLocation getModelResource(CamelEntity camel) {
        return resource("geo/entity/animal/" + agePrefix(camel) + "camel_entity.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CamelEntity camel) {
        return resource("textures/entity/animal/" + agePrefix(camel) + "camel_entity.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CamelEntity camel) {
        return resource("animations/entity/animal/" + agePrefix(camel) + "camel_entity.animation.json");
    }

    private static String agePrefix(CamelEntity camel) {
        return camel.isBaby() ? "baby_" : "";
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }
}
