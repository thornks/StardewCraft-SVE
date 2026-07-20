package com.stardew.craft.sve;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stardew.craft.client.render.StardewGeoBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class YarnSpoolerBlockEntityRenderer extends StardewGeoBlockRenderer<YarnSpoolerBlockEntity> {
    private static final ResourceLocation BUBBLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "stardewcraft", "textures/gui/bubble.png");

    public YarnSpoolerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new YarnSpoolerGeoModel());
    }

    @Nullable
    @Override
    public RenderType getRenderType(YarnSpoolerBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void render(YarnSpoolerBlockEntity spooler, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(spooler, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        ItemStack product = spooler.getDisplayProduct();
        if (!spooler.isReady() || product.isEmpty() || spooler.getLevel() == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 1.425, 0.5);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        float width = 0.625F;
        float height = 0.75F;
        float left = -width / 2.0F;
        float right = width / 2.0F;
        VertexConsumer bubble = bufferSource.getBuffer(RenderType.entityTranslucent(BUBBLE_TEXTURE));
        var pose = poseStack.last().pose();

        bubble.addVertex(pose, left, height, 0).setColor(255, 255, 255, 255).setUv(0, 0)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        bubble.addVertex(pose, right, height, 0).setColor(255, 255, 255, 255).setUv(1, 0)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        bubble.addVertex(pose, right, 0, 0).setColor(255, 255, 255, 255).setUv(1, 1)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        bubble.addVertex(pose, left, 0, 0).setColor(255, 255, 255, 255).setUv(0, 1)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);

        float itemScale = 0.4375F;
        poseStack.pushPose();
        poseStack.translate(0, height - 0.09375F - itemScale / 2.0F, 0.001F);
        poseStack.scale(itemScale, itemScale, 0.001F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                product, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, spooler.getLevel(), 0);
        poseStack.popPose();
        poseStack.popPose();
    }
}
