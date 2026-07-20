package com.stardew.craft.sve.mixin;

import com.stardew.craft.communitycenter.data.BundleDataManager;
import com.stardew.craft.communitycenter.data.BundleDefinition;
import com.stardew.craft.sve.SveBundleContext;
import com.stardew.craft.sve.SveCommunityBundles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(value = BundleDataManager.class, remap = false)
public abstract class BundleDataManagerDifficultyMixin {
    @Inject(method = "getBundle", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$getFarmBundle(int id, CallbackInfoReturnable<BundleDefinition> cir) {
        UUID player = SveBundleContext.currentPlayer();
        if (player != null && SveCommunityBundles.isReady()) {
            cir.setReturnValue(SveCommunityBundles.getBundleForPlayer(player, id));
        }
    }

    @Inject(method = "getBundlesForArea", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$getFarmBundlesForArea(
            int areaId, CallbackInfoReturnable<List<BundleDefinition>> cir) {
        UUID player = SveBundleContext.currentPlayer();
        if (player != null && SveCommunityBundles.isReady()) {
            cir.setReturnValue(SveCommunityBundles.getBundlesForArea(player, areaId));
        }
    }

    @Inject(method = "getAllBundles", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$getAllFarmBundles(
            CallbackInfoReturnable<Collection<BundleDefinition>> cir) {
        UUID player = SveBundleContext.currentPlayer();
        if (player != null && SveCommunityBundles.isReady()) {
            cir.setReturnValue(SveCommunityBundles.getAllBundlesForPlayer(player));
        }
    }
}
