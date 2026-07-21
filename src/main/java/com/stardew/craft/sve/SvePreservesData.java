package com.stardew.craft.sve;

import com.stardew.craft.item.artisan.PreserveType;

import java.util.List;
import java.util.Map;

/** Preserves-jar and dehydrator products derived from the SVE crop catalog. */
public final class SvePreservesData {
    public static final int PRESERVES_JAR_MINUTES = 4_000;
    public static final int DEHYDRATOR_MINUTES = 1_260;
    public static final int DEHYDRATOR_INPUT_COUNT = 5;

    private static final Map<String, String> COLORS = Map.ofEntries(
            Map.entry("cucumber", "#4CAF50"),
            Map.entry("butternut_squash", "#FF9800"),
            Map.entry("gold_carrot", "#FFD700"),
            Map.entry("sweet_potato", "#E65100"),
            Map.entry("joja_berry", "#FF4081"),
            Map.entry("joja_veggie", "#00E676"),
            Map.entry("monster_fruit", "#8E24AA"),
            Map.entry("salal_berry", "#6F37A5"),
            Map.entry("slime_berry", "#8FAE35"),
            Map.entry("ancient_fiber", "#708B50"),
            Map.entry("monster_mushroom", "#FF6F00"),
            Map.entry("void_root", "#56357F"));

    private static final List<Product> PRESERVES_JAR = SveCropData.all().stream()
            .map(SvePreservesData::preservesJarProduct)
            .toList();

    private static final List<Product> DEHYDRATOR_CROPS = SveCropData.all().stream()
            .filter(crop -> crop.produceType() == SveCropData.ProduceType.FRUIT
                    || crop.producePath().equals("monster_mushroom"))
            .map(SvePreservesData::dehydratorProduct)
            .toList();

    private SvePreservesData() {
    }

    public static List<Product> preservesJar() {
        return PRESERVES_JAR;
    }

    public static List<Product> dehydratorCrops() {
        return DEHYDRATOR_CROPS;
    }

    public static Product displayProduct(String inputPath, PreserveType type) {
        return java.util.stream.Stream.concat(PRESERVES_JAR.stream(), DEHYDRATOR_CROPS.stream())
                .filter(product -> product.inputPath().equals(inputPath) && product.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown SVE preserves product: " + inputPath + " / " + type));
    }

    public static String color(String inputPath) {
        String color = COLORS.get(inputPath);
        if (color == null) throw new IllegalArgumentException("Missing SVE preserves color: " + inputPath);
        return color;
    }

    private static Product preservesJarProduct(SveCropData.Definition crop) {
        PreserveType type = crop.produceType() == SveCropData.ProduceType.FRUIT
                ? PreserveType.JELLY
                : PreserveType.PICKLES;
        return product(crop, type, PRESERVES_JAR_MINUTES, 1);
    }

    private static Product dehydratorProduct(SveCropData.Definition crop) {
        PreserveType type = crop.produceType() == SveCropData.ProduceType.FRUIT
                ? PreserveType.DRIED_FRUIT
                : PreserveType.DRIED_MUSHROOMS;
        return product(crop, type, DEHYDRATOR_MINUTES, DEHYDRATOR_INPUT_COUNT);
    }

    private static Product product(
            SveCropData.Definition crop,
            PreserveType type,
            int minutes,
            int consume
    ) {
        String suffix = switch (type) {
            case JELLY -> "jelly";
            case PICKLES -> "pickles";
            case DRIED_FRUIT -> "dried_fruit";
            case DRIED_MUSHROOMS -> "dried_mushrooms";
            default -> throw new IllegalArgumentException("Unsupported crop preserve type: " + type);
        };
        return new Product(
                crop.producePath(),
                crop.producePath() + "_" + suffix,
                "stardewcraft:" + suffix,
                type,
                minutes,
                consume,
                true,
                crop.produceSellPrice(),
                crop.edibility(),
                color(crop.producePath()));
    }

    public record Product(
            String inputPath,
            String displayOutputPath,
            String machineOutputId,
            PreserveType type,
            int processingMinutes,
            int consume,
            boolean keepInputQuality,
            int ingredientPrice,
            int ingredientEdibility,
            String colorHex
    ) {
    }
}
