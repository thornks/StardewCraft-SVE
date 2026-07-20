package com.stardew.craft.sve.animal;

import com.stardew.craft.animal.model.AnimalTypeCatalog;
import com.stardew.craft.animal.service.AnimalShopService;
import com.stardew.craft.api.v1.agriculture.StardewAnimalData;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SveAnimalRulesTest {
    private SveAnimalRulesTest() {
    }

    public static void main(String[] args) {
        shopAndGrowthRulesMatchSveDesign();
        salePricesClampFriendshipAndReachExactCaps();
        camelCannotReproduce();
        definitionsDriveCompatibilityAdapters();
        incubationAndProductionRulesMatchRuntimeFlow();
        System.out.println("SVE animal rule regression suite passed");
    }

    private static void shopAndGrowthRulesMatchSveDesign() {
        expectEquals(12_000, SveAnimalRules.purchasePrice(SveAnimalRules.GOOSE_ID), "goose purchase price");
        expectEquals(24_000, SveAnimalRules.purchasePrice(SveAnimalRules.CAMEL_ID), "camel purchase price");
        expectEquals(5, SveAnimalRules.daysToMature(SveAnimalRules.GOOSE_ID), "goose maturity");
        expectEquals(5, SveAnimalRules.daysToMature(SveAnimalRules.CAMEL_ID), "camel maturity");
        expectEquals(2, SveAnimalRules.produceIntervalDays(SveAnimalRules.GOOSE_ID), "goose produce interval");
        expectEquals(2, SveAnimalRules.produceIntervalDays(SveAnimalRules.CAMEL_ID), "camel produce interval");
        expectEquals("coop", SveAnimalRules.requiredBuildingFamily(SveAnimalRules.GOOSE_ID), "goose building");
        expectEquals("barn", SveAnimalRules.requiredBuildingFamily(SveAnimalRules.CAMEL_ID), "camel building");
    }

    private static void salePricesClampFriendshipAndReachExactCaps() {
        expectEquals(3_600, SveAnimalRules.sellPrice(SveAnimalRules.GOOSE_ID, -1), "goose minimum sale");
        expectEquals(15_600, SveAnimalRules.sellPrice(SveAnimalRules.GOOSE_ID, 1_000), "goose maximum sale");
        expectEquals(15_600, SveAnimalRules.sellPrice(SveAnimalRules.GOOSE_ID, 5_000), "goose clamped sale");
        expectEquals(8_584, SveAnimalRules.sellPrice(SveAnimalRules.CAMEL_ID, -1), "camel minimum sale");
        expectEquals(37_200, SveAnimalRules.sellPrice(SveAnimalRules.CAMEL_ID, 1_000), "camel maximum sale");
        expectEquals(37_200, SveAnimalRules.sellPrice(SveAnimalRules.CAMEL_ID, 5_000), "camel clamped sale");

        int previousGoose = -1;
        int previousCamel = -1;
        for (int friendship = 0; friendship <= 1_000; friendship++) {
            int goose = SveAnimalRules.sellPrice(SveAnimalRules.GOOSE_ID, friendship);
            int camel = SveAnimalRules.sellPrice(SveAnimalRules.CAMEL_ID, friendship);
            expect(goose >= previousGoose, "goose sale price must be monotonic");
            expect(camel >= previousCamel, "camel sale price must be monotonic");
            expect(goose <= 15_600, "goose sale price exceeds cap");
            expect(camel <= 37_200, "camel sale price exceeds cap");
            previousGoose = goose;
            previousCamel = camel;
        }
    }

    private static void camelCannotReproduce() {
        expect(!SveAnimalRules.canReproduce(SveAnimalRules.CAMEL_ID), "camel reproduction must be disabled");
        expect(SveAnimalRules.canReproduce(SveAnimalRules.GOOSE_ID), "goose rule must remain available");
    }

    private static void definitionsDriveCompatibilityAdapters() {
        expectEquals(2, SveAnimalRules.definitions().size(), "animal definition count");
        expectEquals(2, new HashSet<>(SveAnimalRules.definitions().stream()
                .map(SveAnimalRules.Definition::id).toList()).size(), "unique animal definition IDs");
        expectEquals(SveAnimalRules.GOOSE_ID, SveAnimalRules.definition("GOOSE").id(),
                "case-insensitive animal lookup");

        Map<String, AnimalShopService.ShopAnimalRule> shopRules =
                SveAnimalCompatibility.appendShopRules(Map.of());
        expectEquals(2, shopRules.size(), "SVE shop rule count");
        expectEquals(12_000, shopRules.get(SveAnimalRules.GOOSE_ID).price(), "goose shop adapter price");
        expectEquals(24_000, shopRules.get(SveAnimalRules.CAMEL_ID).price(), "camel shop adapter price");
        expectEquals(3, shopRules.get(SveAnimalRules.GOOSE_ID).requiredTier(), "goose shop tier");
        expectEquals("barn", shopRules.get(SveAnimalRules.CAMEL_ID).family(), "camel shop family");

        List<String> order = SveAnimalCompatibility.appendShopOrder(List.of("chicken"));
        order = SveAnimalCompatibility.appendShopOrder(order);
        expectEquals(List.of("chicken", SveAnimalRules.GOOSE_ID, SveAnimalRules.CAMEL_ID), order,
                "stable shop order");
        expectEquals(Set.of("chicken", SveAnimalRules.GOOSE_ID, SveAnimalRules.CAMEL_ID),
                SveAnimalCompatibility.appendKnownTypeIds(Set.of("chicken")), "known animal IDs");

        AnimalTypeCatalog.AnimalTypeSpec goose =
                SveAnimalCompatibility.typeSpec(SveAnimalRules.GOOSE_ID);
        AnimalTypeCatalog.AnimalTypeSpec camel =
                SveAnimalCompatibility.typeSpec(SveAnimalRules.CAMEL_ID);
        expectEquals("coop", goose.family(), "goose type adapter family");
        expectEquals(5, goose.daysToMature(), "goose type adapter maturity");
        expectEquals("barn", camel.family(), "camel type adapter family");
        expectEquals(5, camel.daysToMature(), "camel type adapter maturity");

        expectEquals(SveAnimalRules.GOOSE_VARIANT_INDEX,
                SveAnimalCompatibility.variantIndex(SveAnimalRules.GOOSE_ID, -1), "goose query variant");
        expectEquals(SveAnimalRules.CAMEL_ID,
                SveAnimalCompatibility.menuAnimalType(null, SveAnimalRules.CAMEL_VARIANT_INDEX),
                "camel query variant recovery");
        expectEquals(7, SveAnimalCompatibility.variantIndex("chicken", 7),
                "base animal query variant fallback");
        expectEquals(null, SveAnimalCompatibility.typeSpec("chicken"),
                "base animal type must not be intercepted");

        StardewAnimalData gooseData = SveAnimalData.forType(SveAnimalRules.GOOSE_ID);
        StardewAnimalData camelData = SveAnimalData.forType(SveAnimalRules.CAMEL_ID);
        expectEquals(12_000, gooseData.purchasePrice(), "goose public data price");
        expectEquals(2, gooseData.produceIntervalDays(), "goose public data interval");
        expectEquals(ResourceLocation.fromNamespaceAndPath("stardewcraftsve", "goose_egg"),
                gooseData.produce(), "goose public data product");
        expectEquals(24_000, camelData.purchasePrice(), "camel public data price");
        expectEquals(2, camelData.produceIntervalDays(), "camel public data interval");
        expectEquals(ResourceLocation.fromNamespaceAndPath("stardewcraftsve", "camel_wool"),
                camelData.produce(), "camel public data product");
    }

    private static void incubationAndProductionRulesMatchRuntimeFlow() {
        ResourceLocation gooseEgg = ResourceLocation.fromNamespaceAndPath("stardewcraftsve", "goose_egg");
        ResourceLocation goldenEgg = ResourceLocation.fromNamespaceAndPath(
                "stardewcraftsve", "golden_goose_egg");
        expectEquals(SveAnimalRules.GOOSE_ID,
                SveAnimalCompatibility.animalTypeForIncubatorInput(gooseEgg), "goose egg incubation type");
        expectEquals(null, SveAnimalCompatibility.animalTypeForIncubatorInput(goldenEgg),
                "golden goose egg must not incubate");

        expect(!SveAnimalProduction.isProductionDue(SveAnimalRules.GOOSE_ID, false, false, 1),
                "goose must wait two days");
        expect(SveAnimalProduction.isProductionDue(SveAnimalRules.GOOSE_ID, false, false, 2),
                "goose must produce on day two");
        expect(SveAnimalProduction.isProductionDue(SveAnimalRules.CAMEL_ID, false, false, 2),
                "camel must produce on day two");
        expect(!SveAnimalProduction.isProductionDue(SveAnimalRules.GOOSE_ID, true, false, 2),
                "baby goose must not produce");
        expect(!SveAnimalProduction.isProductionDue(SveAnimalRules.CAMEL_ID, false, true, 2),
                "offline catch-up must not duplicate produce");

        expect(Double.compare(0.01D, SveAnimalRules.GOLDEN_GOOSE_EGG_CHANCE) == 0,
                "golden goose egg chance must be 1 percent");
        expect(SveAnimalProduction.isGoldenGooseEggRoll(0.0D), "zero roll must produce golden egg");
        expect(SveAnimalProduction.isGoldenGooseEggRoll(0.009999D),
                "roll below one percent must produce golden egg");
        expect(!SveAnimalProduction.isGoldenGooseEggRoll(0.01D),
                "one-percent boundary must not produce golden egg");
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectEquals(String expected, String actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
