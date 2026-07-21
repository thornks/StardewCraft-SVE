package com.stardew.craft.sve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Regression checks for SVE consumable stats, animation, buffs, and duration conversion. */
public final class SveConsumableRegressionTest {
    private static final Path ITEM_SOURCE = Path.of("src/main/java/com/stardew/craft/sve/ModItems.java");

    private SveConsumableRegressionTest() {}

    public static void main(String[] args) throws IOException {
        expectEquals(14, SveConsumableData.MINECRAFT_TICKS_PER_SDV_DURATION_UNIT,
                "Stardew duration conversion factor");
        expectEquals(13_440, SveConsumableData.durationTicks(960),
                "Lucky Lunch reference duration");

        Map<String, SveConsumableData.Definition> definitions = definitions();
        expectEquals(18, definitions.size(), "SVE standalone consumable count");
        expectEquals(9L, definitions.values().stream().filter(SveConsumableData.Definition::drink).count(),
                "drink animation count");
        validateGoldenProperties(definitions);
        validateRegistrationWiring(definitions);
        System.out.println("SVE consumable regression suite passed: 18 items, duration factor 14");
    }

    private static Map<String, SveConsumableData.Definition> definitions() {
        Map<String, SveConsumableData.Definition> definitions = new LinkedHashMap<>();
        for (SveConsumableData.Definition definition : SveConsumableData.all()) {
            expect(definitions.put(definition.path(), definition) == null,
                    "duplicate consumable definition " + definition.path());
            definition.buffs().forEach(buff -> expectEquals(
                    SveConsumableData.durationTicks(definition.sveDuration()), buff.durationTicks(),
                    definition.path() + " converted duration"));
        }
        return definitions;
    }

    private static void validateGoldenProperties(Map<String, SveConsumableData.Definition> definitions) {
        Map<String, String> expected = Map.ofEntries(
                stat("aged_blue_moon_wine", "10000/100/true/600/LUCK:7"),
                stat("blue_moon_wine", "700/10/true/180/LUCK:2"),
                stat("dewdrop_berry", "45/90/false/5143/LUCK:2,MAX_ENERGY:50,MAGNETIC_RADIUS:75,SPEED:2"),
                stat("green_mushroom", "1250/500/false/1600/LUCK:1,MAX_ENERGY:200,MAGNETIC_RADIUS:25"),
                stat("grampleton_orange_chicken", "400/65/false/720/FARMING:3"),
                stat("seed_cookie", "35/30/false/600/MAX_ENERGY:30"),
                stat("aegis_elixir", "12000/1/false/25/DEFENSE:255"),
                stat("armor_elixir", "2000/2/false/400/DEFENSE:15"),
                stat("barbarian_elixir", "10000/1/false/100/ATTACK:99"),
                stat("bombardier_elixir", "5000/50/true/250/ATTACK:50"),
                stat("gravity_elixir", "1500/1/true/5143/MAGNETIC_RADIUS:999"),
                stat("haste_elixir", "2000/2/true/400/SPEED:3"),
                stat("hero_elixir", "2650/2/false/400/ATTACK:20"),
                stat("lightning_elixir", "5000/1/true/88/SPEED:8"),
                stat("marsh_tonic", "750/50/true/550/SPEED:1,DEFENSE:5,ATTACK:10"),
                stat("sports_drink", "300/60/true/1000/MAX_ENERGY:50"),
                stat("stamina_capsule", "1000/4/false/1600/MAX_ENERGY:150,SPEED:1"),
                stat("super_joja_cola", "350/150/true/1600/LUCK:2,MAGNETIC_RADIUS:50,SPEED:2")
        );
        expectEquals(expected.keySet(), definitions.keySet(), "golden consumable property set");
        for (SveConsumableData.Definition definition : definitions.values()) {
            String buffs = definition.sourceBuffs().stream()
                    .map(buff -> buff.type().name() + ":" + buff.amount())
                    .collect(Collectors.joining(","));
            String actual = definition.price() + "/" + definition.edibility() + "/" + definition.drink()
                    + "/" + definition.sveDuration() + "/" + buffs;
            expectEquals(expected.get(definition.path()), actual, definition.path() + " properties");
        }
    }

    private static void validateRegistrationWiring(Map<String, SveConsumableData.Definition> definitions)
            throws IOException {
        String source = Files.readString(ITEM_SOURCE);
        for (String path : definitions.keySet()) {
            String expected = path.equals("green_mushroom")
                    ? "byPath(\"green_mushroom\")"
                    : "consumable(\"" + path + "\")";
            expect(source.contains(expected), "item registration must use consumable data for " + path);
        }
        expect(source.contains("createQualityItem(\"stardewcraft.type.forage\", true"),
                "green mushroom must preserve quality support");
        expect(!source.contains("new DishBuff"), "ModItems must not hard-code SVE buffs");
    }

    private static Map.Entry<String, String> stat(String path, String value) {
        return Map.entry(path, value);
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
