package com.stardew.craft.sve.tree;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SveFruitTreeGeoModel extends GeoModel<SveFruitTreeBlockEntity> {
    @Override
    public ResourceLocation getModelResource(SveFruitTreeBlockEntity animatable) {
        return animatable.getFruitTreeType().matureModel();
    }

    @Override
    public ResourceLocation getTextureResource(SveFruitTreeBlockEntity animatable) {
        return animatable.getFruitTreeType().matureTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(SveFruitTreeBlockEntity animatable) {
        return null;
    }
}
