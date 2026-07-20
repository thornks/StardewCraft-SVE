package com.stardew.craft.sve.mixin;

import com.stardew.craft.npc.runtime.NpcFriendshipRewardService;
import com.stardew.craft.sve.SveFriendshipRewards;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs add-on rewards at the same point as StardewCraft's built-in rewards. */
@Mixin(value = NpcFriendshipRewardService.class, remap = false)
public abstract class NpcFriendshipRewardServiceMixin {
    @Inject(method = "applyEligibleRewards", at = @At("RETURN"), cancellable = true, require = 1)
    private static void stardewcraftsve$applyRewards(
            ServerPlayer player,
            String npcId,
            int points,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (SveFriendshipRewards.applyEligible(player, npcId, points)) {
            cir.setReturnValue(true);
        }
    }
}
