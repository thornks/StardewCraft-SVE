package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Regression checks for SVE keg products, recipes, and client assets. */
public final class SveKegRegressionTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path RECIPE = RESOURCES.resolve("data/stardewcraft/artisan/sve_keg.json");
    private static final Path MODELS = RESOURCES.resolve("assets/stardewcraftsve/models/item");
    private static final Path TEXTURES = RESOURCES.resolve("assets/stardewcraftsve/textures/item/artisan");

    private SveKegRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, ExpectedProduct> expected = expectedProducts();
        expectEquals(12, SveKegData.all().size(), "product count");
        expectEquals(4L, SveKegData.all().stream().filter(SveKegData.Product::supportsQuality).count(), "wine count");
        expectEquals(8L, SveKegData.all().stream().filter(product -> !product.supportsQuality()).count(), "juice count");

        Map<String, SveKegData.Product> actual = new LinkedHashMap<>();
        for (SveKegData.Product product : SveKegData.all()) {
            expect(actual.put(product.inputPath(), product) == null, "duplicate input " + product.inputPath());
        }
        expectEquals(expected.keySet(), actual.keySet(), "keg input set");

        for (var entry : expected.entrySet()) {
            SveKegData.Product product = actual.get(entry.getKey());
            ExpectedProduct rule = entry.getValue();
            expectEquals(rule.outputPath(), product.outputPath(), entry.getKey() + " output");
            expectEquals(rule.type(), product.type(), entry.getKey() + " type");
            expectEquals(rule.minutes(), product.processingMinutes(), entry.getKey() + " minutes");
            expectEquals(rule.sellPrice(), product.sellPrice(), entry.getKey() + " price");
            expectEquals(rule.energy(), product.energy(), entry.getKey() + " energy");
            expectEquals(rule.health(), product.health(), entry.getKey() + " health");
            expectEquals(rule.type() == SveKegData.ProductType.WINE,
                    product.supportsQuality(), entry.getKey() + " quality support");
            validateAssets(product);
        }

        validateRecipeJson(actual);
        System.out.println("SVE keg regression suite passed: 4 wines, 8 juices");
    }

    private static void validateRecipeJson(Map<String, SveKegData.Product> products) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(RECIPE)).getAsJsonObject();
        expectEquals("keg", root.get("machine").getAsString(), "machine id");
        expectEquals(products.size(), root.getAsJsonArray("recipes").size(), "recipe count");

        Map<String, JsonObject> recipes = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("recipes")) {
            JsonObject recipe = element.getAsJsonObject();
            String input = recipe.get("input").getAsString();
            expect(input.startsWith("stardewcraftsve:"), "SVE recipe input namespace: " + input);
            expect(recipes.put(input.substring(input.indexOf(':') + 1), recipe) == null,
                    "duplicate recipe input " + input);
            expect(!recipe.get("output").getAsString().contains("blue_moon_wine"),
                    "Blue Moon Wine must remain purchase-only");
        }
        expectEquals(products.keySet(), recipes.keySet(), "recipe input set");

        for (SveKegData.Product product : products.values()) {
            JsonObject recipe = recipes.get(product.inputPath());
            expectEquals("stardewcraftsve:" + product.outputPath(),
                    recipe.get("output").getAsString(), product.inputPath() + " recipe output");
            expectEquals(product.processingMinutes(),
                    recipe.get("minutes").getAsInt(), product.inputPath() + " recipe minutes");
        }
    }

    private static void validateAssets(SveKegData.Product product) throws IOException {
        String suffix = product.type() == SveKegData.ProductType.WINE ? "wine" : "juice";
        Path texture = TEXTURES.resolve(suffix).resolve(product.inputPath() + ".png");
        expect(Files.isRegularFile(texture) && Files.size(texture) > 0,
                "missing texture " + texture);
        var image = ImageIO.read(texture.toFile());
        expect(image != null, "invalid texture " + texture);
        expect(image.getWidth() % 16 == 0 && image.getHeight() % 16 == 0,
                "texture dimensions must support mip level 4: " + texture
                        + " (" + image.getWidth() + "x" + image.getHeight() + ")");

        Path model = MODELS.resolve(product.outputPath() + ".json");
        expect(Files.isRegularFile(model), "missing model " + model);
        JsonObject baseModel = JsonParser.parseString(Files.readString(model)).getAsJsonObject();
        expectEquals("stardewcraftsve:item/artisan/" + suffix + "/" + product.inputPath(),
                baseModel.getAsJsonObject("textures").get("layer0").getAsString(),
                product.outputPath() + " texture reference");

        if (!product.supportsQuality()) {
            expect(!baseModel.has("overrides"), product.outputPath() + " must not have quality overrides");
            return;
        }
        expectEquals(3, baseModel.getAsJsonArray("overrides").size(),
                product.outputPath() + " quality override count");
        for (String quality : new String[]{"silver", "gold", "iridium"}) {
            Path qualityModel = MODELS.resolve(product.outputPath() + "_" + quality + ".json");
            expect(Files.isRegularFile(qualityModel), "missing quality model " + qualityModel);
        }
    }

    private static Map<String, ExpectedProduct> expectedProducts() {
        return Map.ofEntries(
                expected("joja_berry", "joja_berry_wine", SveKegData.ProductType.WINE, 8820, 1950, 329, 147),
                expected("monster_fruit", "monster_fruit_wine", SveKegData.ProductType.WINE, 8820, 4575, 372, 166),
                expected("salal_berry", "salal_berry_wine", SveKegData.ProductType.WINE, 8820, 225, 122, 54),
                expected("slime_berry", "slime_berry_wine", SveKegData.ProductType.WINE, 8820, 195, -43, -19),
                expected("cucumber", "cucumber_juice", SveKegData.ProductType.JUICE, 5040, 78, 90, 40),
                expected("butternut_squash", "butternut_squash_juice", SveKegData.ProductType.JUICE, 5040, 450, 100, 44),
                expected("gold_carrot", "gold_carrot_juice", SveKegData.ProductType.JUICE, 5040, 2250, 576, 258),
                expected("sweet_potato", "sweet_potato_juice", SveKegData.ProductType.JUICE, 5040, 630, 100, 44),
                expected("joja_veggie", "joja_veggie_juice", SveKegData.ProductType.JUICE, 5040, 2565, 1000, 450),
                expected("ancient_fiber", "ancient_fiber_juice", SveKegData.ProductType.JUICE, 5040, 326, 176, 78),
                expected("monster_mushroom", "monster_mushroom_juice", SveKegData.ProductType.JUICE, 5040, 1912, 376, 168),
                expected("void_root", "void_root_juice", SveKegData.ProductType.JUICE, 5040, 528, -174, -78));
    }

    private static Map.Entry<String, ExpectedProduct> expected(
            String input, String output, SveKegData.ProductType type,
            int minutes, int sellPrice, int energy, int health
    ) {
        return Map.entry(input, new ExpectedProduct(output, type, minutes, sellPrice, energy, health));
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private record ExpectedProduct(
            String outputPath,
            SveKegData.ProductType type,
            int minutes,
            int sellPrice,
            int energy,
            int health
    ) {
    }
}
