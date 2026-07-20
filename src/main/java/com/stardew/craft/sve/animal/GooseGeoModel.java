package com.stardew.craft.sve.animal;

import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GooseGeoModel extends GeoModel<GooseEntity> {
    @Override
    public ResourceLocation getModelResource(GooseEntity goose) {
        return resource("geo/entity/animal/" + agePrefix(goose) + "goose_entity.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GooseEntity goose) {
        return resource("textures/entity/animal/" + agePrefix(goose) + "goose_entity.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GooseEntity goose) {
        return resource("animations/entity/animal/" + agePrefix(goose) + "goose_entity.animation.json");
    }

    private static String agePrefix(GooseEntity goose) {
        return goose.isBaby() ? "baby_" : "";
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }
}
