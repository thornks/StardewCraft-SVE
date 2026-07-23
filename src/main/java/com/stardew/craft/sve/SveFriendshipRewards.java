package com.stardew.craft.sve;

import com.stardew.craft.mail.MailService;
import com.stardew.craft.npc.runtime.NpcFriendshipDataManager;
import com.stardew.craft.player.PlayerDataEventHandler;
import com.stardew.craft.player.PlayerDataManager;
import com.stardew.craft.player.PlayerStardewData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Event-driven SVE friendship flags, mail, and recipe unlocks. */
public final class SveFriendshipRewards {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/friendship-rewards");
    private static final List<Rule> RULES = Stream.concat(List.of(
            Rule.flag("morris", 2500, "morris_10_hearts"),
            Rule.flag("krobus", 2500, "krobus_10_hearts"),
            Rule.flag("sophia", 1500, "sophia_6_hearts"),
            Rule.mail("gunther", 750, "gunther_three_hearts"),
            Rule.mail("gunther", 1500, "gunther_six_hearts"),
            Rule.mail("marlon", 500, "marlon_two_hearts"),
            Rule.mail("marlon", 1000, "marlon_four_hearts"),
            Rule.mail("marlon", 1500, "marlon_six_hearts"),
            Rule.mail("marlon", 2000, "marlon_eight_hearts"),
            Rule.mail("marlon", 2500, "marlon_ten_hearts")
    ).stream(), SveCookingData.all().stream()
            .filter(definition -> definition.unlock().type() == SveCookingData.UnlockType.FRIENDSHIP)
            .map(definition -> Rule.recipe(
                    definition.unlock().source(),
                    definition.unlock().friendshipPoints(),
                    definition.path()))).toList();

    private SveFriendshipRewards() {}

    public static Map<String, RecipeUnlock> recipeUnlocks() {
        return RULES.stream()
                .filter(rule -> rule.recipe() != null)
                .collect(Collectors.toUnmodifiableMap(
                        rule -> StardewcraftsveMod.MODID + ":" + rule.recipe(),
                        rule -> new RecipeUnlock(rule.npcId(), rule.points())));
    }

    static Map<String, String> mailTriggers() {
        return RULES.stream()
                .filter(rule -> rule.mailForTomorrow() != null)
                .collect(Collectors.toUnmodifiableMap(
                        rule -> "stardewcraft:"
                                + rule.mailForTomorrow().toLowerCase(Locale.ROOT),
                        rule -> rule.npcId() + " friendship "
                                + (rule.points() / 250) + " hearts"));
    }

    public static boolean applyEligible(ServerPlayer player, String npcId, int points) {
        if (player == null || npcId == null || npcId.isBlank()) return false;

        String normalizedNpc = normalizeNpcId(npcId);
        PlayerStardewData data = PlayerDataManager.getPlayerData(player);
        boolean changed = false;
        boolean playerDataChanged = false;

        for (Rule rule : RULES) {
            if (!rule.npcId().equals(normalizedNpc) || points < rule.points()) continue;

            if (rule.flag() != null && !data.hasMailFlag(rule.flag())) {
                data.addMailFlag(rule.flag());
                changed = true;
                playerDataChanged = true;
            }
            if (rule.mailForTomorrow() != null
                    && !MailService.hasOrWillReceiveMail(player, rule.mailForTomorrow())) {
                MailService.addMailForTomorrow(player, rule.mailForTomorrow());
                changed = true;
            }
            if (rule.recipe() != null
                    && data.unlockRecipe(StardewcraftsveMod.MODID + ":" + rule.recipe())) {
                changed = true;
                playerDataChanged = true;
            }
        }

        if (playerDataChanged) {
            PlayerDataEventHandler.syncPlayerData(player, data);
        }
        if (changed) {
            LOGGER.info("Applied SVE friendship rewards for {} at {} points", normalizedNpc, points);
        }
        return changed;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        NpcFriendshipDataManager friendship = NpcFriendshipDataManager.get(player.serverLevel());
        for (var entry : friendship.getPointsForPlayer(player.getUUID()).entrySet()) {
            applyEligible(player, entry.getKey(), entry.getValue());
        }
    }

    private static String normalizeNpcId(String npcId) {
        String normalized = npcId.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private record Rule(String npcId, int points, String flag, String mailForTomorrow, String recipe) {
        private static Rule flag(String npcId, int points, String flag) {
            return new Rule(npcId, points, flag, null, null);
        }

        private static Rule mail(String npcId, int points, String mailId) {
            return new Rule(npcId, points, null, mailId, null);
        }

        private static Rule recipe(String npcId, int points, String recipeId) {
            return new Rule(npcId, points, null, null, recipeId);
        }
    }

    public record RecipeUnlock(String npcId, int points) {
    }
}
