package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regression checks for SVE cooking ingredients, item properties, and unlock routes. */
public final class SveCookingRegressionTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path DATA = RESOURCES.resolve("data");
    private static final Path RECIPES = DATA.resolve("stardewcraftsve/cooking/recipes");
    private static final Path ITEM_SOURCE = Path.of("src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final Path VOID_MAYO_SOURCE =
            Path.of("src/main/java/com/stardew/craft/sve/VoidMayoSandwichItem.java");
    private static final Set<String> NON_COOKING_OUTPUTS = Set.of(
            "grampleton_orange_chicken", "seed_cookie", "void_mayo_sandwich");

    private SveCookingRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, SveCookingData.Definition> definitions = definitions();
        expectEquals(26, definitions.size(), "SVE cooking definition count");
        expectEquals(16L, definitions.values().stream()
                .filter(definition -> definition.unlock().type() == SveCookingData.UnlockType.SHOP).count(),
                "shop unlock count");
        expectEquals(9L, definitions.values().stream()
                .filter(definition -> definition.unlock().type() == SveCookingData.UnlockType.FRIENDSHIP).count(),
                "friendship unlock count");
        expectEquals(1L, definitions.values().stream()
                .filter(definition -> definition.unlock().type() == SveCookingData.UnlockType.MAIL).count(),
                "mail unlock count");
        expectEquals(1L, definitions.values().stream().filter(SveCookingData.Definition::drink).count(),
                "drink count");

        validateGoldenProperties(definitions);
        validateRecipes(definitions);
        validateRegistrationWiring(definitions.keySet());
        validateVoidMayoSandwich();
        validateUnlocks(definitions);
        validateCropUse(definitions.values());
        System.out.println("SVE cooking regression suite passed: 26 recipes and 26 unlock routes");
    }

    private static Map<String, SveCookingData.Definition> definitions() {
        Map<String, SveCookingData.Definition> definitions = new LinkedHashMap<>();
        for (SveCookingData.Definition definition : SveCookingData.all()) {
            expect(definitions.put(definition.path(), definition) == null,
                    "duplicate cooking definition " + definition.path());
        }
        return definitions;
    }

    private static void validateGoldenProperties(Map<String, SveCookingData.Definition> definitions) {
        Map<String, String> expected = Map.ofEntries(
                stat("baked_berry_oatmeal_supreme", "400/80/false/FARMING:4:96000,FORAGING:4:96000,SPEED:2:96000"),
                stat("big_bark_burger", "400/85/false/SPEED:1:28800,ATTACK:3:28800"),
                stat("flower_cookie", "185/75/false/LUCK:2:48000,FORAGING:2:48000,SPEED:2:48000"),
                stat("frog_legs", "400/30/false/SPEED:2:28800,DEFENSE:2:28800"),
                stat("glazed_butterfish", "800/80/false/FISHING:2:28800,LUCK:2:28800"),
                stat("mixed_berry_pie", "250/75/false/FARMING:3:96000,MAX_ENERGY:50:96000"),
                stat("mushroom_berry_rice", "115/1/false/MINING:3:48000,MAX_ENERGY:-50:48000,MAGNETIC_RADIUS:32:48000,DEFENSE:3:48000,ATTACK:3:48000"),
                stat("seaweed_salad", "200/70/false/FISHING:1:54000,MAX_ENERGY:30:54000"),
                stat("void_delight", "800/1/false/MINING:2:36000,LUCK:2:36000,MAX_ENERGY:20:36000,MAGNETIC_RADIUS:2:36000,SPEED:2:36000,ATTACK:3:36000"),
                stat("void_salmon_sushi", "800/1/false/FISHING:3:72000,LUCK:3:72000,MAX_ENERGY:80:72000,DEFENSE:5:72000"),
                stat("birch_syrup", "500/35/true/"),
                stat("candy", "2000/50/false/LUCK:1:30000,MAX_ENERGY:70:30000,SPEED:1:30000"),
                stat("chocolate_truffle_bar", "4500/75/false/"),
                stat("vegan_cone", "350/50/false/"),
                stat("ice_cream_sundae", "400/80/false/MAX_ENERGY:75:36000,SPEED:1:36000"),
                stat("prismatic_pop", "2500/100/false/MINING:1:43200,LUCK:3:43200,SPEED:1:43200,DEFENSE:3:43200,ATTACK:3:43200"),
                stat("fish_dumpling", "100/45/false/FISHING:1:24000"),
                stat("gingerbread_man", "125/50/false/"),
                stat("ramen", "250/75/false/"),
                stat("baked_potato", "225/70/false/"),
                stat("grilled_cheese_sandwich", "100/85/false/"),
                stat("pineapple_custard_crepe", "300/90/false/LUCK:3:30000,SPEED:1:30000"),
                stat("nectarine_fruit_bread", "150/60/false/"),
                stat("glazed_pear", "400/100/false/MINING:2:42000,MAGNETIC_RADIUS:32:42000,SPEED:1:42000,DEFENSE:6:42000"),
                stat("stuffed_persimmon", "500/110/false/FORAGING:5:36000"),
                stat("cheese_charcuterie", "750/120/false/FARMING:4:24000")
        );
        expectEquals(expected.keySet(), definitions.keySet(), "golden cooking property set");
        for (SveCookingData.Definition definition : definitions.values()) {
            String buffs = definition.buffs().stream()
                    .map(buff -> buff.type().name() + ":" + buff.level() + ":" + buff.durationTicks())
                    .collect(java.util.stream.Collectors.joining(","));
            String actual = definition.price() + "/" + definition.edibility() + "/"
                    + definition.drink() + "/" + buffs;
            expectEquals(expected.get(definition.path()), actual, definition.path() + " properties");
        }
    }

    private static Map.Entry<String, String> stat(String path, String value) {
        return Map.entry(path, value);
    }

    private static void validateRecipes(Map<String, SveCookingData.Definition> definitions) throws IOException {
        Map<String, Path> files = new LinkedHashMap<>();
        try (var paths = Files.list(RECIPES)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).sorted().toList()) {
                String name = path.getFileName().toString();
                files.put(name.substring(0, name.length() - ".json".length()), path);
            }
        }
        expectEquals(definitions.keySet(), files.keySet(), "cooking recipe file set");
        for (SveCookingData.Definition definition : definitions.values()) {
            JsonObject recipe = JsonParser.parseString(Files.readString(files.get(definition.path()))).getAsJsonObject();
            expectEquals("stardewcraftsve:" + definition.path(), recipe.get("output").getAsString(),
                    definition.path() + " output");
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            expectEquals(definition.ingredients().size(), ingredients.size(), definition.path() + " ingredient count");
            for (int index = 0; index < ingredients.size(); index++) {
                validateIngredient(definition.path(), index, definition.ingredients().get(index),
                        ingredients.get(index).getAsJsonObject());
            }
        }
        for (String excluded : NON_COOKING_OUTPUTS) {
            expect(!Files.exists(RECIPES.resolve(excluded + ".json")),
                    excluded + " must not be a cooking recipe");
        }
    }

    private static void validateIngredient(
            String recipe,
            int index,
            SveCookingData.Ingredient expected,
            JsonObject actual
    ) {
        String label = recipe + " ingredient " + index;
        expectEquals(expected.count(), actual.has("count") ? actual.get("count").getAsInt() : 1,
                label + " count");
        switch (expected.kind()) {
            case ITEM -> expectEquals(expected.ids().getFirst(), actual.get("item").getAsString(), label + " item");
            case TAG -> {
                expectEquals(expected.ids().getFirst(), actual.get("tag").getAsString(), label + " tag");
                expectEquals(expected.displayItem(), actual.get("display_item").getAsString(), label + " display item");
            }
            case CATEGORIES -> {
                List<String> categories = actual.getAsJsonArray("categories").asList().stream()
                        .map(JsonElement::getAsString).toList();
                expectEquals(expected.ids(), categories, label + " categories");
                expectEquals(expected.displayItem(), actual.get("display_item").getAsString(), label + " display item");
            }
        }
    }

    private static void validateRegistrationWiring(Set<String> definitions) throws IOException {
        String source = Files.readString(ITEM_SOURCE);
        for (String path : definitions) {
            expect(source.contains("() -> cooking(\"" + path + "\")"),
                    "item registration must use cooking data for " + path);
        }
        for (String excluded : NON_COOKING_OUTPUTS) {
            expect(!source.contains("() -> cooking(\"" + excluded + "\")"),
                    excluded + " must stay outside the cooking catalog");
        }
    }

    private static void validateVoidMayoSandwich() throws IOException {
        String registration = Files.readString(ITEM_SOURCE);
        String implementation = Files.readString(VOID_MAYO_SOURCE);
        expect(registration.contains("new VoidMayoSandwichItem(stackableProperties())"),
                "void mayo sandwich must use its dedicated item behavior");
        expect(implementation.contains("NAUSEA_DURATION_TICKS = 10 * 20"),
                "void mayo sandwich nausea must last 10 seconds");
        expect(implementation.contains("new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION_TICKS)"),
                "void mayo sandwich must apply nausea after consumption");
        expect(implementation.contains("super(100, 1, List.of(), properties, false)"),
                "void mayo sandwich nausea must stay out of the displayed food buff list");
    }

    private static void validateUnlocks(Map<String, SveCookingData.Definition> definitions) throws IOException {
        Map<String, String> shopUnlocks = scanShopUnlocks();
        Map<String, String> mailUnlocks = scanMailUnlocks();
        Map<String, SveFriendshipRewards.RecipeUnlock> friendshipUnlocks = SveFriendshipRewards.recipeUnlocks();

        for (SveCookingData.Definition definition : definitions.values()) {
            String recipeId = "stardewcraftsve:" + definition.path();
            SveCookingData.Unlock unlock = definition.unlock();
            switch (unlock.type()) {
                case SHOP -> expectEquals(unlock.source(), shopUnlocks.get(recipeId), recipeId + " shop unlock");
                case MAIL -> {
                    expectEquals(unlock.source(), mailUnlocks.get(recipeId), recipeId + " mail unlock");
                    expectEquals(unlock.friendshipPoints(), 750, recipeId + " mail threshold");
                }
                case FRIENDSHIP -> expectEquals(
                        new SveFriendshipRewards.RecipeUnlock(unlock.source(), unlock.friendshipPoints()),
                        friendshipUnlocks.get(recipeId), recipeId + " friendship unlock");
            }
        }
        expectEquals(9, friendshipUnlocks.size(), "runtime friendship recipe count");
    }

    private static Map<String, String> scanShopUnlocks() throws IOException {
        Map<String, String> unlocks = new LinkedHashMap<>();
        try (var paths = Files.walk(DATA)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/shops/"))
                    .filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonElement parsed = JsonParser.parseString(Files.readString(file));
                if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("entries")) continue;
                String source = resourceStem(file);
                for (JsonElement element : parsed.getAsJsonObject().getAsJsonArray("entries")) {
                    JsonObject entry = element.getAsJsonObject();
                    if (!entry.has("item")) continue;
                    String item = entry.get("item").getAsString();
                    if (!item.startsWith("recipe:stardewcraftsve:")) continue;
                    String previous = unlocks.put(item.substring("recipe:".length()), source);
                    expect(previous == null, "duplicate cooking shop unlock " + item);
                }
            }
        }
        return unlocks;
    }

    private static Map<String, String> scanMailUnlocks() throws IOException {
        Map<String, String> unlocks = new LinkedHashMap<>();
        try (var paths = Files.walk(DATA)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/mail/"))
                    .filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonElement parsed = JsonParser.parseString(Files.readString(file));
                if (!parsed.isJsonArray()) continue;
                for (JsonElement element : parsed.getAsJsonArray()) {
                    JsonObject mail = element.getAsJsonObject();
                    if (!mail.has("learnedRecipe")) continue;
                    String recipe = mail.get("learnedRecipe").getAsString();
                    if (!recipe.startsWith("stardewcraftsve:")) continue;
                    expect(mail.has("recipeIsCooking") && mail.get("recipeIsCooking").getAsBoolean(),
                            recipe + " mail must mark a cooking recipe");
                    String id = mail.get("id").getAsString();
                    String source = id.substring(0, id.indexOf('_'));
                    String previous = unlocks.put(recipe, source);
                    expect(previous == null, "duplicate cooking mail unlock " + recipe);
                }
            }
        }
        return unlocks;
    }

    private static String resourceStem(Path file) {
        Path relative = DATA.relativize(file);
        String namespace = relative.getName(0).toString();
        String name = file.getFileName().toString();
        return namespace + ":" + name.substring(0, name.length() - ".json".length());
    }

    private static void validateCropUse(java.util.Collection<SveCookingData.Definition> definitions) {
        Set<String> crops = SveCropData.all().stream()
                .map(crop -> "stardewcraftsve:" + crop.producePath())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> used = new LinkedHashSet<>();
        definitions.stream().flatMap(definition -> definition.ingredients().stream())
                .filter(ingredient -> ingredient.kind() == SveCookingData.IngredientKind.ITEM)
                .map(ingredient -> ingredient.ids().getFirst())
                .filter(crops::contains)
                .forEach(used::add);
        expectEquals(Set.of("stardewcraftsve:salal_berry"), used,
                "original SVE crops used directly in cooking");
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
