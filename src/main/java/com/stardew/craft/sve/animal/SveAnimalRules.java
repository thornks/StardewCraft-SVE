package com.stardew.craft.sve.animal;

import com.stardew.craft.entity.animal.CoopAnimalVariant;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Central gameplay values for SVE farm animals. */
public final class SveAnimalRules {
    public static final String GOOSE_ID = "goose";
    public static final String CAMEL_ID = "camel";
    public static final int GOOSE_VARIANT_INDEX = CoopAnimalVariant.values().length;
    public static final int CAMEL_VARIANT_INDEX = GOOSE_VARIANT_INDEX + 1;

    public static final int GOOSE_PURCHASE_PRICE = 12_000;
    public static final int CAMEL_PURCHASE_PRICE = 24_000;
    public static final int GOOSE_DAYS_TO_MATURE = 5;
    public static final int CAMEL_DAYS_TO_MATURE = 5;
    public static final int GOOSE_PRODUCE_INTERVAL_DAYS = 2;
    public static final int CAMEL_PRODUCE_INTERVAL_DAYS = 2;
    public static final double GOLDEN_GOOSE_EGG_CHANCE = 0.01D;

    public static final int GOOSE_SELL_BASE_PRICE = 12_000;
    public static final int CAMEL_SELL_BASE_PRICE = 28_616;
    public static final int CAMEL_MAX_SELL_PRICE = 37_200;

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(
                    GOOSE_ID,
                    "coop",
                    3,
                    GOOSE_PURCHASE_PRICE,
                    GOOSE_DAYS_TO_MATURE,
                    GOOSE_PRODUCE_INTERVAL_DAYS,
                    "goose_egg",
                    "goose_egg",
                    GOOSE_SELL_BASE_PRICE,
                    15_600,
                    true,
                    GOOSE_VARIANT_INDEX,
                    "Goose",
                    "stardewcraftsve.animal.shop.desc.goose",
                    "stardewcraft.animal.shop.lock.coop_t3"
            ),
            new Definition(
                    CAMEL_ID,
                    "barn",
                    3,
                    CAMEL_PURCHASE_PRICE,
                    CAMEL_DAYS_TO_MATURE,
                    CAMEL_PRODUCE_INTERVAL_DAYS,
                    "camel_wool",
                    null,
                    CAMEL_SELL_BASE_PRICE,
                    CAMEL_MAX_SELL_PRICE,
                    false,
                    CAMEL_VARIANT_INDEX,
                    "Camel",
                    "stardewcraftsve.animal.shop.desc.camel",
                    "stardewcraft.animal.shop.lock.barn_t3"
            )
    );
    private static final Map<String, Definition> BY_ID = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(Definition::id, Function.identity()));

    private SveAnimalRules() {
    }

    public static boolean isSveAnimal(String animalTypeId) {
        return definition(animalTypeId) != null;
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    public static Definition definition(String animalTypeId) {
        if (animalTypeId == null || animalTypeId.isBlank()) return null;
        return BY_ID.get(animalTypeId.trim().toLowerCase(Locale.ROOT));
    }

    public static String requiredBuildingFamily(String animalTypeId) {
        Definition definition = definition(animalTypeId);
        return definition == null ? "" : definition.buildingFamily();
    }

    public static int purchasePrice(String animalTypeId) {
        Definition definition = definition(animalTypeId);
        return definition == null ? 0 : definition.purchasePrice();
    }

    public static int daysToMature(String animalTypeId) {
        Definition definition = definition(animalTypeId);
        return definition == null ? 0 : definition.daysToMature();
    }

    public static int produceIntervalDays(String animalTypeId) {
        Definition definition = definition(animalTypeId);
        return definition == null ? 0 : definition.produceIntervalDays();
    }

    public static boolean canReproduce(String animalTypeId) {
        Definition definition = definition(animalTypeId);
        return definition == null || definition.canReproduce();
    }

    public static int sellPrice(String animalTypeId, int friendship) {
        Definition definition = definition(animalTypeId);
        if (definition == null) return 0;

        int clampedFriendship = Math.max(0, Math.min(1_000, friendship));
        double friendshipRatio = clampedFriendship / 1_000.0D;
        int price = (int) Math.floor(definition.sellBasePrice() * (friendshipRatio + 0.3D));
        return Math.min(definition.maxSellPrice(), price);
    }

    public record Definition(
            String id,
            String buildingFamily,
            int requiredBuildingTier,
            int purchasePrice,
            int daysToMature,
            int produceIntervalDays,
            String produceItemPath,
            String incubationItemPath,
            int sellBasePrice,
            int maxSellPrice,
            boolean canReproduce,
            int variantIndex,
            String displayName,
            String descriptionKey,
            String lockReasonKey
    ) {
    }
}
