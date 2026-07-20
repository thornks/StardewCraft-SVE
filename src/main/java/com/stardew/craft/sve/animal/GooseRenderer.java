package com.stardew.craft.sve.animal;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class GooseRenderer extends SveFarmAnimalRenderer<GooseEntity> {
    public GooseRenderer(EntityRendererProvider.Context context) {
        super(context, new GooseGeoModel(), 0.32F);
    }
}
