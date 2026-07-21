package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stardew.craft.sve.collection.SveCollectionCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Locks object metadata that is easy to confuse with acquisition or collection grouping. */
public final class SveObjectMetadataRegressionTest {
    private static final Path SOURCE = Path.of("src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final Path ASSETS = Path.of("src/main/resources/assets");
    private static final Path DATA = Path.of("src/main/resources/data");

    private static final Set<String> ARTIFACTS = Set.of(
            "amber", "boomerang", "faded_button", "fossilized_apple",
            "old_coin", "rusty_shield", "stone_of_yoba");

    private SveObjectMetadataRegressionTest() {
    }

    public static void main(String[] args) throws IOException {
        String source = Files.readString(SOURCE);
        validateCollections();
        validateItemTypesAndFishStats(source);
        validateSmokedFish(source);
        validateMuseumData();
        System.out.println("SVE object metadata regression suite passed: artifacts="
                + ARTIFACTS.size() + ", fishCollection=43, smokerInputs="
                + SveFishData.SVE_FISH.size());
    }

    private static void validateCollections() {
        Set<String> artifacts = pathsForTab(2);
        expect(artifacts.equals(ARTIFACTS), "Unexpected artifact collection: " + artifacts);

        Set<String> fishCollection = pathsForTab(1);
        Set<String> expectedFishCollection = new LinkedHashSet<>(SveFishData.SVE_FISH);
        expectedFishCollection.remove("razor_trout");
        expectedFishCollection.add("dulse_seaweed");
        expect(fishCollection.size() == 43, "Expected 43 fish collection entries");
        expect(fishCollection.equals(expectedFishCollection),
                "Fish collection must contain category Fish entries, including Dulse but excluding Razor Trout");

        expect(pathsForTab(0).contains("magic_lamp"),
                "Magic Lamp must remain in the shipping collection");
    }

    private static void validateItemTypesAndFishStats(String source) {
        expect(source.contains("new SimpleStardewItem(\"stardewcraft.type.monster_loot\", 15000"),
                "Magic Lamp must be monster loot, not an artifact");
        expect(source.contains("new SimpleStardewItem(\"stardewcraft.type.misc\", 1000"),
                "Money Bag must use the miscellaneous type");
        expect(source.contains("new SimpleStardewItem(\"stardewcraft.type.misc\", 10000"),
                "Ornate Treasure Chest must use the miscellaneous type");

        expectFish(source, "sea_sponge", 75, -62, -11, 40, "sinker");
        expectFish(source, "starfish", 150, -50, -9, 75, "sinker");
        expectFish(source, "swamp_crab", 150, 50, 22, 35, "sinker");
    }

    private static void expectFish(
            String source,
            String id,
            int price,
            int energy,
            int health,
            int difficulty,
            String behavior
    ) {
        Pattern pattern = Pattern.compile(
                "ITEMS\\.register\\(\\\"" + Pattern.quote(id) + "\\\",\\s*\\(\\) -> new FishItem\\("
                        + "\\s*new int\\[\\]\\{" + price + ",[^}]+},"
                        + "\\s*new int\\[\\]\\{" + energy + ",[^}]+},"
                        + "\\s*new int\\[\\]\\{" + health + ",[^}]+},"
                        + "\\s*" + difficulty + ", \\\"" + behavior + "\\\"",
                Pattern.DOTALL);
        expect(pattern.matcher(source).find(), "Incorrect FishItem metadata for " + id);
    }

    private static void validateSmokedFish(String source) throws IOException {
        Set<String> expected = Set.copyOf(SveFishData.SVE_FISH);
        expect(expected.size() == 43, "Expected 43 category -4 smoker inputs");
        expect(!expected.contains("dulse_seaweed"), "Dulse Seaweed must not be smokeable");
        expect(SveContentAcquisitionCatalog.exclusions().containsKey(
                        "stardewcraftsve:smoked_sea_sponge"),
                "Smoked Sea Sponge must remain planned until its SVE resource zone is ported");
        expect(SveContentAcquisitionCatalog.exclusions().containsKey(
                        "stardewcraftsve:smoked_swamp_crab"),
                "Smoked Swamp Crab must remain planned until its SVE resource zone is ported");

        Set<String> registrations = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("SMOKED_FISH_ITEMS\\.register\\(\"smoked_([^\"]+)\"")
                .matcher(source);
        while (matcher.find()) registrations.add(matcher.group(1));
        expect(registrations.equals(expected), "Smoked fish registrations differ: " + registrations);

        Set<String> fishTag = readNamespacedValues(DATA.resolve("stardewcraft/tags/item/fishes.json"));
        expect(fishTag.equals(expected), "Fish item tag differs from smoker inputs: " + fishTag);

        JsonObject smoker = JsonParser.parseString(Files.readString(
                DATA.resolve("stardewcraft/artisan/sve_fish_smoker.json"))).getAsJsonObject();
        Set<String> recipeInputs = new LinkedHashSet<>();
        for (JsonElement recipe : smoker.getAsJsonArray("recipes")) {
            recipeInputs.add(path(recipe.getAsJsonObject().get("input").getAsString()));
        }
        expect(recipeInputs.equals(expected), "Fish smoker recipes differ: " + recipeInputs);

        for (String fish : expected) {
            expect(Files.isRegularFile(ASSETS.resolve(
                    "stardewcraft/textures/item/fish/smoked/" + fish + ".png")),
                    "Missing smoked texture for " + fish);
            for (String namespace : Set.of("stardewcraft", "stardewcraftsve")) {
                for (String suffix : Set.of("", "_base", "_base_silver", "_base_gold",
                        "_base_iridium", "_silver", "_gold", "_iridium")) {
                    expect(Files.isRegularFile(ASSETS.resolve(namespace + "/models/item/smoked_"
                                    + fish + suffix + ".json")),
                            "Missing smoked model for " + namespace + ":" + fish + suffix);
                }
            }
        }
    }

    private static void validateMuseumData() throws IOException {
        Set<String> artifactTag = readNamespacedValues(
                DATA.resolve("stardewcraft/tags/item/artifacts.json"));
        expect(artifactTag.equals(ARTIFACTS), "Artifact tag differs: " + artifactTag);

        JsonObject reward = JsonParser.parseString(Files.readString(DATA.resolve(
                "stardewcraftsve/museum_rewards/rewards/galdoran_gem.json"))).getAsJsonObject();
        Set<String> sveRequirements = new LinkedHashSet<>();
        for (JsonElement value : reward.getAsJsonArray("required_items")) {
            String id = value.getAsString();
            if (id.startsWith("stardewcraftsve:")) sveRequirements.add(path(id));
        }
        expect(sveRequirements.equals(ARTIFACTS),
                "Galdoran reward must require exactly the seven SVE artifacts: " + sveRequirements);
    }

    private static Set<String> pathsForTab(int tab) {
        Set<String> result = new LinkedHashSet<>();
        for (String id : SveCollectionCatalog.configuredItemIdsForTab(tab)) result.add(path(id));
        return Set.copyOf(result);
    }

    private static Set<String> readNamespacedValues(Path file) throws IOException {
        JsonArray values = JsonParser.parseString(Files.readString(file))
                .getAsJsonObject().getAsJsonArray("values");
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement value : values) {
            String id = value.getAsString();
            if (id.startsWith("stardewcraftsve:")) result.add(path(id));
        }
        return Set.copyOf(result);
    }

    private static String path(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
