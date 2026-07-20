package com.stardew.craft.sve.animal;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class CamelRenderer extends SveFarmAnimalRenderer<CamelEntity> {
    public CamelRenderer(EntityRendererProvider.Context context) {
        super(context, new CamelGeoModel(), 0.55F);
    }
}
