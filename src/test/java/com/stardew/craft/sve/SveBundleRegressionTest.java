package com.stardew.craft.sve;

import com.stardew.craft.communitycenter.data.BundleDefinition;
import com.stardew.craft.communitycenter.data.BundleIngredient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure catalog regression checks; no Minecraft world or client is required. */
public final class SveBundleRegressionTest {
    private static final int[] BUNDLE_IDS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25, 26,
            31, 32, 33, 34, 35, 36
    };

    private SveBundleRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        List<BundleDefinition> base = baseCatalog();
        SveCommunityBundles.Catalog catalog = SveCommunityBundles.buildCatalogs(base);
        Map<Integer, BundleDefinition> standard = catalog.standard();
        Map<Integer, BundleDefinition> hard = catalog.hard();

        expectEquals(BUNDLE_IDS.length, standard.size(), "standard bundle count");
        expectEquals(BUNDLE_IDS.length, hard.size(), "hard bundle count");
        expectEquals(5, standard.get(0).ingredients().size(), "standard spring slots");
        expectEquals(5, standard.get(0).requiredCount(), "standard spring required count");
        expectEquals(5, hard.get(0).ingredients().getLast().stack(), "hard SVE crop quantity");
        expectEquals(0, hard.get(0).ingredients().getLast().quality(), "hard crop quality");
        expectEquals(11, standard.get(5).ingredients().size(), "standard artisan slots");
        expectEquals(11, hard.get(5).ingredients().size(), "hard artisan slots");
        expectEquals(1, hard.get(23).requiredCount(), "hard vault required count");
        expectEquals(5_000, hard.get(23).ingredients().getFirst().stack(), "hard vault amount");
        BundleDefinition baseMissing = base.stream()
                .filter(bundle -> bundle.bundleId() == 36)
                .findFirst()
                .orElseThrow();
        expectEquals(baseMissing, standard.get(36), "standard missing bundle must remain host-owned");
        expectEquals(baseMissing, hard.get(36), "hard missing bundle must remain host-owned");

        Set<String> sveIngredients = SveCommunityBundles.sveIngredientIds(catalog);
        expectEquals(26, sveIngredients.size(), "playable SVE bundle ingredient count");
        expect(!sveIngredients.contains("stardewcraftsve:shark"),
                "unplayable missing bundle shark must not enter the SVE catalog");
        expect(!sveIngredients.contains("stardewcraftsve:shiny_lunaloo"),
                "unplayable missing bundle lunaloo must not enter the SVE catalog");

        Map<String, com.google.gson.JsonElement> resources = SveTestContentResources.loadResources();
        Set<String> registeredItems = SveTestContentResources.registeredItems();
        SveContentAcquisitionService.Snapshot acquisition =
                SveContentAcquisitionService.inspect(resources, registeredItems);
        List<String> acquisitionProblems = SveBundleAudit.acquisitionProblems(catalog, acquisition);
        expect(acquisitionProblems.isEmpty(),
                "playable bundle acquisition problems: " + acquisitionProblems);

        Map<String, com.google.gson.JsonElement> withoutSeedShop = new LinkedHashMap<>(resources);
        withoutSeedShop.remove("stardewcraft:shops/seed_shop.json");
        SveContentAcquisitionService.Snapshot brokenAcquisition =
                SveContentAcquisitionService.inspect(withoutSeedShop, registeredItems);
        expect(containsProblem(
                        SveBundleAudit.acquisitionProblems(catalog, brokenAcquisition),
                        "stardewcraftsve:cucumber"),
                "bundle audit must fail when the effective cucumber shop route is removed");
        SveAcquisitionEntrypoints acceptedEntrypoints =
                SveAcquisitionEntrypoints.fromResources(resources);
        SveContentAcquisitionService.Snapshot retainedShopSnapshot =
                SveContentAcquisitionService.inspect(
                        withoutSeedShop, registeredItems, acceptedEntrypoints);
        expect(!containsProblem(
                        SveBundleAudit.acquisitionProblems(catalog, retainedShopSnapshot),
                        "stardewcraftsve:cucumber"),
                "rejected shop candidates must not replace the last accepted snapshot");

        Map<String, com.google.gson.JsonElement> withoutSveFishing = new LinkedHashMap<>(resources);
        withoutSveFishing.remove("stardewcraftsve:fishing/locations/sve_fish.json");
        SveContentAcquisitionService.Snapshot brokenFishing =
                SveContentAcquisitionService.inspect(withoutSveFishing, registeredItems);
        expect(containsProblem(
                        SveBundleAudit.acquisitionProblems(catalog, brokenFishing),
                        "stardewcraftsve:big_bark_burger"),
                "bundle audit must follow indirect Puppyfish recipe dependencies");
        SveContentAcquisitionService.Snapshot retainedFishingSnapshot =
                SveContentAcquisitionService.inspect(
                        withoutSveFishing, registeredItems, acceptedEntrypoints);
        expect(!containsProblem(
                        SveBundleAudit.acquisitionProblems(catalog, retainedFishingSnapshot),
                        "stardewcraftsve:big_bark_burger"),
                "resource candidates must not replace the effective fishing snapshot");

        SveCommunityBundles.Catalog unregisteredCatalog = withIngredient(
                catalog, "stardewcraftsve:test_unregistered_bundle_item");
        expect(containsProblem(
                        SveBundleAudit.acquisitionProblems(unregisteredCatalog, acquisition),
                        "is not registered"),
                "bundle audit must reject unregistered ingredients");
        SveCommunityBundles.Catalog excludedCatalog = withIngredient(
                catalog, "stardewcraftsve:void_mayo_sandwich");
        expect(containsProblem(
                        SveBundleAudit.acquisitionProblems(excludedCatalog, acquisition),
                        "not survival-obtainable"),
                "bundle audit must reject explicitly excluded ingredients");

        List<BundleDefinition> playableOnly = base.stream()
                .filter(bundle -> SveCommunityBundles.isImplementedBundleArea(bundle.areaId()))
                .toList();
        SveCommunityBundles.Catalog playableCatalog = SveCommunityBundles.buildCatalogs(playableOnly);
        expectEquals(BUNDLE_IDS.length - 1, playableCatalog.standard().size(),
                "catalog without unimplemented host area");
        expectEquals(playableCatalog.standard().keySet(), playableCatalog.hard().keySet(),
                "playable-only catalog parity");

        BundleDefinition futureHostBundle = new BundleDefinition(
                99, 7, "Future Host Bundle", "test.bundle.future", "", List.of(
                new BundleIngredient("base:future", "base:future", 0, 1, 0)), 0, 1);
        List<BundleDefinition> withFutureArea = new ArrayList<>(base);
        withFutureArea.add(futureHostBundle);
        SveCommunityBundles.Catalog futureCatalog = SveCommunityBundles.buildCatalogs(withFutureArea);
        expectEquals(futureHostBundle, futureCatalog.standard().get(99),
                "future host area must pass through standard catalog");
        expectEquals(futureHostBundle, futureCatalog.hard().get(99),
                "future host area must pass through hard catalog");

        SveBundleDifficultyData data = new SveBundleDifficultyData();
        java.util.UUID oldOwner = java.util.UUID.randomUUID();
        java.util.UUID newOwner = java.util.UUID.randomUUID();
        data.setHard(oldOwner, true);
        data.transfer(oldOwner, newOwner);
        expect(data.isHard(newOwner), "hard mode transfer");
        data.transfer(newOwner, oldOwner);
        expect(!data.isHard(newOwner), "hard mode reverse transfer cleanup");
        data.setHard(oldOwner, false);
        data.transfer(oldOwner, newOwner);
        expect(!data.isHard(newOwner), "standard mode transfer must remain standard");

        System.out.println("SVE bundle regression suite passed");
    }

    private static boolean containsProblem(List<String> problems, String fragment) {
        return problems.stream().anyMatch(problem -> problem.contains(fragment));
    }

    private static SveCommunityBundles.Catalog withIngredient(
            SveCommunityBundles.Catalog catalog,
            String itemId
    ) {
        Map<Integer, BundleDefinition> standard = new LinkedHashMap<>(catalog.standard());
        Map<Integer, BundleDefinition> hard = new LinkedHashMap<>(catalog.hard());
        standard.put(0, appendIngredient(standard.get(0), itemId));
        hard.put(0, appendIngredient(hard.get(0), itemId));
        return new SveCommunityBundles.Catalog(standard, hard);
    }

    private static BundleDefinition appendIngredient(BundleDefinition definition, String itemId) {
        List<BundleIngredient> ingredients = new ArrayList<>(definition.ingredients());
        ingredients.add(new BundleIngredient(itemId, itemId, 0, 1, 0));
        return new BundleDefinition(
                definition.bundleId(), definition.areaId(), definition.internalName(),
                definition.displayNameKey(), definition.rewardString(), List.copyOf(ingredients),
                definition.color(), definition.requiredCount());
    }

    private static List<BundleDefinition> baseCatalog() {
        List<BundleDefinition> result = new ArrayList<>();
        for (int id : BUNDLE_IDS) {
            int slotCount = switch (id) {
                case 0, 1, 2, 3, 13, 14, 15, 16, 17, 32, 33 -> 4;
                case 35 -> 3;
                case 4 -> 6;
                case 5 -> 13;
                case 6, 7, 8, 10, 21 -> 4;
                case 36 -> 6;
                case 34 -> 6;
                case 9 -> 3;
                case 11 -> 10;
                case 19 -> 9;
                case 20 -> 3;
                case 22 -> 4;
                case 23, 24, 25, 26 -> 1;
                case 31 -> 6;
                default -> throw new IllegalArgumentException("Unknown test bundle " + id);
            };
            List<BundleIngredient> ingredients = new ArrayList<>();
            for (int slot = 0; slot < slotCount; slot++) {
                boolean money = id >= 23 && id <= 26;
                String itemId = money ? null : "base:item_" + id + "_" + slot;
                ingredients.add(new BundleIngredient(itemId, itemId, money ? -1 : 0,
                        money ? 2_500 : 1, money ? 2_500 : 0));
            }
            int area = switch (id) {
                case 0, 1, 2, 3, 4, 5 -> 0;
                case 13, 14, 15, 16, 17, 19 -> 1;
                case 6, 7, 8, 9, 10, 11 -> 2;
                case 20, 21, 22 -> 3;
                case 23, 24, 25, 26 -> 4;
                case 31, 32, 33, 34, 35 -> 5;
                case 36 -> 6;
                default -> throw new IllegalArgumentException("Unknown test bundle " + id);
            };
            result.add(new BundleDefinition(id, area,
                    "Bundle " + id, "test.bundle." + id, "O 1 1", ingredients,
                    0, Math.min(4, slotCount)));
        }
        return result;
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
