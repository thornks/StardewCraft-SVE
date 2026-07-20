package com.stardew.craft.sve.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.stardew.craft.communitycenter.menu.BundleMenu;
import com.stardew.craft.sve.SveBundleContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BundleMenu.class, remap = false)
public abstract class BundleMenuDifficultyMixin {
    @WrapMethod(method = "tryDeposit", require = 1)
    private boolean stardewcraftsve$withDepositContext(
            Player player,
            int bundleId,
            int ingredientSlot,
            ItemStack stack,
            Operation<Boolean> original
    ) {
        SveBundleContext.enter(player.getUUID());
        try {
            return original.call(player, bundleId, ingredientSlot, stack);
        } finally {
            SveBundleContext.exit();
        }
    }

    @WrapMethod(method = "tryPurchaseVault", require = 1)
    private boolean stardewcraftsve$withPurchaseContext(
            Player player,
            int bundleId,
            Operation<Boolean> original
    ) {
        SveBundleContext.enter(player.getUUID());
        try {
            return original.call(player, bundleId);
        } finally {
            SveBundleContext.exit();
        }
    }

    @WrapMethod(method = "handlePartialDeposit", require = 1)
    private boolean stardewcraftsve$withPartialDepositContext(
            ServerPlayer player,
            int bundleId,
            int ingredientIndex,
            int amount,
            ItemStack source,
            Operation<Boolean> original
    ) {
        SveBundleContext.enter(player.getUUID());
        try {
            return original.call(player, bundleId, ingredientIndex, amount, source);
        } finally {
            SveBundleContext.exit();
        }
    }

    @WrapMethod(method = "retrieveOneFromPartial", require = 1)
    private void stardewcraftsve$withPartialRetrieveContext(
            ServerPlayer player,
            int mode,
            Operation<Void> original
    ) {
        SveBundleContext.enter(player.getUUID());
        try {
            original.call(player, mode);
        } finally {
            SveBundleContext.exit();
        }
    }

    @WrapMethod(method = "returnAllPartials", require = 1)
    private void stardewcraftsve$withReturnPartialsContext(
            Player player,
            boolean drop,
            Operation<Void> original
    ) {
        SveBundleContext.enter(player.getUUID());
        try {
            original.call(player, drop);
        } finally {
            SveBundleContext.exit();
        }
    }
}
