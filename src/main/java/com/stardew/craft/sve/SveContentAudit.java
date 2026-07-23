package com.stardew.craft.sve;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Read-only audit of effective acquisition routes for every registered SVE item. */
public final class SveContentAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/content-audit");
    private static final int MAX_REPORTED_ISSUES = 32;

    private SveContentAudit() {
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            SveContentAcquisitionService.Snapshot snapshot = SveContentAcquisitionService.inspect(
                    source.getServer());
            Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions =
                    SveContentAcquisitionCatalog.exclusions();
            SveContentAcquisitionGraph.Evaluation evaluation = snapshot.evaluation();
            List<Issue> issues = new ArrayList<>(buildIssues(evaluation, exclusions));
            snapshot.validationProblems().forEach(problem ->
                    issues.add(Issue.error(problem)));
            long errors = issues.stream().filter(Issue::error).count();
            long warnings = issues.size() - errors;
            int creative = countExclusions(exclusions,
                    SveContentAcquisitionCatalog.ExclusionType.CREATIVE_ONLY);
            int display = countExclusions(exclusions,
                    SveContentAcquisitionCatalog.ExclusionType.DISPLAY_ONLY);
            int planned = countExclusions(exclusions,
                    SveContentAcquisitionCatalog.ExclusionType.PLANNED_CONTENT);

            source.sendSuccess(() -> Component.literal(
                    "SVE content audit: registered=" + snapshot.registeredItems().size()
                            + ", reachable=" + evaluation.reachable().size()
                            + ", creative=" + creative
                            + ", display=" + display
                            + ", planned=" + planned
                            + ", errors=" + errors
                            + ", warnings=" + warnings
            ).withStyle(errors == 0 ? ChatFormatting.GREEN : ChatFormatting.RED), false);
            report(source, issues);
            return errors == 0 ? 1 : 0;
        } catch (Exception exception) {
            LOGGER.error("Failed to audit SVE content acquisition", exception);
            source.sendFailure(Component.literal("SVE content audit failed: " + exception.getMessage()));
            return 0;
        }
    }

    static List<Issue> buildIssues(
            SveContentAcquisitionGraph.Evaluation evaluation,
            Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions
    ) {
        List<Issue> issues = new ArrayList<>();
        evaluation.unclassified().stream().sorted()
                .forEach(item -> issues.add(Issue.error("No acquisition route for " + item)));
        evaluation.blocked().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> issues.add(Issue.error(
                        "Blocked acquisition route for " + entry.getKey() + "; "
                                + blockedDetail(entry.getValue()))));
        evaluation.unknownOutputs().stream().sorted()
                .forEach(item -> issues.add(Issue.error("Acquisition data outputs unregistered item " + item)));
        evaluation.missingDependencies().stream().sorted()
                .forEach(item -> issues.add(Issue.error("Acquisition route requires unregistered item " + item)));
        evaluation.staleExclusions().stream().sorted()
                .forEach(item -> issues.add(Issue.error("Reachable item still has an exclusion: " + item)));
        evaluation.unknownExclusions().stream().sorted()
                .forEach(item -> issues.add(Issue.error("Exclusion references unregistered item " + item)));

        exclusions.entrySet().stream()
                .filter(entry -> entry.getValue().type()
                        == SveContentAcquisitionCatalog.ExclusionType.PLANNED_CONTENT)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> issues.add(Issue.warning(
                        "Planned content is not survival-obtainable: " + entry.getKey()
                                + " (" + entry.getValue().reason() + ")")));
        return List.copyOf(issues);
    }

    private static String blockedDetail(java.util.Set<String> candidates) {
        return candidates.isEmpty()
                ? "an item selector has no live candidates"
                : "unresolved SVE candidates " + candidates;
    }

    private static int countExclusions(
            Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions,
            SveContentAcquisitionCatalog.ExclusionType type
    ) {
        return (int) exclusions.values().stream().filter(value -> value.type() == type).count();
    }

    private static void report(CommandSourceStack source, List<Issue> issues) {
        int shown = Math.min(MAX_REPORTED_ISSUES, issues.size());
        for (int index = 0; index < shown; index++) {
            Issue issue = issues.get(index);
            source.sendSystemMessage(Component.literal(issue.message())
                    .withStyle(issue.error() ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (issues.size() > shown) {
            source.sendSystemMessage(Component.literal(
                    "... " + (issues.size() - shown) + " more issue(s); see server log"
            ).withStyle(ChatFormatting.GRAY));
        }
        for (Issue issue : issues) {
            if (issue.error()) LOGGER.error(issue.message());
            else LOGGER.warn(issue.message());
        }
    }

    record Issue(boolean error, String message) {
        private static Issue error(String message) {
            return new Issue(true, message);
        }

        private static Issue warning(String message) {
            return new Issue(false, message);
        }
    }
}
