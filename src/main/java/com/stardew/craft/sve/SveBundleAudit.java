package com.stardew.craft.sve;

import com.stardew.craft.communitycenter.data.BundleDefinition;
import com.stardew.craft.communitycenter.data.BundleIngredient;
import com.stardew.craft.communitycenter.state.CommunityCenterSavedData;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.farm.FarmInstanceRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only validation of SVE community-center catalogs and saved progress. */
public final class SveBundleAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/bundle-audit");
    private static final int MAX_REPORTED_ISSUES = 20;

    private SveBundleAudit() {
    }

    public static int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        if (level == null) {
            source.sendFailure(Component.literal("Stardew Valley dimension is not loaded"));
            return 0;
        }

        Audit audit = audit(level);
        ChatFormatting color = audit.errors == 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal(
                "SVE bundle audit: standard=" + audit.standardBundles
                        + ", hard=" + audit.hardBundles
                        + ", players=" + audit.players
                        + ", errors=" + audit.errors
                        + ", warnings=" + audit.warnings
        ).withStyle(color), false);

        int shown = Math.min(MAX_REPORTED_ISSUES, audit.issues.size());
        for (int i = 0; i < shown; i++) {
            Issue issue = audit.issues.get(i);
            source.sendSystemMessage(Component.literal(issue.message)
                    .withStyle(issue.error ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (audit.issues.size() > shown) {
            source.sendSystemMessage(Component.literal(
                    "... " + (audit.issues.size() - shown) + " more issue(s); see server log"
            ).withStyle(ChatFormatting.GRAY));
        }
        for (Issue issue : audit.issues) {
            if (issue.error) LOGGER.error(issue.message);
            else LOGGER.warn(issue.message);
        }
        return audit.errors == 0 ? 1 : 0;
    }

    public static Audit audit(ServerLevel level) {
        Audit audit = new Audit();
        Map<Integer, BundleDefinition> standard = SveCommunityBundles.standardBundlesSnapshot();
        Map<Integer, BundleDefinition> hard = SveCommunityBundles.hardBundlesSnapshot();
        audit.standardBundles = standard.size();
        audit.hardBundles = hard.size();

        addProblems(audit, SveCommunityBundles.validateCatalog("standard", standard));
        addProblems(audit, SveCommunityBundles.validateCatalog("hard", hard));
        if (!standard.keySet().equals(hard.keySet())) {
            audit.error("standard and hard bundle ID sets differ");
        }

        for (String itemId : SveCommunityBundles.sveIngredientIds(
                new SveCommunityBundles.Catalog(standard, hard))) {
            if (SveBundleAcquisitionCatalog.routeFor(itemId) == null) {
                audit.error("Missing acquisition route for bundle ingredient " + itemId);
            }
        }

        CommunityCenterSavedData savedData = CommunityCenterSavedData.get(level);
        FarmInstanceRegistry farms = FarmInstanceRegistry.get();
        Map<UUID, ?> rawPlayers = savedData instanceof SveCommunityCenterDataView view
                ? view.stardewcraftsve$getPlayerDataView() : Map.of();
        for (UUID player : savedData.getAllPlayers()) {
            audit.players++;
            UUID owner = farms.getOwnerForPlayer(player);
            if (owner == null) {
                audit.warning("Community-center player " + player + " is not attached to a farm");
                continue;
            }
            boolean hardMode = SveBundleDifficultyData.get(level).isHard(owner);
            Map<Integer, BundleDefinition> catalog = hardMode ? hard : standard;
            validateProgress(rawPlayers.get(player), player, catalog, hardMode, audit);
        }

        for (UUID owner : SveBundleDifficultyData.get(level).hardFarmOwners()) {
            if (farms.getFarm(owner) == null) {
                audit.error("Hard-bundle marker points to missing farm owner " + owner);
            }
        }
        return audit;
    }

    private static void validateProgress(
            Object rawProgress,
            UUID player,
            Map<Integer, BundleDefinition> catalog,
            boolean hardMode,
            Audit audit
    ) {
        var progress = SveCommunityCenterDataView.progress(rawProgress);
        if (progress == null) {
            audit.error("Player " + player + " has an unreadable community-center progress record");
            return;
        }
        Map<Integer, boolean[]> slotsByBundle = progress.stardewcraftsve$getBundleSlots();
        Map<Integer, Boolean> rewards = progress.stardewcraftsve$getBundleRewards();
        String label = "Player " + player + (hardMode ? " (hard)" : " (standard)");
        for (BundleDefinition definition : catalog.values()) {
            boolean[] slots = slotsByBundle.get(definition.bundleId());
            if (slots == null) {
                continue;
            }
            if (slots.length != definition.totalSlots()) {
                audit.error(label + " bundle " + definition.bundleId() + " has "
                        + slots.length + " slots; expected " + definition.totalSlots());
                continue;
            }
            int filled = 0;
            for (boolean slot : slots) if (slot) filled++;
            if (filled > slots.length) {
                audit.error(label + " bundle " + definition.bundleId() + " has invalid filled-slot count");
            }
            if (Boolean.TRUE.equals(rewards.get(definition.bundleId()))
                    && filled < definition.requiredCount()) {
                audit.error(label + " bundle " + definition.bundleId()
                        + " is complete below required count " + definition.requiredCount());
            }
        }
        for (Integer bundleId : slotsByBundle.keySet()) {
            if (!catalog.containsKey(bundleId)) {
                audit.warning(label + " contains progress for unknown bundle " + bundleId);
            }
        }
    }

    private static void addProblems(Audit audit, Collection<String> problems) {
        for (String problem : problems) audit.error(problem);
    }

    public static final class Audit {
        private final List<Issue> issues = new ArrayList<>();
        private int standardBundles;
        private int hardBundles;
        private int players;
        private int errors;
        private int warnings;

        private void error(String message) {
            errors++;
            issues.add(new Issue(true, message));
        }

        private void warning(String message) {
            warnings++;
            issues.add(new Issue(false, message));
        }

        public int errors() {
            return errors;
        }

        public int warnings() {
            return warnings;
        }
    }

    private record Issue(boolean error, String message) {
    }
}
