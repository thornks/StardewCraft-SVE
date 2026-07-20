package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extracts acquisition roots and transformations from effective data-pack JSON. */
public final class SveContentDataScanner {
    private static final String SVE_PREFIX = StardewcraftsveMod.MODID + ":";

    private SveContentDataScanner() {
    }

    public static void scan(Map<String, JsonElement> resources, SveContentAcquisitionGraph graph) {
        Map<String, Recipe> cookingRecipes = new LinkedHashMap<>();
        Map<String, Recipe> craftingRecipes = new LinkedHashMap<>();
        Map<String, List<String>> recipeUnlocks = new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> resource : resources.entrySet()) {
            if (!resource.getValue().isJsonObject()) continue;
            String path = path(resource.getKey());
            if (path.startsWith("cooking/recipes/") && path.endsWith(".json")) {
                readRecipe(resource, "cooking/recipes/", cookingRecipes);
            } else if (path.startsWith("player/crafting_recipes/") && path.endsWith(".json")) {
                readRecipe(resource, "player/crafting_recipes/", craftingRecipes);
            }
        }

        SveContentAcquisitionCatalog.friendshipRecipeUnlocks().forEach((recipe, detail) ->
                recipeUnlocks.computeIfAbsent(recipe, ignored -> new ArrayList<>()).add(detail));

        for (Map.Entry<String, JsonElement> resource : resources.entrySet()) {
            JsonElement value = resource.getValue();
            if (!value.isJsonObject() && !value.isJsonArray()) continue;
            String path = path(resource.getKey());
            if (path.startsWith("shops/") && value.isJsonObject()) {
                scanShop(resource.getKey(), value.getAsJsonObject(), graph, recipeUnlocks);
            } else if (path.startsWith("mail/") && value.isJsonArray()) {
                scanMail(resource.getKey(), value.getAsJsonArray(), graph, recipeUnlocks);
            } else if (path.startsWith("fishing/locations/") && value.isJsonObject()) {
                scanFishingLocation(resource.getKey(), value.getAsJsonObject(), graph);
            } else if (path.equals("fishing/fishing_treasure.json") && value.isJsonObject()) {
                scanFishingTreasure(resource.getKey(), value.getAsJsonObject(), graph);
            } else if (path.startsWith("forage_zones/") && value.isJsonObject()) {
                scanForageZone(resource.getKey(), value.getAsJsonObject(), graph);
            } else if (path.startsWith("fishpond/pond_data/") && value.isJsonObject()) {
                scanFishPond(resource.getKey(), value.getAsJsonObject(), graph);
            } else if (path.startsWith("artisan/") && value.isJsonObject()) {
                scanArtisan(resource.getKey(), value.getAsJsonObject(), graph);
            }
        }

        craftingRecipes.forEach((id, recipe) -> graph.addRoute(
                recipe.output(), "crafting", id, recipe.ingredients()));
        cookingRecipes.forEach((id, recipe) -> {
            List<String> unlocks = recipeUnlocks.get(id);
            if (unlocks == null || unlocks.isEmpty()) return;
            for (String unlock : unlocks) {
                graph.addRoute(recipe.output(), "cooking", id + " via " + unlock, recipe.ingredients());
            }
        });
    }

    private static void readRecipe(
            Map.Entry<String, JsonElement> resource,
            String prefix,
            Map<String, Recipe> target
    ) {
        JsonObject object = resource.getValue().getAsJsonObject();
        String output = string(object, "output");
        if (output == null) return;
        List<String> ingredients = new ArrayList<>();
        JsonArray array = array(object, "ingredients");
        if (array != null) {
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                String item = string(element.getAsJsonObject(), "item");
                if (item != null) ingredients.add(item);
            }
        }
        String resourcePath = path(resource.getKey());
        String recipePath = resourcePath.substring(prefix.length(), resourcePath.length() - ".json".length());
        target.put(namespace(resource.getKey()) + ":" + recipePath, new Recipe(output, ingredients));
    }

    private static void scanShop(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph,
            Map<String, List<String>> recipeUnlocks
    ) {
        JsonArray entries = array(object, "entries");
        if (entries == null) return;
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String item = string(entry, "item");
            if (item == null) continue;
            if (item.startsWith("recipe:")) {
                String recipe = item.substring("recipe:".length());
                recipeUnlocks.computeIfAbsent(recipe, ignored -> new ArrayList<>())
                        .add("shop " + resourceId);
                continue;
            }
            List<String> prerequisites = new ArrayList<>();
            String tradeItem = string(entry, "trade_item");
            if (tradeItem != null) prerequisites.add(tradeItem);
            graph.addRoute(item, "shop", resourceId, prerequisites);
        }
    }

    private static void scanMail(
            String resourceId,
            JsonArray letters,
            SveContentAcquisitionGraph graph,
            Map<String, List<String>> recipeUnlocks
    ) {
        for (JsonElement element : letters) {
            if (!element.isJsonObject()) continue;
            JsonObject letter = element.getAsJsonObject();
            String mailId = string(letter, "id");
            String detail = resourceId + (mailId == null ? "" : "#" + mailId);
            JsonArray attached = array(letter, "attachedItems");
            if (attached != null) {
                for (JsonElement attachedElement : attached) {
                    if (!attachedElement.isJsonObject()) continue;
                    String item = string(attachedElement.getAsJsonObject(), "id");
                    if (item != null) graph.addSource(item, "mail", detail);
                }
            }
            String learnedRecipe = string(letter, "learnedRecipe");
            if (learnedRecipe != null) {
                recipeUnlocks.computeIfAbsent(learnedRecipe, ignored -> new ArrayList<>())
                        .add("mail " + detail);
            }
        }
    }

    private static void scanFishingLocation(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        JsonArray fish = array(object, "fish");
        if (fish == null) return;
        for (JsonElement element : fish) {
            if (!element.isJsonObject()) continue;
            String item = string(element.getAsJsonObject(), "item");
            if (item != null) graph.addSource(item, "fishing", resourceId);
        }
    }

    private static void scanFishingTreasure(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        for (String pool : List.of("commonLoot", "rareLoot", "goldenLoot")) {
            JsonArray entries = array(object, pool);
            if (entries == null) continue;
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) continue;
                String item = string(element.getAsJsonObject(), "item");
                if (item != null) graph.addSource(item, "fishing_treasure", resourceId + "#" + pool);
            }
        }
    }

    private static void scanForageZone(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        JsonArray entries = array(object, "entries");
        if (entries == null) return;
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            String block = string(element.getAsJsonObject(), "block");
            if (block == null) continue;
            int separator = block.indexOf(':');
            if (separator < 0) continue;
            String itemPath = block.substring(separator + 1);
            if (!itemPath.startsWith("forage_")) continue;
            graph.addSource(block.substring(0, separator + 1) + itemPath.substring("forage_".length()),
                    "forage", resourceId);
        }
    }

    private static void scanFishPond(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        String fish = string(object, "fish");
        JsonArray productions = array(object, "produced_items");
        if (fish == null || productions == null) return;
        for (JsonElement element : productions) {
            if (!element.isJsonObject()) continue;
            String item = string(element.getAsJsonObject(), "item");
            if (item != null) graph.addRoute(item, "fish_pond", resourceId, List.of(fish));
        }
    }

    private static void scanArtisan(
            String resourceId,
            JsonObject object,
            SveContentAcquisitionGraph graph
    ) {
        JsonArray recipes = array(object, "recipes");
        if (recipes == null) return;
        for (JsonElement element : recipes) {
            if (!element.isJsonObject()) continue;
            JsonObject recipe = element.getAsJsonObject();
            String input = string(recipe, "input");
            if (input == null) continue;
            String output = string(recipe, "output");
            if (output != null) graph.addRoute(output, "machine", resourceId, List.of(input));

            String outputMode = string(recipe, "outputMode");
            if ("smoked".equals(outputMode) && input.startsWith(SVE_PREFIX)) {
                graph.addRoute(SVE_PREFIX + "smoked_" + input.substring(SVE_PREFIX.length()),
                        "fish_smoker", resourceId, List.of(input));
            } else if ("seedmaker".equals(outputMode)) {
                String seed = seedForCrop(input);
                if (seed != null) graph.addRoute(seed, "seed_maker", resourceId, List.of(input));
            }
        }
    }

    private static String seedForCrop(String crop) {
        return switch (crop) {
            case "stardewcraftsve:cucumber" -> "stardewcraftsve:cucumber_seed";
            case "stardewcraftsve:butternut_squash" -> "stardewcraftsve:butternut_squash_seed";
            case "stardewcraftsve:sweet_potato" -> "stardewcraftsve:sweet_potato_seed";
            case "stardewcraftsve:joja_veggie" -> "stardewcraftsve:joja_veggie_seeds";
            case "stardewcraftsve:joja_berry" -> "stardewcraftsve:joja_berry_starter";
            case "stardewcraftsve:monster_fruit" -> "stardewcraftsve:stalk_seed";
            default -> null;
        };
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static String namespace(String resourceId) {
        int separator = resourceId.indexOf(':');
        return separator < 0 ? "minecraft" : resourceId.substring(0, separator);
    }

    private static String path(String resourceId) {
        int separator = resourceId.indexOf(':');
        return separator < 0 ? resourceId : resourceId.substring(separator + 1);
    }

    private record Recipe(String output, List<String> ingredients) {
        private Recipe {
            ingredients = List.copyOf(ingredients);
        }
    }
}
