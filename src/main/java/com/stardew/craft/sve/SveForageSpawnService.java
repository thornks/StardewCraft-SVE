package com.stardew.craft.sve;

import com.stardew.craft.api.v1.world.StardewForageZoneDefinition;
import com.stardew.craft.world.data.ForageZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/** Debug placement helper backed by StardewCraft's reloadable forage zones. */
public final class SveForageSpawnService {
    private SveForageSpawnService() {}

    public static BlockPos spawnOneForBlock(ServerLevel level, Block block) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        RandomSource random = level.getRandom();

        for (var registered : ForageZoneData.available(level)) {
            StardewForageZoneDefinition zone = registered.getValue();
            boolean containsBlock = zone.entries().stream()
                    .anyMatch(entry -> entry.block().equals(blockId));
            if (!containsBlock) continue;

            for (int attempt = 0; attempt < 64; attempt++) {
                StardewForageZoneDefinition.Rect area = pickArea(zone.areas(), random);
                int x = randomBetween(random, area.minX(), area.maxX());
                int z = randomBetween(random, area.minZ(), area.maxZ());
                if (!level.hasChunk(x >> 4, z >> 4)) continue;

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (y < area.minY() || y > area.maxY()) continue;

                BlockPos surfacePos = new BlockPos(x, y, z);
                BlockPos placePos = surfacePos.above();
                if (!isValidSurface(level.getBlockState(surfacePos), zone.surface())) continue;

                BlockState existing = level.getBlockState(placePos);
                if (!existing.isAir() && !existing.canBeReplaced()) continue;
                if (!level.canSeeSky(placePos)) continue;
                if (!block.defaultBlockState().canSurvive(level, placePos)) continue;

                level.setBlock(placePos, block.defaultBlockState(), Block.UPDATE_ALL);
                return placePos;
            }
        }
        return null;
    }

    private static StardewForageZoneDefinition.Rect pickArea(
            List<StardewForageZoneDefinition.Rect> areas,
            RandomSource random
    ) {
        int totalWeight = areas.stream().mapToInt(StardewForageZoneDefinition.Rect::weight).sum();
        int roll = random.nextInt(totalWeight);
        for (StardewForageZoneDefinition.Rect area : areas) {
            roll -= area.weight();
            if (roll < 0) return area;
        }
        return areas.getLast();
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static boolean isValidSurface(
            BlockState state,
            StardewForageZoneDefinition.Surface surface
    ) {
        if (surface == StardewForageZoneDefinition.Surface.SAND) {
            return state.is(Blocks.SAND);
        }
        return state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.DIRT);
    }
}
