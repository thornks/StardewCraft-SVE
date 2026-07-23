package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps the strict Mixin config synchronized with its source files. */
public final class SveMixinConfigTest {
    private static final Path CONFIG = Path.of(
            "src/main/resources/stardewcraftsve.mixins.json");
    private static final Path SOURCE_ROOT = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin");
    private static final int EXPECTED_COMMON_MIXINS = 38;
    private static final int EXPECTED_CLIENT_MIXINS = 5;

    private SveMixinConfigTest() {
    }

    public static void main(String[] args) throws IOException {
        JsonObject config = JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();
        Set<String> common = entries(config.getAsJsonArray("mixins"));
        Set<String> client = entries(config.getAsJsonArray("client"));

        expectEquals(EXPECTED_COMMON_MIXINS, common.size(), "common Mixin count");
        expectEquals(EXPECTED_CLIENT_MIXINS, client.size(), "client Mixin count");

        Set<String> overlap = new HashSet<>(common);
        overlap.retainAll(client);
        expect(overlap.isEmpty(), "Mixins configured on both sides: " + overlap);

        Set<String> configured = new LinkedHashSet<>(common);
        configured.addAll(client);
        Set<String> sources;
        try (var paths = Files.list(SOURCE_ROOT)) {
            sources = paths
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.java$", ""))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Set<String> missingSources = new LinkedHashSet<>(configured);
        missingSources.removeAll(sources);
        expect(missingSources.isEmpty(), "Configured Mixins without source: " + missingSources);

        Set<String> unconfiguredSources = new LinkedHashSet<>(sources);
        unconfiguredSources.removeAll(configured);
        expect(unconfiguredSources.isEmpty(), "Mixin sources absent from config: " + unconfiguredSources);
        expect(!configured.contains("BundleDataReloadMixin"),
                "bundle reload must use AddReloadListenerEvent, not a host-internal Mixin");

        System.out.println("SVE Mixin config regression suite passed: common="
                + common.size() + ", client=" + client.size());
    }

    private static Set<String> entries(JsonArray array) {
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement element : array) {
            String name = element.getAsString();
            expect(result.add(name), "Duplicate Mixin entry: " + name);
        }
        return result;
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
