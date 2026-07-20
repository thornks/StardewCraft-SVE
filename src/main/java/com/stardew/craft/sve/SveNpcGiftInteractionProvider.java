package com.stardew.craft.sve;

import com.stardew.craft.api.v1.npc.StardewNpcInteractionContext;
import com.stardew.craft.api.v1.npc.StardewNpcInteractions;
import com.stardew.craft.network.payload.OpenGiftConfirmPayload;
import com.stardew.craft.npc.data.NpcSocialRules;
import com.stardew.craft.npc.runtime.NpcInteractionService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

/** Lets socialized service NPCs enter StardewCraft's normal gift flow. */
public final class SveNpcGiftInteractionProvider {
    private static final Set<ResourceLocation> TARGET_NPCS = Set.of(
            baseNpcId("gunther"),
            baseNpcId("marlon"),
            baseNpcId("morris")
    );

    private SveNpcGiftInteractionProvider() {}

    public static void register() {
        StardewNpcInteractions.register(
                ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "service_npc_gifts"),
                2000,
                SveNpcGiftInteractionProvider::interact
        );
    }

    private static InteractionResult interact(StardewNpcInteractionContext context) {
        if (!TARGET_NPCS.contains(context.npcId())) return InteractionResult.PASS;

        ServerPlayer player = context.player();
        String npcId = context.npcId().getPath();
        ItemStack heldItem = player.getItemInHand(context.hand());
        if (heldItem.isEmpty()
                || !NpcSocialRules.canReceiveGifts(npcId, player)
                || !NpcInteractionService.canBeGivenAsGift(heldItem)) {
            return InteractionResult.PASS;
        }

        String itemDisplayName = serializeItemName(heldItem, player);
        String npcDisplayName = context.npc().getDisplayName().getString();
        context.npc().facePlayerTemporarily(player, 60, () ->
                PacketDistributor.sendToPlayer(player,
                        new OpenGiftConfirmPayload(npcId, itemDisplayName, npcDisplayName)));
        return InteractionResult.SUCCESS;
    }

    private static String serializeItemName(ItemStack stack, ServerPlayer player) {
        try {
            return Component.Serializer.toJson(stack.getHoverName(), player.serverLevel().registryAccess());
        } catch (Exception ignored) {
            return stack.getHoverName().getString();
        }
    }

    private static ResourceLocation baseNpcId(String path) {
        return ResourceLocation.fromNamespaceAndPath("stardewcraft", path);
    }
}
