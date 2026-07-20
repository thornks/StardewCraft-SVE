package com.stardew.craft.sve.mixin;

import com.stardew.craft.entity.npc.StardewNpcEntity;
import com.stardew.craft.npc.runtime.NpcInteractionService;
import com.stardew.craft.npc.runtime.NpcSpawnManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Resolves Morris directly because Joja NPCs are excluded from the spawn tracker. */
@Mixin(NpcInteractionService.class)
public abstract class MorrisGiftConfirmationMixin {
    private static final String MORRIS_ID = "morris";

    @Redirect(
            method = "handleConfirmedGift",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/stardew/craft/npc/runtime/NpcSpawnManager;getTrackedNpc(Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lcom/stardew/craft/entity/npc/StardewNpcEntity;"
            ),
            require = 1
    )
    private static StardewNpcEntity sve$resolveMorrisForGift(ServerLevel level, String npcId) {
        StardewNpcEntity trackedNpc = NpcSpawnManager.getTrackedNpc(level, npcId);
        if (trackedNpc != null || !MORRIS_ID.equalsIgnoreCase(npcId)) {
            return trackedNpc;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof StardewNpcEntity npc
                    && !npc.isRemoved()
                    && npc.isAlive()
                    && MORRIS_ID.equalsIgnoreCase(npc.getNpcId())) {
                return npc;
            }
        }
        return null;
    }
}
