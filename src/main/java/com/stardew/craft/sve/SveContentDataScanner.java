package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Extracts acquisition roots and transformations from effective data-pack JSON. */
public final class SveContentDataScanner {
    private static final String SVE_PREFIX = StardewcraftsveMod.MODID + ":";

    private SveContentDataScanner() {
    }

    public static List<String> scan(
            Map<String, JsonElement> resources,
            SveContentAcquisitionGraph graph
    ) {
        return scan(resources, graph, SveAcquisitionEntrypoints.fromResources(resources));
    }

    static List<String> scan(
            Map<String, JsonElement> resources,
            SveContentAcquisitionGraph graph,
            SveAcquisitionEntrypoints entrypoints
    ) {
        List<String> problems = new ArrayList<>();
        Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes =
                new LinkedHashMap<>(entrypoints.cookingRecipes());
        Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes =
                new LinkedHashMap<>(entrypoints.craftingRecipes());
        Map<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> recipeUnlocks =
                new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> resource : resources.entrySet()) {
            if (!resource.getValue().isJsonObject()) continue;
            String path = path(resource.getKey());
            if (!entrypoints.covers(SveAcquisitionEntrypoints.Domain.COOKING)
                    && path.startsWith("cooking/recipes/") && path.endsWith(".json")) {
                readRecipe(resource, "cooking/recipes/",
                        SveAcquisitionEntrypoints.RecipeKind.COOKING,
                        cookingRecipes, problems);
            } else if (!entrypoints.covers(SveAcquisitionEntrypoints.Domain.CRAFTING)
                    && path.startsWith("player/crafting_recipes/") && path.endsWith(".json")) {
                readRecipe(resource, "player/crafting_recipes/",
                        SveAcquisitionEntrypoints.RecipeKind.CRAFTING,
                        craftingRecipes, problems);
            }
        }

        Set<String> recipeCollisions = new LinkedHashSet<>(cookingRecipes.keySet());
        recipeCollisions.retainAll(craftingRecipes.keySet());
        recipeCollisions.stream()
                .filter(id -> id.startsWith(SVE_PREFIX)
                        || cookingRecipes.get(id).output().startsWith(SVE_PREFIX)
                        || craftingRecipes.get(id).output().startsWith(SVE_PREFIX))
                .sorted().forEach(id -> problems.add(
                "Recipe id " + id + " exists in both cooking and crafting catalogs; "
                        + "StardewCraft stores unlocks without kind"));

        SveContentAcquisitionCatalog.friendshipRecipeUnlocks().forEach((recipe, detail) ->
                addRecipeUnlock(recipeUnlocks, recipe, detail, List.of()));

        entrypoints.shops().values().forEach(shop ->
                scanShop(shop, graph, recipeUnlocks,
                        cookingRecipes, craftingRecipes, problems));

        for (Map.Entry<String, JsonElement> resource : resources.entrySet()) {
            JsonElement value = resource.getValue();
            if (!value.isJsonObject() && !value.isJsonArray()) continue;
            String path = path(resource.getKey());
            if (!entrypoints.covers(SveAcquisitionEntrypoints.Domain.MAIL)
                    && path.startsWith("mail/") && value.isJsonArray()) {
                scanMail(resource.getKey(), value.getAsJsonArray(), graph,
                        recipeUnlocks, cookingRecipes, craftingRecipes, problems);
            } else if (!entrypoints.covers(
                    SveAcquisitionEntrypoints.Domain.FISHING_TREASURE)
                    && path.startsWith("fishing/treasure_pools/")
                    && path.endsWith(".json") && value.isJsonObject()) {
                scanFishingTreasurePool(
                        resource.getKey(), value.getAsJsonObject(), graph, problems);
            } else if (!entrypoints.covers(SveAcquisitionEntrypoints.Domain.FORAGE)
                    && path.startsWith("forage_zones/") && value.isJsonObject()) {
                scanForageZone(resource.getKey(), value.getAsJsonObject(), graph);
            } else if (!entrypoints.covers(SveAcquisitionEntrypoints.Domain.ARTISAN)
                    && path.startsWith("artisan/") && value.isJsonObject()) {
                scanArtisan(resource.getKey(), value.getAsJsonObject(), graph);
            }
        }

        if (!entrypoints.covers(SveAcquisitionEntrypoints.Domain.FISH_POND)) {
            scanCandidateFishPonds(resources, graph, problems);
        }

        entrypoints.mail().forEach(letter -> scanMail(letter, graph, recipeUnlocks));
        entrypoints.routes().forEach(route -> graph.addRouteWithRequirements(
                route.output(), route.kind(), route.detail(), route.requirements()));

        entrypoints.fishingLocations().values().forEach(location ->
                scanFishingLocation(location, graph));

        validateRecipeUnlocks(recipeUnlocks, cookingRecipes, craftingRecipes, problems);
        craftingRecipes.forEach((id, recipe) -> {
            SveAcquisitionEntrypoints.RecipeKey key = new SveAcquisitionEntrypoints.RecipeKey(
                    SveAcquisitionEntrypoints.RecipeKind.CRAFTING, id);
            addRecipeRoutes(graph, key, recipe, recipeUnlocks.get(key));
        });
        cookingRecipes.forEach((id, recipe) -> {
            SveAcquisitionEntrypoints.RecipeKey key = new SveAcquisitionEntrypoints.RecipeKey(
                    SveAcquisitionEntrypoints.RecipeKind.COOKING, id);
            addRecipeRoutes(graph, key, recipe, recipeUnlocks.get(key));
        });
        return problems.stream().distinct().toList();
    }

    private static void readRecipe(
            Map.Entry<String, JsonElement> resource,
            String prefix,
            SveAcquisitionEntrypoints.RecipeKind kind,
            Map<String, SveAcquisitionEntrypoints.Recipe> target,
            List<String> problems
    ) {
        JsonObject object = resource.getValue().getAsJsonObject();
        String output = string(object, "output");
        if (output == null) return;
        String resourcePath = path(resource.getKey());
        String recipePath = resourcePath.substring(
                prefix.length(), resourcePath.length() - ".json".length());
        String recipeId = namespace(resource.getKey()) + ":" + recipePath;
        List<SveContentAcquisitionGraph.Requirement> ingredients = new ArrayList<>();
        JsonArray array = array(object, "ingredients");
        if (array != null) {
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                ingredients.add(readCandidateRequirement(
                        recipeId, element.getAsJsonObject(), problems));
            }
        }
        List<String> intrinsicUnlocks = new ArrayList<>();
        if (kind == SveAcquisitionEntrypoints.RecipeKind.CRAFTING) {
            String legacyCondition = string(object, "legacy_unlock_condition");
            JsonArray unlockWhen = array(object, "unlock_when");
            if (unlockWhen != null && !unlockWhen.isEmpty()) {
                intrinsicUnlocks.add("intrinsic conditions");
            } else if (SveAcquisitionEntrypoints.isAutomaticLegacyUnlock(legacyCondition)) {
                intrinsicUnlocks.add("intrinsic condition " + legacyCondition.trim());
            }
        }
        target.put(recipeId,
                new SveAcquisitionEntrypoints.Recipe(output, ingredients, intrinsicUnlocks));
    }

    private static SveContentAcquisitionGraph.Requirement readCandidateRequirement(
            String recipeId,
            JsonObject ingredient,
            List<String> problems
    ) {
        String item = string(ingredient, "item");
        if (item != null && !item.isBlank()) {
            return SveContentAcquisitionGraph.Requirement.exact(item);
        }

        String tag = string(ingredient, "tag");
        String selector;
        if (tag != null && !tag.isBlank()) {
            selector = "tag:" + tag.trim();
        } else {
            JsonArray categories = array(ingredient, "categories");
            if (categories != null && !categories.isEmpty()) {
                List<String> values = new ArrayList<>();
                for (JsonElement category : categories) {
                    if (category.isJsonPrimitive()) values.add(category.getAsString());
                }
                selector = "categories:" + String.join(",", values);
            } else {
                selector = "invalid ingredient";
            }
        }
        problems.add("Candidate recipe " + recipeId + " selector " + selector
                + " requires the live item registry to resolve");
        return SveContentAcquisitionGraph.Requirement.anyOf(selector, List.of());
    }

    private static void scanShop(
            SveAcquisitionEntrypoints.Shop shop,
            SveContentAcquisitionGraph graph,
            Map<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> recipeUnlocks,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes,
            List<String> problems
    ) {
        for (SveAcquisitionEntrypoints.ShopEntry entry : shop.entries()) {
            String item = entry.itemId();
            if (item.startsWith("recipe:")) {
                String recipeId = item.substring("recipe:".length());
                SveAcquisitionEntrypoints.RecipeKey recipe = resolveShopRecipe(
                        shop.id(), recipeId, cookingRecipes, craftingRecipes, problems);
                if (recipe == null) continue;
                List<String> prerequisites = entry.tradeItemId() == null
                        ? List.of() : List.of(entry.tradeItemId());
                addRecipeUnlock(recipeUnlocks, recipe,
                        "shop " + shop.id(), prerequisites);
                continue;
            }
            List<String> prerequisites = new ArrayList<>();
            String tradeItem = entry.tradeItemId();
            if (tradeItem != null) prerequisites.add(tradeItem);
            graph.addRoute(item, "shop", shop.id(), prerequisites);
        }
    }

    private static void scanMail(
            String resourceId,
            JsonArray letters,
            SveContentAcquisitionGraph graph,
            Map<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> recipeUnlocks,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes,
            List<String> problems
    ) {
        for (JsonElement element : letters) {
            if (!element.isJsonObject()) continue;
            JsonObject letter = element.getAsJsonObject();
            String mailId = string(letter, "id");
            String detail = resourceId + (mailId == null ? "" : "#" + mailId);
            String canonicalMailId = mailDefinitionId(resourceId, mailId);
            reportAliasConflict(letter, "attached_items", "attachedItems", detail, problems);

            JsonArray attached = firstArray(letter, "attached_items", "attachedItems");
            List<String> attachedItems = new ArrayList<>();
            if (attached != null) {
                for (JsonElement attachedElement : attached) {
                    if (!attachedElement.isJsonObject()) continue;
                    String item = firstString(
                            attachedElement.getAsJsonObject(), "item", "id");
                    if (item != null) attachedItems.add(item);
                }
            }

            SveAcquisitionEntrypoints.RecipeKey learnedRecipe =
                    readMailRecipeKey(letter, detail, problems);
            SveContentAcquisitionCatalog.MailTrigger trigger = canonicalMailId == null
                    ? null : SveContentAcquisitionCatalog.mailTriggers().get(canonicalMailId);
            boolean hasSveAcquisition = attachedItems.stream()
                    .anyMatch(item -> item.startsWith(SVE_PREFIX))
                    || isSveRecipeOutput(
                    learnedRecipe, cookingRecipes, craftingRecipes);
            if (trigger == null) {
                if (hasSveAcquisition) {
                    problems.add("Mail " + (canonicalMailId == null ? detail : canonicalMailId)
                            + " defines SVE acquisition but has no delivery trigger");
                }
                continue;
            }

            String routeDetail = "mail-open " + canonicalMailId + " via " + trigger.detail();
            attachedItems.forEach(item -> graph.addRoute(
                    item, "mail", routeDetail, trigger.prerequisites()));
            if (learnedRecipe != null) addRecipeUnlock(
                    recipeUnlocks, learnedRecipe, routeDetail, trigger.prerequisites());
        }
    }

    private static boolean isSveRecipeOutput(
            SveAcquisitionEntrypoints.RecipeKey key,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes
    ) {
        if (key == null) return false;
        SveAcquisitionEntrypoints.Recipe recipe =
                (key.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING
                        ? cookingRecipes : craftingRecipes).get(key.id());
        return recipe != null && recipe.output().startsWith(SVE_PREFIX);
    }

    static SveAcquisitionEntrypoints.RecipeKey readMailRecipeKey(
            JsonObject letter,
            String detail,
            List<String> problems
    ) {
        reportAliasConflict(letter, "learned_recipe", "learnedRecipe", detail, problems);
        reportAliasConflict(letter, "recipe_is_cooking", "recipeIsCooking", detail, problems);
        String learnedRecipe = firstString(
                letter, "learned_recipe", "learnedRecipe");
        if (learnedRecipe == null) return null;
        SveAcquisitionEntrypoints.RecipeKind kind = booleanValue(
                letter, "recipe_is_cooking", "recipeIsCooking")
                ? SveAcquisitionEntrypoints.RecipeKind.COOKING
                : SveAcquisitionEntrypoints.RecipeKind.CRAFTING;
        SveAcquisitionEntrypoints.RecipeKey key =
                SveAcquisitionEntrypoints.RecipeKey.tryCreate(kind, learnedRecipe);
        if (key == null) {
            problems.add("Mail " + detail
                    + " has invalid learned recipe id " + learnedRecipe);
        }
        return key;
    }

    private static void addRecipeRoutes(
            SveContentAcquisitionGraph graph,
            SveAcquisitionEntrypoints.RecipeKey key,
            SveAcquisitionEntrypoints.Recipe recipe,
            List<RecipeUnlock> externalUnlocks
    ) {
        List<RecipeUnlock> unlocks = new ArrayList<>();
        recipe.intrinsicUnlocks().forEach(detail ->
                unlocks.add(new RecipeUnlock(detail, List.of())));
        if (externalUnlocks != null) unlocks.addAll(externalUnlocks);
        for (RecipeUnlock unlock : unlocks) {
            List<SveContentAcquisitionGraph.Requirement> requirements =
                    new ArrayList<>(recipe.ingredients());
            unlock.prerequisites().stream()
                    .map(SveContentAcquisitionGraph.Requirement::exact)
                    .forEach(requirements::add);
            graph.addRouteWithRequirements(recipe.output(), key.kind().routeKind(),
                    key.id() + " via " + unlock.detail(), requirements);
        }
    }

    private static void scanMail(
            SveAcquisitionEntrypoints.Mail letter,
            SveContentAcquisitionGraph graph,
            Map<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> recipeUnlocks
    ) {
        if (!letter.isDeliverable()) return;
        String detail = "mail-open " + letter.id() + " via " + letter.deliveryDetail();
        letter.attachedItems().forEach(item -> graph.addRoute(
                item, "mail", detail, letter.deliveryPrerequisites()));
        if (letter.learnedRecipe() != null) {
            addRecipeUnlock(recipeUnlocks, letter.learnedRecipe(),
                    detail, letter.deliveryPrerequisites());
        }
    }

    private static void addRecipeUnlock(
            Map<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> recipeUnlocks,
            SveAcquisitionEntrypoints.RecipeKey recipe,
            String detail,
            List<String> prerequisites
    ) {
        recipeUnlocks.computeIfAbsent(recipe, ignored -> new ArrayList<>())
                .add(new RecipeUnlock(detail, prerequisites));
    }

    private static SveAcquisitionEntrypoints.RecipeKey resolveShopRecipe(
            String shopId,
            String rawId,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes,
            List<String> problems
    ) {
        SveAcquisitionEntrypoints.RecipeKey cooking =
                SveAcquisitionEntrypoints.RecipeKey.tryCreate(
                        SveAcquisitionEntrypoints.RecipeKind.COOKING, rawId);
        if (cooking == null) {
            problems.add("Shop " + shopId + " has invalid recipe id " + rawId);
            return null;
        }
        boolean isCooking = cookingRecipes.containsKey(cooking.id());
        boolean isCrafting = craftingRecipes.containsKey(cooking.id());
        if (isCooking == isCrafting) {
            if (!isCooking && !cooking.id().startsWith(SVE_PREFIX)) return null;
            problems.add("Shop " + shopId + " recipe " + cooking.id()
                    + (isCooking ? " is ambiguous between cooking and crafting"
                    : " is absent from both recipe catalogs"));
            return null;
        }
        return new SveAcquisitionEntrypoints.RecipeKey(
                isCooking ? SveAcquisitionEntrypoints.RecipeKind.COOKING
                        : SveAcquisitionEntrypoints.RecipeKind.CRAFTING,
                cooking.id());
    }

    private static void validateRecipeUnlocks(
            Map<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> recipeUnlocks,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes,
            List<String> problems
    ) {
        for (Map.Entry<SveAcquisitionEntrypoints.RecipeKey, List<RecipeUnlock>> entry
                : recipeUnlocks.entrySet()) {
            SveAcquisitionEntrypoints.RecipeKey key = entry.getKey();
            Map<String, SveAcquisitionEntrypoints.Recipe> expected =
                    key.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING
                            ? cookingRecipes : craftingRecipes;
            if (expected.containsKey(key.id())) continue;
            Map<String, SveAcquisitionEntrypoints.Recipe> opposite =
                    key.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING
                            ? craftingRecipes : cookingRecipes;
            String detail = entry.getValue().isEmpty()
                    ? "unknown source" : entry.getValue().getFirst().detail();
            if (opposite.containsKey(key.id())) {
                problems.add("Recipe unlock " + detail + " declares "
                        + key.kind().routeKind() + " recipe " + key.id()
                        + " but it exists only in the opposite catalog");
            } else {
                problems.add("Recipe unlock " + detail + " references unknown "
                        + key.kind().routeKind() + " recipe " + key.id());
            }
        }
    }

    private static void scanFishingLocation(
            SveAcquisitionEntrypoints.FishingLocation location,
            SveContentAcquisitionGraph graph
    ) {
        for (SveAcquisitionEntrypoints.FishingRule rule : location.rules()) {
            if (!rule.isAcquisitionSource()) continue;
            graph.addSource(rule.itemId(), "fishing",
                    location.key() + "#" + rule.id());
        }
    }

    private static void scanFishingTreasurePool(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph,
            List<String> problems
    ) {
        float chance = decimal(object, "chance", 1.0f);
        if (!Float.isFinite(chance) || chance <= 0.0f
                || integer(object, "rolls", 1) <= 0) {
            return;
        }
        JsonArray entries = array(object, "entries");
        if (entries == null) return;
        String detail = fishingTreasurePoolId(resourceId);
        for (int index = 0; index < entries.size(); index++) {
            JsonElement element = entries.get(index);
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            if (integer(entry, "weight", 1) <= 0
                    || !SveAcquisitionEntrypoints.hasReachableTreasureEligibility(
                    integer(entry, "min_fishing_level", 0),
                    integer(entry, "max_fishing_level", 100),
                    integer(entry, "min_water_distance", 0),
                    integer(entry, "max_water_distance", 5))) {
                continue;
            }
            String item = SveAcquisitionEntrypoints.directTreasureItem(entry.get("query"));
            if (item != null) {
                graph.addSource(item, "fishing_treasure", detail + "#" + index);
            } else if (StardewcraftsveMod.MODID.equals(namespace(resourceId))) {
                problems.add("Fishing treasure pool " + detail + " entry " + index
                        + " has no enumerable direct item query");
            }
        }
    }

    private static String fishingTreasurePoolId(String resourceId) {
        String resourcePath = path(resourceId);
        String prefix = "fishing/treasure_pools/";
        String poolPath = resourcePath.substring(
                prefix.length(), resourcePath.length() - ".json".length());
        return namespace(resourceId) + ":" + poolPath;
    }

    private static void scanForageZone(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        JsonArray entries = array(object, "entries");
        if (entries == null) return;
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            String block = string(element.getAsJsonObject(), "block");
            if (block == null) continue;
            int separator = block.indexOf(':');
            if (separator < 0) continue;
            String itemPath = block.substring(separator + 1);
            if (!itemPath.startsWith("forage_")) continue;
            graph.addSource(block.substring(0, separator + 1) + itemPath.substring("forage_".length()),
                    "forage", resourceId);
        }
    }

    private static void scanFishPond(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph,
            List<String> problems
    ) {
        String fish = string(object, "fish");
        JsonArray productions = array(object, "produced_items");
        if (fish == null || productions == null) return;
        ResourceLocation fishId = ResourceLocation.tryParse(fish);
        float baseMinimum = decimal(object, "base_min_produce_chance", 0.15f);
        float baseMaximum = decimal(object, "base_max_produce_chance", 0.95f);
        if (fishId == null || !Float.isFinite(baseMinimum)
                || !Float.isFinite(baseMaximum)
                || baseMinimum < 0.0f || baseMaximum > 1.0f
                || baseMaximum < baseMinimum || baseMaximum <= 0.0f) {
            return;
        }
        int configuredMaximum = integer(object, "max_population", -1);
        int maximumPopulation = configuredMaximum > 0
                ? configuredMaximum : 10;
        Map<Integer, List<String>> gates = readCandidateFishPondGates(
                resourceId, object, problems);
        Map<Integer, Integer> selectable = selectableCandidateFishPondProductions(
                productions, baseMinimum, baseMaximum, maximumPopulation);
        for (Map.Entry<Integer, Integer> selection : selectable.entrySet()) {
            int index = selection.getKey();
            JsonObject production = productions.get(index).getAsJsonObject();
            String item = string(production, "item");
            ResourceLocation outputId = item == null ? null : ResourceLocation.tryParse(item);
            int minimumCount = integer(production, "min_count", 1);
            int maximumCount = integer(production, "max_count", 1);
            if (outputId == null || minimumCount <= 0 || maximumCount < minimumCount) {
                continue;
            }

            List<SveContentAcquisitionGraph.Requirement> requirements = new ArrayList<>();
            requirements.add(SveContentAcquisitionGraph.Requirement.exact(fishId.toString()));
            if (configuredMaximum <= 0) {
                gates.entrySet().stream()
                        .filter(gate -> gate.getKey() > 0
                                && gate.getKey() <= selection.getValue())
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(gate -> requirements.add(
                                SveContentAcquisitionGraph.Requirement.anyOf(
                                        "fish_pond_gate:" + fishId + "@" + gate.getKey(),
                                        gate.getValue())));
            }
            graph.addRouteWithRequirements(outputId.toString(), "fish_pond",
                    resourceId + "#" + index, requirements);
        }
    }

    private static void scanCandidateFishPonds(
            Map<String, JsonElement> resources,
            SveContentAcquisitionGraph graph,
            List<String> problems
    ) {
        Map<String, Map.Entry<String, JsonElement>> winnerByFish = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .filter(resource -> path(resource.getKey())
                        .startsWith("fishpond/pond_data/"))
                .filter(resource -> path(resource.getKey()).endsWith(".json"))
                .filter(resource -> resource.getValue().isJsonObject())
                .forEach(resource -> {
                    String fish = string(resource.getValue().getAsJsonObject(), "fish");
                    if (fish != null && ResourceLocation.tryParse(fish) != null) {
                        winnerByFish.putIfAbsent(fish, resource);
                    }
                });
        winnerByFish.values().forEach(resource -> scanFishPond(
                resource.getKey(), resource.getValue().getAsJsonObject(), graph, problems));
    }

    private static Map<Integer, Integer> selectableCandidateFishPondProductions(
            JsonArray productions,
            float baseMinimum,
            float baseMaximum,
            int maximumPopulation
    ) {
        Map<Integer, Integer> firstPopulation = new LinkedHashMap<>();
        for (int population = 1; population <= maximumPopulation; population++) {
            double dailyChance = baseMinimum == baseMaximum
                    ? baseMinimum
                    : baseMinimum + (baseMaximum - baseMinimum) * (population / 10.0D);
            if (dailyChance <= 0.0D) continue;
            Set<Integer> selected = new LinkedHashSet<>();
            selected.add(-1);
            for (int index = 0; index < productions.size(); index++) {
                JsonElement element = productions.get(index);
                if (!element.isJsonObject()) continue;
                JsonObject candidate = element.getAsJsonObject();
                float chance = decimal(candidate, "chance", 0.0f);
                int requiredPopulation = integer(candidate, "required_population", 0);
                if (requiredPopulation < 0 || population < requiredPopulation
                        || !Float.isFinite(chance) || chance <= 0.0f || chance > 1.0f) {
                    continue;
                }
                int candidatePrecedence = integer(candidate, "precedence", 0);
                Set<Integer> next = new LinkedHashSet<>();
                for (int current : selected) {
                    if (current >= 0) {
                        JsonObject currentProduction = productions.get(current).getAsJsonObject();
                        if (integer(currentProduction, "precedence", 0)
                                <= candidatePrecedence) {
                            next.add(current);
                            continue;
                        }
                    }
                    if (chance < 1.0f) next.add(current);
                    next.add(index);
                }
                selected = next;
            }
            for (int index : selected) {
                if (index >= 0) firstPopulation.putIfAbsent(index, population);
            }
        }
        return java.util.Collections.unmodifiableMap(
                new LinkedHashMap<>(firstPopulation));
    }

    private static Map<Integer, List<String>> readCandidateFishPondGates(
            String resourceId,
            JsonObject object,
            List<String> problems
    ) {
        JsonElement gatesElement = object.get("population_gates");
        if (gatesElement == null || gatesElement.isJsonNull()) return Map.of();
        if (!gatesElement.isJsonObject()) {
            problems.add("Fish pond " + resourceId + " has invalid population_gates");
            return Map.of();
        }
        Map<Integer, List<String>> gates = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> gate : gatesElement.getAsJsonObject().entrySet()) {
            int population;
            try {
                population = Integer.parseInt(gate.getKey());
            } catch (NumberFormatException exception) {
                problems.add("Fish pond " + resourceId
                        + " has non-numeric population gate " + gate.getKey());
                continue;
            }
            List<String> alternatives = new ArrayList<>();
            if (gate.getValue().isJsonArray()) {
                for (JsonElement candidate : gate.getValue().getAsJsonArray()) {
                    if (!candidate.isJsonObject()) continue;
                    JsonObject request = candidate.getAsJsonObject();
                    String item = string(request, "item");
                    ResourceLocation itemId = item == null
                            ? null : ResourceLocation.tryParse(item);
                    if (itemId != null && integer(request, "weight", 1) > 0) {
                        alternatives.add(itemId.toString());
                    }
                }
            }
            List<String> distinct = alternatives.stream().distinct().sorted().toList();
            if (distinct.isEmpty()) {
                problems.add("Fish pond " + resourceId + " population gate "
                        + population + " has no valid requested item");
            }
            gates.put(population, distinct);
        }
        return Map.copyOf(gates);
    }

    private static void scanArtisan(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        JsonArray recipes = array(object, "recipes");
        if (recipes == null) return;
        for (JsonElement element : recipes) {
            if (!element.isJsonObject()) continue;
            JsonObject recipe = element.getAsJsonObject();
            String input = string(recipe, "input");
            if (input == null) continue;
            String output = string(recipe, "output");
            if (output != null) graph.addRoute(output, "machine", resourceId, List.of(input));

            String outputMode = string(recipe, "outputMode");
            if ("smoked".equals(outputMode) && input.startsWith(SVE_PREFIX)) {
                graph.addRoute(SVE_PREFIX + "smoked_" + input.substring(SVE_PREFIX.length()),
                        "fish_smoker", resourceId, List.of(input));
            } else if ("seedmaker".equals(outputMode)) {
                String seed = SveAcquisitionEntrypoints.seedForCrop(input);
                if (seed != null) graph.addRoute(seed, "seed_maker", resourceId, List.of(input));
            }
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonArray firstArray(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonArray value = array(object, key);
            if (value != null) return value;
        }
        return null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = string(object, key);
            if (value != null) return value;
        }
        return null;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive()
                ? element.getAsInt() : fallback;
    }

    private static float decimal(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive()
                ? element.getAsFloat() : fallback;
    }

    private static boolean booleanValue(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive()
                    && element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
        }
        return false;
    }

    private static void reportAliasConflict(
            JsonObject object,
            String canonical,
            String legacy,
            String detail,
            List<String> problems
    ) {
        if (object.has(canonical) && object.has(legacy)
                && !object.get(canonical).equals(object.get(legacy))) {
            problems.add("Mail " + detail + " has conflicting " + canonical
                    + " and " + legacy + "; " + canonical + " wins");
        }
    }

    private static String mailDefinitionId(String resourceId, String rawId) {
        String value = rawId;
        if (value == null || value.isBlank()) {
            String resourcePath = path(resourceId);
            if (resourcePath.startsWith("mail/")) {
                resourcePath = resourcePath.substring("mail/".length());
            }
            if (resourcePath.endsWith(".json")) {
                resourcePath = resourcePath.substring(
                        0, resourcePath.length() - ".json".length());
            }
            value = resourcePath;
        }
        value = value.trim();
        ResourceLocation id;
        if (value.indexOf(':') >= 0) {
            id = ResourceLocation.tryParse(value);
        } else {
            String namespace = namespace(resourceId);
            String path = "stardewcraft".equals(namespace)
                    ? value.toLowerCase(java.util.Locale.ROOT) : value;
            id = ResourceLocation.tryBuild(namespace, path);
        }
        return id == null ? null : id.toString();
    }

    private static String namespace(String resourceId) {
        int separator = resourceId.indexOf(':');
        return separator < 0 ? "minecraft" : resourceId.substring(0, separator);
    }

    private static String path(String resourceId) {
        int separator = resourceId.indexOf(':');
        return separator < 0 ? resourceId : resourceId.substring(separator + 1);
    }

    private record RecipeUnlock(String detail, List<String> prerequisites) {
        private RecipeUnlock {
            detail = detail == null ? "unknown" : detail;
            prerequisites = List.copyOf(prerequisites);
        }
    }
}
