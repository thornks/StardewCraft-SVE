package com.stardew.craft.sve;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ButterChurnerBlockEntityRenderer implements BlockEntityRenderer<ButterChurnerBlockEntity> {

    private static final ResourceLocation BUBBLE_TEX = ResourceLocation.fromNamespaceAndPath("stardewcraft", "textures/gui/bubble.png");

    public ButterChurnerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ButterChurnerBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean ready = be.isReady();
        ItemStack product = be.getDisplayProduct();
        BlockState state = be.getBlockState();
        Level level = be.getLevel();

        // Render block with working animation
        if (level != null) {
            poseStack.pushPose();
            if (be.isWorking() && !ready) {
                applyKegWorkingPose(poseStack, level, be.getBlockPos(), partialTick);
            }
            var blockRenderer = Minecraft.getInstance().getBlockRenderer();
            var model = blockRenderer.getBlockModel(state);
            var modelRenderer = blockRenderer.getModelRenderer();
            var actualRenderType = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(state, false);
            var consumer = buffer.getBuffer(actualRenderType);
            var random = net.minecraft.util.RandomSource.create(0L);
            modelRenderer.tesselateBlock(level, model, state, be.getBlockPos(), poseStack, consumer, true, random, 0L, packedOverlay);
            poseStack.popPose();
        }

        // Render bubble + product when ready
        if (!ready || product.isEmpty() || level == null) return;

        float y = getBubbleY(state, level, be.getBlockPos());
        poseStack.pushPose();

        // Position above block, facing camera
        poseStack.translate(0.5, y, 0.5);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // Render bubble background — quad sits with bottom at origin, extending upward
        float bw = 0.625f;
        float bh = 0.75f;
        float hw = -bw / 2.0f;   // -0.3125
        float halfW = bw / 2.0f; // 0.3125

        VertexConsumer bubbleConsumer = buffer.getBuffer(RenderType.entityTranslucent(BUBBLE_TEX));
        var mat = poseStack.last().pose();

        bubbleConsumer.addVertex(mat, hw, bh, 0)
                .setColor(255, 255, 255, 255).setUv(0, 0)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        bubbleConsumer.addVertex(mat, halfW, bh, 0)
                .setColor(255, 255, 255, 255).setUv(1, 0)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        bubbleConsumer.addVertex(mat, halfW, 0, 0)
                .setColor(255, 255, 255, 255).setUv(1, 1)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        bubbleConsumer.addVertex(mat, hw, 0, 0)
                .setColor(255, 255, 255, 255).setUv(0, 1)
                .setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);

        // Render product item inside the billboard space (matches cheese press)
        float itemScale = 0.4375f;
        float itemX = hw + 0.09375f + itemScale / 2.0f;   // = 0
        float itemY = bh - 0.09375f - itemScale / 2.0f;    // = 0.4375
        poseStack.pushPose();
        poseStack.translate(itemX, itemY, 0.001f);
        poseStack.scale(itemScale, itemScale, 0.001f);
        Minecraft.getInstance().getItemRenderer()
                .renderStatic(product, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,
                        poseStack, buffer, level, 0);
        poseStack.popPose();

        // Render count badge (BubbleItemCountRenderer) — omitted for now, single item only

        poseStack.popPose(); // bubble space
    }

    private static float getBubbleY(BlockState state, Level level, BlockPos pos) {
        var shape = state.getShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.empty());
        float maxY = shape.isEmpty() ? 1.0f : (float) shape.max(net.minecraft.core.Direction.Axis.Y);
        // Cheese press is a 2-block structure with shape extending to y=2, so its bubble
        // sits at ~2.05. For our single-block machine we need a large gap to match.
        return maxY + 0.425f;
    }

    // ===== Working animation — replicated from CheesePressBlockEntityRenderer =====

    private record Keyframe(float t, float sx, float sy, float sz, float y) {}

    private static void applyKegWorkingPose(PoseStack poseStack, Level level, BlockPos pos, float partialTick) {
        float cycleTime = getCycleTime(level, pos, partialTick);

        Keyframe kf0 = new Keyframe(0, 1.05f, 1.45f, 1.05f, -0.1f);
        Keyframe kf1 = new Keyframe(8, 1.35f, 1.05f, 1.35f, 0.05f);
        Keyframe kf2 = new Keyframe(15, 1.20f, 1.20f, 1.20f, 0);
        Keyframe kf3 = new Keyframe(23, 1.25f, 1.15f, 1.25f, 0.02f);
        Keyframe kf4 = new Keyframe(30, 1.05f, 1.45f, 1.05f, -0.1f);

        Keyframe prev, next;
        if (cycleTime < kf1.t()) {
            prev = kf0; next = kf1;
        } else if (cycleTime < kf2.t()) {
            prev = kf1; next = kf2;
        } else if (cycleTime < kf3.t()) {
            prev = kf2; next = kf3;
        } else {
            prev = kf3; next = kf4;
        }

        float delta = (cycleTime - prev.t()) / (next.t() - prev.t());

        float sx = lerp(delta, prev.sx(), next.sx());
        float sy = lerp(delta, prev.sy(), next.sy());
        float sz = lerp(delta, prev.sz(), next.sz());
        float yOff = lerp(delta, prev.y(), next.y());

        // Normalize to 1.2 center
        sx /= 1.2f;
        sy /= 1.2f;
        sz /= 1.2f;

        // Dampen the animation
        float damping = 0.55f;
        sx = 1.0f + (sx - 1.0f) * damping;
        sy = 1.0f + (sy - 1.0f) * damping;
        sz = 1.0f + (sz - 1.0f) * damping;

        // Clamp
        sx = clamp(sx, 0.95f, 1.05f);
        sy = clamp(sy, 0.95f, 1.05f);
        sz = clamp(sz, 0.95f, 1.05f);
        yOff = clamp(yOff * 0.3f, 0, 0.04f);

        // Apply transformation
        poseStack.translate(0, yOff, 0);
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(sx, sy, sz);
        poseStack.translate(-0.5, -0.5, -0.5);
    }

    private static float getCycleTime(Level level, BlockPos pos, float partialTick) {
        long hash = pos.asLong() * -7046029254386353131L;
        float offset = (float) (hash >>> 40) / 4096.0f;
        float time = (float) level.getGameTime() + partialTick + offset * 30.0f;
        float cycle = time % 30.0f;
        if (cycle < 0) cycle += 30.0f;
        return cycle;
    }

    private static float lerp(float delta, float a, float b) {
        return a + (b - a) * delta;
    }

    private static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }
}
