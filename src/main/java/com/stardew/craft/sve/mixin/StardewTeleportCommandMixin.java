package com.stardew.craft.sve.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.stardew.craft.command.StardewTeleportCommand;
import com.stardew.craft.sve.SveDebugSeasonState;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables debug season only when an administrator selects a real season. */
@Mixin(StardewTeleportCommand.class)
public abstract class StardewTeleportCommandMixin {
    @Inject(method = "setSeason", at = @At("HEAD"), require = 1)
    private static void stardewcraftsve$disableForRealSeason(
            CommandContext<CommandSourceStack> context, int season,
            CallbackInfoReturnable<Integer> callback) {
        SveDebugSeasonState.setEnabled(context.getSource().getServer(), false);
    }

    @Inject(method = "resetTime", at = @At("HEAD"), require = 1)
    private static void stardewcraftsve$disableForTimeReset(
            CommandContext<CommandSourceStack> context,
            CallbackInfoReturnable<Integer> callback) {
        SveDebugSeasonState.setEnabled(context.getSource().getServer(), false);
    }
}
