package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regression checks for SVE shop items, prices, stock, trades, and availability conditions. */
public final class SveShopRegressionTest {
    private static final Path DATA = Path.of("src/main/resources/data");

    private SveShopRegressionTest() {}

    public static void main(String[] args) throws IOException {
        Map<String, SveShopData.Shop> shops = new LinkedHashMap<>();
        int entryCount = 0;
        for (SveShopData.Shop shop : SveShopData.all()) {
            expect(shops.put(shop.id(), shop) == null, "duplicate shop definition " + shop.id());
            entryCount += shop.entries().size();
        }
        expectEquals(21, shops.size(), "audited shop count");
        expectEquals(88, entryCount, "audited shop entry count");
        expectEquals(6, SveShopData.sourceOmissions().size(), "explicit unregistered source rows");
        expectEquals(List.of("ChloeVendor1", "ChloeVendor3", "ChloeVendor4"),
                SveShopData.deferredShopVariants(), "deferred NPC-scheduled shop variants");

        for (SveShopData.Shop shop : shops.values()) validateShop(shop);
        System.out.println("SVE shop regression suite passed: 21 shops, 88 audited entries");
    }

    private static void validateShop(SveShopData.Shop expectedShop) throws IOException {
        String[] id = expectedShop.id().split(":", 2);
        Path file = DATA.resolve(id[0]).resolve("shops").resolve(id[1] + ".json");
        expect(Files.exists(file), "missing shop file " + expectedShop.id());
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        JsonArray rawEntries = root.getAsJsonArray("entries");
        expect(rawEntries != null, expectedShop.id() + " missing entries");

        Map<String, List<JsonObject>> actual = new LinkedHashMap<>();
        for (JsonElement element : rawEntries) {
            JsonObject entry = element.getAsJsonObject();
            String item = entry.get("item").getAsString();
            actual.computeIfAbsent(item, ignored -> new ArrayList<>()).add(entry);
        }

        Map<String, SveShopData.Entry> expected = new LinkedHashMap<>();
        for (SveShopData.Entry entry : expectedShop.entries()) {
            expect(expected.put(entry.item(), entry) == null,
                    expectedShop.id() + " duplicate catalog item " + entry.item());
            List<JsonObject> rows = actual.get(entry.item());
            expect(rows != null && !rows.isEmpty(), expectedShop.id() + " missing item " + entry.item());
            validateEntry(expectedShop.id(), entry, rows.getFirst());
        }

        if (expectedShop.id().startsWith("stardewcraftsve:")) {
            expectEquals(expected.keySet(), actual.keySet(), expectedShop.id() + " complete inventory");
            actual.forEach((item, rows) -> expectEquals(1, rows.size(), expectedShop.id() + " duplicate " + item));
        }
    }

    private static void validateEntry(String shop, SveShopData.Entry expected, JsonObject actual) {
        String label = shop + " / " + expected.item();
        expectEquals(expected.price(), integer(actual, "price", 0), label + " price");
        expectEquals(expected.stock(), integer(actual, "stock", 0), label + " stock");
        expectEquals(expected.tradeItem(), string(actual, "trade_item"), label + " trade item");
        expectEquals(expected.tradeItemCount(), integer(actual, "trade_item_count", 0), label + " trade count");
        expectEquals(expected.seasons(), integers(actual.getAsJsonArray("seasons")), label + " seasons");
        expectEquals(expected.minYear(), integer(actual, "min_year", 0), label + " minimum year");
        expectEquals(expected.mailFlag(), string(actual, "mail_flag"), label + " mail flag");
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : null;
    }

    private static List<Integer> integers(JsonArray array) {
        if (array == null) return List.of();
        List<Integer> values = new ArrayList<>(array.size());
        for (JsonElement element : array) values.add(element.getAsInt());
        return List.copyOf(values);
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
