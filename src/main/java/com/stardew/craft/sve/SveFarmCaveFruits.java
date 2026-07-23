package com.stardew.craft.sve;

import com.stardew.craft.interior.InteriorSubspaceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Extends the host's fruit-tree sub-pool without changing its berry and apple branches. */
public final class SveFarmCaveFruits {
    private static final int HOST_ORCHARD_FRUIT_COUNT = 5;
    private static final int TOTAL_ORCHARD_FRUIT_COUNT = 8;

    private SveFarmCaveFruits() {}

    public static Block extendOrchardFruit(Block original, RandomSource random) {
        if (!isHostOrchardFruit(original)) return original;

        return switch (extensionSlot(random.nextInt(TOTAL_ORCHARD_FRUIT_COUNT))) {
            case 0 -> ModBlocks.FORAGE_PEAR.get();
            case 1 -> ModBlocks.FORAGE_NECTARINE.get();
            case 2 -> ModBlocks.FORAGE_PERSIMMON.get();
            default -> original;
        };
    }

    static int extensionSlot(int orchardRoll) {
        if (orchardRoll < 0 || orchardRoll >= TOTAL_ORCHARD_FRUIT_COUNT) {
            throw new IllegalArgumentException("Invalid fruit-cave orchard roll: " + orchardRoll);
        }
        return orchardRoll < HOST_ORCHARD_FRUIT_COUNT ? -1 : orchardRoll - HOST_ORCHARD_FRUIT_COUNT;
    }

    public static void clear(ServerLevel level, BlockPos caveOrigin) {
        for (int x = 0; x < InteriorSubspaceManager.FARM_CAVE_SCHEM_W; x++) {
            for (int z = 0; z < InteriorSubspaceManager.FARM_CAVE_SCHEM_L; z++) {
                BlockPos pos = caveOrigin.offset(x, 1, z);
                if (isSveCaveFruit(level.getBlockState(pos).getBlock())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static boolean isHostOrchardFruit(Block block) {
        return block == com.stardew.craft.block.ModBlocks.FORAGE_APRICOT.get()
                || block == com.stardew.craft.block.ModBlocks.FORAGE_ORANGE.get()
                || block == com.stardew.craft.block.ModBlocks.FORAGE_PEACH.get()
                || block == com.stardew.craft.block.ModBlocks.FORAGE_POMEGRANATE.get()
                || block == com.stardew.craft.block.ModBlocks.FORAGE_MANGO.get();
    }

    private static boolean isSveCaveFruit(Block block) {
        return block == ModBlocks.FORAGE_PEAR.get()
                || block == ModBlocks.FORAGE_NECTARINE.get()
                || block == ModBlocks.FORAGE_PERSIMMON.get();
    }
}
