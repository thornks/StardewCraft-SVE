package com.stardew.craft.sve.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.stardew.craft.communitycenter.data.BundleDefinition;
import com.stardew.craft.communitycenter.state.CommunityCenterSavedData;
import com.stardew.craft.sve.SveBundleContext;
import com.stardew.craft.sve.SveCommunityBundles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Mixin(value = CommunityCenterSavedData.class, remap = false)
public abstract class CommunityCenterSavedDataMixin {
    @Shadow @Final private Map<UUID, ?> playerData;

    @Inject(method = "getSlots(Ljava/util/UUID;I)[Z", at = @At("RETURN"), cancellable = true, require = 1)
    private void stardewcraftsve$resizeBundleSlots(
            UUID player,
            int bundleId,
            CallbackInfoReturnable<boolean[]> cir
    ) {
        BundleDefinition definition = SveCommunityBundles.getBundleForPlayer(player, bundleId);
        boolean[] current = cir.getReturnValue();
        if (definition == null || current.length == definition.totalSlots()) return;

        boolean[] resized = Arrays.copyOf(current, definition.totalSlots());
        Object progress = playerData.get(player);
        if (progress instanceof CommunityCenterPlayerProgressAccessor accessor) {
            accessor.stardewcraftsve$getBundleSlots().put(bundleId, resized);
            ((CommunityCenterSavedData) (Object) this).setDirty();
            cir.setReturnValue(resized);
        }
    }

    @Inject(method = "getBundleSlotsView", at = @At("HEAD"), require = 1)
    private void stardewcraftsve$normalizeBeforeSync(
            UUID player,
            CallbackInfoReturnable<Map<Integer, boolean[]>> cir
    ) {
        CommunityCenterSavedData self = (CommunityCenterSavedData) (Object) this;
        for (BundleDefinition definition : SveCommunityBundles.getAllBundlesForPlayer(player)) {
            self.getSlots(player, definition.bundleId());
        }
    }

    @Inject(method = "isBundleComplete", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$useFarmRequiredCount(
            UUID player, int bundleId, CallbackInfoReturnable<Boolean> cir) {
        BundleDefinition definition = SveCommunityBundles.getBundleForPlayer(player, bundleId);
        if (definition == null) {
            cir.setReturnValue(false);
            return;
        }
        CommunityCenterSavedData self = (CommunityCenterSavedData) (Object) this;
        cir.setReturnValue(self.countFilledSlots(player, bundleId) >= definition.requiredCount());
    }

    @WrapMethod(method = "completeAll", require = 1)
    private void stardewcraftsve$withCompleteAllContext(UUID player, Operation<Void> original) {
        SveBundleContext.enter(player);
        try {
            original.call(player);
        } finally {
            SveBundleContext.exit();
        }
    }
}
