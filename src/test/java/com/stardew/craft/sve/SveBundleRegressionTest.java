package com.stardew.craft.sve;

import com.stardew.craft.communitycenter.data.BundleDefinition;
import com.stardew.craft.communitycenter.data.BundleIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure catalog regression checks; no Minecraft world or client is required. */
public final class SveBundleRegressionTest {
    private static final int[] BUNDLE_IDS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24, 25, 26,
            31, 32, 33, 34, 35, 36
    };

    private SveBundleRegressionTest() {
    }

    public static void main(String[] args) {
        SveCommunityBundles.Catalog catalog = SveCommunityBundles.buildCatalogs(baseCatalog());
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
        expectEquals(7, hard.get(36).ingredients().size(), "hard missing bundle slots");

        for (String itemId : SveCommunityBundles.sveIngredientIds(catalog)) {
            expect(SveBundleAcquisitionCatalog.routeFor(itemId) != null,
                    "missing acquisition route for " + itemId);
        }

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
            int area = id <= 5 ? 0 : id <= 19 ? 1 : id <= 22 ? 3 : id <= 26 ? 4 : id <= 35 ? 5 : 6;
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
