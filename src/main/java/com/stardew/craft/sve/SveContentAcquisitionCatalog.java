package com.stardew.craft.sve;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Programmatic acquisition routes and explicit non-survival item classifications. */
public final class SveContentAcquisitionCatalog {
    private static final String PREFIX = StardewcraftsveMod.MODID + ":";

    private static final Map<String, Exclusion> EXCLUSIONS = createExclusions();
    private static final Map<String, String> FRIENDSHIP_RECIPE_UNLOCKS = SveCookingData.all().stream()
            .filter(definition -> definition.unlock().type() == SveCookingData.UnlockType.FRIENDSHIP)
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    definition -> PREFIX + definition.path(),
                    definition -> definition.unlock().source() + " friendship "
                            + (definition.unlock().friendshipPoints() / 250) + " hearts"));

    private SveContentAcquisitionCatalog() {
    }

    public static Map<String, Exclusion> exclusions() {
        return EXCLUSIONS;
    }

    public static Map<String, String> friendshipRecipeUnlocks() {
        return FRIENDSHIP_RECIPE_UNLOCKS;
    }

    public static void addProgrammaticRoutes(SveContentAcquisitionGraph graph) {
        addAnimalRoutes(graph);
        addTreeRoutes(graph);
        addCropRoutes(graph);
        addArtifactSpotRoutes(graph);
    }

    private static void addAnimalRoutes(SveContentAcquisitionGraph graph) {
        source(graph, "goose_egg", "animal", "Goose production every other day");
        source(graph, "golden_goose_egg", "animal", "Goose bonus production");
        source(graph, "camel_wool", "animal", "Camel production every other day");
    }

    private static void addTreeRoutes(SveContentAcquisitionGraph graph) {
        source(graph, "fir_cone", "wild_tree", "Fir trees generated on player farms");
        source(graph, "birch_seed", "wild_tree", "Birch trees generated on player farms");
        source(graph, "fir_wax", "tapper", "Tapper attached to a mature fir tree");
        source(graph, "birch_water", "tapper", "Tapper attached to a mature birch tree");

        transform(graph, "pear", "fruit_tree", "Mature pear tree harvest", "pear_sapling");
        transform(graph, "nectarine", "fruit_tree", "Mature nectarine tree harvest", "nectarine_sapling");
        transform(graph, "persimmon", "fruit_tree", "Mature persimmon tree harvest", "persimmon_sapling");
    }

    private static void addCropRoutes(SveContentAcquisitionGraph graph) {
        crop(graph, "cucumber_seed", "cucumber");
        crop(graph, "butternut_squash_seed", "butternut_squash");
        crop(graph, "gold_carrot_seed", "gold_carrot");
        crop(graph, "sweet_potato_seed", "sweet_potato");
        crop(graph, "joja_berry_starter", "joja_berry");
        crop(graph, "joja_veggie_seeds", "joja_veggie");
        crop(graph, "stalk_seed", "monster_fruit");
        crop(graph, "salal_berry_seed", "salal_berry");
        crop(graph, "slime_seed", "slime_berry");
        crop(graph, "ancient_ferns_seed", "ancient_fiber");
        crop(graph, "fungus_seed", "monster_mushroom");
        crop(graph, "void_seed", "void_root");
    }

    private static void addArtifactSpotRoutes(SveContentAcquisitionGraph graph) {
        for (String item : List.of(
                "salal_berry_seed", "ancient_ferns_seed", "faded_button", "old_coin",
                "fossilized_apple", "stone_of_yoba", "amber", "boomerang", "rusty_shield")) {
            source(graph, item, "artifact_spot", "Location-aware SVE artifact spot drop");
        }
    }

    private static void crop(SveContentAcquisitionGraph graph, String seed, String harvest) {
        transform(graph, harvest, "crop", "Mature SVE crop harvest", seed);
    }

    private static void source(SveContentAcquisitionGraph graph, String item, String kind, String detail) {
        graph.addSource(id(item), kind, detail);
    }

    private static void transform(
            SveContentAcquisitionGraph graph,
            String output,
            String kind,
            String detail,
            String... prerequisites
    ) {
        graph.addRoute(id(output), kind, detail,
                java.util.Arrays.stream(prerequisites).map(SveContentAcquisitionCatalog::id).toList());
    }

    private static Map<String, Exclusion> createExclusions() {
        Map<String, Exclusion> exclusions = new LinkedHashMap<>();
        exclude(exclusions, ExclusionType.CREATIVE_ONLY, "Development or creative-mode item",
                "goose_spawn_egg", "camel_spawn_egg", "debug_wand");
        exclude(exclusions, ExclusionType.DISPLAY_ONLY, "JEI flavored-product display stack",
                "joja_berry_jelly", "monster_fruit_jelly", "salal_berry_jelly", "slime_berry_jelly",
                "cucumber_pickles", "butternut_squash_pickles", "gold_carrot_pickles",
                "sweet_potato_pickles", "joja_veggie_pickles", "ancient_fiber_pickles",
                "monster_mushroom_pickles", "void_root_pickles",
                "joja_berry_dried_fruit", "monster_fruit_dried_fruit",
                "salal_berry_dried_fruit", "slime_berry_dried_fruit",
                "green_mushroom_dried_mushrooms", "mega_purple_mushroom_dried_mushrooms",
                "monster_mushroom_dried_mushrooms", "mushroom_colony_dried_mushrooms",
                "poison_mushroom_dried_mushrooms", "sve_roe", "sve_aged_roe");
        exclude(exclusions, ExclusionType.PLANNED_CONTENT,
                "Original SVE data defines the item but no initial acquisition route",
                "fungus_seed", "slime_seed", "stalk_seed", "void_seed",
                "monster_mushroom", "slime_berry", "monster_fruit", "void_root",
                "monster_mushroom_juice", "slime_berry_wine", "monster_fruit_wine", "void_root_juice",
                "void_mayo_sandwich", "super_joja_cola", "super_starfruit");
        exclude(exclusions, ExclusionType.PLANNED_CONTENT,
                "Requires an SVE map, monster, or resource zone that is not ported",
                "green_mushroom", "mega_purple_mushroom", "sea_sponge",
                "sludge", "supernatural_goo", "swamp_crab", "swamp_essence", "swamp_flower",
                "void_shard", "void_soul", "rusty_blade", "gold_slime_egg",
                "bombardier_elixir", "marsh_tonic",
                "diamond_wand", "heavy_shield", "monster_splitter");
        exclude(exclusions, ExclusionType.PLANNED_CONTENT,
                "Requires an unported SVE quest, event, or special interaction",
                "dewdrop_berry", "honey_jar", "golden_key", "mermaid_bracelet", "tree_coin",
                "aurora_vineyard_property_deed", "property_deed",
                "animal_mastery", "brewing_mastery", "cheese_mastery", "crafting_mastery",
                "grape_mastery", "starfruit_mastery", "strawberry_mastery", "warp_magic");
        return Map.copyOf(exclusions);
    }

    private static void exclude(
            Map<String, Exclusion> exclusions,
            ExclusionType type,
            String reason,
            String... paths
    ) {
        for (String path : paths) exclusions.put(id(path), new Exclusion(type, reason));
    }

    private static String id(String path) {
        return PREFIX + path;
    }

    public enum ExclusionType {
        CREATIVE_ONLY,
        DISPLAY_ONLY,
        PLANNED_CONTENT
    }

    public record Exclusion(ExclusionType type, String reason) {
    }
}
