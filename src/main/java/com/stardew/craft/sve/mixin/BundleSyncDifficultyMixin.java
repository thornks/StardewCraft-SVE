package com.stardew.craft.sve.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.stardew.craft.communitycenter.network.BundleSyncPayload;
import com.stardew.craft.sve.SveBundleContext;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BundleSyncPayload.class, remap = false)
public abstract class BundleSyncDifficultyMixin {
    @WrapMethod(method = "sendFullSync", require = 1)
    private static void stardewcraftsve$withSyncContext(
            ServerPlayer player,
            Operation<Void> original
    ) {
        SveBundleContext.enter(player.getUUID());
        try {
            original.call(player);
        } finally {
            SveBundleContext.exit();
        }
    }
}
