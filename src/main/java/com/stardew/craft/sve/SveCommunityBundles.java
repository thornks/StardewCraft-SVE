package com.stardew.craft.sve;

import com.mojang.logging.LogUtils;
import com.stardew.craft.communitycenter.data.BundleDataManager;
import com.stardew.craft.communitycenter.data.BundleDefinition;
import com.stardew.craft.communitycenter.data.BundleIngredient;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds the standard and hard SVE community-center catalogs from StardewCraft's reload snapshot. */
public final class SveCommunityBundles {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Map<Integer, BundleDefinition> standardBundles = Map.of();
    private static volatile Map<Integer, BundleDefinition> hardBundles = Map.of();

    private SveCommunityBundles() {}

    public static synchronized void apply() {
        Map<Integer, BundleDefinition> base = copy(BundleDataManager.getAllBundles());
        if (base.isEmpty()) return;

        Map<Integer, BundleDefinition> standard = buildStandard(base);
        Map<Integer, BundleDefinition> hard = buildHard(base);
        standardBundles = Collections.unmodifiableMap(new LinkedHashMap<>(standard));
        hardBundles = Collections.unmodifiableMap(new LinkedHashMap<>(hard));

        Map<Integer, String> areaNames = new LinkedHashMap<>();
        Map<Integer, String> areaDisplayKeys = new LinkedHashMap<>();
        for (int areaId = 0; areaId <= 6; areaId++) {
            String name = BundleDataManager.getAreaName(areaId);
            String displayKey = BundleDataManager.getAreaDisplayNameKey(areaId);
            if (name != null) areaNames.put(areaId, name);
            if (displayKey != null) areaDisplayKeys.put(areaId, displayKey);
        }

        // Standard remains the context-free server catalog. Player operations are selected per farm.
        BundleDataManager.applyFromNetwork(standard.values(), areaNames, areaDisplayKeys);
        LOGGER.info("[SVE] Prepared standard and hard community-center catalogs ({} bundles each)",
                standard.size());
    }

    public static BundleDefinition getBundleForPlayer(UUID player, int bundleId) {
        if (standardBundles.isEmpty()) return BundleDataManager.getBundle(bundleId);
        return catalogFor(player).get(bundleId);
    }

    public static List<BundleDefinition> getBundlesForArea(UUID player, int areaId) {
        if (standardBundles.isEmpty()) return BundleDataManager.getBundlesForArea(areaId);
        return catalogFor(player).values().stream()
                .filter(bundle -> bundle.areaId() == areaId)
                .toList();
    }

    public static Collection<BundleDefinition> getAllBundlesForPlayer(UUID player) {
        if (standardBundles.isEmpty()) return BundleDataManager.getAllBundles();
        return catalogFor(player).values();
    }

    public static boolean isReady() {
        return !standardBundles.isEmpty();
    }

    private static Map<Integer, BundleDefinition> catalogFor(UUID player) {
        if (player != null && SveBundleDifficultyData.isHardForPlayer(player) && !hardBundles.isEmpty()) {
            return hardBundles;
        }
        return standardBundles;
    }

    private static Map<Integer, BundleDefinition> buildStandard(Map<Integer, BundleDefinition> base) {
        Map<Integer, BundleDefinition> bundles = new LinkedHashMap<>(base);
        append(bundles, 0, 5, item("stardewcraftsve:cucumber"));
        append(bundles, 1, 5, item("stardewcraftsve:butternut_squash"));
        append(bundles, 2, 5, item("stardewcraftsve:sweet_potato"));
        append(bundles, 3, 5,
                item("stardewcraftsve:cucumber", 5, 2),
                item("stardewcraftsve:butternut_squash", 5, 2),
                item("stardewcraftsve:sweet_potato", 5, 2));
        replaceArtisanBundle(bundles, 1, "BO 12 1");

        append(bundles, 14, 4, item("stardewcraftsve:red_baneberry"));
        append(bundles, 15, 5, item("stardewcraftsve:mushroom_colony"));
        append(bundles, 16, 5, item("stardewcraftsve:bearberrys"));
        replace(bundles, 17, 4, null, List.of(
                item("stardewcraft:wood_normal", 499, 0),
                item("stardewcraft:clay", 10, 0),
                item("stardewcraft:stone", 199, 0),
                item("stardewcraft:wood_hard", 30, 0)));
        replaceExoticForagingBundle(bundles, 1, 8, "O 235 5");

        append(bundles, 6, 6, item("stardewcraftsve:minnow"), item("stardewcraftsve:goldfish"));
        append(bundles, 7, 5, item("stardewcraftsve:tadpole"));
        append(bundles, 8, 5, item("stardewcraftsve:starfish"));
        append(bundles, 9, 4, item("stardewcraftsve:frog"));
        append(bundles, 10, 6,
                item("stardewcraftsve:king_salmon"), item("stardewcraftsve:butterfish"));

        append(bundles, 31, 7, item("stardewcraftsve:candy"));
        append(bundles, 32, 5, item("stardewcraftsve:amber"));
        append(bundles, 33, 5, item("stardewcraftsve:lucky_four_leaf_clover"));
        append(bundles, 34, 7, item("stardewcraftsve:persimmon"));
        append(bundles, 36, 6, item("stardewcraftsve:shark"));
        return bundles;
    }

    private static Map<Integer, BundleDefinition> buildHard(Map<Integer, BundleDefinition> base) {
        Map<Integer, BundleDefinition> bundles = new LinkedHashMap<>(base);

        replaceWithAdded(bundles, 0, 5, "O 465 40", 5, 0,
                item("stardewcraftsve:cucumber", 5, 0));
        replaceWithAdded(bundles, 1, 5, "O 621 3", 5, 0,
                item("stardewcraftsve:butternut_squash", 5, 0));
        replaceWithAdded(bundles, 2, 5, "BO 10 3", 5, 0,
                item("stardewcraftsve:sweet_potato", 5, 0));
        replaceWithAdded(bundles, 3, 5, "BO 15 3", 10, 2,
                item("stardewcraftsve:cucumber", 10, 2),
                item("stardewcraftsve:butternut_squash", 10, 2),
                item("stardewcraftsve:sweet_potato", 10, 2));
        replace(bundles, 4, 6, "BO stardewcraftsve:butter_churner 5",
                resizeAll(base.get(4).ingredients(), 5, 0));
        replaceArtisanBundle(bundles, 5, "BO 12 3");

        replaceWithAdded(bundles, 13, 5, "O 495 50", 3, 0,
                item("stardewcraftsve:lucky_four_leaf_clover"));
        replaceWithAdded(bundles, 14, 4, "O 496 50", 3, 0,
                item("stardewcraftsve:red_baneberry", 10, 0));
        replaceWithAdded(bundles, 15, 5, "O 497 50", 3, 0,
                item("stardewcraftsve:mushroom_colony", 5, 0));
        replaceWithAdded(bundles, 16, 5, "O 498 50", 3, 0,
                item("stardewcraftsve:bearberrys", 3, 0));
        replace(bundles, 17, 4, "BO 114 10", List.of(
                item("stardewcraft:wood_normal", 999, 0),
                item("stardewcraft:clay", 20, 0),
                item("stardewcraft:stone", 499, 0),
                item("stardewcraft:wood_hard", 99, 0)));
        replaceExoticForagingBundle(bundles, 3, 8, "O 235 10");

        replaceFishBundle(bundles, base, 6, 6, "O DeluxeBait 99", 3,
                item("stardewcraftsve:minnow", 3, 0), item("stardewcraftsve:goldfish", 3, 0));
        replaceFishBundle(bundles, base, 7, 5, "O 687 3", 3,
                item("stardewcraftsve:tadpole", 3, 0));
        replaceFishBundle(bundles, base, 8, 5, "O 690 10", 3,
                item("stardewcraftsve:starfish", 3, 0));
        replaceFishBundle(bundles, base, 9, 4, "R 517 1", 3,
                item("stardewcraftsve:frog", 3, 0));
        replaceFishBundle(bundles, base, 10, 6, "O 242 10", 3,
                item("stardewcraftsve:king_salmon", 3, 0), item("stardewcraftsve:butterfish", 3, 0));
        replace(bundles, 11, 5, "O 710 5", resizeAll(base.get(11).ingredients(), 5, 0));

        replace(bundles, 20, 3, "BO 13 3", resizeAll(base.get(20).ingredients(), 5, 0));
        replace(bundles, 21, 4, "O 749 10", resizeAll(base.get(21).ingredients(), 5, 0));
        replace(bundles, 22, 2, "R 518 1", resize(base.get(22).ingredients(),
                new int[]{199, 30, 50, 50}, new int[]{0, 0, 0, 0}));

        replaceVault(bundles, base, 23, 5_000, "O 220 5");
        replaceVault(bundles, base, 24, 10_000, "O 369 60");
        replaceVault(bundles, base, 25, 20_000, "BO 9 2");
        replaceVault(bundles, base, 26, 50_000, "BO 21 2");

        List<BundleIngredient> chef = resize(base.get(31).ingredients().subList(0, 3),
                new int[]{3, 10, 5}, new int[]{0, 0, 0});
        chef.add(item("stardewcraftsve:mixed_berry_pie"));
        chef.add(item("stardewcraftsve:big_bark_burger"));
        chef.add(item("stardewcraftsve:glazed_butterfish"));
        chef.add(item("stardewcraftsve:candy"));
        replace(bundles, 31, 6, "O 221 5", chef);

        appendWithReward(bundles, base, 32, 5, "BO 20 2", item("stardewcraftsve:amber"));
        List<BundleIngredient> enchanter = resize(base.get(33).ingredients(),
                new int[]{3, 10, 1, 5}, new int[]{0, 0, 0, 0});
        enchanter.add(item("stardewcraftsve:lucky_four_leaf_clover", 3, 0));
        replace(bundles, 33, 5, "O 336 10", enchanter);

        List<BundleIngredient> dye = resizeAll(base.get(34).ingredients(), 3, 0);
        dye.add(item("stardewcraftsve:persimmon", 3, 0));
        replace(bundles, 34, 6, "BO 25 2", dye);

        List<BundleIngredient> fodder = resize(base.get(35).ingredients(),
                new int[]{50, 99, 5}, new int[]{0, 0, 0});
        fodder.add(item("stardewcraft:fiber", 30, 0));
        replace(bundles, 35, 4, "BO 104 3", fodder);

        List<BundleIngredient> missing = resize(base.get(36).ingredients(),
                new int[]{2, 2, 2, 10, 2, 2}, new int[]{1, 0, 0, 2, 2, 0});
        missing.add(item("stardewcraftsve:shiny_lunaloo"));
        replace(bundles, 36, 6, "", missing);
        return bundles;
    }

    private static void replaceArtisanBundle(Map<Integer, BundleDefinition> bundles, int count, String reward) {
        BundleDefinition base = bundles.get(5);
        if (!hasIngredients(base, 9)) return;
        List<BundleIngredient> ingredients = resizeAll(base.ingredients().subList(0, 6), count, 0);
        ingredients.add(item("stardewcraftsve:butter", count, 0));
        ingredients.add(resize(base.ingredients().get(7), count, 0));
        ingredients.add(resize(base.ingredients().get(8), count, 0));
        ingredients.add(item("stardewcraftsve:nectarine", count, 0));
        ingredients.add(item("stardewcraftsve:pear", count, 0));
        replace(bundles, 5, 8, reward, ingredients);
    }

    private static void replaceExoticForagingBundle(Map<Integer, BundleDefinition> bundles, int count,
                                                     int requiredCount, String reward) {
        BundleDefinition base = bundles.get(19);
        if (!hasIngredients(base, 8)) return;
        List<BundleIngredient> ingredients = resizeAll(base.ingredients().subList(0, 5), count, 0);
        ingredients.add(item("stardewcraftsve:fir_wax", count, 0));
        ingredients.add(item("stardewcraftsve:birch_water", count, 0));
        ingredients.add(resize(base.ingredients().get(7), count, 0));
        ingredients.add(item("stardewcraftsve:poison_mushroom", count, 0));
        replace(bundles, 19, requiredCount, reward, ingredients);
    }

    private static void replaceFishBundle(Map<Integer, BundleDefinition> bundles,
                                          Map<Integer, BundleDefinition> base, int id, int requiredCount,
                                          String reward, int count, BundleIngredient... additions) {
        List<BundleIngredient> ingredients = resizeAll(base.get(id).ingredients(), count, 0);
        ingredients.addAll(List.of(additions));
        replace(bundles, id, requiredCount, reward, ingredients);
    }

    private static void replaceWithAdded(Map<Integer, BundleDefinition> bundles, int id, int requiredCount,
                                         String reward, int count, int quality, BundleIngredient... additions) {
        List<BundleIngredient> ingredients = resizeAll(bundles.get(id).ingredients(), count, quality);
        ingredients.addAll(List.of(additions));
        replace(bundles, id, requiredCount, reward, ingredients);
    }

    private static void appendWithReward(Map<Integer, BundleDefinition> bundles,
                                         Map<Integer, BundleDefinition> base, int id, int requiredCount,
                                         String reward, BundleIngredient... additions) {
        List<BundleIngredient> ingredients = new ArrayList<>(base.get(id).ingredients());
        ingredients.addAll(List.of(additions));
        replace(bundles, id, requiredCount, reward, ingredients);
    }

    private static void replaceVault(Map<Integer, BundleDefinition> bundles,
                                     Map<Integer, BundleDefinition> base, int id, int amount, String reward) {
        BundleIngredient source = base.get(id).ingredients().getFirst();
        replace(bundles, id, 1, reward, List.of(
                new BundleIngredient(source.itemId(), source.sdvId(), source.category(), amount, amount)));
    }

    private static List<BundleIngredient> resizeAll(List<BundleIngredient> source, int count, int quality) {
        List<BundleIngredient> result = new ArrayList<>(source.size());
        for (BundleIngredient ingredient : source) result.add(resize(ingredient, count, quality));
        return result;
    }

    private static List<BundleIngredient> resize(List<BundleIngredient> source, int[] counts, int[] qualities) {
        if (source.size() != counts.length || counts.length != qualities.length) {
            throw new IllegalArgumentException("Mismatched hard bundle ingredient counts");
        }
        List<BundleIngredient> result = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) result.add(resize(source.get(i), counts[i], qualities[i]));
        return result;
    }

    private static BundleIngredient resize(BundleIngredient ingredient, int count, int quality) {
        return new BundleIngredient(ingredient.itemId(), ingredient.sdvId(), ingredient.category(), count, quality);
    }

    private static Map<Integer, BundleDefinition> copy(Collection<BundleDefinition> source) {
        Map<Integer, BundleDefinition> result = new LinkedHashMap<>();
        for (BundleDefinition definition : source) result.put(definition.bundleId(), definition);
        return result;
    }

    private static boolean hasIngredients(BundleDefinition definition, int minimum) {
        if (definition != null && definition.ingredients().size() >= minimum) return true;
        LOGGER.warn("[SVE] Skipping incompatible bundle definition; expected at least {} ingredients", minimum);
        return false;
    }

    private static void append(Map<Integer, BundleDefinition> bundles, int id, int requiredCount,
                               BundleIngredient... additions) {
        BundleDefinition base = bundles.get(id);
        if (base == null) {
            LOGGER.warn("[SVE] Cannot patch missing community-center bundle {}", id);
            return;
        }
        List<BundleIngredient> ingredients = new ArrayList<>(base.ingredients());
        ingredients.addAll(List.of(additions));
        replace(bundles, id, requiredCount, null, ingredients);
    }

    private static void replace(Map<Integer, BundleDefinition> bundles, int id, int requiredCount,
                                String reward, List<BundleIngredient> ingredients) {
        BundleDefinition base = bundles.get(id);
        if (base == null) {
            LOGGER.warn("[SVE] Cannot replace missing community-center bundle {}", id);
            return;
        }
        bundles.put(id, new BundleDefinition(
                base.bundleId(), base.areaId(), base.internalName(), base.displayNameKey(),
                reward == null ? base.rewardString() : reward, List.copyOf(ingredients),
                base.color(), requiredCount));
    }

    private static BundleIngredient item(String id) {
        return item(id, 1, 0);
    }

    private static BundleIngredient item(String id, int count, int quality) {
        return new BundleIngredient(id, id, 0, count, quality);
    }
}
