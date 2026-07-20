package com.stardew.craft.sve;

import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Spawns the SVE-only Secret Woods artifact spots once per world day. */
public final class SveArtifactSpotSpawnService {
    private static final String DATA_ID = "stardewcraftsve_secret_woods_artifact_spots";
    private static final SveWorldRegions.BlockRegion ZONE = SveWorldRegions.SECRET_WOODS;

    private SveArtifactSpotSpawnService() {}

    public static void tick(ServerLevel level) {
        if (level.dimension() != ModDimensions.STARDEW_VALLEY) return;

        StardewTimeManager time = StardewTimeManager.get();
        if (time == null) return;

        int day = time.getAbsoluteDay();
        DayState state = level.getDataStorage().computeIfAbsent(DayState.factory(), DATA_ID);
        if (state.lastProcessedDay() == day) return;

        spawnForDay(level);
        state.markProcessed(day);
    }

    private static void spawnForDay(ServerLevel level) {
        Block spotBlock = com.stardew.craft.block.ModBlocks.ARTIFACT_SPOT_DIRT.get();
        Block yellowDirt = com.stardew.craft.block.ModBlocks.YELLOW_DIRT.get();
        RandomSource random = level.getRandom();

        loadEntireZone(level);

        List<BlockPos> existingSpots = new ArrayList<>();

        for (int x = ZONE.minX(); x <= ZONE.maxX(); x++) {
            for (int z = ZONE.minZ(); z <= ZONE.maxZ(); z++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (!ZONE.containsY(y)) continue;

                BlockPos surface = new BlockPos(x, y, z);
                BlockState surfaceState = level.getBlockState(surface);
                if (surfaceState.is(yellowDirt)) {
                    level.setBlock(surface, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                } else if (surfaceState.is(spotBlock)) {
                    if (random.nextDouble() < 0.15D) {
                        level.setBlock(surface, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        existingSpots.add(surface);
                    }
                }
            }
        }

        int target = 3 + random.nextInt(4);
        while (existingSpots.size() > target) {
            BlockPos excess = existingSpots.remove(random.nextInt(existingSpots.size()));
            level.setBlock(excess, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }

        int toPlace = target - existingSpots.size();
        int placed = 0;
        for (int attempt = 0; attempt < 128 && placed < toPlace; attempt++) {
            int x = ZONE.randomX(random);
            int z = ZONE.randomZ(random);

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            if (!ZONE.containsY(y)) continue;

            BlockPos surface = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(surface);
            if (!isValidSurface(ground)) continue;
            if (level.getBlockState(surface.below()).isAir()) continue;

            BlockPos above = surface.above();
            if (!level.getBlockState(above).isAir() || !level.canSeeSky(above)) continue;

            level.setBlock(surface, spotBlock.defaultBlockState(), Block.UPDATE_ALL);
            placed++;
        }
    }

    private static void loadEntireZone(ServerLevel level) {
        for (int chunkX = ZONE.minChunkX(); chunkX <= ZONE.maxChunkX(); chunkX++) {
            for (int chunkZ = ZONE.minChunkZ(); chunkZ <= ZONE.maxChunkZ(); chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static boolean isValidSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.DIRT);
    }

    private static final class DayState extends SavedData {
        private int lastProcessedDay = Integer.MIN_VALUE;

        private DayState() {}

        private DayState(CompoundTag tag) {
            lastProcessedDay = tag.contains("LastProcessedDay")
                    ? tag.getInt("LastProcessedDay")
                    : Integer.MIN_VALUE;
        }

        private int lastProcessedDay() {
            return lastProcessedDay;
        }

        private void markProcessed(int day) {
            lastProcessedDay = day;
            setDirty();
        }

        @Override
        @Nonnull
        public CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
            tag.putInt("LastProcessedDay", lastProcessedDay);
            return tag;
        }

        private static SavedData.Factory<DayState> factory() {
            return new SavedData.Factory<>(DayState::new, (tag, provider) -> new DayState(tag));
        }
    }
}
