package com.stardew.craft.sve.animal;

import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.animal.service.AnimalProducePlacementService;
import com.stardew.craft.item.quality.QualityHelper;
import com.stardew.craft.sve.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class SveAnimalProduction {
    private SveAnimalProduction() {
    }

    public static void beforeDailyUpdate(
            ServerLevel level,
            AnimalWorldData worldData,
            FarmAnimalRecord record,
            int absoluteDaysPlayed,
            boolean offlineCatchUp
    ) {
        if (!isProductionDue(
                record.animalTypeId(),
                record.isBaby(),
                offlineCatchUp,
                record.daysSinceLastProduce())) {
            return;
        }

        if (SveAnimalRules.GOOSE_ID.equals(record.animalTypeId())) {
            produceGooseEggs(level, worldData, record, absoluteDaysPlayed);
        } else if (SveAnimalRules.CAMEL_ID.equals(record.animalTypeId())) {
            produceCamelWool(level, worldData, record, absoluteDaysPlayed);
        }
    }

    private static void produceGooseEggs(
            ServerLevel level,
            AnimalWorldData worldData,
            FarmAnimalRecord record,
            int absoluteDaysPlayed
    ) {
        RandomSource random = randomForAnimalDay(record.animalId(), absoluteDaysPlayed);
        int quality = rollQuality(record, random);
        ItemStack egg = new ItemStack(ModItems.GOOSE_EGG.get());
        QualityHelper.setQuality(egg, quality);
        if (!placeProduce(level, worldData, record, egg)) {
            return;
        }

        record.resetDaysSinceLastProduce();
        if (isGoldenGooseEggRoll(random.nextDouble())) {
            ItemStack goldenEgg = new ItemStack(ModItems.GOLDEN_GOOSE_EGG.get());
            QualityHelper.setQuality(goldenEgg, quality);
            placeProduce(level, worldData, record, goldenEgg);
        }
    }

    private static void produceCamelWool(
            ServerLevel level,
            AnimalWorldData worldData,
            FarmAnimalRecord record,
            int absoluteDaysPlayed
    ) {
        RandomSource random = randomForAnimalDay(record.animalId(), absoluteDaysPlayed);
        ItemStack wool = new ItemStack(ModItems.CAMEL_WOOL.get());
        QualityHelper.setQuality(wool, rollQuality(record, random));
        if (placeProduce(level, worldData, record, wool)) {
            record.resetDaysSinceLastProduce();
        }
    }

    private static boolean placeProduce(
            ServerLevel level,
            AnimalWorldData worldData,
            FarmAnimalRecord record,
            ItemStack stack
    ) {
        boolean placed = AnimalProducePlacementService.placeInHome(level, worldData, record, stack);
        if (!placed) {
            placed = AnimalProducePlacementService.dropInHome(level, worldData, record, stack);
        }
        if (placed && record.hasEatenAnimalCracker()) {
            ItemStack duplicate = stack.copy();
            if (!AnimalProducePlacementService.placeInHome(level, worldData, record, duplicate)) {
                AnimalProducePlacementService.dropInHome(level, worldData, record, duplicate);
            }
        }
        return placed;
    }

    private static int rollQuality(FarmAnimalRecord record, RandomSource random) {
        double chance = record.friendship() / 1000.0D - (1.0D - record.happiness() / 225.0D);
        chance = Math.max(0.0D, Math.min(1.0D, chance));
        double roll = random.nextDouble();
        if (roll < chance * 0.25D) {
            return QualityHelper.IRIDIUM;
        }
        if (roll < chance * 0.5D) {
            return QualityHelper.GOLD;
        }
        if (roll < chance) {
            return QualityHelper.SILVER;
        }
        return QualityHelper.NORMAL;
    }

    private static RandomSource randomForAnimalDay(long animalId, int absoluteDaysPlayed) {
        long seed = 0x9E3779B97F4A7C15L;
        seed ^= animalId * 0xBF58476D1CE4E5B9L;
        seed ^= (long) absoluteDaysPlayed * 0x94D049BB133111EBL;
        return RandomSource.create(seed);
    }

    public static boolean isProductionDue(
            String animalTypeId,
            boolean baby,
            boolean offlineCatchUp,
            int daysSinceLastProduce
    ) {
        int interval = SveAnimalRules.produceIntervalDays(animalTypeId);
        return interval > 0 && !baby && !offlineCatchUp && daysSinceLastProduce >= interval;
    }

    public static boolean isGoldenGooseEggRoll(double roll) {
        return roll >= 0.0D && roll < SveAnimalRules.GOLDEN_GOOSE_EGG_CHANCE;
    }
}
