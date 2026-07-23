package com.stardew.craft.sve;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Shared filesystem fixtures for offline acquisition regression tests. */
final class SveTestContentResources {
    private static final Path DATA_ROOT = Path.of("src/main/resources/data");
    private static final Path ITEM_SOURCE = Path.of(
            "src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final List<Pattern> REGISTRATION_PATTERNS = List.of(
            Pattern.compile("(?<![A-Z_])(?:ITEMS|STARDEWCRAFT_ITEMS)"
                    + "\\.register\\(\\s*\"([^\"]+)\""),
            Pattern.compile("artisan\\(\\s*\"([^\"]+)\""),
            Pattern.compile("\\breg\\(\\s*\"([^\"]+)\""));

    private SveTestContentResources() {
    }

    static Set<String> registeredItems() throws IOException {
        Set<String> items = new LinkedHashSet<>();
        String source = Files.readString(ITEM_SOURCE);
        for (Pattern pattern : REGISTRATION_PATTERNS) {
            var matcher = pattern.matcher(source);
            while (matcher.find()) items.add(StardewcraftsveMod.MODID + ":" + matcher.group(1));
        }
        SveKegData.all().stream()
                .map(product -> StardewcraftsveMod.MODID + ":" + product.outputPath())
                .forEach(items::add);
        Stream.concat(SvePreservesData.preservesJar().stream(), SvePreservesData.dehydratorCrops().stream())
                .map(product -> StardewcraftsveMod.MODID + ":" + product.displayOutputPath())
                .forEach(items::add);
        SveFishData.SVE_FISH.stream()
                .map(path -> StardewcraftsveMod.MODID + ":smoked_" + path)
                .forEach(items::add);
        return Set.copyOf(items);
    }

    static Map<String, JsonElement> loadResources() throws IOException {
        Map<String, JsonElement> resources = new LinkedHashMap<>();
        try (var paths = Files.walk(DATA_ROOT)) {
            for (Path file : paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json")).toList()) {
                Path relative = DATA_ROOT.relativize(file);
                if (relative.getNameCount() < 2) continue;
                String namespace = relative.getName(0).toString();
                if (!namespace.equals("stardewcraft") && !namespace.equals(StardewcraftsveMod.MODID)) continue;
                String resourcePath = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                resources.put(namespace + ":" + resourcePath,
                        JsonParser.parseString(Files.readString(file)));
            }
        }
        return Map.copyOf(resources);
    }
}
