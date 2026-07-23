package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stardew.craft.api.v1.shop.StardewShopDefinition;
import com.stardew.craft.api.v1.shop.StardewShopEntry;
import com.stardew.craft.fishing.data.SpawnFishRule;
import com.stardew.craft.fishpond.service.FishPondDataService;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.item.artisan.PreserveType;
import com.stardew.craft.player.RecipeIdNormalizer;
import com.stardew.craft.sve.collection.SveCollectionCatalog;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Offline acquisition-closure checks; no server or game world is required. */
public final class SveContentAcquisitionTest {
    private SveContentAcquisitionTest() {
    }

    public static void main(String[] args) throws IOException {
        Set<String> registered = SveTestContentResources.registeredItems();
        Map<String, JsonElement> resources = SveTestContentResources.loadResources();
        SveContentAcquisitionService.Snapshot snapshot =
                SveContentAcquisitionService.inspect(resources, registered);
        SveContentAcquisitionGraph.Evaluation evaluation = snapshot.evaluation();

        validateCandidateSnapshot(resources, snapshot);
        if (registered.size() != 281) {
            throw new AssertionError("Unexpected SVE item registry size: " + registered.size());
        }
        validateCollections(registered, evaluation);
        validateRecipeQueryRoutes(resources, registered, snapshot.routes());
        validateMailTriggers(snapshot);
        validateLiveShopAdapter();
        validateFishingTreasureSnapshots(resources, registered, snapshot);
        validateFishPondSnapshots(resources, registered, snapshot);
        validateEffectiveEntrypointOverrides(resources, registered, snapshot);
        validateRequirementAlternatives();
        validateArtisanSelectorPriority();
        validateTypedRecipeUnlocks();
        validateCandidateMailFieldNames();
        validateExcludedDependenciesDoNotUnlock();
        validateHost052FishingRuleSemantics();
        System.out.println("SVE content acquisition regression suite passed: registered="
                + registered.size() + ", reachable=" + evaluation.reachable().size()
                + ", excluded=" + SveContentAcquisitionCatalog.exclusions().size());
    }

    private static void validateCandidateSnapshot(
            Map<String, JsonElement> resources,
            SveContentAcquisitionService.Snapshot snapshot
    ) {
        CandidateLimitations limitations = candidateLimitations(resources);
        Set<String> actualProblems = Set.copyOf(snapshot.validationProblems());
        expect(actualProblems.equals(limitations.problems()),
                "Unexpected candidate-only validation problems: expected="
                        + limitations.problems() + ", actual=" + actualProblems);

        SveContentAcquisitionGraph.Evaluation evaluation = snapshot.evaluation();
        Set<String> unexpectedBlocked = new LinkedHashSet<>(evaluation.blocked().keySet());
        unexpectedBlocked.removeAll(limitations.outputs());
        for (String output : limitations.outputs()) {
            Set<String> missing = evaluation.blocked().get(output);
            expect(missing == null || missing.isEmpty(),
                    "Unresolved selector reported false mandatory items for " + output
                            + ": " + missing);
        }
        expect(evaluation.unclassified().isEmpty(),
                "Unclassified SVE items: " + evaluation.unclassified());
        expect(unexpectedBlocked.isEmpty(),
                "Unexpected blocked SVE items: " + unexpectedBlocked);
        expect(evaluation.unknownOutputs().isEmpty(),
                "Unknown acquisition outputs: " + evaluation.unknownOutputs());
        expect(evaluation.missingDependencies().isEmpty(),
                "Missing acquisition dependencies: " + evaluation.missingDependencies());
        expect(evaluation.staleExclusions().isEmpty(),
                "Stale acquisition exclusions: " + evaluation.staleExclusions());
        expect(evaluation.unknownExclusions().isEmpty(),
                "Unknown acquisition exclusions: " + evaluation.unknownExclusions());
    }

    private static CandidateLimitations candidateLimitations(
            Map<String, JsonElement> resources
    ) {
        Set<String> outputs = new LinkedHashSet<>();
        Set<String> problems = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> resource : resources.entrySet()) {
            String fullId = resource.getKey();
            int separator = fullId.indexOf(':');
            String namespace = separator < 0 ? "minecraft" : fullId.substring(0, separator);
            String path = separator < 0 ? fullId : fullId.substring(separator + 1);
            String prefix;
            if (path.startsWith("cooking/recipes/") && path.endsWith(".json")) {
                prefix = "cooking/recipes/";
            } else if (path.startsWith("player/crafting_recipes/")
                    && path.endsWith(".json")) {
                prefix = "player/crafting_recipes/";
            } else {
                continue;
            }
            if (!resource.getValue().isJsonObject()) continue;
            JsonObject recipe = resource.getValue().getAsJsonObject();
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            if (ingredients == null) continue;
            String recipePath = path.substring(
                    prefix.length(), path.length() - ".json".length());
            String recipeId = namespace + ":" + recipePath;
            for (JsonElement element : ingredients) {
                if (!element.isJsonObject()) continue;
                JsonObject ingredient = element.getAsJsonObject();
                if (ingredient.has("item")) continue;
                String selector;
                if (ingredient.has("tag")) {
                    selector = "tag:" + ingredient.get("tag").getAsString().trim();
                } else if (ingredient.has("categories")) {
                    List<String> categories = new java.util.ArrayList<>();
                    for (JsonElement category : ingredient.getAsJsonArray("categories")) {
                        categories.add(category.getAsString());
                    }
                    selector = "categories:" + String.join(",", categories);
                } else {
                    selector = "invalid ingredient";
                }
                if (recipe.has("output")) outputs.add(recipe.get("output").getAsString());
                problems.add("Candidate recipe " + recipeId + " selector " + selector
                        + " requires the live item registry to resolve");
            }
        }
        return new CandidateLimitations(Set.copyOf(outputs), Set.copyOf(problems));
    }

    private static void validateLiveShopAdapter() {
        StardewShopDefinition definition = new StardewShopDefinition(
                "probe", "", "", List.of(
                shopEntry("stardewcraftsve:cucumber_seed", 0, null),
                shopEntry("recipe:stardewcraftsve:small_hardwood_fence", 1,
                        "stardewcraftsve:void_soul"),
                shopEntry("stardewcraftsve:pear_sapling", Integer.MAX_VALUE, null)),
                List.of(), List.of());
        List<SveAcquisitionEntrypoints.ShopEntry> entries =
                StardewCraftAcquisitionSnapshotAdapter.readShopEntries(definition);
        expect(entries.size() == 2, "live shop adapter did not filter zero stock");
        expect(entries.getFirst().itemId().equals(
                        "recipe:stardewcraftsve:small_hardwood_fence"),
                "live shop adapter lost a recipe entry");
        expect("stardewcraftsve:void_soul".equals(entries.getFirst().tradeItemId()),
                "live shop adapter lost a recipe trade item");
        expect(entries.getLast().itemId().equals("stardewcraftsve:pear_sapling"),
                "live shop adapter lost an unlimited-stock item");
    }

    private static void validateMailTriggers(
            SveContentAcquisitionService.Snapshot snapshot
    ) {
        expect(snapshot.routes().getOrDefault(
                        "stardewcraftsve:baked_potato", List.of()).stream()
                        .anyMatch(route -> "cooking".equals(route.kind())
                                && route.detail().contains(
                                "mail-open stardewcraft:gunther_three_hearts")),
                "Baked Potato recipe is not tied to opening its friendship mail");

        Set<String> artifacts = Set.of(
                "stardewcraftsve:amber", "stardewcraftsve:boomerang",
                "stardewcraftsve:faded_button", "stardewcraftsve:fossilized_apple",
                "stardewcraftsve:old_coin", "stardewcraftsve:rusty_shield",
                "stardewcraftsve:stone_of_yoba");
        SveContentAcquisitionGraph.Route galdoran = snapshot.routes().getOrDefault(
                        "stardewcraftsve:galdoran_gem", List.of()).stream()
                .filter(route -> "mail".equals(route.kind()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Galdoran Gem has no triggered museum-mail route"));
        expect(Set.copyOf(galdoran.prerequisites()).equals(artifacts),
                "Galdoran Gem mail lost its seven SVE artifact prerequisites");
    }

    private static StardewShopEntry shopEntry(String item, int stock, String tradeItem) {
        return new StardewShopEntry(
                item, "", "", 0, stock, Optional.ofNullable(tradeItem),
                tradeItem == null ? 0 : 1, List.of(), 1, 0, Optional.empty(),
                -1, 0, 1, List.of());
    }

    private static void validateFishingTreasureSnapshots(
            Map<String, JsonElement> resources,
            Set<String> registered,
            SveContentAcquisitionService.Snapshot candidateSnapshot
    ) {
        String poolResource =
                "stardewcraftsve:fishing/treasure_pools/sve_rare_treasure.json";
        Set<String> outputs = Set.of(
                "stardewcraftsve:ornate_treasure_chest",
                "stardewcraftsve:money_bag",
                "stardewcraftsve:magic_lamp");
        for (String output : outputs) {
            expect(candidateSnapshot.routes().getOrDefault(output, List.of()).stream()
                            .anyMatch(route -> "fishing_treasure".equals(route.kind())
                                    && route.detail().startsWith(
                                    "stardewcraftsve:sve_rare_treasure#")),
                    "Candidate additive treasure pool has no route for " + output);
        }

        SveAcquisitionEntrypoints candidates =
                SveAcquisitionEntrypoints.fromResources(resources);
        Set<SveAcquisitionEntrypoints.Domain> covered = Set.of(
                SveAcquisitionEntrypoints.Domain.SHOP,
                SveAcquisitionEntrypoints.Domain.FISHING,
                SveAcquisitionEntrypoints.Domain.FISHING_TREASURE);
        SveAcquisitionEntrypoints emptyLive = new SveAcquisitionEntrypoints(
                candidates.shops(), candidates.fishingLocations(), covered,
                Map.of(), Map.of(), List.of(), List.of(), List.of());
        SveContentAcquisitionService.Snapshot emptyLiveSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, emptyLive);
        for (String output : outputs) {
            expect(!hasRoute(emptyLiveSnapshot, output, "fishing_treasure"),
                    "Empty accepted treasure snapshot fell back to candidate JSON for " + output);
        }

        SveAcquisitionEntrypoints.Route acceptedRoute =
                new SveAcquisitionEntrypoints.Route(
                        "stardewcraftsve:money_bag", "fishing_treasure",
                        "stardewcraftsve:accepted_probe#0", List.of());
        SveAcquisitionEntrypoints acceptedLive = new SveAcquisitionEntrypoints(
                candidates.shops(), candidates.fishingLocations(), covered,
                Map.of(), Map.of(), List.of(), List.of(acceptedRoute), List.of());
        SveContentAcquisitionService.Snapshot acceptedLiveSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, acceptedLive);
        expect(hasRoute(acceptedLiveSnapshot,
                        "stardewcraftsve:money_bag", "fishing_treasure"),
                "Accepted live treasure route was not used");
        expect(!hasRoute(acceptedLiveSnapshot,
                        "stardewcraftsve:magic_lamp", "fishing_treasure"),
                "Candidate treasure route was merged into an accepted live snapshot");

        Map<String, JsonElement> ineffectiveResources = new LinkedHashMap<>(resources);
        JsonObject ineffectivePool = resources.get(poolResource).getAsJsonObject().deepCopy();
        JsonArray entries = ineffectivePool.getAsJsonArray("entries");
        entries.get(1).getAsJsonObject().addProperty("weight", 0);
        entries.get(2).getAsJsonObject().addProperty("min_fishing_level", 100);
        entries.get(2).getAsJsonObject().addProperty("max_fishing_level", 100);
        ineffectiveResources.put(poolResource, ineffectivePool);
        SveContentAcquisitionService.Snapshot ineffectiveSnapshot =
                SveContentAcquisitionService.inspect(ineffectiveResources, registered);
        expect(hasRoute(ineffectiveSnapshot,
                        "stardewcraftsve:ornate_treasure_chest", "fishing_treasure"),
                "Valid treasure entry disappeared beside ineffective entries");
        expect(!hasRoute(ineffectiveSnapshot,
                        "stardewcraftsve:money_bag", "fishing_treasure"),
                "Zero-weight treasure entry became an acquisition source");
        expect(!hasRoute(ineffectiveSnapshot,
                        "stardewcraftsve:magic_lamp", "fishing_treasure"),
                "Unreachable fishing-level treasure entry became an acquisition source");

        Map<String, JsonElement> zeroChanceResources = new LinkedHashMap<>(resources);
        JsonObject zeroChancePool = resources.get(poolResource).getAsJsonObject().deepCopy();
        zeroChancePool.addProperty("chance", 0.0f);
        zeroChanceResources.put(poolResource, zeroChancePool);
        SveContentAcquisitionService.Snapshot zeroChanceSnapshot =
                SveContentAcquisitionService.inspect(zeroChanceResources, registered);
        for (String output : outputs) {
            expect(!hasRoute(zeroChanceSnapshot, output, "fishing_treasure"),
                    "Zero-chance treasure pool became an acquisition source for " + output);
        }

        Map<String, JsonElement> unknownQueryResources = new LinkedHashMap<>(resources);
        JsonObject unknownQueryPool = resources.get(poolResource).getAsJsonObject().deepCopy();
        JsonObject unknownQuery = new JsonObject();
        unknownQuery.addProperty("type", "stardewcraftsve:dynamic_probe");
        unknownQuery.add("data", new JsonObject());
        unknownQueryPool.getAsJsonArray("entries").get(1).getAsJsonObject()
                .add("query", unknownQuery);
        unknownQueryResources.put(poolResource, unknownQueryPool);
        SveContentAcquisitionService.Snapshot unknownQuerySnapshot =
                SveContentAcquisitionService.inspect(unknownQueryResources, registered);
        expect(!hasRoute(unknownQuerySnapshot,
                        "stardewcraftsve:money_bag", "fishing_treasure"),
                "Unknown treasure item query became an acquisition source");
        expect(unknownQuerySnapshot.validationProblems().stream().anyMatch(problem ->
                        problem.contains("sve_rare_treasure")
                                && problem.contains("no enumerable direct item query")),
                "Unknown treasure item query was not diagnosed");
    }

    private static void validateFishPondSnapshots(
            Map<String, JsonElement> resources,
            Set<String> registered,
            SveContentAcquisitionService.Snapshot candidateSnapshot
    ) {
        String voidPondResource =
                "stardewcraftsve:fishpond/pond_data/void_eel.json";
        SveContentAcquisitionGraph.Route swirlRoute = candidateSnapshot.routes()
                .getOrDefault("stardewcraftsve:swirl_stone", List.of()).stream()
                .filter(route -> "fish_pond".equals(route.kind())
                        && route.detail().startsWith(voidPondResource + "#"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Candidate void-eel pond has no Swirl Stone route"));
        Map<String, List<String>> swirlRequirements = new LinkedHashMap<>();
        swirlRoute.requirements().forEach(requirement ->
                swirlRequirements.put(requirement.selector(), requirement.alternatives()));
        expect(swirlRequirements.equals(Map.of(
                        "item:stardewcraftsve:void_eel",
                        List.of("stardewcraftsve:void_eel"),
                        "fish_pond_gate:stardewcraftsve:void_eel@2",
                        List.of("stardewcraft:void_essence"),
                        "fish_pond_gate:stardewcraftsve:void_eel@7",
                        List.of("stardewcraft:void_egg"),
                        "fish_pond_gate:stardewcraftsve:void_eel@10",
                        List.of("stardewcraftsve:void_pebble"))),
                "Fish pond route lost its fish/gate AND-of-OR requirements: "
                        + swirlRequirements);

        SveAcquisitionEntrypoints candidates =
                SveAcquisitionEntrypoints.fromResources(resources);
        Set<SveAcquisitionEntrypoints.Domain> covered = Set.of(
                SveAcquisitionEntrypoints.Domain.SHOP,
                SveAcquisitionEntrypoints.Domain.FISHING,
                SveAcquisitionEntrypoints.Domain.FISH_POND);
        SveAcquisitionEntrypoints emptyLive = new SveAcquisitionEntrypoints(
                candidates.shops(), candidates.fishingLocations(), covered,
                Map.of(), Map.of(), List.of(), List.of(), List.of());
        SveContentAcquisitionService.Snapshot emptyLiveSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, emptyLive);
        expect(!hasRoute(emptyLiveSnapshot,
                        "stardewcraftsve:swirl_stone", "fish_pond"),
                "Empty accepted fish pond view fell back to candidate JSON");
        expect(!hasRoute(emptyLiveSnapshot,
                        "stardewcraftsve:shark_tooth", "fish_pond"),
                "Empty accepted fish pond view retained another candidate pond route");

        SveAcquisitionEntrypoints.Route acceptedRoute =
                new SveAcquisitionEntrypoints.Route(
                        "stardewcraftsve:swirl_stone", "fish_pond",
                        "stardewcraftsve:accepted_pond_probe#0",
                        List.of(SveContentAcquisitionGraph.Requirement.exact(
                                "stardewcraftsve:void_eel")));
        SveAcquisitionEntrypoints acceptedLive = new SveAcquisitionEntrypoints(
                candidates.shops(), candidates.fishingLocations(), covered,
                Map.of(), Map.of(), List.of(), List.of(acceptedRoute), List.of());
        SveContentAcquisitionService.Snapshot acceptedLiveSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, acceptedLive);
        expect(hasRoute(acceptedLiveSnapshot,
                        "stardewcraftsve:swirl_stone", "fish_pond"),
                "Accepted live fish pond route was not used");
        expect(!hasRoute(acceptedLiveSnapshot,
                        "stardewcraftsve:shark_tooth", "fish_pond"),
                "Candidate fish pond route leaked into the accepted live domain");

        Map<String, JsonElement> zeroDailyResources = new LinkedHashMap<>(resources);
        JsonObject zeroDailyPond = resources.get(voidPondResource).getAsJsonObject().deepCopy();
        zeroDailyPond.addProperty("base_min_produce_chance", 0.0f);
        zeroDailyPond.addProperty("base_max_produce_chance", 0.0f);
        zeroDailyResources.put(voidPondResource, zeroDailyPond);
        expect(!hasRoute(SveContentAcquisitionService.inspect(
                                zeroDailyResources, registered),
                        "stardewcraftsve:swirl_stone", "fish_pond"),
                "Zero daily-production chance became a fish pond source");

        Map<String, JsonElement> fixedOneResources = new LinkedHashMap<>(resources);
        JsonObject fixedOnePond = resources.get(voidPondResource).getAsJsonObject().deepCopy();
        fixedOnePond.addProperty("max_population", 1);
        fixedOneResources.put(voidPondResource, fixedOnePond);
        expect(!hasRoute(SveContentAcquisitionService.inspect(
                                fixedOneResources, registered),
                        "stardewcraftsve:swirl_stone", "fish_pond"),
                "Production above a fixed population cap became reachable");

        Map<String, JsonElement> shadowedResources = new LinkedHashMap<>(resources);
        JsonObject shadowedPond = resources.get(voidPondResource).getAsJsonObject().deepCopy();
        shadowedPond.remove("population_gates");
        JsonArray shadowedProductions = new JsonArray();
        shadowedProductions.add(JsonParser.parseString("""
                {
                  "item": "stardewcraftsve:void_pebble",
                  "required_population": 0,
                  "chance": 1.0,
                  "precedence": 0,
                  "min_count": 1,
                  "max_count": 1
                }
                """).getAsJsonObject());
        shadowedProductions.add(JsonParser.parseString("""
                {
                  "item": "stardewcraftsve:swirl_stone",
                  "required_population": 0,
                  "chance": 1.0,
                  "precedence": 0,
                  "min_count": 1,
                  "max_count": 1
                }
                """).getAsJsonObject());
        shadowedPond.add("produced_items", shadowedProductions);
        shadowedResources.put(voidPondResource, shadowedPond);
        SveContentAcquisitionService.Snapshot shadowedSnapshot =
                SveContentAcquisitionService.inspect(shadowedResources, registered);
        expect(hasRoute(shadowedSnapshot,
                        "stardewcraftsve:void_pebble", "fish_pond"),
                "First guaranteed fish pond production disappeared");
        expect(!hasRoute(shadowedSnapshot,
                        "stardewcraftsve:swirl_stone", "fish_pond"),
                "Guaranteed earlier production did not shadow the later equal-precedence item");

        Map<String, JsonElement> duplicateResources = new LinkedHashMap<>(resources);
        JsonObject laterDuplicate = resources.get(voidPondResource).getAsJsonObject().deepCopy();
        JsonArray duplicateProductions = new JsonArray();
        duplicateProductions.add(JsonParser.parseString("""
                {
                  "item": "stardewcraftsve:money_bag",
                  "required_population": 0,
                  "chance": 1.0,
                  "min_count": 1,
                  "max_count": 1
                }
                """).getAsJsonObject());
        laterDuplicate.add("produced_items", duplicateProductions);
        duplicateResources.put(
                "stardewcraftsve:fishpond/pond_data/zzz_void_eel_override.json",
                laterDuplicate);
        SveContentAcquisitionService.Snapshot duplicateSnapshot =
                SveContentAcquisitionService.inspect(duplicateResources, registered);
        expect(!hasRoute(duplicateSnapshot,
                        "stardewcraftsve:money_bag", "fish_pond"),
                "Later duplicate fish pond definition was merged into the first winner");
        expect(hasRoute(duplicateSnapshot,
                        "stardewcraftsve:swirl_stone", "fish_pond"),
                "First fish pond definition did not remain the candidate winner");

        FishPondDataService.PondData acceptedPond = new FishPondDataService.PondData(
                "stardewcraftsve:accepted_pond",
                Set.of("item_id:stardewcraftsve:void_eel"),
                -1, -1, 0.15D, 0.95D, List.of(), List.of(
                new FishPondDataService.ProducedItem(
                        "money", "stardewcraftsve:money_bag",
                        8, 0.5D, 0, "", 1, 1),
                new FishPondDataService.ProducedItem(
                        "guaranteed", "stardewcraftsve:void_pebble",
                        1, 1.0D, 0, "", 1, 1),
                new FishPondDataService.ProducedItem(
                        "missing", "stardewcraftsve:magic_lamp",
                        8, 0.5D, -1, "", 1, 1),
                new FishPondDataService.ProducedItem(
                        "zero", "stardewcraftsve:swirl_stone",
                        1, 0.0D, -2, "", 1, 1)),
                Map.of(
                        2, List.of("stardewcraft:tuna 1 1"),
                        4, List.of("stardewcraft:salmonberry 1 1",
                                "stardewcraft:cherry 1 1"),
                        7, List.of("stardewcraftsve:void_pebble 1 1")));
        List<String> adapterProblems = new java.util.ArrayList<>();
        List<SveAcquisitionEntrypoints.Route> acceptedRoutes =
                StardewCraftAcquisitionSnapshotAdapter.fishPondRoutes(
                        Map.of("stardewcraftsve:void_eel", acceptedPond),
                        adapterProblems,
                        item -> !"stardewcraftsve:magic_lamp".equals(item));
        SveAcquisitionEntrypoints.Route acceptedMoney = acceptedRoutes.stream()
                .filter(route -> "stardewcraftsve:money_bag".equals(route.output()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Accepted fish pond view lost a selectable production"));
        expect(acceptedMoney.requirements().size() == 4,
                "Accepted fish pond route did not require fish plus three gates");
        expect(acceptedMoney.requirements().stream().anyMatch(requirement ->
                        requirement.selector().endsWith("@4")
                                && requirement.alternatives().equals(List.of(
                                "stardewcraft:cherry", "stardewcraft:salmonberry"))),
                "Accepted fish pond gate did not preserve weighted candidates as OR");
        expect(acceptedRoutes.stream().noneMatch(route ->
                        "stardewcraftsve:magic_lamp".equals(route.output())
                                || "stardewcraftsve:swirl_stone".equals(route.output())),
                "Invalid or zero-chance accepted production became a route");
        expect(adapterProblems.stream().anyMatch(problem ->
                        problem.contains("missing output stardewcraftsve:magic_lamp")),
                "Unresolvable accepted fish pond output was not diagnosed");
    }

    private static void validateEffectiveEntrypointOverrides(
            Map<String, JsonElement> resources,
            Set<String> registered,
            SveContentAcquisitionService.Snapshot candidateSnapshot
    ) {
        SveAcquisitionEntrypoints candidates =
                SveAcquisitionEntrypoints.fromResources(resources);
        SveContentAcquisitionService.Snapshot explicitCandidates =
                SveContentAcquisitionService.inspect(resources, registered, candidates);
        expect(candidateSnapshot.evaluation().equals(explicitCandidates.evaluation()),
                "candidate-only inspection changed under explicit entrypoints");
        expect(candidateSnapshot.routes().equals(explicitCandidates.routes()),
                "candidate-only routes changed under explicit entrypoints");
        expect(candidateSnapshot.registeredItems().equals(explicitCandidates.registeredItems()),
                "candidate-only item registry changed under explicit entrypoints");
        expect(!SveAcquisitionEntrypoints.isSelectablePool("othermod:foreign_pool"),
                "standalone SVE audit accepted a third-party fishing pool");
        expectUnsupported(() -> candidateSnapshot.routes().clear(),
                "snapshot route map is mutable");
        List<SveContentAcquisitionGraph.Route> burgerRoutes = candidateSnapshot.routes()
                .get("stardewcraftsve:big_bark_burger");
        expectUnsupported(burgerRoutes::clear, "snapshot route list is mutable");
        expectUnsupported(burgerRoutes.getFirst().prerequisites()::clear,
                "snapshot route prerequisites are mutable");

        SveAcquisitionEntrypoints noLiveShops = new SveAcquisitionEntrypoints(
                Map.of(), candidates.fishingLocations());
        SveContentAcquisitionService.Snapshot noShopSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, noLiveShops);
        expect(!hasRoute(noShopSnapshot, "stardewcraftsve:cucumber_seed", "shop"),
                "an empty live shop snapshot must suppress candidate shop JSON");
        expect(!noShopSnapshot.evaluation().reachable().contains(
                        "stardewcraftsve:big_bark_burger"),
                "shop recipe unlock survived an empty live shop snapshot");
        expect(!hasRoute(noShopSnapshot,
                        "stardewcraftsve:small_hardwood_fence", "crafting"),
                "shop-only crafting recipe survived an empty live shop snapshot");

        Map<String, JsonElement> eventLockedResources = new LinkedHashMap<>(resources);
        eventLockedResources.put(
                "stardewcraftsve:player/crafting_recipes/small_hardwood_fence.json",
                JsonParser.parseString("""
                        {
                          "output": "stardewcraftsve:small_hardwood_fence",
                          "ingredients": [{"item": "stardewcraft:wood_hard"}],
                          "legacy_unlock_condition": "l 5"
                        }
                        """));
        SveContentAcquisitionService.Snapshot eventLockedSnapshot =
                SveContentAcquisitionService.inspect(eventLockedResources, registered,
                        new SveAcquisitionEntrypoints(
                                Map.of(), candidates.fishingLocations()));
        expect(!hasRoute(eventLockedSnapshot,
                        "stardewcraftsve:small_hardwood_fence", "crafting"),
                "event-only legacy condition became an automatic crafting unlock");

        Map<String, SveAcquisitionEntrypoints.Shop> replacementShops = Map.of(
                "stardewcraftsve:runtime_probe",
                new SveAcquisitionEntrypoints.Shop(
                        "stardewcraftsve:runtime_probe",
                        List.of(new SveAcquisitionEntrypoints.ShopEntry(
                                "stardewcraftsve:cucumber_seed", null))));
        SveContentAcquisitionService.Snapshot replacementSnapshot =
                SveContentAcquisitionService.inspect(resources, registered,
                        new SveAcquisitionEntrypoints(
                                replacementShops, candidates.fishingLocations()));
        expect(hasRoute(replacementSnapshot, "stardewcraftsve:cucumber_seed", "shop"),
                "replacement live shop route was not used");
        expect(!hasRoute(replacementSnapshot, "stardewcraftsve:pear_sapling", "shop"),
                "candidate shops were merged into a replacement live domain");

        SveAcquisitionEntrypoints.Shop tradedRecipeShop =
                new SveAcquisitionEntrypoints.Shop(
                        "stardewcraftsve:trade_probe",
                        List.of(new SveAcquisitionEntrypoints.ShopEntry(
                                "recipe:stardewcraftsve:small_hardwood_fence",
                                "stardewcraftsve:void_soul")));
        SveContentAcquisitionService.Snapshot tradedRecipeSnapshot =
                SveContentAcquisitionService.inspect(resources, registered,
                        new SveAcquisitionEntrypoints(
                                Map.of(tradedRecipeShop.id(), tradedRecipeShop),
                                candidates.fishingLocations()));
        expect(!tradedRecipeSnapshot.evaluation().reachable().contains(
                        "stardewcraftsve:small_hardwood_fence"),
                "unobtainable SVE recipe trade item did not block crafting");
        expect(tradedRecipeSnapshot.evaluation().blocked()
                        .getOrDefault("stardewcraftsve:small_hardwood_fence", Set.of())
                        .contains("stardewcraftsve:void_soul"),
                "blocked recipe did not report its trade-item dependency");

        Map<String, JsonElement> withoutShopCandidates = new LinkedHashMap<>();
        resources.forEach((id, value) -> {
            if (!id.substring(id.indexOf(':') + 1).startsWith("shops/")) {
                withoutShopCandidates.put(id, value);
            }
        });
        SveContentAcquisitionService.Snapshot acceptedShopSnapshot =
                SveContentAcquisitionService.inspect(
                        withoutShopCandidates, registered, candidates);
        expect(acceptedShopSnapshot.evaluation().reachable().contains(
                        "stardewcraftsve:big_bark_burger"),
                "accepted live shop snapshot did not unlock a candidate cooking recipe");

        SveContentAcquisitionService.Snapshot noFishingSnapshot =
                SveContentAcquisitionService.inspect(resources, registered,
                        new SveAcquisitionEntrypoints(candidates.shops(), Map.of()));
        expect(!hasRoute(noFishingSnapshot, "stardewcraftsve:puppyfish", "fishing"),
                "an empty live fishing snapshot must suppress candidate fishing JSON");

        SveAcquisitionEntrypoints emptyLiveArtisan = new SveAcquisitionEntrypoints(
                candidates.shops(), candidates.fishingLocations(),
                Set.of(SveAcquisitionEntrypoints.Domain.SHOP,
                        SveAcquisitionEntrypoints.Domain.FISHING,
                        SveAcquisitionEntrypoints.Domain.ARTISAN),
                Map.of(), Map.of(), List.of(), List.of(), List.of());
        SveContentAcquisitionService.Snapshot noArtisanSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, emptyLiveArtisan);
        expect(!hasRoute(noArtisanSnapshot,
                        "stardewcraftsve:smoked_king_salmon", "fish_smoker"),
                "an empty live artisan snapshot must suppress candidate machine JSON");

        SveAcquisitionEntrypoints.Route acceptedSmokerRoute =
                new SveAcquisitionEntrypoints.Route(
                        "stardewcraftsve:smoked_king_salmon", "fish_smoker",
                        "stardewcraft:sve_fish_smoker/king_salmon",
                        List.of(SveContentAcquisitionGraph.Requirement.exact(
                                "stardewcraftsve:king_salmon")));
        SveAcquisitionEntrypoints liveArtisan = new SveAcquisitionEntrypoints(
                candidates.shops(), candidates.fishingLocations(),
                Set.of(SveAcquisitionEntrypoints.Domain.SHOP,
                        SveAcquisitionEntrypoints.Domain.FISHING,
                        SveAcquisitionEntrypoints.Domain.ARTISAN),
                Map.of(), Map.of(), List.of(), List.of(acceptedSmokerRoute), List.of());
        SveContentAcquisitionService.Snapshot acceptedArtisanSnapshot =
                SveContentAcquisitionService.inspect(resources, registered, liveArtisan);
        expect(hasRoute(acceptedArtisanSnapshot,
                        "stardewcraftsve:smoked_king_salmon", "fish_smoker"),
                "accepted live artisan route was not used");

        expect(!SveAcquisitionEntrypoints.hasPositiveBaselineCatchProbability(
                        0.0f, 0.0f, 1.0f, 0, 0, -1, 4, 0.0f, false),
                "zero unbuffed first-roll chance was treated as a positive baseline");
        expect(!SveAcquisitionEntrypoints.hasPositiveBaselineCatchProbability(
                        1.0f, 0.0f, 1.0f, 12, 0, -1, 4, 0.0f, true),
                "ignoreFishDataRequirements bypassed the fishing-level gate");
        expect(SveAcquisitionEntrypoints.hasPositiveBaselineCatchProbability(
                        1.0f, 0.0f, 0.0f, 1, 0, -1, 4, 0.0f, false),
                "effective fishing level was omitted from the second-roll probability");

        SveAcquisitionEntrypoints.FishingLocation filteredLocation =
                new SveAcquisitionEntrypoints.FishingLocation(
                        "stardewcraftsve:sve_fish", List.of(
                        new SveAcquisitionEntrypoints.FishingRule(
                                "display", "stardewcraftsve:puppyfish",
                                true, true, true),
                        new SveAcquisitionEntrypoints.FishingRule(
                                "zero", "stardewcraftsve:tadpole",
                                false, false, true),
                        new SveAcquisitionEntrypoints.FishingRule(
                                "unmapped", "stardewcraftsve:starfish",
                                false, true, false),
                        new SveAcquisitionEntrypoints.FishingRule(
                                "valid", "stardewcraftsve:king_salmon",
                                false, true, true)));
        SveContentAcquisitionService.Snapshot filteredFishingSnapshot =
                SveContentAcquisitionService.inspect(resources, registered,
                        new SveAcquisitionEntrypoints(candidates.shops(), Map.of(
                                filteredLocation.key(), filteredLocation)));
        expect(hasRoute(filteredFishingSnapshot, "stardewcraftsve:puppyfish", "fishing"),
                "runtime-catchable display-only rule was hidden from acquisition");
        expect(filteredFishingSnapshot.validationProblems().stream().anyMatch(problem ->
                        problem.contains("displayOnly") && problem.contains("#display")),
                "runtime-catchable display-only rule was not diagnosed");
        expect(!hasRoute(filteredFishingSnapshot, "stardewcraftsve:tadpole", "fishing"),
                "zero-probability fishing rule became an acquisition source");
        expect(!hasRoute(filteredFishingSnapshot, "stardewcraftsve:starfish", "fishing"),
                "unmapped fishing rule became an acquisition source");
        expect(hasRoute(filteredFishingSnapshot, "stardewcraftsve:king_salmon", "fishing"),
                "valid live fishing rule was not added");

        Map<String, SveAcquisitionEntrypoints.Shop> mutable = new LinkedHashMap<>(
                candidates.shops());
        SveAcquisitionEntrypoints copied = new SveAcquisitionEntrypoints(
                mutable, candidates.fishingLocations());
        mutable.clear();
        expect(!copied.shops().isEmpty(),
                "effective entrypoints did not defensively copy their domain maps");
    }

    private static boolean hasRoute(
            SveContentAcquisitionService.Snapshot snapshot,
            String itemId,
            String kind
    ) {
        return snapshot.routes().getOrDefault(itemId, List.of()).stream()
                .anyMatch(route -> kind.equals(route.kind()));
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectUnsupported(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (UnsupportedOperationException expected) {
            // Expected immutable view.
        }
    }

    private static void validateRecipeQueryRoutes(
            Map<String, JsonElement> resources,
            Set<String> registered,
            Map<String, List<SveContentAcquisitionGraph.Route>> routes
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
            boolean queryRoute = routes.getOrDefault(output, List.of()).stream()
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
        Set<String> pondFish = SveFishData.SVE_FISH.stream()
                .map(path -> StardewcraftsveMod.MODID + ":" + path)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        pondFish.remove("stardewcraftsve:razor_trout");
        pondFish.add("stardewcraftsve:dulse_seaweed");
        if (!fishCollection.equals(Set.copyOf(pondFish))) {
            throw new AssertionError("Fish collection and fish pond catalogs differ: collection="
                    + fishCollection + ", ponds=" + pondFish);
        }
    }

    private static void validateRequirementAlternatives() {
        String exact = "stardewcraftsve:test_exact";
        String first = "stardewcraftsve:test_first";
        String second = "stardewcraftsve:test_second";
        String output = "stardewcraftsve:test_any_of_output";
        String emptyOutput = "stardewcraftsve:test_empty_selector_output";
        String externalOutput = "stardewcraftsve:test_external_selector_output";
        String unknown = "stardewcraftsve:test_unknown_alternative";
        String otherUnknown = "stardewcraftsve:test_other_unknown_alternative";
        String externalOptionalUnknown = "stardewcraftsve:test_external_optional_unknown";
        String registeredOptionalUnknown = "stardewcraftsve:test_registered_optional_unknown";
        String unknownExactOutput = "stardewcraftsve:test_unknown_exact_output";
        String allUnknownOutput = "stardewcraftsve:test_all_unknown_output";
        String mixedUnknownOutput = "stardewcraftsve:test_mixed_unknown_output";

        SveContentAcquisitionGraph.Requirement alternatives =
                SveContentAcquisitionGraph.Requirement.anyOf(
                        "tag:stardewcraftsve:test", List.of(second, first, first));
        expect(alternatives.alternatives().equals(List.of(first, second)),
                "Any-of alternatives are not stable, sorted, and distinct");
        expectUnsupported(alternatives.alternatives()::clear,
                "Any-of alternatives are mutable");

        SveContentAcquisitionGraph graph = new SveContentAcquisitionGraph();
        graph.addSource(exact, "test", "exact source");
        graph.addRouteWithRequirements(output, "test", "and plus any-of", List.of(
                SveContentAcquisitionGraph.Requirement.exact(exact), alternatives));
        graph.addRouteWithRequirements(emptyOutput, "test", "empty selector", List.of(
                SveContentAcquisitionGraph.Requirement.anyOf(
                        "tag:stardewcraftsve:empty", List.of())));
        graph.addRouteWithRequirements(externalOutput, "test", "external alternative", List.of(
                SveContentAcquisitionGraph.Requirement.anyOf(
                        "tag:stardewcraftsve:mixed",
                        List.of(externalOptionalUnknown, "stardewcraft:egg_white"))));
        graph.addRoute(unknownExactOutput, "test", "unknown exact", List.of(unknown));
        graph.addRouteWithRequirements(allUnknownOutput, "test", "all unknown", List.of(
                SveContentAcquisitionGraph.Requirement.anyOf(
                        "tag:stardewcraftsve:unknowns",
                        List.of(unknown, otherUnknown))));
        graph.addRouteWithRequirements(mixedUnknownOutput, "test", "mixed registered", List.of(
                SveContentAcquisitionGraph.Requirement.anyOf(
                        "tag:stardewcraftsve:mixed_registered",
                        List.of(first, registeredOptionalUnknown))));

        Set<String> registered = Set.of(
                exact, first, second, output, emptyOutput, externalOutput,
                unknownExactOutput, allUnknownOutput, mixedUnknownOutput);
        SveContentAcquisitionGraph.Evaluation blocked = graph.evaluate(registered, Map.of());
        expect(!blocked.reachable().contains(output),
                "AND route ignored its unresolved any-of requirement");
        expect(blocked.blocked().getOrDefault(output, Set.of())
                        .equals(Set.of(first, second)),
                "Any-of route did not report its alternative candidates");
        expect(!blocked.reachable().contains(emptyOutput)
                        && blocked.blocked().containsKey(emptyOutput)
                        && blocked.blocked().get(emptyOutput).isEmpty(),
                "Empty selector became reachable or fabricated a mandatory item");
        expect(blocked.reachable().contains(externalOutput),
                "External any-of candidate was not treated as an acquisition boundary");
        expect(blocked.missingDependencies().contains(unknown)
                        && blocked.missingDependencies().contains(otherUnknown),
                "Exact or all-unknown requirements did not report missing dependencies");
        expect(!blocked.missingDependencies().contains(externalOptionalUnknown),
                "Unknown candidate beside an external alternative became mandatory");
        expect(blocked.blocked().containsKey(mixedUnknownOutput)
                        && !blocked.missingDependencies().contains(registeredOptionalUnknown),
                "Unknown candidate beside a registered alternative became mandatory");

        graph.addSource(first, "test", "first alternative source");
        SveContentAcquisitionGraph.Evaluation reachable = graph.evaluate(registered, Map.of());
        expect(reachable.reachable().contains(output),
                "One reachable any-of candidate did not satisfy the route");
    }

    private static void validateTypedRecipeUnlocks() {
        for (String raw : new String[]{
                null, "", "Fried_Egg", "stardewcraftsve:probe",
                "recipe:hashbrowns", "recipe:stardewcraftsve:probe",
                "recipe:recipe:hashbrowns", "bad id"}) {
            ResourceLocation expected = RecipeIdNormalizer.definitionId(raw);
            SveAcquisitionEntrypoints.RecipeKey actual =
                    SveAcquisitionEntrypoints.RecipeKey.tryCreate(
                            SveAcquisitionEntrypoints.RecipeKind.COOKING, raw);
            expect(java.util.Objects.equals(
                            expected == null ? null : expected.toString(),
                            actual == null ? null : actual.id()),
                    "RecipeKey normalization differs from StardewCraft for " + raw);
        }
        expect(new SveAcquisitionEntrypoints.RecipeKey(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, "Fried_Egg")
                        .id().equals("stardewcraft:fried_egg"),
                "Bare legacy recipe id did not lowercase into the StardewCraft namespace");
        expect(new SveAcquisitionEntrypoints.RecipeKey(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, "recipe:hashbrowns")
                        .id().equals("stardewcraft:hashbrowns"),
                "Single legacy recipe prefix was not removed");
        expect(new SveAcquisitionEntrypoints.RecipeKey(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING,
                        "recipe:stardewcraftsve:probe")
                        .id().equals("stardewcraftsve:probe"),
                "Namespaced recipe prefix was not normalized");
        expect(SveAcquisitionEntrypoints.RecipeKey.tryCreate(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, "bad id") == null,
                "Invalid recipe id was accepted");
        expect(SveAcquisitionEntrypoints.RecipeKey.tryCreate(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING,
                        "recipe:recipe:hashbrowns") == null,
                "Double recipe prefix was accepted");

        String sharedId = "stardewcraftsve:shared_recipe_probe";
        String cookingOutput = "stardewcraftsve:typed_cooking_output";
        String craftingOutput = "stardewcraftsve:typed_crafting_output";
        SveAcquisitionEntrypoints.Recipe cooking = recipe(cookingOutput);
        SveAcquisitionEntrypoints.Recipe crafting = recipe(craftingOutput);
        SveAcquisitionEntrypoints.RecipeKey cookingKey =
                new SveAcquisitionEntrypoints.RecipeKey(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, sharedId);

        SveAcquisitionEntrypoints.Shop ambiguousShop = new SveAcquisitionEntrypoints.Shop(
                "stardewcraftsve:ambiguous_shop",
                List.of(new SveAcquisitionEntrypoints.ShopEntry(
                        "recipe:" + sharedId, null)));
        SveAcquisitionEntrypoints collisionEntrypoints = recipeEntrypoints(
                Map.of(sharedId, cooking), Map.of(sharedId, crafting),
                Map.of(ambiguousShop.id(), ambiguousShop),
                List.of(new SveAcquisitionEntrypoints.Mail(
                        "stardewcraftsve:typed_mail", List.of(), cookingKey,
                        "test delivery", List.of())));
        SveContentAcquisitionService.Snapshot collision =
                SveContentAcquisitionService.inspect(Map.of(),
                        Set.of(cookingOutput, craftingOutput), collisionEntrypoints);
        expect(hasRoute(collision, cookingOutput, "cooking"),
                "Cooking mail did not unlock its typed cooking recipe");
        expect(!hasRoute(collision, craftingOutput, "crafting"),
                "Cooking mail leaked into a same-id crafting recipe");
        expect(collision.routes().values().stream().flatMap(List::stream)
                        .noneMatch(route -> route.detail().contains("ambiguous_shop")),
                "Ambiguous shop silently selected one recipe catalog");
        expect(collision.validationProblems().stream().anyMatch(problem ->
                        problem.contains(sharedId) && problem.contains("both cooking and crafting")),
                "Cross-catalog recipe collision was not diagnosed");
        expect(collision.validationProblems().stream().anyMatch(problem ->
                        problem.contains("ambiguous_shop") && problem.contains("ambiguous")),
                "Ambiguous shop recipe was not rejected");

        String craftingOnlyId = "stardewcraftsve:crafting_only_probe";
        SveAcquisitionEntrypoints.RecipeKey wrongCookingKey =
                new SveAcquisitionEntrypoints.RecipeKey(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, craftingOnlyId);
        SveContentAcquisitionService.Snapshot wrongMail =
                SveContentAcquisitionService.inspect(Map.of(), Set.of(craftingOutput),
                        recipeEntrypoints(Map.of(), Map.of(craftingOnlyId, crafting),
                                Map.of(), List.of(new SveAcquisitionEntrypoints.Mail(
                                        "stardewcraftsve:wrong_mail", List.of(), wrongCookingKey,
                                        "test delivery", List.of()))));
        expect(!hasRoute(wrongMail, craftingOutput, "crafting"),
                "Wrong recipeIsCooking value unlocked the opposite catalog");
        expect(wrongMail.validationProblems().stream().anyMatch(problem ->
                        problem.contains("wrong_mail") && problem.contains("opposite catalog")),
                "Wrong mail recipe kind was not diagnosed");

        String orphanId = "stardewcraftsve:orphan_recipe_probe";
        SveAcquisitionEntrypoints.RecipeKey orphanKey =
                new SveAcquisitionEntrypoints.RecipeKey(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, orphanId);
        SveContentAcquisitionService.Snapshot orphanMail =
                SveContentAcquisitionService.inspect(Map.of(), Set.of(cookingOutput),
                        recipeEntrypoints(Map.of(orphanId, cooking), Map.of(), Map.of(),
                                List.of(new SveAcquisitionEntrypoints.Mail(
                                        "stardewcraftsve:orphan_mail", List.of(), orphanKey,
                                        null, List.of()))));
        expect(!hasRoute(orphanMail, cookingOutput, "cooking"),
                "Orphan mail definition unlocked a recipe without a delivery trigger");
        expect(orphanMail.validationProblems().stream().anyMatch(problem ->
                        problem.contains("orphan_mail")
                                && problem.contains("no delivery trigger")),
                "Orphan SVE acquisition mail was not diagnosed");

        String cookingId = "stardewcraftsve:unique_cooking_probe";
        String craftingId = "stardewcraftsve:unique_crafting_probe";
        SveAcquisitionEntrypoints.Shop typedShop = new SveAcquisitionEntrypoints.Shop(
                "stardewcraftsve:typed_shop", List.of(
                new SveAcquisitionEntrypoints.ShopEntry("recipe:" + cookingId, null),
                new SveAcquisitionEntrypoints.ShopEntry("recipe:" + craftingId, null),
                new SveAcquisitionEntrypoints.ShopEntry(
                        "recipe:stardewcraftsve:missing_probe", null)));
        SveContentAcquisitionService.Snapshot typedShopSnapshot =
                SveContentAcquisitionService.inspect(Map.of(),
                        Set.of(cookingOutput, craftingOutput),
                        recipeEntrypoints(Map.of(cookingId, cooking),
                                Map.of(craftingId, crafting),
                                Map.of(typedShop.id(), typedShop), List.of()));
        expect(hasRoute(typedShopSnapshot, cookingOutput, "cooking"),
                "Shop did not infer a unique cooking recipe");
        expect(hasRoute(typedShopSnapshot, craftingOutput, "crafting"),
                "Shop did not infer a unique crafting recipe");
        expect(typedShopSnapshot.validationProblems().stream().anyMatch(problem ->
                        problem.contains("missing_probe")
                                && problem.contains("absent from both")),
                "Unknown SVE shop recipe was not diagnosed");
    }

    private static void validateArtisanSelectorPriority() {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                "stardewcraftsve", "selector_probe");
        ResourceLocation machineId = ResourceLocation.fromNamespaceAndPath(
                "stardewcraft", "probe_machine");
        ResourceLocation inputId = ResourceLocation.fromNamespaceAndPath(
                "stardewcraft", "wood");
        var inputTag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "probe_items"));
        ResourceLocation outputId = ResourceLocation.fromNamespaceAndPath(
                "stardewcraftsve", "probe_output");

        ArtisanRecipeDataManager.Recipe modeFirst = artisanRecipe(
                recipeId, machineId, inputId, inputTag,
                ArtisanRecipeDataManager.InputMode.CROP_TYPE, outputId);
        expect(StardewCraftAcquisitionSnapshotAdapter.artisanSelector(modeFirst)
                        .equals("input_mode:crop_type"),
                "Artisan selector did not preserve inputMode runtime priority");

        ArtisanRecipeDataManager.Recipe itemSecond = artisanRecipe(
                recipeId, machineId, inputId, inputTag,
                ArtisanRecipeDataManager.InputMode.DEFAULT, outputId);
        expect(StardewCraftAcquisitionSnapshotAdapter.artisanSelector(itemSecond)
                        .equals("item:stardewcraft:wood"),
                "Artisan selector did not prefer inputId over inputTag");

        ArtisanRecipeDataManager.Recipe tagLast = artisanRecipe(
                recipeId, machineId, null, inputTag,
                ArtisanRecipeDataManager.InputMode.DEFAULT, outputId);
        expect(StardewCraftAcquisitionSnapshotAdapter.artisanSelector(tagLast)
                        .equals("tag:c:probe_items"),
                "Artisan selector did not fall back to inputTag");
    }

    private static ArtisanRecipeDataManager.Recipe artisanRecipe(
            ResourceLocation id,
            ResourceLocation machine,
            ResourceLocation input,
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag,
            ArtisanRecipeDataManager.InputMode mode,
            ResourceLocation output
    ) {
        return new ArtisanRecipeDataManager.Recipe(
                id, machine, input, tag, mode, output,
                1, 10, 1, false, -1, PreserveType.JELLY,
                new ArtisanRecipeDataManager.SeedMakerRule(0.0, 0.0, 1, 1, 1, 1),
                ArtisanRecipeDataManager.OutputMode.FIXED);
    }

    private static SveAcquisitionEntrypoints.Recipe recipe(String output) {
        return new SveAcquisitionEntrypoints.Recipe(
                output,
                List.of(SveContentAcquisitionGraph.Requirement.exact(
                        "stardewcraft:wood")),
                List.of());
    }

    private static SveAcquisitionEntrypoints recipeEntrypoints(
            Map<String, SveAcquisitionEntrypoints.Recipe> cooking,
            Map<String, SveAcquisitionEntrypoints.Recipe> crafting,
            Map<String, SveAcquisitionEntrypoints.Shop> shops,
            List<SveAcquisitionEntrypoints.Mail> mail
    ) {
        return new SveAcquisitionEntrypoints(
                shops, Map.of(), Set.of(SveAcquisitionEntrypoints.Domain.values()),
                cooking, crafting, mail, List.of(), List.of());
    }

    private static void validateCandidateMailFieldNames() {
        List<String> problems = new java.util.ArrayList<>();
        SveAcquisitionEntrypoints.RecipeKey camel =
                SveContentDataScanner.readMailRecipeKey(JsonParser.parseString("""
                        {
                          "learnedRecipe": "stardewcraftsve:camel_mail",
                          "recipeIsCooking": true
                        }
                        """).getAsJsonObject(), "camel", problems);
        SveAcquisitionEntrypoints.RecipeKey snake =
                SveContentDataScanner.readMailRecipeKey(JsonParser.parseString("""
                        {
                          "learned_recipe": "stardewcraftsve:snake_mail",
                          "recipe_is_cooking": true
                        }
                        """).getAsJsonObject(), "snake", problems);
        SveAcquisitionEntrypoints.RecipeKey defaultCrafting =
                SveContentDataScanner.readMailRecipeKey(JsonParser.parseString("""
                        {"learned_recipe": "stardewcraftsve:default_mail"}
                        """).getAsJsonObject(), "default", problems);
        SveAcquisitionEntrypoints.RecipeKey conflict =
                SveContentDataScanner.readMailRecipeKey(JsonParser.parseString("""
                        {
                          "learned_recipe": "stardewcraftsve:canonical_mail",
                          "learnedRecipe": "stardewcraftsve:legacy_mail",
                          "recipe_is_cooking": false,
                          "recipeIsCooking": true
                        }
                        """).getAsJsonObject(), "conflict", problems);

        expect(camel != null && camel.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING
                        && camel.id().endsWith(":camel_mail"),
                "Camel-case mail fields lost their cooking recipe kind");
        expect(snake != null && snake.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING
                        && snake.id().endsWith(":snake_mail"),
                "Snake-case mail fields lost their cooking recipe kind");
        expect(defaultCrafting != null
                        && defaultCrafting.kind()
                        == SveAcquisitionEntrypoints.RecipeKind.CRAFTING,
                "Missing recipe_is_cooking did not default to crafting");
        expect(conflict != null
                        && conflict.kind() == SveAcquisitionEntrypoints.RecipeKind.CRAFTING
                        && conflict.id().endsWith(":canonical_mail"),
                "Canonical mail fields did not override conflicting legacy aliases");
        expect(problems.stream().filter(problem -> problem.contains("conflict")).count() == 2,
                "Conflicting mail aliases were not diagnosed independently");

        SveAcquisitionEntrypoints.RecipeKey liveAdapter =
                StardewCraftAcquisitionSnapshotAdapter.toRecipeKey(
                        "stardewcraftsve:adapter_mail", true);
        expect(liveAdapter != null
                        && liveAdapter.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING,
                "Live mail adapter dropped recipeIsCooking");
    }

    private static void validateExcludedDependenciesDoNotUnlock() {
        String excludedItem = "stardewcraftsve:test_excluded_source";
        String downstreamItem = "stardewcraftsve:test_excluded_downstream";
        SveContentAcquisitionGraph graph = new SveContentAcquisitionGraph();
        graph.addSource(excludedItem, "test", "excluded source");
        graph.addRoute(downstreamItem, "test", "depends on excluded source", List.of(excludedItem));

        Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions = Map.of(
                excludedItem,
                new SveContentAcquisitionCatalog.Exclusion(
                        SveContentAcquisitionCatalog.ExclusionType.PLANNED_CONTENT,
                        "test exclusion"));
        SveContentAcquisitionGraph.Evaluation evaluation = graph.evaluate(
                Set.of(excludedItem, downstreamItem), exclusions);

        if (evaluation.reachable().contains(excludedItem)
                || evaluation.reachable().contains(downstreamItem)) {
            throw new AssertionError("Excluded acquisition source unlocked a survival route");
        }
        if (!evaluation.staleExclusions().contains(excludedItem)) {
            throw new AssertionError("Raw route for excluded item was not reported as stale");
        }
        if (!evaluation.blocked().getOrDefault(downstreamItem, Set.of()).contains(excludedItem)) {
            throw new AssertionError("Downstream route did not report its excluded prerequisite");
        }
    }

    private static void validateHost052FishingRuleSemantics() {
        JsonObject pool = JsonParser.parseString("""
                {
                  "location": "stardewcraftsve:sve_fish",
                  "fish": [
                    {
                      "id": "random",
                      "item": "stardewcraftsve:puppyfish",
                      "randomItems": [
                        "stardewcraftsve:king_salmon",
                        "stardewcraftsve:tadpole"
                      ],
                      "chance": 1.0,
                      "ignoreFishDataRequirements": true,
                      "biomeTags": ["#stardewcraft:is_beach"]
                    },
                    {
                      "id": "luck_only",
                      "item": "stardewcraftsve:puppyfish",
                      "chance": 0.0,
                      "chanceBoostPerLuckLevel": 0.05,
                      "ignoreFishDataRequirements": true,
                      "biomeTags": ["#stardewcraft:is_beach"]
                    },
                    {
                      "id": "ginger_only",
                      "item": "stardewcraftsve:baby_lunaloo",
                      "chance": 1.0,
                      "ignoreFishDataRequirements": true,
                      "biomeTags": ["#stardewcraft:is_ginger_island_ocean"]
                    },
                    {
                      "id": "ginger_or_beach",
                      "item": "stardewcraftsve:starfish",
                      "chance": 1.0,
                      "ignoreFishDataRequirements": true,
                      "biomeTags": [
                        "#stardewcraft:is_ginger_island_ocean",
                        "#stardewcraft:is_beach"
                      ]
                    },
                    {
                      "id": "mine_100_only",
                      "item": "stardewcraftsve:void_eel",
                      "chance": 1.0,
                      "ignoreFishDataRequirements": true,
                      "biomeTags": ["#stardewcraft:is_mines_100"]
                    },
                    {
                      "id": "mine_60_or_100",
                      "item": "stardewcraftsve:void_eel",
                      "chance": 1.0,
                      "ignoreFishDataRequirements": true,
                      "biomeTags": [
                        "#stardewcraft:is_mines_60",
                        "#stardewcraft:is_mines_100"
                      ]
                    }
                  ]
                }
                """).getAsJsonObject();
        SveAcquisitionEntrypoints.FishingLocation location =
                SveAcquisitionEntrypoints.fromResources(Map.of(
                        "stardewcraftsve:fishing/locations/host_052_probe.json", pool))
                        .fishingLocations().get("stardewcraftsve:sve_fish");
        expect(location != null, "0.5.2 fishing probe pool was not parsed");

        Set<String> randomOutputs = location.rules().stream()
                .filter(rule -> "random".equals(rule.id()))
                .map(SveAcquisitionEntrypoints.FishingRule::itemId)
                .collect(java.util.stream.Collectors.toSet());
        expect(randomOutputs.equals(Set.of(
                        "stardewcraftsve:king_salmon", "stardewcraftsve:tadpole")),
                "randomItems did not replace the rule's primary item: " + randomOutputs);
        List<SveAcquisitionEntrypoints.FishingRule> luckOnly = fishingRules(
                location, "luck_only");
        expect(luckOnly.stream().allMatch(
                        SveAcquisitionEntrypoints.FishingRule::isAcquisitionSource),
                "positive chanceBoostPerLuckLevel did not make a zero-base rule reachable");
        List<SveAcquisitionEntrypoints.FishingRule> gingerOnly = fishingRules(
                location, "ginger_only");
        expect(gingerOnly.stream().noneMatch(
                        SveAcquisitionEntrypoints.FishingRule::isAcquisitionSource),
                "registered but inaccessible Ginger Island ocean became an acquisition source");
        List<SveAcquisitionEntrypoints.FishingRule> gingerOrBeach = fishingRules(
                location, "ginger_or_beach");
        expect(gingerOrBeach.stream().allMatch(
                        SveAcquisitionEntrypoints.FishingRule::isAcquisitionSource),
                "an accessible beach alternative was hidden with the Ginger Island route");
        List<SveAcquisitionEntrypoints.FishingRule> mine100Only = fishingRules(
                location, "mine_100_only");
        expect(mine100Only.stream().noneMatch(
                        SveAcquisitionEntrypoints.FishingRule::isAcquisitionSource),
                "the 0.5.2 mines-100 early override leaked into location-pool acquisition");
        List<SveAcquisitionEntrypoints.FishingRule> mine60Or100 = fishingRules(
                location, "mine_60_or_100");
        expect(mine60Or100.stream().allMatch(
                        SveAcquisitionEntrypoints.FishingRule::isAcquisitionSource),
                "a non-preempted mines-60 alternative was lost");

        SpawnFishRule acceptedRule = SpawnFishRule.fromJson(
                pool.getAsJsonArray("fish").get(0).getAsJsonObject());
        expect(Set.copyOf(StardewCraftAcquisitionSnapshotAdapter.fishingRuleOutputs(
                        acceptedRule)).equals(randomOutputs),
                "live 0.5.2 adapter and candidate randomItems semantics diverged");
        expect(SveAcquisitionEntrypoints.hasPositiveBaselineCatchProbability(
                        0.0f, 0.05f, 1.0f, 0, 0, -1, 4, 0.0f, true),
                "positive luck-level boost remained unreachable");
        expect(!SveAcquisitionEntrypoints.hasPositiveBaselineCatchProbability(
                        0.0f, -0.05f, 1.0f, 0, 0, -1, 4, 0.0f, true),
                "negative luck-level boost created a catch route");

        Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions =
                SveContentAcquisitionCatalog.exclusions();
        for (String path : List.of(
                "baby_lunaloo", "barred_knifejaw", "blue_tang", "clownfish",
                "ocean_sunfish", "seahorse", "shark", "viper_eel")) {
            expect(exclusions.containsKey("stardewcraftsve:" + path),
                    "Ginger Island-only fish lacks an explicit exclusion: " + path);
            expect(exclusions.containsKey("stardewcraftsve:smoked_" + path),
                    "Ginger Island-only smoked fish lacks an explicit exclusion: " + path);
        }
        expect(!exclusions.containsKey("stardewcraftsve:lunaloo")
                        && !exclusions.containsKey("stardewcraftsve:shiny_lunaloo"),
                "Moonlight Jellies catches were incorrectly locked with Ginger Island");
        for (String path : List.of(
                "shark_tooth", "fireworks_red", "fireworks_purple", "fireworks_green")) {
            expect(exclusions.containsKey("stardewcraftsve:" + path),
                    "Ginger Island-locked fish pond output lacks an exclusion: " + path);
        }
        expect(!exclusions.containsKey("stardewcraftsve:red_slime_egg"),
                "Shiny Lunaloo's reachable red slime egg output was incorrectly excluded");
    }

    private static List<SveAcquisitionEntrypoints.FishingRule> fishingRules(
            SveAcquisitionEntrypoints.FishingLocation location,
            String id
    ) {
        List<SveAcquisitionEntrypoints.FishingRule> rules = location.rules().stream()
                .filter(rule -> id.equals(rule.id()))
                .toList();
        expect(!rules.isEmpty(), "Fishing probe rule was not parsed: " + id);
        return rules;
    }

    private record CandidateLimitations(Set<String> outputs, Set<String> problems) {
        private CandidateLimitations {
            outputs = Set.copyOf(outputs);
            problems = Set.copyOf(problems);
        }
    }

}
