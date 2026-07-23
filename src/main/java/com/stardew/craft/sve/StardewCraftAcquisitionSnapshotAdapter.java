package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.stardew.craft.api.v1.fishing.StardewFishingTreasureEntry;
import com.stardew.craft.api.v1.fishing.StardewFishingTreasurePoolDefinition;
import com.stardew.craft.api.v1.query.StardewItemQueries;
import com.stardew.craft.api.v1.shop.StardewShopDefinition;
import com.stardew.craft.api.v1.shop.StardewShopEntry;
import com.stardew.craft.api.v1.production.StardewCookingIngredient;
import com.stardew.craft.api.v1.production.StardewCookingRecipeDefinition;
import com.stardew.craft.api.v1.world.StardewForageZoneDefinition;
import com.stardew.craft.cooking.service.VanillaCookingRecipeData;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.core.ModMiningDimensions;
import com.stardew.craft.fishing.data.FishingDataManager;
import com.stardew.craft.fishing.data.FishingLocationData;
import com.stardew.craft.fishing.data.FishingTreasurePoolData;
import com.stardew.craft.fishing.data.SpawnFishRule;
import com.stardew.craft.fishpond.service.FishPondDataService;
import com.stardew.craft.fishpond.service.FishPondQualifiedItemService;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.mail.MailRegistry;
import com.stardew.craft.player.StardewCraftingRecipeData;
import com.stardew.craft.shop.ShopDataLoader;
import com.stardew.craft.world.data.ForageZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Isolates StardewCraft internal snapshot APIs from the acquisition graph. */
final class StardewCraftAcquisitionSnapshotAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            "stardewcraftsve/acquisition-snapshot");
    private static final TagKey<Biome> GINGER_ISLAND_OCEAN = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "is_ginger_island_ocean"));
    private static final TagKey<Biome> MINES_20 = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "is_mines_20"));
    private static final TagKey<Biome> MINES_60 = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "is_mines_60"));
    private static final TagKey<Biome> MINES_100 = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "is_mines_100"));

    private StardewCraftAcquisitionSnapshotAdapter() {
    }

    static SveAcquisitionEntrypoints capture(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        List<SveAcquisitionEntrypoints.Route> routes = new ArrayList<>();
        routes.addAll(captureForageRoutes());
        routes.addAll(captureArtisanRoutes(problems));
        routes.addAll(captureFishingTreasureRoutes(problems));
        routes.addAll(captureFishPondRoutes(problems));
        Map<String, SveAcquisitionEntrypoints.Recipe> cooking =
                captureCookingRecipes(problems);
        Map<String, SveAcquisitionEntrypoints.Recipe> crafting =
                captureCraftingRecipes(problems);
        List<SveAcquisitionEntrypoints.Mail> mail = captureMail(
                problems, cooking, crafting);
        return new SveAcquisitionEntrypoints(
                captureShops(),
                captureFishing(server, server.getResourceManager()),
                Set.of(
                        SveAcquisitionEntrypoints.Domain.SHOP,
                        SveAcquisitionEntrypoints.Domain.FISHING,
                        SveAcquisitionEntrypoints.Domain.FISHING_TREASURE,
                        SveAcquisitionEntrypoints.Domain.FISH_POND,
                        SveAcquisitionEntrypoints.Domain.COOKING,
                        SveAcquisitionEntrypoints.Domain.CRAFTING,
                        SveAcquisitionEntrypoints.Domain.MAIL,
                        SveAcquisitionEntrypoints.Domain.FORAGE,
                        SveAcquisitionEntrypoints.Domain.ARTISAN),
                cooking,
                crafting,
                mail,
                routes,
                problems);
    }

    static Map<String, SveAcquisitionEntrypoints.Recipe> captureCookingRecipes(
            List<String> problems
    ) {
        Map<String, SveAcquisitionEntrypoints.Recipe> recipes = new LinkedHashMap<>();
        VanillaCookingRecipeData.snapshot().definitions().entrySet().stream()
                .filter(entry -> isRelevantNamespace(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    StardewCookingRecipeDefinition definition = entry.getValue();
                    List<SveContentAcquisitionGraph.Requirement> ingredients =
                            definition.ingredients().stream()
                                    .map(ingredient -> cookingRequirement(
                                            entry.getKey(), ingredient, problems))
                                    .toList();
                    recipes.put(entry.getKey().toString(),
                            new SveAcquisitionEntrypoints.Recipe(
                                    definition.output().toString(), ingredients, List.of()));
                });
        return Map.copyOf(recipes);
    }

    static Map<String, SveAcquisitionEntrypoints.Recipe> captureCraftingRecipes(
            List<String> problems
    ) {
        Map<String, SveAcquisitionEntrypoints.Recipe> recipes = new LinkedHashMap<>();
        StardewCraftingRecipeData.snapshot().definitions().entrySet().stream()
                .filter(entry -> isRelevantNamespace(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    StardewCraftingRecipeData.RecipeEntry definition = entry.getValue();
                    if (definition.output() == null || definition.output().item() == null
                            || definition.output().item().isBlank()) {
                        return;
                    }
                    List<SveContentAcquisitionGraph.Requirement> ingredients =
                            definition.ingredients().stream()
                            .map(ingredient -> craftingRequirement(
                                    entry.getKey(), ingredient, problems))
                            .toList();
                    List<String> intrinsicUnlocks = new ArrayList<>();
                    if (!definition.unlockWhen().isEmpty()) {
                        intrinsicUnlocks.add("intrinsic conditions");
                    } else if (SveAcquisitionEntrypoints.isAutomaticLegacyUnlock(
                            definition.unlockCondition())) {
                        intrinsicUnlocks.add("intrinsic condition "
                                + definition.unlockCondition().trim());
                    }
                    recipes.put(entry.getKey().toString(),
                            new SveAcquisitionEntrypoints.Recipe(
                                    definition.output().item(), ingredients, intrinsicUnlocks));
                });
        return Map.copyOf(recipes);
    }

    static List<SveAcquisitionEntrypoints.Mail> captureMail(
            List<String> problems,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes
    ) {
        List<SveAcquisitionEntrypoints.Mail> mail = new ArrayList<>();
        Map<String, SveContentAcquisitionCatalog.MailTrigger> triggers =
                SveContentAcquisitionCatalog.mailTriggers();
        MailRegistry.snapshot().definitions().entrySet().stream()
                .filter(entry -> isRelevantNamespace(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String mailId = entry.getKey().toString();
                    SveAcquisitionEntrypoints.RecipeKey learnedRecipe = null;
                    if (entry.getValue().learnedRecipe().isPresent()) {
                        learnedRecipe = toRecipeKey(
                                entry.getValue().learnedRecipe().get(),
                                entry.getValue().recipeIsCooking());
                        if (learnedRecipe == null) {
                            problems.add("Mail " + entry.getKey()
                                    + " has invalid learned recipe id "
                                    + entry.getValue().learnedRecipe().get());
                        }
                    }
                    List<String> attachedItems = entry.getValue().attachedItems().stream()
                            .map(attached -> attached.item().toString()).toList();
                    SveContentAcquisitionCatalog.MailTrigger trigger = triggers.get(mailId);
                    boolean hasSveAcquisition = attachedItems.stream()
                            .anyMatch(item -> item.startsWith(StardewcraftsveMod.MODID + ":"))
                            || isSveRecipeOutput(
                            learnedRecipe, cookingRecipes, craftingRecipes);
                    if (trigger == null && hasSveAcquisition) {
                        problems.add("Mail " + mailId
                                + " defines SVE acquisition but has no delivery trigger");
                    }
                    mail.add(new SveAcquisitionEntrypoints.Mail(
                            mailId, attachedItems, learnedRecipe,
                            trigger == null ? null : trigger.detail(),
                            trigger == null ? List.of() : trigger.prerequisites()));
                });
        return List.copyOf(mail);
    }

    private static boolean isSveRecipeOutput(
            SveAcquisitionEntrypoints.RecipeKey key,
            Map<String, SveAcquisitionEntrypoints.Recipe> cookingRecipes,
            Map<String, SveAcquisitionEntrypoints.Recipe> craftingRecipes
    ) {
        if (key == null) return false;
        Map<String, SveAcquisitionEntrypoints.Recipe> recipes =
                key.kind() == SveAcquisitionEntrypoints.RecipeKind.COOKING
                        ? cookingRecipes : craftingRecipes;
        SveAcquisitionEntrypoints.Recipe recipe = recipes.get(key.id());
        return recipe != null
                && recipe.output().startsWith(StardewcraftsveMod.MODID + ":");
    }

    static SveAcquisitionEntrypoints.RecipeKey toRecipeKey(
            String learnedRecipe,
            boolean cooking
    ) {
        return SveAcquisitionEntrypoints.RecipeKey.tryCreate(
                cooking ? SveAcquisitionEntrypoints.RecipeKind.COOKING
                        : SveAcquisitionEntrypoints.RecipeKind.CRAFTING,
                learnedRecipe);
    }

    private static SveContentAcquisitionGraph.Requirement cookingRequirement(
            ResourceLocation recipeId,
            StardewCookingIngredient ingredient,
            List<String> problems
    ) {
        List<String> alternatives = matchingRegisteredItems(ingredient::matches);
        if (alternatives.isEmpty()) {
            problems.add("Cooking recipe " + recipeId + " selector "
                    + ingredient.matcherKey() + " resolves to no registered items");
        }
        return SveContentAcquisitionGraph.Requirement.anyOf(
                ingredient.matcherKey(), alternatives);
    }

    private static SveContentAcquisitionGraph.Requirement craftingRequirement(
            ResourceLocation recipeId,
            StardewCraftingRecipeData.IngredientEntry ingredient,
            List<String> problems
    ) {
        String tag = ingredient.tag();
        if (tag != null && !tag.isBlank()) {
            ResourceLocation tagId = ResourceLocation.tryParse(tag.trim());
            if (tagId == null) {
                problems.add("Crafting recipe " + recipeId
                        + " has invalid item tag " + tag);
                return SveContentAcquisitionGraph.Requirement.anyOf(
                        "tag:" + tag.trim(), List.of());
            }
            List<String> alternatives = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.TagKey.create(
                            Registries.ITEM, tagId))
                    .forEach(holder -> alternatives.add(
                            BuiltInRegistries.ITEM.getKey(holder.value()).toString()));
            if (alternatives.isEmpty()) {
                problems.add("Crafting recipe " + recipeId + " selector tag:"
                        + tagId + " resolves to no registered items");
            }
            return SveContentAcquisitionGraph.Requirement.anyOf(
                    "tag:" + tagId, alternatives);
        }

        ResourceLocation itemId = ResourceLocation.tryParse(ingredient.item());
        if (itemId == null) {
            problems.add("Crafting recipe " + recipeId
                    + " has no valid item or tag ingredient");
            return SveContentAcquisitionGraph.Requirement.anyOf(
                    "invalid ingredient", List.of());
        }
        if (!BuiltInRegistries.ITEM.containsKey(itemId)
                || BuiltInRegistries.ITEM.get(itemId) == Items.AIR) {
            problems.add("Crafting recipe " + recipeId + " selector item:" + itemId
                    + " resolves to no registered items");
            return SveContentAcquisitionGraph.Requirement.anyOf(
                    "item:" + itemId, List.of());
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        String canonicalId = BuiltInRegistries.ITEM.getKey(item).toString();
        return SveContentAcquisitionGraph.Requirement.anyOf(
                "item:" + itemId, List.of(canonicalId));
    }

    private static List<String> matchingRegisteredItems(
            java.util.function.Predicate<ItemStack> predicate
    ) {
        List<String> matches = new ArrayList<>();
        for (ResourceLocation itemId : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) continue;
            if (predicate.test(new ItemStack(item))) matches.add(itemId.toString());
        }
        matches.sort(String::compareTo);
        return List.copyOf(matches);
    }

    static List<SveAcquisitionEntrypoints.Route> captureForageRoutes() {
        List<SveAcquisitionEntrypoints.Route> routes = new ArrayList<>();
        ForageZoneData.snapshot().definitions().entrySet().stream()
                .filter(entry -> isRelevantNamespace(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    StardewForageZoneDefinition definition = entry.getValue();
                    if (definition.maxDailySpawn() <= 0) return;
                    for (StardewForageZoneDefinition.Entry forage : definition.entries()) {
                        if (forage.chance() <= 0.0) continue;
                        String item = forageItemId(forage.block());
                        if (item != null) routes.add(new SveAcquisitionEntrypoints.Route(
                                item, "forage", entry.getKey().toString(), List.of()));
                    }
                });
        return List.copyOf(routes);
    }

    static List<SveAcquisitionEntrypoints.Route> captureArtisanRoutes(
            List<String> problems
    ) {
        List<SveAcquisitionEntrypoints.Route> routes = new ArrayList<>();
        ArtisanRecipeDataManager.snapshot().definitions().values().stream()
                .filter(recipe -> isRelevantNamespace(recipe.id().getNamespace()))
                .sorted(java.util.Comparator.comparing(recipe -> recipe.id().toString()))
                .forEach(recipe -> addArtisanRoute(recipe, routes, problems));
        return List.copyOf(routes);
    }

    static List<SveAcquisitionEntrypoints.Route> captureFishingTreasureRoutes(
            List<String> problems
    ) {
        // Capture once so a rejected or concurrent reload cannot leak candidate data.
        Map<ResourceLocation, StardewFishingTreasurePoolDefinition> definitions =
                FishingTreasurePoolData.snapshot().definitions();
        return fishingTreasureRoutes(definitions, problems);
    }

    static List<SveAcquisitionEntrypoints.Route> fishingTreasureRoutes(
            Map<ResourceLocation, StardewFishingTreasurePoolDefinition> definitions,
            List<String> problems
    ) {
        List<SveAcquisitionEntrypoints.Route> routes = new ArrayList<>();
        definitions.entrySet().stream()
                .filter(entry -> isRelevantNamespace(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> addFishingTreasureRoutes(
                        entry.getKey(), entry.getValue(), routes, problems));
        return List.copyOf(routes);
    }

    private static void addFishingTreasureRoutes(
            ResourceLocation poolId,
            StardewFishingTreasurePoolDefinition pool,
            List<SveAcquisitionEntrypoints.Route> routes,
            List<String> problems
    ) {
        if (!Float.isFinite(pool.chance()) || pool.chance() <= 0.0f || pool.rolls() <= 0) {
            return;
        }
        boolean ownedPool = StardewcraftsveMod.MODID.equals(poolId.getNamespace());
        for (int index = 0; index < pool.entries().size(); index++) {
            StardewFishingTreasureEntry treasure = pool.entries().get(index);
            if (treasure.weight() <= 0
                    || !SveAcquisitionEntrypoints.hasReachableTreasureEligibility(
                    treasure.minFishingLevel(), treasure.maxFishingLevel(),
                    treasure.minWaterDistance(), treasure.maxWaterDistance())) {
                continue;
            }
            if (!"stardewcraft:item".equals(treasure.query().type().toString())) {
                if (ownedPool) {
                    problems.add("Fishing treasure pool " + poolId + " entry " + index
                            + " uses non-enumerable item query " + treasure.query().type());
                }
                continue;
            }
            var encoded = StardewItemQueries.CODEC
                    .encodeStart(JsonOps.INSTANCE, treasure.query()).result();
            if (encoded.isEmpty()) {
                if (ownedPool) {
                    problems.add("Fishing treasure pool " + poolId + " entry " + index
                            + " could not encode its accepted item query");
                }
                continue;
            }
            String output = SveAcquisitionEntrypoints.directTreasureItem(encoded.get());
            if (output == null) {
                if (ownedPool) {
                    problems.add("Fishing treasure pool " + poolId + " entry " + index
                            + " has no valid direct item output");
                }
                continue;
            }
            routes.add(new SveAcquisitionEntrypoints.Route(
                    output, "fishing_treasure", poolId + "#" + index, List.of()));
        }
    }

    static List<SveAcquisitionEntrypoints.Route> captureFishPondRoutes(
            List<String> problems
    ) {
        FishPondDataService service = FishPondDataService.get();
        Map<String, FishPondDataService.PondData> accepted = new LinkedHashMap<>();
        for (String path : SveFishData.SVE_FISH) {
            ResourceLocation fishId = ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, path);
            if (!BuiltInRegistries.ITEM.containsKey(fishId)
                    || BuiltInRegistries.ITEM.get(fishId) == Items.AIR) {
                problems.add("Fish pond audit cannot resolve registered fish " + fishId);
                continue;
            }
            ItemStack fish = new ItemStack(BuiltInRegistries.ITEM.get(fishId));
            FishPondDataService.PondData definition = service.resolve(fish).orElse(null);
            if (definition == null) {
                problems.add("No accepted fish pond definition resolves for " + fishId);
                continue;
            }
            if (!definition.requiredTags().contains("item_id:" + fishId)) {
                problems.add("No accepted SVE-specific fish pond definition resolves for "
                        + fishId + "; host fallback was ignored");
                continue;
            }
            accepted.put(fishId.toString(), definition);
        }
        return fishPondRoutes(accepted, problems,
                item -> !FishPondQualifiedItemService.createItemStack(item, 1).isEmpty());
    }

    static List<SveAcquisitionEntrypoints.Route> fishPondRoutes(
            Map<String, FishPondDataService.PondData> definitions,
            List<String> problems,
            java.util.function.Predicate<String> resolvableItem
    ) {
        List<SveAcquisitionEntrypoints.Route> routes = new ArrayList<>();
        definitions.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> addFishPondRoutes(
                        entry.getKey(), entry.getValue(), routes, problems, resolvableItem));
        return List.copyOf(routes);
    }

    private static void addFishPondRoutes(
            String fishId,
            FishPondDataService.PondData pond,
            List<SveAcquisitionEntrypoints.Route> routes,
            List<String> problems,
            java.util.function.Predicate<String> resolvableItem
    ) {
        if (!Double.isFinite(pond.baseMinProduceChance())
                || !Double.isFinite(pond.baseMaxProduceChance())
                || pond.baseMinProduceChance() < 0.0D
                || pond.baseMaxProduceChance() > 1.0D
                || pond.baseMaxProduceChance() < pond.baseMinProduceChance()
                || pond.baseMaxProduceChance() <= 0.0D) {
            return;
        }
        int maximumPopulation = pond.maxPopulation() > 0
                ? pond.maxPopulation() : 10;
        for (int index = 0; index < pond.producedItems().size(); index++) {
            FishPondDataService.ProducedItem production = pond.producedItems().get(index);
            if (production.condition() != null && !production.condition().isBlank()
                    && production.itemId().startsWith(StardewcraftsveMod.MODID + ":")) {
                problems.add("Fish pond production " + pond.id() + "#" + index
                        + " uses a legacy condition that acquisition audit cannot prove: "
                        + production.condition());
            }
        }
        Map<Integer, Integer> selectable = selectableFishPondProductions(
                pond, maximumPopulation);
        for (Map.Entry<Integer, Integer> selection : selectable.entrySet()) {
            int index = selection.getKey();
            FishPondDataService.ProducedItem production = pond.producedItems().get(index);
            ResourceLocation outputId = ResourceLocation.tryParse(production.itemId());
            if (outputId == null || !resolvableItem.test(production.itemId())) {
                if (production.itemId().startsWith(StardewcraftsveMod.MODID + ":")) {
                    problems.add("Fish pond production " + pond.id() + "#" + index
                            + " references missing output " + production.itemId());
                }
                continue;
            }
            List<SveContentAcquisitionGraph.Requirement> requirements =
                    fishPondRequirements(fishId, pond, selection.getValue(),
                            problems, resolvableItem);
            routes.add(new SveAcquisitionEntrypoints.Route(
                    outputId.toString(), "fish_pond", pond.id() + "#" + index,
                    requirements));
        }
    }

    private static Map<Integer, Integer> selectableFishPondProductions(
            FishPondDataService.PondData pond,
            int maximumPopulation
    ) {
        Map<Integer, Integer> firstPopulation = new LinkedHashMap<>();
        for (int population = 1; population <= maximumPopulation; population++) {
            if (dailyFishPondChance(pond, population) <= 0.0D) continue;
            Set<Integer> selected = new LinkedHashSet<>();
            selected.add(-1);
            for (int index = 0; index < pond.producedItems().size(); index++) {
                FishPondDataService.ProducedItem candidate = pond.producedItems().get(index);
                if (population < candidate.requiredPopulation()
                        || candidate.requiredPopulation() < 0
                        || !Double.isFinite(candidate.chance())
                        || candidate.chance() <= 0.0D
                        || candidate.chance() > 1.0D
                        || (candidate.condition() != null
                        && !candidate.condition().isBlank())) {
                    continue;
                }
                Set<Integer> next = new LinkedHashSet<>();
                for (int current : selected) {
                    if (current >= 0 && pond.producedItems().get(current).precedence()
                            <= candidate.precedence()) {
                        next.add(current);
                        continue;
                    }
                    if (candidate.chance() < 1.0D) next.add(current);
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

    private static double dailyFishPondChance(
            FishPondDataService.PondData pond,
            int population
    ) {
        double minimum = pond.baseMinProduceChance();
        double maximum = pond.baseMaxProduceChance();
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) return 0.0D;
        if (minimum == maximum) return minimum;
        return minimum + (maximum - minimum) * (population / 10.0D);
    }

    private static List<SveContentAcquisitionGraph.Requirement> fishPondRequirements(
            String fishId,
            FishPondDataService.PondData pond,
            int requiredPopulation,
            List<String> problems,
            java.util.function.Predicate<String> resolvableItem
    ) {
        List<SveContentAcquisitionGraph.Requirement> requirements = new ArrayList<>();
        requirements.add(SveContentAcquisitionGraph.Requirement.exact(fishId));
        if (pond.maxPopulation() > 0) return List.copyOf(requirements);

        pond.populationGates().entrySet().stream()
                .filter(entry -> entry.getKey() > 0
                        && entry.getKey() <= requiredPopulation)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<String> alternatives = entry.getValue().stream()
                            .map(StardewCraftAcquisitionSnapshotAdapter::fishPondGateItem)
                            .filter(java.util.Objects::nonNull)
                            .filter(resolvableItem)
                            .distinct()
                            .sorted()
                            .toList();
                    if (alternatives.isEmpty()) {
                        problems.add("Fish pond " + pond.id() + " population gate "
                                + entry.getKey() + " has no resolvable requested item");
                    }
                    requirements.add(SveContentAcquisitionGraph.Requirement.anyOf(
                            "fish_pond_gate:" + fishId + "@" + entry.getKey(), alternatives));
                });
        return List.copyOf(requirements);
    }

    private static String fishPondGateItem(String request) {
        if (request == null || request.isBlank()) return null;
        String item = request.trim().split("\\s+")[0];
        ResourceLocation itemId = ResourceLocation.tryParse(item);
        return itemId == null ? null : itemId.toString();
    }

    private static Map<String, SveAcquisitionEntrypoints.Shop> captureShops() {
        // Capture once so a concurrent reload cannot mix two accepted versions.
        Map<ResourceLocation, StardewShopDefinition> definitions =
                ShopDataLoader.snapshot().definitions();
        Map<String, SveAcquisitionEntrypoints.Shop> shops = new LinkedHashMap<>();
        definitions.entrySet().stream()
                .filter(entry -> isRelevantNamespace(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!entry.getValue().inventoryProviders().isEmpty()) {
                        LOGGER.warn("Acquisition audit cannot enumerate dynamic inventory providers "
                                        + "for shop {}: {}",
                                entry.getKey(), entry.getValue().inventoryProviders());
                    }
                    shops.put(entry.getKey().toString(),
                            new SveAcquisitionEntrypoints.Shop(
                                    entry.getKey().toString(),
                                    readShopEntries(entry.getValue())));
                });
        return shops;
    }

    static List<SveAcquisitionEntrypoints.ShopEntry> readShopEntries(
            StardewShopDefinition definition
    ) {
        List<SveAcquisitionEntrypoints.ShopEntry> entries = new ArrayList<>();
        for (StardewShopEntry entry : definition.entries()) {
            if (entry.stock() == 0 || entry.item() == null || entry.item().isBlank()) continue;
            entries.add(new SveAcquisitionEntrypoints.ShopEntry(
                    entry.item(), entry.tradeItem().orElse(null)));
        }
        return List.copyOf(entries);
    }

    private static Map<String, SveAcquisitionEntrypoints.FishingLocation> captureFishing(
            MinecraftServer server,
            ResourceManager resourceManager
    ) {
        FishingDataManager manager = FishingDataManager.get();
        Map<String, FishingLocationData> activePools = manager.getLocationDataSnapshot();
        Map<RuleKey, Boolean> displayOnly = readDisplayOnlyMetadata(resourceManager);
        List<FishingEnvironment> environments = captureFishingEnvironments(server);
        if (environments.isEmpty()) {
            throw new IllegalStateException(
                    "Stardew fishing dimensions expose no possible biomes");
        }

        Map<String, SveAcquisitionEntrypoints.FishingLocation> result = new LinkedHashMap<>();
        activePools.entrySet().stream()
                .filter(entry -> SveAcquisitionEntrypoints.isSelectablePool(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<SveAcquisitionEntrypoints.FishingRule> rules = new ArrayList<>();
                    for (SpawnFishRule rule : entry.getValue().fish()) {
                        RuleKey key = new RuleKey(entry.getKey(), rule.id(), rule.itemId());
                        boolean mapped = isEnvironmentReachable(
                                entry.getKey(), rule, environments);
                        boolean positiveBaselineProbability =
                                SveAcquisitionEntrypoints.hasPositiveBaselineCatchProbability(
                                        rule.chance(), rule.chanceBoostPerLuckLevel(),
                                        rule.spawnRate(), rule.minFishingLevel(),
                                        rule.minDistanceFromShore(), rule.maxDistanceFromShore(),
                                        rule.maxDepth(), rule.depthMultiplier(),
                                        rule.ignoreFishDataRequirements());
                        for (String output : fishingRuleOutputs(rule)) {
                            rules.add(new SveAcquisitionEntrypoints.FishingRule(
                                    rule.id(), output,
                                    displayOnly.getOrDefault(key, false),
                                    positiveBaselineProbability, mapped));
                        }
                    }
                    result.put(entry.getKey(), new SveAcquisitionEntrypoints.FishingLocation(
                            entry.getKey(), rules));
                });
        return result;
    }

    private static List<FishingEnvironment> captureFishingEnvironments(MinecraftServer server) {
        List<FishingEnvironment> result = new ArrayList<>();
        List<Holder.Reference<Biome>> registeredBiomes = server.registryAccess()
                .registryOrThrow(Registries.BIOME).holders().toList();
        addFishingEnvironments(
                server.getLevel(ModDimensions.STARDEW_VALLEY), registeredBiomes, result);
        addFishingEnvironments(
                server.getLevel(ModMiningDimensions.STARDEW_MINING), registeredBiomes, result);
        return List.copyOf(result);
    }

    private static void addFishingEnvironments(
            ServerLevel level,
            List<Holder.Reference<Biome>> registeredBiomes,
            List<FishingEnvironment> target
    ) {
        if (level == null) return;
        // Stardew dimensions use flat generators; their real map biomes are patched later.
        for (Holder<Biome> biome : registeredBiomes) {
            Set<String> keys = new LinkedHashSet<>(
                    FishingDataManager.resolveVanillaAlignedLocationKeysStatic(
                            level, biome, BlockPos.ZERO));
            // collectCandidatesByKeys always adds Default during ordinary fishing.
            keys.add("Default");
            target.add(new FishingEnvironment(biome, Set.copyOf(keys)));
        }
    }

    private static boolean isEnvironmentReachable(
            String poolKey,
            SpawnFishRule rule,
            List<FishingEnvironment> environments
    ) {
        for (FishingEnvironment environment : environments) {
            if (!isCurrentlyAccessibleFishingEnvironment(environment.biome())
                    || isLocationPoolPreempted(environment.biome())) {
                continue;
            }
            if (environment.locationKeys().contains(poolKey)
                    && rule.matchesBiome(environment.biome())) {
                return true;
            }
        }
        return false;
    }

    static List<String> fishingRuleOutputs(SpawnFishRule rule) {
        return SveAcquisitionEntrypoints.fishingRuleOutputs(
                rule.itemId(), rule.randomItemIds());
    }

    private static boolean isCurrentlyAccessibleFishingEnvironment(Holder<Biome> biome) {
        return !biome.is(GINGER_ISLAND_OCEAN)
                && biome.unwrapKey().map(key -> !key.location().equals(
                        ResourceLocation.fromNamespaceAndPath(
                                "stardewcraft", "ginger_island_ocean"))).orElse(true);
    }

    /** Mirrors the early-return ordering in StardewCraft 0.5.2's mine catch override. */
    private static boolean isLocationPoolPreempted(Holder<Biome> biome) {
        return biome.is(MINES_100) && !biome.is(MINES_20) && !biome.is(MINES_60);
    }

    private static Map<RuleKey, Boolean> readDisplayOnlyMetadata(ResourceManager manager) {
        Map<String, JsonElement> resources = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> selected = manager.listResources(
                "fishing/locations",
                id -> isRelevantNamespace(id.getNamespace()) && id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : selected.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                resources.put(entry.getKey().toString(), JsonParser.parseReader(reader));
            } catch (Exception exception) {
                LOGGER.warn("Could not read fishing display metadata from {}: {}",
                        entry.getKey(), exception.getMessage());
            }
        }

        Map<RuleKey, Boolean> metadata = new LinkedHashMap<>();
        for (JsonElement element : resources.values()) {
            if (!element.isJsonObject()) continue;
            JsonObject root = element.getAsJsonObject();
            String poolKey = primitiveString(root, "location");
            JsonElement fishElement = root.get("fish");
            if (poolKey == null || fishElement == null || !fishElement.isJsonArray()) continue;
            JsonArray fish = fishElement.getAsJsonArray();
            for (JsonElement ruleElement : fish) {
                if (!ruleElement.isJsonObject()) continue;
                JsonObject rule = ruleElement.getAsJsonObject();
                String ruleId = primitiveString(rule, "id");
                String itemId = primitiveString(rule, "item");
                if (ruleId == null || itemId == null) continue;
                metadata.put(new RuleKey(poolKey, ruleId, itemId),
                        primitiveBoolean(rule, "displayOnly"));
            }
        }
        return Map.copyOf(metadata);
    }

    private static String primitiveString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static boolean primitiveBoolean(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }

    private static String forageItemId(ResourceLocation block) {
        String path = block.getPath();
        if (!path.startsWith("forage_")) return null;
        return ResourceLocation.fromNamespaceAndPath(
                block.getNamespace(), path.substring("forage_".length())).toString();
    }

    private static void addArtisanRoute(
            ArtisanRecipeDataManager.Recipe recipe,
            List<SveAcquisitionEntrypoints.Route> routes,
            List<String> problems
    ) {
        if (recipe.outputMode() == ArtisanRecipeDataManager.OutputMode.COPY_INPUT) {
            // Copy-input recipes do not make a new item acquisition route.
            return;
        }
        if (recipe.outputMode() == ArtisanRecipeDataManager.OutputMode.FIXED
                && (recipe.outputId() == null
                || !StardewcraftsveMod.MODID.equals(recipe.outputId().getNamespace()))) {
            return;
        }

        ResolvedInputs resolved = resolveArtisanInputs(recipe, problems);
        String detail = recipe.id().toString();
        switch (recipe.outputMode()) {
            case FIXED -> {
                ResourceLocation outputId = recipe.outputId();
                if (outputId == null) return;
                routes.add(new SveAcquisitionEntrypoints.Route(
                        outputId.toString(), "machine", detail,
                        List.of(SveContentAcquisitionGraph.Requirement.anyOf(
                                resolved.selector(), resolved.itemIds()))));
            }
            case SMOKED -> {
                for (String input : resolved.itemIds()) {
                    if (!input.startsWith(StardewcraftsveMod.MODID + ":")) continue;
                    String path = input.substring(input.indexOf(':') + 1);
                    routes.add(new SveAcquisitionEntrypoints.Route(
                            StardewcraftsveMod.MODID + ":smoked_" + path,
                            "fish_smoker", detail,
                            List.of(SveContentAcquisitionGraph.Requirement.exact(input))));
                }
            }
            case SEEDMAKER -> {
                for (String input : resolved.itemIds()) {
                    String seed = SveAcquisitionEntrypoints.seedForCrop(input);
                    if (seed != null) routes.add(new SveAcquisitionEntrypoints.Route(
                            seed, "seed_maker", detail,
                            List.of(SveContentAcquisitionGraph.Requirement.exact(input))));
                }
            }
            case COPY_INPUT -> throw new IllegalStateException("COPY_INPUT handled above");
        }
    }

    static ResolvedInputs resolveArtisanInputs(
            ArtisanRecipeDataManager.Recipe recipe,
            List<String> problems
    ) {
        String selector = artisanSelector(recipe);
        List<String> itemIds = matchingRegisteredItems(recipe::matches);
        if (itemIds.isEmpty()) {
            problems.add("Artisan recipe " + recipe.id() + " selector " + selector
                    + " resolves to no registered items");
        }
        return new ResolvedInputs(selector, itemIds);
    }

    static String artisanSelector(ArtisanRecipeDataManager.Recipe recipe) {
        if (recipe.inputMode() != null
                && recipe.inputMode() != ArtisanRecipeDataManager.InputMode.DEFAULT) {
            return "input_mode:"
                    + recipe.inputMode().name().toLowerCase(java.util.Locale.ROOT);
        }
        if (recipe.inputId() != null) return "item:" + recipe.inputId();
        if (recipe.inputTag() != null) return "tag:" + recipe.inputTag().location();
        return "unknown input";
    }

    private static boolean isRelevantNamespace(String namespace) {
        return StardewcraftsveMod.MODID.equals(namespace) || "stardewcraft".equals(namespace);
    }

    private record RuleKey(String poolKey, String ruleId, String itemId) {
    }

    private record FishingEnvironment(Holder<Biome> biome, Set<String> locationKeys) {
    }

    record ResolvedInputs(String selector, List<String> itemIds) {
        ResolvedInputs {
            itemIds = List.copyOf(itemIds);
        }
    }
}
