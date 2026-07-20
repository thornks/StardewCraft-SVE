package com.stardew.craft.sve.animal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public abstract class SveFarmAnimalRenderer<T extends SveFarmAnimalEntity> extends GeoEntityRenderer<T> {
    private static final ResourceLocation EMOTES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "textures/gui/emotes.png");
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int ICON_SIZE = 16;
    private static final float EMOTE_SIZE = 0.8F;

    protected SveFarmAnimalRenderer(
            EntityRendererProvider.Context context,
            GeoModel<T> model,
            float shadowRadius
    ) {
        super(context, model);
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void render(
            T animal,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        super.render(animal, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        int frameIndex = animal.getCurrentEmoteFrameIndex();
        if (frameIndex < 0) {
            return;
        }

        int frameX = (frameIndex * ICON_SIZE) % TEXTURE_WIDTH;
        int frameY = ((frameIndex * ICON_SIZE) / TEXTURE_WIDTH) * ICON_SIZE;
        float u0 = frameX / (float) TEXTURE_WIDTH;
        float u1 = (frameX + ICON_SIZE) / (float) TEXTURE_WIDTH;
        float v0 = frameY / (float) TEXTURE_HEIGHT;
        float v1 = (frameY + ICON_SIZE) / (float) TEXTURE_HEIGHT;

        poseStack.pushPose();
        double emoteOffset = animal instanceof CamelEntity ? 0.65D : 0.22D;
        poseStack.translate(0.0D, animal.getBbHeight() + emoteOffset, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        float half = EMOTE_SIZE * 0.5F;
        VertexConsumer vertices = bufferSource.getBuffer(RenderType.entityTranslucent(EMOTES_TEXTURE));
        vertices.addVertex(poseStack.last().pose(), -half, half, 0.0F)
                .setColor(255, 255, 255, 255).setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        vertices.addVertex(poseStack.last().pose(), half, half, 0.0F)
                .setColor(255, 255, 255, 255).setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        vertices.addVertex(poseStack.last().pose(), half, -half, 0.0F)
                .setColor(255, 255, 255, 255).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        vertices.addVertex(poseStack.last().pose(), -half, -half, 0.0F)
                .setColor(255, 255, 255, 255).setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0, 0, 1);
        poseStack.popPose();
    }
}
