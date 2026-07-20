package com.stardew.craft.sve.mixin;

import com.stardew.craft.client.gui.FarmSelectionScreen;
import com.stardew.craft.sve.client.SveFarmDifficultyCheckbox;
import com.stardew.craft.sve.network.BundleDifficultySelectionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FarmSelectionScreen.class, remap = false)
public abstract class FarmSelectionScreenMixin extends Screen {
    @Shadow private int rightX;
    @Shadow private int rightW;
    @Shadow private EditBox nameField;

    @Unique private boolean stardewcraftsve$hardBundles;
    @Unique private SveFarmDifficultyCheckbox stardewcraftsve$hardBundlesCheckbox;

    protected FarmSelectionScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"), require = 1)
    private void stardewcraftsve$addHardBundleOption(CallbackInfo ci) {
        int y = nameField.getY() + nameField.getHeight() + 7;
        stardewcraftsve$hardBundlesCheckbox = new SveFarmDifficultyCheckbox(
                rightX, y, rightW, font,
                Component.translatable("gui.stardewcraftsve.farm_selection.hard_bundles"),
                stardewcraftsve$hardBundles,
                selected -> stardewcraftsve$hardBundles = selected);
        stardewcraftsve$hardBundlesCheckbox.setTooltip(Tooltip.create(Component.translatable(
                "gui.stardewcraftsve.farm_selection.hard_bundles.tooltip")));
        addRenderableWidget(stardewcraftsve$hardBundlesCheckbox);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 1)
    private void stardewcraftsve$renderHardBundleOption(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (stardewcraftsve$hardBundlesCheckbox == null) return;
        stardewcraftsve$hardBundlesCheckbox.render(graphics, mouseX, mouseY, partialTick);
        if (stardewcraftsve$hardBundlesCheckbox.isHovered()) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.stardewcraftsve.farm_selection.hard_bundles.tooltip"), mouseX, mouseY);
        }
    }

    @Inject(method = "sendSelection", at = @At("HEAD"), require = 1)
    private void stardewcraftsve$sendBundleDifficulty(
            String farmTypeId, String farmName, boolean forceCancelPending, CallbackInfo ci) {
        PacketDistributor.sendToServer(new BundleDifficultySelectionPayload(stardewcraftsve$hardBundles));
    }
}
