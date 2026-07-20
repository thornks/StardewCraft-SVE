package com.stardew.craft.sve;

import java.util.List;

/** Keg products derived from the authoritative SVE crop catalog. */
public final class SveKegData {
    public static final int WINE_MINUTES = 8_820;
    public static final int JUICE_MINUTES = 5_040;

    private static final List<Product> PRODUCTS = SveCropData.all().stream()
            .map(SveKegData::productFor)
            .toList();

    private SveKegData() {
    }

    public static List<Product> all() {
        return PRODUCTS;
    }

    public static Product byInputPath(String inputPath) {
        return PRODUCTS.stream()
                .filter(product -> product.inputPath().equals(inputPath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SVE keg input: " + inputPath));
    }

    private static Product productFor(SveCropData.Definition crop) {
        boolean wine = crop.produceType() == SveCropData.ProduceType.FRUIT;
        ProductType type = wine ? ProductType.WINE : ProductType.JUICE;
        int sellPrice = wine
                ? crop.produceSellPrice() * 3
                : (int) Math.floor(crop.produceSellPrice() * 2.25D);

        int cropEnergy = (int) Math.ceil(crop.edibility() * 2.5D);
        int cropHealth = (int) (cropEnergy * 0.45F);
        int energy = wine ? (int) (cropEnergy * 1.75D) : cropEnergy * 2;
        int health = wine ? (int) (cropHealth * 1.75D) : cropHealth * 2;

        return new Product(
                crop.producePath(),
                crop.producePath() + (wine ? "_wine" : "_juice"),
                type,
                wine ? WINE_MINUTES : JUICE_MINUTES,
                sellPrice,
                energy,
                health,
                wine);
    }

    public enum ProductType {
        WINE,
        JUICE
    }

    public record Product(
            String inputPath,
            String outputPath,
            ProductType type,
            int processingMinutes,
            int sellPrice,
            int energy,
            int health,
            boolean supportsQuality
    ) {
        public Product {
            if (inputPath == null || inputPath.isBlank()) throw new IllegalArgumentException("inputPath");
            if (outputPath == null || outputPath.isBlank()) throw new IllegalArgumentException("outputPath");
            if (type == null) throw new IllegalArgumentException("type");
            if (processingMinutes <= 0) throw new IllegalArgumentException("processingMinutes");
            if (sellPrice < 0) throw new IllegalArgumentException("sellPrice");
            if (supportsQuality != (type == ProductType.WINE)) {
                throw new IllegalArgumentException("Only wine supports quality: " + outputPath);
            }
        }
    }
}
