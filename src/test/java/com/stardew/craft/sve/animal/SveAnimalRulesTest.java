package com.stardew.craft.sve.animal;

public final class SveAnimalRulesTest {
    private SveAnimalRulesTest() {
    }

    public static void main(String[] args) {
        shopAndGrowthRulesMatchSveDesign();
        salePricesClampFriendshipAndReachExactCaps();
        camelCannotReproduce();
        goldenGooseEggChanceIsOnePercent();
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
    }

    private static void camelCannotReproduce() {
        expect(!SveAnimalRules.canReproduce(SveAnimalRules.CAMEL_ID), "camel reproduction must be disabled");
        expect(SveAnimalRules.canReproduce(SveAnimalRules.GOOSE_ID), "goose rule must remain available");
    }

    private static void goldenGooseEggChanceIsOnePercent() {
        expect(Double.compare(0.01D, SveAnimalRules.GOLDEN_GOOSE_EGG_CHANCE) == 0,
                "golden goose egg chance must be 1 percent");
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
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
