package com.stardew.craft.sve.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.stardew.craft.communitycenter.network.BundleClaimRewardPayload;
import com.stardew.craft.sve.SveBundleContext;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BundleClaimRewardPayload.class, remap = false)
public abstract class BundleClaimRewardDifficultyMixin {
    @WrapMethod(method = "lambda$handle$0", require = 1)
    private static void stardewcraftsve$withClaimContext(
            IPayloadContext context,
            BundleClaimRewardPayload payload,
            Operation<Void> original
    ) {
        SveBundleContext.enter(context.player().getUUID());
        try {
            original.call(context, payload);
        } finally {
            SveBundleContext.exit();
        }
    }
}
