package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.stardew.craft.sve.collection.SveCollectionCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Offline acquisition-closure checks; no server or game world is required. */
public final class SveContentAcquisitionTest {
    private static final Path DATA_ROOT = Path.of("src/main/resources/data");
    private static final Path ITEM_SOURCE = Path.of(
            "src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final List<Pattern> REGISTRATION_PATTERNS = List.of(
            Pattern.compile("(?:ITEMS|STARDEWCRAFT_ITEMS|SMOKED_FISH_ITEMS)"
                    + "\\.register\\(\\s*\"([^\"]+)\""),
            Pattern.compile("artisan\\(\\s*\"([^\"]+)\""),
            Pattern.compile("\\breg\\(\\s*\"([^\"]+)\"")
    );

    private SveContentAcquisitionTest() {
    }

    public static void main(String[] args) throws IOException {
        Set<String> registered = registeredItems();
        Map<String, JsonElement> resources = loadResources();
        SveContentAcquisitionGraph graph = new SveContentAcquisitionGraph();
        SveContentDataScanner.scan(resources, graph);
        SveContentAcquisitionCatalog.addProgrammaticRoutes(graph);
        SveContentAcquisitionGraph.Evaluation evaluation = graph.evaluate(
                registered, SveContentAcquisitionCatalog.exclusions());

        if (evaluation.hasErrors()) {
            StringBuilder message = new StringBuilder("SVE content acquisition audit failed:\n");
            SveContentAudit.buildIssues(evaluation, SveContentAcquisitionCatalog.exclusions()).stream()
                    .filter(SveContentAudit.Issue::error)
                    .forEach(issue -> message.append(" - ").append(issue.message()).append('\n'));
            throw new AssertionError(message);
        }
        if (registered.size() != 259) {
            throw new AssertionError("Unexpected SVE item registry size: " + registered.size());
        }
        validateCollections(registered, evaluation);
        validateRecipeQueryRoutes(resources, registered, graph);
        System.out.println("SVE content acquisition regression suite passed: registered="
                + registered.size() + ", reachable=" + evaluation.reachable().size()
                + ", excluded=" + SveContentAcquisitionCatalog.exclusions().size());
    }

    private static void validateRecipeQueryRoutes(
            Map<String, JsonElement> resources,
            Set<String> registered,
            SveContentAcquisitionGraph graph
    ) {
        for (Map.Entry<String, JsonElement> resource : resources.entrySet()) {
            String path = resource.getKey().substring(resource.getKey().indexOf(':') + 1);
            String expectedKind;
            if (path.startsWith("cooking/recipes/")) expectedKind = "cooking";
            else if (path.startsWith("player/crafting_recipes/")) expectedKind = "crafting";
            else continue;

            if (!resource.getValue().isJsonObject()) {
                throw new AssertionError("Recipe resource is not an object: " + resource.getKey());
            }
            JsonElement outputElement = resource.getValue().getAsJsonObject().get("output");
            if (outputElement == null || !outputElement.isJsonPrimitive()) {
                throw new AssertionError("Recipe has no output: " + resource.getKey());
            }
            String output = outputElement.getAsString();
            if (!output.startsWith(StardewcraftsveMod.MODID + ":")) continue;
            if (!registered.contains(output)) {
                throw new AssertionError("Recipe outputs unregistered item " + output);
            }
            boolean queryRoute = graph.routes().getOrDefault(output, List.of()).stream()
                    .anyMatch(route -> expectedKind.equals(route.kind()));
            if (!queryRoute) {
                throw new AssertionError("Recipe has no reachable unlock/query route: "
                        + resource.getKey() + " -> " + output);
            }
        }
    }

    private static void validateCollections(
            Set<String> registered,
            SveContentAcquisitionGraph.Evaluation evaluation
    ) {
        for (int tab = 0; tab <= 3; tab++) {
            for (String item : SveCollectionCatalog.configuredItemIdsForTab(tab)) {
                if (!registered.contains(item)) {
                    throw new AssertionError("Collection tab " + tab + " references unregistered item " + item);
                }
                if (!evaluation.reachable().contains(item)
                        && !SveContentAcquisitionCatalog.exclusions().containsKey(item)) {
                    throw new AssertionError("Collection tab " + tab + " contains unobtainable item " + item);
                }
            }
        }
        Set<String> fishCollection = Set.copyOf(SveCollectionCatalog.configuredItemIdsForTab(1));
        Set<String> pondFish = SveFishData.FISH_POND_ITEMS.stream()
                .map(path -> StardewcraftsveMod.MODID + ":" + path)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        pondFish.remove("stardewcraftsve:razor_trout");
        pondFish.add("stardewcraftsve:dulse_seaweed");
        if (!fishCollection.equals(Set.copyOf(pondFish))) {
            throw new AssertionError("Fish collection and fish pond catalogs differ: collection="
                    + fishCollection + ", ponds=" + pondFish);
        }
    }

    private static Set<String> registeredItems() throws IOException {
        Set<String> items = new LinkedHashSet<>();
        String source = Files.readString(ITEM_SOURCE);
        for (Pattern pattern : REGISTRATION_PATTERNS) {
            var matcher = pattern.matcher(source);
            while (matcher.find()) items.add(StardewcraftsveMod.MODID + ":" + matcher.group(1));
        }
        return Set.copyOf(items);
    }

    private static Map<String, JsonElement> loadResources() throws IOException {
        Map<String, JsonElement> resources = new LinkedHashMap<>();
        try (var paths = Files.walk(DATA_ROOT)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json")).toList()) {
                Path relative = DATA_ROOT.relativize(file);
                if (relative.getNameCount() < 2) continue;
                String namespace = relative.getName(0).toString();
                if (!namespace.equals("stardewcraft") && !namespace.equals(StardewcraftsveMod.MODID)) continue;
                String resourcePath = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                resources.put(namespace + ":" + resourcePath,
                        JsonParser.parseString(Files.readString(file)));
            }
        }
        return Map.copyOf(resources);
    }
}
