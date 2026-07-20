package com.stardew.craft.sve.tree;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stardew.craft.client.render.StardewGeoBlockRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;

public class SveFruitTreeBlockEntityRenderer extends StardewGeoBlockRenderer<SveFruitTreeBlockEntity> {
    public SveFruitTreeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new SveFruitTreeGeoModel());
    }

    @Nullable
    @Override
    public RenderType getRenderType(SveFruitTreeBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SveFruitTreeBlockEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int renderColor) {
        boolean oldHidden = bone.isHidden();
        if (bone.getParent() != null && "fruit".equals(bone.getParent().getName())) {
            bone.setHidden(!shouldRenderFruitBone(
                    animatable.getFruitCount(), animatable.getMaxStoredFruit(), bone));
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, renderColor);
        bone.setHidden(oldHidden);
    }

    private static boolean shouldRenderFruitBone(int fruitCount, int maxStoredFruit, GeoBone bone) {
        int maximum = Math.max(1, maxStoredFruit);
        int clampedCount = Math.max(0, Math.min(maximum, fruitCount));
        if (clampedCount == 0) {
            return false;
        }
        if (clampedCount >= maximum) {
            return true;
        }

        GeoBone fruitRoot = bone.getParent();
        if (fruitRoot == null || fruitRoot.getChildBones().isEmpty()) {
            return false;
        }
        int total = fruitRoot.getChildBones().size();
        int visible = Math.max(1,
                (int) Math.ceil(total * clampedCount / (double) maximum));
        int index = fruitRoot.getChildBones().indexOf(bone);
        return index >= 0 && index < visible;
    }
}
