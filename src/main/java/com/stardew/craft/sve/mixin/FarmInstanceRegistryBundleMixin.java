package com.stardew.craft.sve.mixin;

import com.stardew.craft.farm.FarmInstance;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.farm.FarmType;
import com.stardew.craft.communitycenter.network.BundleSyncPayload;
import com.stardew.craft.sve.SveBundleDifficultyData;
import com.stardew.craft.sve.SveBundleSelectionPending;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = FarmInstanceRegistry.class, remap = false)
public abstract class FarmInstanceRegistryBundleMixin {
    @Inject(method = "createFarm", at = @At("RETURN"), require = 1)
    private void stardewcraftsve$saveBundleDifficulty(
            UUID owner, String ownerName, String farmName, FarmType farmType,
            CallbackInfoReturnable<FarmInstance> cir) {
        Boolean hard = SveBundleSelectionPending.consume(owner);
        if (hard == null || cir.getReturnValue() == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            SveBundleDifficultyData.get(server.overworld()).setHard(owner, hard);
            var player = server.getPlayerList().getPlayer(owner);
            if (player != null) BundleSyncPayload.sendFullSync(player);
        }
    }

    @Inject(method = "transferFarm", at = @At("RETURN"), require = 1)
    private void stardewcraftsve$transferBundleDifficulty(
            UUID oldOwner, UUID newOwner, String newOwnerName, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            SveBundleDifficultyData.get(server.overworld()).transfer(oldOwner, newOwner);
            var oldPlayer = server.getPlayerList().getPlayer(oldOwner);
            var newPlayer = server.getPlayerList().getPlayer(newOwner);
            if (oldPlayer != null) BundleSyncPayload.sendFullSync(oldPlayer);
            if (newPlayer != null) BundleSyncPayload.sendFullSync(newPlayer);
        }
    }

    @Inject(method = "deleteFarm", at = @At("RETURN"), require = 1)
    private void stardewcraftsve$deleteBundleDifficulty(
            UUID owner, CallbackInfoReturnable<FarmInstance> cir) {
        if (cir.getReturnValue() == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            SveBundleDifficultyData.get(server.overworld()).remove(owner);
            var player = server.getPlayerList().getPlayer(owner);
            if (player != null) BundleSyncPayload.sendFullSync(player);
        }
    }
}
