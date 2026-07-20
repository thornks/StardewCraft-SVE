package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Canonical Stardew Valley map regions used by SVE runtime systems. */
final class SveWorldRegions {
    static final BlockRegion SECRET_WOODS =
            new BlockRegion(-265, 67, -1, -183, 86, 42);
    static final BlockRegion BACKWOODS =
            new BlockRegion(-151, 63, -237, -124, 91, -65);
    static final List<BlockRegion> MOUNTAIN = List.of(
            new BlockRegion(-123, 69, -104, -43, 79, -65),
            new BlockRegion(11, 81, -151, 121, 93, -80));
    static final BlockRegion FOREST =
            new BlockRegion(-194, 50, -12, -47, 74, 138);

    static final ChunkRegion COMMUNITY_CENTER = new ChunkRegion(2, 4, -5, -3);
    static final ChunkRegion BUS_STOP = new ChunkRegion(-7, -3, -5, -3);
    static final ChunkRegion RAILROAD = new ChunkRegion(-3, 6, -14, -10);

    private SveWorldRegions() {}

    static boolean isMountain(BlockPos pos) {
        return MOUNTAIN.stream().anyMatch(region -> region.containsXZ(pos));
    }

    record BlockRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        BlockRegion {
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException("Invalid block region bounds");
            }
        }

        boolean containsXZ(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        boolean containsY(int y) {
            return y >= minY && y <= maxY;
        }

        int randomX(RandomSource random) {
            return minX + random.nextInt(maxX - minX + 1);
        }

        int randomZ(RandomSource random) {
            return minZ + random.nextInt(maxZ - minZ + 1);
        }

        int minChunkX() {
            return Math.floorDiv(minX, 16);
        }

        int maxChunkX() {
            return Math.floorDiv(maxX, 16);
        }

        int minChunkZ() {
            return Math.floorDiv(minZ, 16);
        }

        int maxChunkZ() {
            return Math.floorDiv(maxZ, 16);
        }
    }

    record ChunkRegion(int minX, int maxX, int minZ, int maxZ) {
        ChunkRegion {
            if (minX > maxX || minZ > maxZ) {
                throw new IllegalArgumentException("Invalid chunk region bounds");
            }
        }

        boolean contains(BlockPos pos) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            return chunkX >= minX && chunkX <= maxX
                    && chunkZ >= minZ && chunkZ <= maxZ;
        }
    }
}
