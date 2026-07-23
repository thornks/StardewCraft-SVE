package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Effective host-owned inputs consumed by the acquisition graph. */
record SveAcquisitionEntrypoints(
        Map<String, Shop> shops,
        Map<String, FishingLocation> fishingLocations,
        Set<Domain> coveredDomains,
        Map<String, Recipe> cookingRecipes,
        Map<String, Recipe> craftingRecipes,
        List<Mail> mail,
        List<Route> routes,
        List<String> adapterProblems
) {
    private static final String SVE_PREFIX = StardewcraftsveMod.MODID + ":";
    private static final String SVE_FISHING_POOL = SVE_PREFIX + "sve_fish";
    private static final String MOONLIGHT_JELLIES_POOL =
            SVE_PREFIX + "moonlight_jellies_festival";
    private static final Set<String> CURRENTLY_UNAVAILABLE_FISHING_BIOMES = Set.of(
            "stardewcraft:ginger_island_ocean");
    private static final Set<String> CURRENTLY_UNAVAILABLE_FISHING_BIOME_TAGS = Set.of(
            "stardewcraft:is_ginger_island_ocean");
    private static final Set<String> LOCATION_POOL_PREEMPTED_BIOMES = Set.of(
            "stardewcraft:mines_100");
    private static final Set<String> LOCATION_POOL_PREEMPTED_BIOME_TAGS = Set.of(
            "stardewcraft:is_mines_100");
    private static final int MAX_EFFECTIVE_FISHING_LEVEL = 11;
    private static final int MAX_EFFECTIVE_WATER_DEPTH = 6;
    private static final int MAX_TREASURE_WATER_DISTANCE = 5;

    SveAcquisitionEntrypoints {
        shops = immutableMap(shops);
        fishingLocations = immutableMap(fishingLocations);
        coveredDomains = Set.copyOf(coveredDomains);
        cookingRecipes = immutableMap(cookingRecipes);
        craftingRecipes = immutableMap(craftingRecipes);
        mail = List.copyOf(mail);
        routes = List.copyOf(routes);
        adapterProblems = List.copyOf(adapterProblems);
    }

    SveAcquisitionEntrypoints(
            Map<String, Shop> shops,
            Map<String, FishingLocation> fishingLocations
    ) {
        this(shops, fishingLocations, Set.of(Domain.SHOP, Domain.FISHING),
                Map.of(), Map.of(), List.of(), List.of(), List.of());
    }

    /** Builds candidate inputs for offline tests where no live game snapshot exists. */
    static SveAcquisitionEntrypoints fromResources(Map<String, JsonElement> resources) {
        Map<String, Shop> shops = new LinkedHashMap<>();
        Map<String, FishingLocation> fishing = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(resource -> {
            JsonElement value = resource.getValue();
            if (value == null || !value.isJsonObject()) return;
            String path = path(resource.getKey());
            if (path.startsWith("shops/") && path.endsWith(".json")) {
                Shop shop = readShop(logicalId(resource.getKey(), "shops/"), value.getAsJsonObject());
                shops.put(shop.id(), shop);
            } else if (path.startsWith("fishing/locations/") && path.endsWith(".json")) {
                FishingLocation location = readFishingLocation(value.getAsJsonObject());
                if (location != null) fishing.put(location.key(), location);
            }
        });
        return new SveAcquisitionEntrypoints(shops, fishing);
    }

    private static Shop readShop(String id, JsonObject object) {
        List<ShopEntry> entries = new ArrayList<>();
        JsonArray array = array(object, "entries");
        if (array != null) {
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                String item = string(entry, "item");
                int stock = integer(entry, "stock", Integer.MAX_VALUE);
                if (item == null || item.isBlank() || stock == 0) continue;
                entries.add(new ShopEntry(item, string(entry, "trade_item")));
            }
        }
        return new Shop(id, entries);
    }

    private static FishingLocation readFishingLocation(JsonObject object) {
        String key = string(object, "location");
        JsonArray array = array(object, "fish");
        if (key == null || key.isBlank() || array == null) return null;

        List<FishingRule> rules = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject rule = element.getAsJsonObject();
            String item = string(rule, "item");
            if (item == null || item.isBlank()) continue;
            String id = string(rule, "id");
            List<String> biomes = strings(rule, "biomes");
            List<String> biomeTags = strings(rule, "biomeTags");
            boolean displayOnly = bool(rule, "displayOnly", false);
            boolean positiveBaselineProbability = hasPositiveBaselineCatchProbability(
                    decimal(rule, "chance", 1.0f),
                    decimal(rule, "chanceBoostPerLuckLevel", 0.0f),
                    decimal(rule, "spawnRate", 1.0f),
                    integer(rule, "minFishingLevel", 0),
                    integer(rule, "minDistanceFromShore", 0),
                    integer(rule, "maxDistanceFromShore", -1),
                    integer(rule, "maxDepth", 4),
                    decimal(rule, "depthMultiplier", 0.0f),
                    bool(rule, "ignoreFishDataRequirements", false));
            boolean mapped = isSelectablePool(key)
                    && hasCandidateBiomeMapping(biomes, biomeTags);
            for (String output : fishingRuleOutputs(item, strings(rule, "randomItems"))) {
                rules.add(new FishingRule(id == null ? "unknown" : id, output,
                        displayOnly, positiveBaselineProbability, mapped));
            }
        }
        return new FishingLocation(key, rules);
    }

    /** Checks for a positive unbuffed roll at some legal skill/depth combination. */
    static boolean hasPositiveBaselineCatchProbability(
            float chance,
            float chanceBoostPerLuckLevel,
            float spawnRate,
            int minFishingLevel,
            int minDistanceFromShore,
            int maxDistanceFromShore,
            int maxDepth,
            float depthMultiplier,
        boolean ignoreFishDataRequirements
    ) {
        if (!Float.isFinite(chance) || !Float.isFinite(chanceBoostPerLuckLevel)) {
            return false;
        }
        // Luck buffs are optional, so level zero preserves any positive base chance while
        // one reachable positive level makes a positive boost independently catchable.
        if (Math.max(0.0f, chance) <= 0.0f && chanceBoostPerLuckLevel <= 0.0f) {
            return false;
        }
        if (!ignoreFishDataRequirements
                && (!Float.isFinite(spawnRate) || !Float.isFinite(depthMultiplier))) {
            return false;
        }

        int firstLevel = Math.max(0, minFishingLevel);
        for (int fishingLevel = firstLevel;
                fishingLevel <= MAX_EFFECTIVE_FISHING_LEVEL;
                fishingLevel++) {
            for (int waterDepth = 0;
                    waterDepth <= MAX_EFFECTIVE_WATER_DEPTH;
                    waterDepth++) {
                if (waterDepth < minDistanceFromShore) continue;
                if (maxDistanceFromShore >= 0 && waterDepth > maxDistanceFromShore) continue;
                if (ignoreFishDataRequirements) return true;
                float dropOff = depthMultiplier * spawnRate;
                float secondRoll = spawnRate
                        - Math.max(0, maxDepth - waterDepth) * dropOff
                        + fishingLevel / 50.0f;
                if (secondRoll > 0.0f) return true;
            }
        }
        return false;
    }

    static boolean hasReachableTreasureEligibility(
            int minFishingLevel,
            int maxFishingLevel,
            int minWaterDistance,
            int maxWaterDistance
    ) {
        for (int fishingLevel = 0;
                fishingLevel <= MAX_EFFECTIVE_FISHING_LEVEL;
                fishingLevel++) {
            if (fishingLevel < minFishingLevel || fishingLevel > maxFishingLevel) continue;
            for (int waterDistance = 0;
                    waterDistance <= MAX_TREASURE_WATER_DISTANCE;
                    waterDistance++) {
                if (waterDistance >= minWaterDistance
                        && waterDistance <= maxWaterDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    static String directTreasureItem(JsonElement queryElement) {
        if (queryElement == null || !queryElement.isJsonObject()) return null;
        JsonObject query = queryElement.getAsJsonObject();
        if (!"stardewcraft:item".equals(string(query, "type"))) return null;
        JsonElement dataElement = query.get("data");
        if (dataElement == null || !dataElement.isJsonObject()) return null;
        JsonObject data = dataElement.getAsJsonObject();
        String item = string(data, "item");
        if (item == null || integer(data, "count", 1) <= 0) return null;
        ResourceLocation itemId = ResourceLocation.tryParse(item);
        return itemId == null ? null : itemId.toString();
    }

    List<String> validationProblems() {
        List<String> problems = new ArrayList<>(adapterProblems);
        addEmptyRecipeSelectors(problems, "Cooking", cookingRecipes);
        addEmptyRecipeSelectors(problems, "Crafting", craftingRecipes);
        for (Route route : routes) {
            for (SveContentAcquisitionGraph.Requirement requirement : route.requirements()) {
                if (requirement.alternatives().isEmpty()) {
                    problems.add("Artisan recipe " + route.detail() + " selector "
                            + requirement.selector() + " resolves to no registered items");
                }
            }
        }
        for (Mail letter : mail) {
            boolean hasSveAcquisition = letter.attachedItems().stream()
                    .anyMatch(item -> item.startsWith(SVE_PREFIX))
                    || isSveRecipeOutput(letter.learnedRecipe());
            if (hasSveAcquisition && !letter.isDeliverable()) {
                problems.add("Mail " + letter.id()
                        + " defines SVE acquisition but has no delivery trigger");
            }
        }
        for (FishingLocation location : fishingLocations.values()) {
            for (FishingRule rule : location.rules()) {
                if (rule.displayOnly() && rule.positiveBaselineProbability()
                        && rule.environmentReachable()) {
                    problems.add("Fishing rule " + location.key() + "#" + rule.id()
                            + " is marked displayOnly but is catchable by StardewCraft");
                }
            }
        }
        return List.copyOf(problems);
    }

    private boolean isSveRecipeOutput(RecipeKey key) {
        if (key == null) return false;
        Recipe recipe = (key.kind() == RecipeKind.COOKING
                ? cookingRecipes : craftingRecipes).get(key.id());
        return recipe != null && recipe.output().startsWith(SVE_PREFIX);
    }

    private static void addEmptyRecipeSelectors(
            List<String> problems,
            String label,
            Map<String, Recipe> recipes
    ) {
        recipes.forEach((id, recipe) -> {
            for (SveContentAcquisitionGraph.Requirement requirement : recipe.ingredients()) {
                if (requirement.alternatives().isEmpty()) {
                    problems.add(label + " recipe " + id + " selector "
                            + requirement.selector() + " resolves to no registered items");
                }
            }
        });
    }

    boolean covers(Domain domain) {
        return coveredDomains.contains(domain);
    }

    static List<String> fishingRuleOutputs(String itemId, List<String> randomItemIds) {
        List<String> selected = randomItemIds != null && !randomItemIds.isEmpty()
                ? randomItemIds : List.of(itemId);
        return selected.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    static boolean hasCandidateBiomeMapping(List<String> biomes, List<String> biomeTags) {
        if (biomes.isEmpty() && biomeTags.isEmpty()) return true;
        if (biomes.stream().anyMatch(SveAcquisitionEntrypoints::isCandidateBiomeReachable)) {
            return true;
        }
        return biomeTags.stream().map(SveAcquisitionEntrypoints::stripTagPrefix)
                .anyMatch(SveAcquisitionEntrypoints::isCandidateBiomeTagReachable);
    }

    private static boolean isCandidateBiomeReachable(String id) {
        return !id.startsWith(SVE_PREFIX)
                && !CURRENTLY_UNAVAILABLE_FISHING_BIOMES.contains(id)
                && !LOCATION_POOL_PREEMPTED_BIOMES.contains(id);
    }

    private static boolean isCandidateBiomeTagReachable(String id) {
        return !id.startsWith(SVE_PREFIX)
                && !CURRENTLY_UNAVAILABLE_FISHING_BIOME_TAGS.contains(id)
                && !LOCATION_POOL_PREEMPTED_BIOME_TAGS.contains(id);
    }

    static boolean isSelectablePool(String key) {
        return SVE_FISHING_POOL.equals(key)
                || MOONLIGHT_JELLIES_POOL.equals(key)
                || !key.contains(":")
                || key.startsWith("stardewcraft:");
    }

    static boolean isAutomaticLegacyUnlock(String rawCondition) {
        if (rawCondition == null) return false;
        String condition = rawCondition.trim();
        if (condition.isEmpty() || "null".equalsIgnoreCase(condition)) return false;
        if ("default".equalsIgnoreCase(condition)) return true;

        String[] parts = condition.split("\\s+");
        if (parts.length >= 3 && "s".equalsIgnoreCase(parts[0])) {
            return isSkill(parts[1]) && isInteger(parts[2]);
        }
        return parts.length >= 2 && isSkill(parts[0]) && isInteger(parts[1]);
    }

    static String seedForCrop(String crop) {
        if (crop == null || !crop.startsWith(SVE_PREFIX)) return null;
        String seedPath = SveSeedMakerData.seedPathForProduce(
                crop.substring(SVE_PREFIX.length()));
        return seedPath == null ? null : SVE_PREFIX + seedPath;
    }

    private static boolean isSkill(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "farming", "fishing", "foraging", "mining", "combat" -> true;
            default -> false;
        };
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String stripTagPrefix(String value) {
        return value.startsWith("#") ? value.substring(1) : value;
    }

    private static String logicalId(String resourceId, String prefix) {
        String path = path(resourceId);
        String suffix = path.substring(prefix.length(), path.length() - ".json".length());
        return namespace(resourceId) + ":" + suffix;
    }

    private static String namespace(String resourceId) {
        int separator = resourceId.indexOf(':');
        return separator < 0 ? "minecraft" : resourceId.substring(0, separator);
    }

    private static String path(String resourceId) {
        int separator = resourceId.indexOf(':');
        return separator < 0 ? resourceId : resourceId.substring(separator + 1);
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    }

    private static float decimal(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsFloat() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    }

    private static List<String> strings(JsonObject object, String key) {
        JsonArray array = array(object, key);
        if (array == null) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) values.add(element.getAsString());
        }
        return List.copyOf(values);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    record Shop(String id, List<ShopEntry> entries) {
        Shop {
            entries = List.copyOf(entries);
        }
    }

    record ShopEntry(String itemId, String tradeItemId) {
    }

    record FishingLocation(String key, List<FishingRule> rules) {
        FishingLocation {
            rules = List.copyOf(rules);
        }
    }

    record FishingRule(
            String id,
            String itemId,
            boolean displayOnly,
            boolean positiveBaselineProbability,
            boolean environmentReachable
    ) {
        boolean isAcquisitionSource() {
            return positiveBaselineProbability && environmentReachable;
        }
    }

    enum Domain {
        SHOP,
        FISHING,
        FISHING_TREASURE,
        FISH_POND,
        COOKING,
        CRAFTING,
        MAIL,
        FORAGE,
        ARTISAN
    }

    enum RecipeKind {
        COOKING("cooking"),
        CRAFTING("crafting");

        private final String routeKind;

        RecipeKind(String routeKind) {
            this.routeKind = routeKind;
        }

        String routeKind() {
            return routeKind;
        }
    }

    record RecipeKey(RecipeKind kind, String id) {
        RecipeKey {
            if (kind == null) throw new IllegalArgumentException("Recipe kind is required");
            id = normalizeRecipeId(id);
        }

        static RecipeKey tryCreate(RecipeKind kind, String rawId) {
            try {
                return new RecipeKey(kind, rawId);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        private static String normalizeRecipeId(String rawId) {
            if (rawId == null || rawId.isBlank()) {
                throw new IllegalArgumentException("Recipe id is required");
            }
            String trimmed = rawId.trim();
            if (trimmed.startsWith("recipe:")) {
                trimmed = trimmed.substring("recipe:".length());
                if (trimmed.startsWith("recipe:")) {
                    throw new IllegalArgumentException("Invalid recipe id: " + rawId);
                }
            }
            if (trimmed.isBlank()) {
                throw new IllegalArgumentException("Recipe id is required");
            }
            ResourceLocation id = trimmed.indexOf(':') >= 0
                    ? ResourceLocation.tryParse(trimmed)
                    : ResourceLocation.tryBuild(
                            "stardewcraft", trimmed.toLowerCase(java.util.Locale.ROOT));
            if (id == null) throw new IllegalArgumentException("Invalid recipe id: " + rawId);
            return id.toString();
        }
    }

    record Recipe(
            String output,
            List<SveContentAcquisitionGraph.Requirement> ingredients,
            List<String> intrinsicUnlocks
    ) {
        Recipe {
            ingredients = List.copyOf(ingredients);
            intrinsicUnlocks = List.copyOf(intrinsicUnlocks);
        }
    }

    record Mail(
            String id,
            List<String> attachedItems,
            RecipeKey learnedRecipe,
            String deliveryDetail,
            List<String> deliveryPrerequisites
    ) {
        Mail {
            attachedItems = List.copyOf(attachedItems);
            deliveryDetail = deliveryDetail == null || deliveryDetail.isBlank()
                    ? null : deliveryDetail.trim();
            deliveryPrerequisites = List.copyOf(deliveryPrerequisites);
        }

        boolean isDeliverable() {
            return deliveryDetail != null;
        }
    }

    record Route(
            String output,
            String kind,
            String detail,
            List<SveContentAcquisitionGraph.Requirement> requirements
    ) {
        Route {
            requirements = List.copyOf(requirements);
        }
    }
}
