package com.stardew.craft.sve;

import com.stardew.craft.api.v1.npc.StardewNpcInteractionContext;
import com.stardew.craft.api.v1.npc.StardewNpcInteractions;
import com.stardew.craft.communitycenter.state.CCStoryFlags;
import com.stardew.craft.entity.npc.StardewNpcEntity;
import com.stardew.craft.mail.MailService;
import com.stardew.craft.network.payload.OpenNpcDialogueScreenPayload;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Supplies SVE Morris dialogue through StardewCraft's public NPC interaction API. */
public final class MorrisDialogueInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/morris-dialogue");
    private static final ResourceLocation MORRIS =
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "morris");
    private static final String NPC_ID = "morris";
    private static final String I18N_PREFIX = "stardewcraftsve.npc.morris.";
    private static final List<String> JOJA_AREAS = List.of(
            "jojaVault", "jojaBoilerRoom", "jojaCraftsRoom", "jojaPantry", "jojaFishTank"
    );

    private MorrisDialogueInterceptor() {}

    public static void register() {
        StardewNpcInteractions.register(
                ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "morris_dialogue"),
                1000,
                MorrisDialogueInterceptor::interact
        );
    }

    private static InteractionResult interact(StardewNpcInteractionContext context) {
        if (!MORRIS.equals(context.npcId())) return InteractionResult.PASS;

        ServerPlayer player = context.player();
        if (!player.getItemInHand(context.hand()).isEmpty()) {
            return InteractionResult.PASS;
        }

        matchRotation(context.npc());
        speak(player, determineDialogueKey(player));
        return InteractionResult.SUCCESS;
    }

    private static void matchRotation(StardewNpcEntity npc) {
        npc.setYRot(90.0f);
        npc.setYHeadRot(90.0f);
    }

    private static String determineDialogueKey(ServerPlayer player) {
        if (CCStoryFlags.isJojaMember(player)) {
            if (hasAnyJojaAreaPending(player)) return "processing";
            return hasAllJojaAreas(player) ? "no_more_cd" : "cd_form_offer";
        }
        if (MailService.hasMailFlagForTomorrow(player, "JojaMember")) {
            return "come_back_later";
        }
        if (!CCStoryFlags.hasFlag(player, "JojaGreeting")) {
            CCStoryFlags.addFlag(player, "JojaGreeting");
            return "introduction";
        }

        boolean communityCenterComplete = CCStoryFlags.hasFlag(player, "ccIsComplete");
        if (communityCenterComplete) {
            return isWeekend() ? "weekend_cc_done" : "first_cc_done";
        }
        return isWeekend() ? "weekend_membership" : "first_membership";
    }

    private static boolean hasAnyJojaAreaPending(ServerPlayer player) {
        return JOJA_AREAS.stream().anyMatch(area -> MailService.hasMailFlagForTomorrow(player, area));
    }

    private static boolean hasAllJojaAreas(ServerPlayer player) {
        for (int i = 0; i < 5; i++) {
            if (!CCStoryFlags.hasFlag(player, jojaButtonToCcFlag(i))) return false;
        }
        return true;
    }

    private static String jojaButtonToCcFlag(int index) {
        return switch (index) {
            case 0 -> "ccVault";
            case 1 -> "ccBoilerRoom";
            case 2 -> "ccCraftsRoom";
            case 3 -> "ccPantry";
            case 4 -> "ccFishTank";
            default -> "";
        };
    }

    private static boolean isWeekend() {
        int day = StardewTimeManager.get().getCurrentDay();
        int dayOfWeek = Math.floorMod(day - 1, 7);
        return dayOfWeek == 5 || dayOfWeek == 6;
    }

    private static void speak(ServerPlayer player, String keySuffix) {
        String translateKey = I18N_PREFIX + keySuffix;
        LOGGER.info("Sending Morris dialogue to {}: {}", player.getName().getString(), translateKey);
        PacketDistributor.sendToPlayer(player,
                new OpenNpcDialogueScreenPayload(NPC_ID, translateKey, 0));
    }
}
