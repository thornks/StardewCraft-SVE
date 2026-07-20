package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only audit of effective acquisition routes for every registered SVE item. */
public final class SveContentAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/content-audit");
    private static final int MAX_REPORTED_ISSUES = 32;

    private SveContentAudit() {
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            Set<String> registered = registeredItems();
            SveContentAcquisitionGraph graph = new SveContentAcquisitionGraph();
            SveContentDataScanner.scan(loadResources(source.getServer().getResourceManager()), graph);
            SveContentAcquisitionCatalog.addProgrammaticRoutes(graph);
            Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions =
                    SveContentAcquisitionCatalog.exclusions();
            SveContentAcquisitionGraph.Evaluation evaluation = graph.evaluate(registered, exclusions);
            List<Issue> issues = buildIssues(evaluation, exclusions);
            long errors = issues.stream().filter(Issue::error).count();
            long warnings = issues.size() - errors;
            int creative = countExclusions(exclusions,
                    SveContentAcquisitionCatalog.ExclusionType.CREATIVE_ONLY);
            int display = countExclusions(exclusions,
                    SveContentAcquisitionCatalog.ExclusionType.DISPLAY_ONLY);
            int planned = countExclusions(exclusions,
                    SveContentAcquisitionCatalog.ExclusionType.PLANNED_CONTENT);

            source.sendSuccess(() -> Component.literal(
                    "SVE content audit: registered=" + registered.size()
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
                        "Blocked acquisition route for " + entry.getKey() + "; missing " + entry.getValue())));
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

    private static Map<String, JsonElement> loadResources(ResourceManager manager) throws IOException {
        Map<ResourceLocation, Resource> selected = new LinkedHashMap<>();
        for (String root : List.of(
                "shops", "mail", "cooking/recipes", "player/crafting_recipes",
                "fishing", "forage_zones", "fishpond/pond_data", "artisan")) {
            selected.putAll(manager.listResources(root, id ->
                    isRelevantNamespace(id.getNamespace())
                            && id.getPath().endsWith(".json")
                            && isRelevantPath(id.getPath())));
        }
        Map<String, JsonElement> resources = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : selected.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                resources.put(entry.getKey().toString(), JsonParser.parseReader(reader));
            }
        }
        return Map.copyOf(resources);
    }

    private static boolean isRelevantNamespace(String namespace) {
        return StardewcraftsveMod.MODID.equals(namespace) || "stardewcraft".equals(namespace);
    }

    private static boolean isRelevantPath(String path) {
        return path.startsWith("shops/")
                || path.startsWith("mail/")
                || path.startsWith("cooking/recipes/")
                || path.startsWith("player/crafting_recipes/")
                || path.startsWith("fishing/")
                || path.startsWith("forage_zones/")
                || path.startsWith("fishpond/pond_data/")
                || path.startsWith("artisan/");
    }

    private static Set<String> registeredItems() {
        Set<String> items = new LinkedHashSet<>();
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> StardewcraftsveMod.MODID.equals(id.getNamespace()))
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(items::add);
        return Set.copyOf(items);
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
