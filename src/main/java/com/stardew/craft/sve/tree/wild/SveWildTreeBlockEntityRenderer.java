package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.client.render.StardewGeoBlockRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class SveWildTreeBlockEntityRenderer extends StardewGeoBlockRenderer<SveWildTreeBlockEntity> {
    public SveWildTreeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new SveWildTreeGeoModel());
    }

    @Nullable
    @Override
    public RenderType getRenderType(SveWildTreeBlockEntity tree, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
