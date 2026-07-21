package com.stardew.craft.sve.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.stardew.craft.client.gui.menu.StardewGameMenuScreen;
import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies the 16x32 SVE sprites missing from StardewCraft's built-in big-craftable atlas. */
@Mixin(value = StardewGameMenuScreen.class, remap = false)
public abstract class StardewGameMenuBigCraftableMixin {
    @Unique private static final int STARDEWCRAFTSVE$BUTTER_CHURNER = -3_753_001;
    @Unique private static final int STARDEWCRAFTSVE$YARN_SPOOLER = -3_753_002;
    @Unique private static final ResourceLocation STARDEWCRAFTSVE$BUTTER_CHURNER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, "textures/gui/crafting/butter_churner.png");
    @Unique private static final ResourceLocation STARDEWCRAFTSVE$YARN_SPOOLER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, "textures/gui/crafting/yarn_spooler.png");

    @Inject(
            method = "vanillaBigCraftableSprite(Ljava/lang/String;)Ljava/lang/Integer;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$resolveBigCraftableSprite(
            String recipeId,
            CallbackInfoReturnable<Integer> cir
    ) {
        ResourceLocation id = ResourceLocation.tryParse(recipeId);
        if (id == null || !StardewcraftsveMod.MODID.equals(id.getNamespace())) return;

        switch (id.getPath()) {
            case "butter_churner" -> cir.setReturnValue(STARDEWCRAFTSVE$BUTTER_CHURNER);
            case "yarn_spooler" -> cir.setReturnValue(STARDEWCRAFTSVE$YARN_SPOOLER);
            default -> {
            }
        }
    }

    @Inject(
            method = "drawVanillaBigCraftable(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFZ)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void stardewcraftsve$drawBigCraftable(
            GuiGraphics graphics,
            int spriteIndex,
            int cellX,
            int cellY,
            int cellWidth,
            int cellHeight,
            float hoverScale,
            boolean craftable,
            CallbackInfo ci
    ) {
        ResourceLocation texture = switch (spriteIndex) {
            case STARDEWCRAFTSVE$BUTTER_CHURNER -> STARDEWCRAFTSVE$BUTTER_CHURNER_TEXTURE;
            case STARDEWCRAFTSVE$YARN_SPOOLER -> STARDEWCRAFTSVE$YARN_SPOOLER_TEXTURE;
            default -> null;
        };
        if (texture == null) return;

        int drawWidth = Math.round(cellWidth * hoverScale);
        int drawHeight = Math.round(cellHeight * hoverScale);
        int drawX = cellX + (cellWidth - drawWidth) / 2;
        int drawY = cellY + (cellHeight - drawHeight) / 2;
        if (!craftable) {
            RenderSystem.setShaderColor(0.41F, 0.41F, 0.41F, 0.4F);
        }
        graphics.blit(texture, drawX, drawY, drawWidth, drawHeight,
                0, 0, 16, 32, 16, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ci.cancel();
    }
}
