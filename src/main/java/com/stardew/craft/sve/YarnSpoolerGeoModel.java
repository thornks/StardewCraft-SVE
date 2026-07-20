package com.stardew.craft.sve;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class YarnSpoolerGeoModel extends GeoModel<YarnSpoolerBlockEntity> {
    @Override
    public ResourceLocation getModelResource(YarnSpoolerBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                StardewcraftsveMod.MODID, "geo/block/machine/yarn_spooler.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(YarnSpoolerBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                StardewcraftsveMod.MODID, "textures/block/machine/yarn_spooler.png");
    }

    @Override
    public ResourceLocation getAnimationResource(YarnSpoolerBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                StardewcraftsveMod.MODID, "animations/block/machine/yarn_spooler.animation.json");
    }
}
