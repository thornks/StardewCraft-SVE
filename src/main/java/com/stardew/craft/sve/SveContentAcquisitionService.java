package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/** Builds an acquisition graph from effective runtime snapshots and accepted resource data. */
final class SveContentAcquisitionService {
    private SveContentAcquisitionService() {
    }

    static Snapshot inspect(MinecraftServer server) throws IOException {
        SveAcquisitionEntrypoints entrypoints =
                StardewCraftAcquisitionSnapshotAdapter.capture(server);
        return inspect(Map.of(), registeredItems(), entrypoints);
    }

    static Snapshot inspect(Map<String, JsonElement> resources, Set<String> registeredItems) {
        return inspect(resources, registeredItems,
                SveAcquisitionEntrypoints.fromResources(resources));
    }

    static Snapshot inspect(
            Map<String, JsonElement> resources,
            Set<String> registeredItems,
            SveAcquisitionEntrypoints entrypoints
    ) {
        SveContentAcquisitionGraph graph = new SveContentAcquisitionGraph();
        List<String> validationProblems = new java.util.ArrayList<>(
                entrypoints.validationProblems());
        validationProblems.addAll(
                SveContentDataScanner.scan(resources, graph, entrypoints));
        SveContentAcquisitionCatalog.addProgrammaticRoutes(graph);
        Set<String> registered = Set.copyOf(registeredItems);
        SveContentAcquisitionGraph.Evaluation evaluation = graph.evaluate(
                registered, SveContentAcquisitionCatalog.exclusions());
        return new Snapshot(registered, graph.routes(), evaluation,
                validationProblems.stream().distinct().toList());
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

    record Snapshot(
            Set<String> registeredItems,
            Map<String, List<SveContentAcquisitionGraph.Route>> routes,
            SveContentAcquisitionGraph.Evaluation evaluation,
            List<String> validationProblems
    ) {
        Snapshot {
            registeredItems = Set.copyOf(registeredItems);
            Map<String, List<SveContentAcquisitionGraph.Route>> copiedRoutes =
                    new LinkedHashMap<>();
            routes.forEach((item, itemRoutes) ->
                    copiedRoutes.put(item, List.copyOf(itemRoutes)));
            routes = Map.copyOf(copiedRoutes);
            validationProblems = List.copyOf(validationProblems);
        }
    }
}
