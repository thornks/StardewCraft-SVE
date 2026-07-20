package com.stardew.craft.sve.animal;

import com.stardew.craft.animal.service.AnimalShopService;
import com.stardew.craft.entity.animal.CoopAnimalVariant;

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

    public static final boolean SHOP_MODELS_READY = true;

    private SveAnimalRules() {
    }

    public static boolean isSveAnimal(String animalTypeId) {
        return GOOSE_ID.equals(animalTypeId) || CAMEL_ID.equals(animalTypeId);
    }

    public static String requiredBuildingFamily(String animalTypeId) {
        if (GOOSE_ID.equals(animalTypeId)) return "coop";
        if (CAMEL_ID.equals(animalTypeId)) return "barn";
        return "";
    }

    public static int purchasePrice(String animalTypeId) {
        if (GOOSE_ID.equals(animalTypeId)) return GOOSE_PURCHASE_PRICE;
        if (CAMEL_ID.equals(animalTypeId)) return CAMEL_PURCHASE_PRICE;
        return 0;
    }

    public static int daysToMature(String animalTypeId) {
        if (GOOSE_ID.equals(animalTypeId)) return GOOSE_DAYS_TO_MATURE;
        if (CAMEL_ID.equals(animalTypeId)) return CAMEL_DAYS_TO_MATURE;
        return 0;
    }

    public static int produceIntervalDays(String animalTypeId) {
        if (GOOSE_ID.equals(animalTypeId)) return GOOSE_PRODUCE_INTERVAL_DAYS;
        if (CAMEL_ID.equals(animalTypeId)) return CAMEL_PRODUCE_INTERVAL_DAYS;
        return 0;
    }

    public static boolean canReproduce(String animalTypeId) {
        return !CAMEL_ID.equals(animalTypeId);
    }

    public static int sellPrice(String animalTypeId, int friendship) {
        int basePrice;
        if (GOOSE_ID.equals(animalTypeId)) {
            basePrice = GOOSE_SELL_BASE_PRICE;
        } else if (CAMEL_ID.equals(animalTypeId)) {
            basePrice = CAMEL_SELL_BASE_PRICE;
        } else {
            return 0;
        }

        int clampedFriendship = Math.max(0, Math.min(1_000, friendship));
        if (CAMEL_ID.equals(animalTypeId) && clampedFriendship == 1_000) {
            return CAMEL_MAX_SELL_PRICE;
        }
        double friendshipRatio = clampedFriendship / 1_000.0D;
        return (int) Math.floor(basePrice * (friendshipRatio + 0.3D));
    }

    public static AnimalShopService.ShopAnimalRule gooseShopRule() {
        return new AnimalShopService.ShopAnimalRule(
                GOOSE_ID,
                "coop",
                3,
                GOOSE_PURCHASE_PRICE,
                "Goose",
                "stardewcraftsve.animal.shop.desc.goose",
                "stardewcraft.animal.shop.lock.coop_t3"
        );
    }

    public static AnimalShopService.ShopAnimalRule camelShopRule() {
        return new AnimalShopService.ShopAnimalRule(
                CAMEL_ID,
                "barn",
                3,
                CAMEL_PURCHASE_PRICE,
                "Camel",
                "stardewcraftsve.animal.shop.desc.camel",
                "stardewcraft.animal.shop.lock.barn_t3"
        );
    }
}
