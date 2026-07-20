package com.stardew.craft.sve;

import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.agriculture.StardewCropData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Authoritative SVE crop rules sourced from the original Data/Crops entries. */
public final class SveCropData {
    public static final Definition CUCUMBER = crop(
            "cucumber_crop", "cucumber_seed", "cucumber", ProduceType.VEGETABLE,
            List.of(0), List.of(1, 2, 3, 3, 3), 2, false,
            2, 3, 0.03F, 0.20D, 35, 18, 75);
    public static final Definition BUTTERNUT_SQUASH = crop(
            "butternut_squash_crop", "butternut_squash_seed", "butternut_squash", ProduceType.VEGETABLE,
            List.of(1), List.of(1, 2, 3, 3, 3), -1, false,
            0, 0, 0.0F, 0.0D, 200, 20, 30);
    public static final Definition GOLD_CARROT = crop(
            "gold_carrot_crop", "gold_carrot_seed", "gold_carrot", ProduceType.VEGETABLE,
            List.of(0, 1, 2), List.of(1, 1, 2, 2), -1, false,
            0, 0, 0.0F, 0.0D, 1000, 115, 300);
    public static final Definition SWEET_POTATO = crop(
            "sweet_potato_crop", "sweet_potato_seed", "sweet_potato", ProduceType.VEGETABLE,
            List.of(2), List.of(1, 2, 3, 3, 3), -1, false,
            0, 0, 0.0F, 0.0D, 280, 20, 45);
    public static final Definition JOJA_BERRY = crop(
            "joja_berry_crop", "joja_berry_starter", "joja_berry", ProduceType.FRUIT,
            List.of(0, 1, 2), List.of(5, 5, 5, 5, 5), 4, true,
            1, 1, 0.05F, 0.15D, 650, 75, 1000);
    public static final Definition JOJA_VEGGIE = crop(
            "joja_veggie_crop", "joja_veggie_seeds", "joja_veggie", ProduceType.VEGETABLE,
            List.of(0, 1, 2), List.of(2, 3, 4, 4), -1, true,
            1, 1, 0.02F, 0.10D, 1140, 200, 200);
    public static final Definition MONSTER_FRUIT = crop(
            "monster_fruit_crop", "stalk_seed", "monster_fruit", ProduceType.FRUIT,
            List.of(1), List.of(3, 6, 6, 5, 5), -1, false,
            0, 0, 0.07F, 0.0D, 1525, 85, 0);
    public static final Definition SALAL_BERRY = crop(
            "salal_berry_crop", "salal_berry_seed", "salal_berry", ProduceType.FRUIT,
            List.of(0, 1), List.of(2, 2, 3, 3, 3), 4, false,
            2, 4, 0.02F, 0.03D, 75, 28, 0);
    public static final Definition SLIME_BERRY = crop(
            "slime_berry_crop", "slime_seed", "slime_berry", ProduceType.FRUIT,
            List.of(0), List.of(2, 3, 2, 3, 3), 4, false,
            1, 3, 0.03F, 0.10D, 65, -10, 0);
    public static final Definition ANCIENT_FIBER = crop(
            "ancient_fiber_crop", "ancient_ferns_seed", "ancient_fiber", ProduceType.VEGETABLE,
            List.of(1), List.of(2, 2, 2, 3, 3), -1, false,
            2, 4, 0.03F, 0.05D, 145, 35, 0);
    public static final Definition MONSTER_MUSHROOM = crop(
            "monster_mushroom_crop", "fungus_seed", "monster_mushroom", ProduceType.VEGETABLE,
            List.of(2), List.of(2, 2, 3, 3, 3), -1, false,
            0, 0, 0.05F, 0.0D, 850, 75, 0);
    public static final Definition VOID_ROOT = crop(
            "void_root_crop", "void_seed", "void_root", ProduceType.VEGETABLE,
            List.of(3), List.of(2, 2, 2, 2), -1, false,
            0, 0, 0.02F, 0.0D, 235, -35, 0);

    private static final List<Definition> ALL = List.of(
            CUCUMBER, BUTTERNUT_SQUASH, GOLD_CARROT, SWEET_POTATO,
            JOJA_BERRY, JOJA_VEGGIE, MONSTER_FRUIT, SALAL_BERRY,
            SLIME_BERRY, ANCIENT_FIBER, MONSTER_MUSHROOM, VOID_ROOT);

    private SveCropData() {
    }

    public static List<Definition> all() {
        return ALL;
    }

    public static void register() {
        StardewAgricultureDataApi.registerCropProvider(
                id("crop_data"), 100,
                (level, pos, state) -> resolve(state));
    }

    private static StardewCropData resolve(BlockState state) {
        if (!(state.getBlock() instanceof SveCropBlock crop)) return null;
        Definition definition = crop.definition();
        return new StardewCropData(
                definition.seasonNames(),
                definition.phaseDays(),
                definition.regrowDays(),
                definition.farmingExperience(),
                baseId("grab"),
                id(definition.producePath()),
                id(definition.seedPath()));
    }

    private static Definition crop(
            String blockPath,
            String seedPath,
            String producePath,
            ProduceType produceType,
            List<Integer> seasons,
            List<Integer> phaseDays,
            int regrowDays,
            boolean raised,
            int minHarvest,
            int maxHarvest,
            float harvestMaxIncreasePerFarmingLevel,
            double extraHarvestChance,
            int produceSellPrice,
            int edibility,
            int seedSellPrice
    ) {
        return new Definition(
                blockPath, seedPath, producePath, produceType,
                seasons, phaseDays, regrowDays, raised,
                minHarvest, maxHarvest, harvestMaxIncreasePerFarmingLevel,
                extraHarvestChance, produceSellPrice, edibility, seedSellPrice);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }

    private static ResourceLocation baseId(String path) {
        return ResourceLocation.fromNamespaceAndPath("stardewcraft", path);
    }

    public enum ProduceType {
        FRUIT,
        VEGETABLE
    }

    public record Definition(
            String blockPath,
            String seedPath,
            String producePath,
            ProduceType produceType,
            List<Integer> seasons,
            List<Integer> phaseDays,
            int regrowDays,
            boolean raised,
            int minHarvest,
            int maxHarvest,
            float harvestMaxIncreasePerFarmingLevel,
            double extraHarvestChance,
            int produceSellPrice,
            int edibility,
            int seedSellPrice
    ) {
        public Definition {
            if (blockPath == null || blockPath.isBlank()) throw new IllegalArgumentException("blockPath");
            if (seedPath == null || seedPath.isBlank()) throw new IllegalArgumentException("seedPath");
            if (producePath == null || producePath.isBlank()) throw new IllegalArgumentException("producePath");
            if (produceType == null) throw new IllegalArgumentException("produceType");
            seasons = List.copyOf(seasons);
            phaseDays = List.copyOf(phaseDays);
            if (seasons.isEmpty() || seasons.stream().anyMatch(value -> value < 0 || value > 3)) {
                throw new IllegalArgumentException("Invalid seasons for " + blockPath);
            }
            if (phaseDays.isEmpty() || phaseDays.stream().anyMatch(value -> value < 0)) {
                throw new IllegalArgumentException("Invalid phase days for " + blockPath);
            }
            if (regrowDays == 0 || regrowDays < -1) throw new IllegalArgumentException("regrowDays");
            if (minHarvest < 0 || maxHarvest < minHarvest) throw new IllegalArgumentException("harvest range");
            if (harvestMaxIncreasePerFarmingLevel < 0.0F) throw new IllegalArgumentException("harvest increase");
            if (extraHarvestChance < 0.0D || extraHarvestChance > 1.0D) {
                throw new IllegalArgumentException("extraHarvestChance");
            }
            if (produceSellPrice < 0 || seedSellPrice < 0) throw new IllegalArgumentException("sell price");
        }

        public int[] seasonIdsArray() {
            return seasons.stream().mapToInt(Integer::intValue).toArray();
        }

        public int[] phaseDaysArray() {
            return phaseDays.stream().mapToInt(Integer::intValue).toArray();
        }

        public boolean isInSeason(int season) {
            return seasons.contains(season);
        }

        public boolean regrows() {
            return regrowDays > 0;
        }

        public int totalGrowthDays() {
            return phaseDays.stream().mapToInt(Integer::intValue).sum();
        }

        public int farmingExperience() {
            return (int) Math.round(16.0D * Math.log(0.018D * produceSellPrice + 1.0D));
        }

        public List<String> seasonNames() {
            return seasons.stream().map(Definition::seasonName).toList();
        }

        private static String seasonName(int season) {
            return switch (season) {
                case 0 -> "spring";
                case 1 -> "summer";
                case 2 -> "fall";
                case 3 -> "winter";
                default -> throw new IllegalArgumentException("Unknown season " + season);
            };
        }
    }
}
