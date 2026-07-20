package com.stardew.craft.sve.mixin;

import com.stardew.craft.npc.data.NpcCapabilityProfile;
import com.stardew.craft.npc.data.NpcSocialRules;
import com.stardew.craft.npc.runtime.NpcFriendshipDataManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.Set;

/** Enables the three service NPCs that become social NPCs in SVE. */
@Mixin(value = NpcSocialRules.class, remap = false)
public abstract class NpcSocialRulesMixin {
    private static final Set<String> SVE_SOCIAL_NPCS = Set.of("gunther", "marlon", "morris");

    @Inject(method = "canSocialize(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$canSocialize(
            String npcId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSveSocialNpc(npcId)) cir.setReturnValue(true);
    }

    @Inject(
            method = "canSocialize(Ljava/lang/String;Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$canSocializeForPlayer(
            String npcId,
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSveSocialNpc(npcId)) cir.setReturnValue(true);
    }

    @Inject(method = "canReceiveGifts(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$canReceiveGifts(
            String npcId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSveSocialNpc(npcId)) cir.setReturnValue(true);
    }

    @Inject(
            method = "canReceiveGifts(Ljava/lang/String;Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$canReceiveGiftsForPlayer(
            String npcId,
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSveSocialNpc(npcId)) cir.setReturnValue(true);
    }

    @Inject(method = "shouldShowOnSocialPage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$showOnSocialPage(
            String npcId,
            NpcCapabilityProfile profile,
            NpcFriendshipDataManager.FriendshipState state,
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSveSocialNpc(npcId)) {
            cir.setReturnValue(profile != null && profile.implemented());
        }
    }

    @Inject(method = "shouldCreateFriendshipForSocialPage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$createSocialPageFriendship(
            String npcId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (isSveSocialNpc(npcId)) cir.setReturnValue(true);
    }

    private static boolean isSveSocialNpc(String npcId) {
        if (npcId == null) return false;
        String normalized = npcId.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator >= 0) normalized = normalized.substring(separator + 1);
        return SVE_SOCIAL_NPCS.contains(normalized);
    }
}
