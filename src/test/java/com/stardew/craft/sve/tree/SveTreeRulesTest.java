package com.stardew.craft.sve.tree;

import com.stardew.craft.sve.tree.wild.SveWildTreeType;

/** Pure tree invariants that can run without a Minecraft world. */
public final class SveTreeRulesTest {
    private SveTreeRulesTest() {
    }

    public static void main(String[] args) {
        fruitTreeGrowthStagesAndSeasons();
        offlineDaysUseTheCorrectSeason();
        wildTreeGeometryAndTapperRules();
        System.out.println("SVE tree rule regression suite passed");
    }

    private static void fruitTreeGrowthStagesAndSeasons() {
        expectEquals(28, SveFruitTreeType.DAYS_TO_MATURE, "fruit tree maturity");
        expectEquals(0, SveFruitTreeType.PEAR.fruitSeason(), "pear season");
        expectEquals(1, SveFruitTreeType.NECTARINE.fruitSeason(), "nectarine season");
        expectEquals(2, SveFruitTreeType.PERSIMMON.fruitSeason(), "persimmon season");
        expectEquals(0, SveFruitTreeType.PEAR.visualStageFromDaysRemaining(28), "stage one");
        expectEquals(1, SveFruitTreeType.PEAR.visualStageFromDaysRemaining(21), "stage two");
        expectEquals(2, SveFruitTreeType.PEAR.visualStageFromDaysRemaining(14), "stage three");
        expectEquals(3, SveFruitTreeType.PEAR.visualStageFromDaysRemaining(7), "stage four");
        expectEquals(3, SveFruitTreeType.PEAR.visualStageFromDaysRemaining(0), "last sapling stage");
    }

    private static void offlineDaysUseTheCorrectSeason() {
        expectEquals(0, SveFruitTreeRules.seasonOfAbsoluteDay(1), "spring start");
        expectEquals(0, SveFruitTreeRules.seasonOfAbsoluteDay(28), "spring end");
        expectEquals(1, SveFruitTreeRules.seasonOfAbsoluteDay(29), "summer start");
        expectEquals(2, SveFruitTreeRules.seasonOfAbsoluteDay(57), "fall start");
        expectEquals(3, SveFruitTreeRules.seasonOfAbsoluteDay(85), "winter start");
        expectEquals(0, SveFruitTreeRules.seasonOfAbsoluteDay(113), "next spring start");
    }

    private static void wildTreeGeometryAndTapperRules() {
        expectEquals(7, SveWildTreeType.FIR.trunkHeight(), "fir trunk height");
        expectEquals(11, SveWildTreeType.FIR.requiredHeight(), "fir clearance height");
        expectEquals(11, SveWildTreeType.FIR.tapperDays(), "fir tapper interval");
        expectEquals(13, SveWildTreeType.BIRCH.trunkHeight(), "birch trunk height");
        expectEquals(15, SveWildTreeType.BIRCH.requiredHeight(), "birch clearance height");
        expectEquals(3, SveWildTreeType.BIRCH.tapperDays(), "birch tapper interval");
    }

    private static void expectEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
