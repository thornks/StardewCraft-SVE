package com.stardew.craft.sve.tree.wild;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SveWildTreeGeoModel extends GeoModel<SveWildTreeBlockEntity> {
    @Override public ResourceLocation getModelResource(SveWildTreeBlockEntity tree) { return tree.getTreeType().model(); }
    @Override public ResourceLocation getTextureResource(SveWildTreeBlockEntity tree) { return tree.getTreeType().texture(); }
    @Override public ResourceLocation getAnimationResource(SveWildTreeBlockEntity tree) { return tree.getTreeType().animation(); }
}
