package com.stardew.craft.sve.tree;

import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tree invariants and host-integration boundaries that can run without a Minecraft world. */
public final class SveTreeRulesTest {
    private static final Path WILD_GROWTH_MANAGER = Path.of(
            "src/main/java/com/stardew/craft/sve/tree/wild/SveWildTreeGrowthManager.java");
    private static final Path DAILY_DEBRIS_MIXIN = Path.of(
            "src/main/java/com/stardew/craft/sve/mixin/FarmDebrisDailyServiceMixin.java");
    private static final String DAILY_DEBRIS_CLASS =
            "/com/stardew/craft/farm/FarmDebrisDailyService.class";
    private static final String SPAWN_RANDOM_DEBRIS_DESCRIPTOR =
            "(Lnet/minecraft/server/level/ServerLevel;Lcom/stardew/craft/farm/FarmInstance;IZI)V";

    private SveTreeRulesTest() {
    }

    public static void main(String[] args) throws IOException {
        fruitTreeGrowthStagesAndSeasons();
        offlineDaysUseTheCorrectSeason();
        wildTreeGeometryAndTapperRules();
        dailyWildTreeSpawningStaysHostOwned();
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
        expect(SveWildTreeType.FIR.trunkHeight() > 0, "fir must have a trunk");
        expect(SveWildTreeType.BIRCH.trunkHeight() > SveWildTreeType.FIR.trunkHeight(),
                "birch trunk must be taller than fir");
    }

    private static void dailyWildTreeSpawningStaysHostOwned() throws IOException {
        String managerSource = Files.readString(WILD_GROWTH_MANAGER);
        expect(!managerSource.contains("spawnNaturalFarmSaplings"),
                "wild-tree growth manager must not run a separate all-farm spawning pass");
        expect(!managerSource.contains("FarmInstanceRegistry"),
                "wild-tree growth manager must not enumerate farms");
        expect(!managerSource.contains("LastNaturalFarmSpawnDay"),
                "legacy independent farm-spawn state must not be persisted");

        String mixinSource = Files.readString(DAILY_DEBRIS_MIXIN);
        expect(mixinSource.contains("@Mixin(FarmDebrisDailyService.class)"),
                "daily debris integration must target the host service");
        expect(mixinSource.contains("method = \"spawnRandomDebris\""),
                "daily debris integration must only wrap host random debris placement");
        expect(mixinSource.contains("SveFarmWildTreeGeneration.placeDebrisBlock"),
                "daily debris integration must reuse the initial-farm sapling replacement rules");
        expect(mixinSource.contains("require = 2"),
                "daily debris integration must fail fast if the 0.5.2 host call count changes");

        ClassNode hostClass = new ClassNode();
        try (InputStream stream = SveTreeRulesTest.class.getResourceAsStream(DAILY_DEBRIS_CLASS)) {
            if (stream == null) throw new AssertionError("Missing host class " + DAILY_DEBRIS_CLASS);
            new ClassReader(stream).accept(hostClass, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        MethodNode target = hostClass.methods.stream()
                .filter(method -> method.name.equals("spawnRandomDebris")
                        && method.desc.equals(SPAWN_RANDOM_DEBRIS_DESCRIPTOR))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "StardewCraft 0.5.2 spawnRandomDebris signature changed"));
        int setBlockCalls = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && instruction instanceof MethodInsnNode method
                    && method.owner.equals("net/minecraft/server/level/ServerLevel")
                    && method.name.equals("setBlock")
                    && method.desc.equals(
                            "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")) {
                setBlockCalls++;
            }
        }
        expectEquals(2, setBlockCalls,
                "StardewCraft 0.5.2 daily debris setBlock call count");
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
