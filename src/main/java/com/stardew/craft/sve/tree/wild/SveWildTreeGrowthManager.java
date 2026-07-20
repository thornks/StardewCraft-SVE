package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.time.StardewTimeManager;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.farm.FarmInstance;
import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.tree.WildTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class SveWildTreeGrowthManager extends SavedData {
    public static final int MATURE_STAGE = 4;
    // Keep the original name so worlds created with the fir implementation migrate in place.
    private static final String DATA_NAME = "stardewcraftsve_fir_trees";
    private static final int NATURAL_SPAWN_ATTEMPTS = 8;
    private static final float NATURAL_SPAWN_CHANCE = 0.15F;
    private final Map<GlobalPos, Entry> saplings = new HashMap<>();
    private int lastNaturalFarmSpawnDay = Integer.MIN_VALUE;

    public void addSapling(ServerLevel level, BlockPos pos, SveWildTreeType type) {
        saplings.putIfAbsent(GlobalPos.of(level.dimension(), pos.immutable()),
                new Entry(type, 0, false, absoluteDay()));
        setDirty();
    }

    public void removeSapling(ServerLevel level, BlockPos pos) {
        if (saplings.remove(GlobalPos.of(level.dimension(), pos.immutable())) != null) setDirty();
    }

    public boolean fertilize(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SveWildTreeSaplingBlock sapling)) return false;
        addSapling(level, pos, sapling.getType());
        Entry entry = entry(level, pos);
        if (entry == null || entry.fertilized) return false;
        entry.fertilized = true;
        setDirty();
        return true;
    }

    public boolean isFertilized(ServerLevel level, BlockPos pos) {
        Entry entry = entry(level, pos);
        return entry != null && entry.fertilized;
    }

    public int getGrowthStage(ServerLevel level, BlockPos pos) {
        Entry entry = entry(level, pos);
        return entry == null ? 0 : entry.stage;
    }

    public boolean isBlocked(ServerLevel level, BlockPos pos) {
        Entry entry = entry(level, pos);
        if (entry == null) return false;
        if (StardewTimeManager.get().getCurrentSeason() == 3 && !entry.fertilized) return true;
        return entry.stage >= MATURE_STAGE - 1 && !canMatureHere(level, pos, entry.type);
    }

    public void growOneDay(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SveWildTreeSaplingBlock sapling)) return;
        addSapling(level, pos, sapling.getType());
        Entry entry = entry(level, pos);
        if (entry != null) processDay(level, pos, entry, StardewTimeManager.get().getCurrentSeason());
    }

    public void tick(ServerLevel level) {
        int today = absoluteDay();
        boolean changed = false;
        if (lastNaturalFarmSpawnDay != today) {
            lastNaturalFarmSpawnDay = today;
            changed = true;
            changed |= spawnNaturalFarmSaplings(level);
        }
        for (Map.Entry<GlobalPos, Entry> mapEntry : new ArrayList<>(saplings.entrySet())) {
            GlobalPos globalPos = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (!globalPos.dimension().equals(level.dimension()) || entry.lastProcessedDay >= today) continue;
            int firstDay = entry.lastProcessedDay + 1;
            int elapsed = Math.min(1120, today - entry.lastProcessedDay);
            entry.lastProcessedDay = today;
            changed = true;
            if (!level.isLoaded(globalPos.pos())) continue;
            for (int offset = 0; offset < elapsed && saplings.containsKey(globalPos); offset++) {
                processDay(level, globalPos.pos(), entry, seasonOfAbsoluteDay(firstDay + offset));
            }
        }
        if (changed) setDirty();
    }

    /**
     * Gives existing farms the same ongoing tree regrowth behavior as newly initialized farms.
     * It samples a few positions instead of scanning or rewriting the whole farm, so player
     * buildings and landscaping are never touched.
     */
    private boolean spawnNaturalFarmSaplings(ServerLevel level) {
        boolean changed = false;
        for (FarmInstance farm : FarmInstanceRegistry.get().getAllFarms()) {
            if (!farm.isInitialized() || level.random.nextFloat() >= NATURAL_SPAWN_CHANCE) continue;
            for (int attempt = 0; attempt < NATURAL_SPAWN_ATTEMPTS; attempt++) {
                if (tryPlaceNaturalSapling(level, farm)) {
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean tryPlaceNaturalSapling(ServerLevel level, FarmInstance farm) {
        BlockPos min = farm.getFarmBoundsMin();
        BlockPos max = farm.getFarmBoundsMax();
        int x = Mth.nextInt(level.random, min.getX(), max.getX());
        int z = Mth.nextInt(level.random, min.getZ(), max.getZ());
        BlockPos groundPos = null;
        for (int y = 100; y >= -64; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(candidate);
            if (ground.isAir()) continue;
            if ((ground.is(ModBlocks.YELLOW_DIRT.get()) || ground.is(Blocks.GRASS_BLOCK))
                    && level.getBlockState(candidate.above()).isAir()
                    && level.canSeeSky(candidate.above())) {
                groundPos = candidate;
            }
            break;
        }
        if (groundPos == null) return false;

        BlockPos saplingPos = groundPos.above();
        if (hasNearbyTree(level, saplingPos)) return false;
        SveWildTreeType type = level.random.nextBoolean() ? SveWildTreeType.FIR : SveWildTreeType.BIRCH;
        BlockState sapling = type.saplingBlock().defaultBlockState();
        if (!sapling.canSurvive(level, saplingPos)) return false;
        level.setBlock(saplingPos, sapling, Block.UPDATE_ALL);
        return true;
    }

    private static boolean hasNearbyTree(ServerLevel level, BlockPos center) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 16) continue;
                BlockState nearby = level.getBlockState(center.offset(dx, 0, dz));
                if (nearby.getBlock() instanceof SveWildTreeBlock
                        || nearby.getBlock() instanceof SveWildTreeSaplingBlock
                        || WildTrees.findByAnyPart(nearby) != null) return true;
            }
        }
        return false;
    }

    private void processDay(ServerLevel level, BlockPos pos, Entry entry, int season) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SveWildTreeSaplingBlock sapling)) {
            removeSapling(level, pos);
            return;
        }
        entry.type = sapling.getType();
        if (entry.stage >= MATURE_STAGE) {
            tryMature(level, pos, entry.type);
            return;
        }
        if (season == 3 && !entry.fertilized) return;
        if (entry.stage >= MATURE_STAGE - 1 && !canMatureHere(level, pos, entry.type)) return;
        float chance = entry.fertilized
                ? SveWildTreeType.FERTILIZED_GROWTH_CHANCE : SveWildTreeType.GROWTH_CHANCE;
        if (level.random.nextFloat() >= chance) return;

        entry.stage++;
        int visualStage = Math.min(entry.stage, MATURE_STAGE - 1);
        if (state.getValue(SveWildTreeSaplingBlock.STAGE) != visualStage) {
            level.setBlock(pos, state.setValue(SveWildTreeSaplingBlock.STAGE, visualStage), Block.UPDATE_ALL);
        }
        if (entry.stage >= MATURE_STAGE) tryMature(level, pos, entry.type);
        setDirty();
    }

    private void tryMature(ServerLevel level, BlockPos pos, SveWildTreeType type) {
        if (!canMatureHere(level, pos, type)) return;
        level.setBlock(pos, type.matureBlock().defaultBlockState(), Block.UPDATE_ALL);
        removeSapling(level, pos);
    }

    private static boolean canMatureHere(ServerLevel level, BlockPos pos, SveWildTreeType type) {
        if (!SveWildTreeBlock.canMature(level, pos, type)) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockState nearby = level.getBlockState(pos.offset(dx, 0, dz));
                if (nearby.getBlock() instanceof SveWildTreeBlock
                        || WildTrees.findByTrunk0(nearby) != null
                        || WildTrees.findByModernRoot(nearby) != null) return false;
            }
        }
        return true;
    }

    public static boolean trySpreadSapling(ServerLevel level, BlockPos root, SveWildTreeType type) {
        int dx = Mth.nextInt(level.random, -3, 3);
        int dz = Mth.nextInt(level.random, -3, 3);
        if (dx == 0 && dz == 0) return false;
        BlockPos target = root.offset(dx, 0, dz);
        if (!SveWildTreePlanting.isPlantableGround(level.getBlockState(target.below()))) return false;
        BlockState sapling = type.saplingBlock().defaultBlockState();
        if (!level.getBlockState(target).canBeReplaced() || !sapling.canSurvive(level, target)) return false;
        level.setBlock(target, sapling, Block.UPDATE_ALL);
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<GlobalPos, Entry> mapEntry : saplings.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Dimension", mapEntry.getKey().dimension().location().toString());
            entryTag.put("Pos", NbtUtils.writeBlockPos(mapEntry.getKey().pos()));
            entryTag.putString("Type", mapEntry.getValue().type.id());
            entryTag.putInt("Stage", mapEntry.getValue().stage);
            entryTag.putBoolean("Fertilized", mapEntry.getValue().fertilized);
            entryTag.putInt("LastProcessedDay", mapEntry.getValue().lastProcessedDay);
            list.add(entryTag);
        }
        tag.put("Saplings", list);
        tag.putInt("LastNaturalFarmSpawnDay", lastNaturalFarmSpawnDay);
        return tag;
    }

    public static SveWildTreeGrowthManager load(CompoundTag tag,
                                                net.minecraft.core.HolderLookup.Provider registries) {
        SveWildTreeGrowthManager manager = new SveWildTreeGrowthManager();
        if (tag.contains("LastNaturalFarmSpawnDay", Tag.TAG_INT)) {
            manager.lastNaturalFarmSpawnDay = tag.getInt("LastNaturalFarmSpawnDay");
        }
        ListTag list = tag.getList("Saplings", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            GlobalPos pos = readGlobalPos(entryTag);
            if (pos != null) {
                // Old fir entries had no Type field.
                SveWildTreeType type = entryTag.contains("Type", Tag.TAG_STRING)
                        ? SveWildTreeType.byId(entryTag.getString("Type")) : SveWildTreeType.FIR;
                manager.saplings.put(pos, new Entry(type,
                        Mth.clamp(entryTag.getInt("Stage"), 0, MATURE_STAGE),
                        entryTag.getBoolean("Fertilized"),
                        entryTag.contains("LastProcessedDay") ? entryTag.getInt("LastProcessedDay") : absoluteDay()));
            }
        }
        return manager;
    }

    public static SveWildTreeGrowthManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SveWildTreeGrowthManager::new, SveWildTreeGrowthManager::load), DATA_NAME);
    }

    private Entry entry(ServerLevel level, BlockPos pos) {
        return saplings.get(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    private static int absoluteDay() {
        StardewTimeManager time = StardewTimeManager.get();
        return (time.getCurrentYear() - 1) * 112 + time.getCurrentSeason() * 28 + time.getCurrentDay();
    }

    private static int seasonOfAbsoluteDay(int day) { return Math.floorMod((day - 1) / 28, 4); }

    @Nullable
    private static GlobalPos readGlobalPos(CompoundTag tag) {
        if (!tag.contains("Dimension", Tag.TAG_STRING) || !tag.contains("Pos", Tag.TAG_COMPOUND)) return null;
        ResourceKey<Level> dimension = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(tag.getString("Dimension")));
        BlockPos pos = NbtUtils.readBlockPos(tag, "Pos").orElse(null);
        return pos == null ? null : GlobalPos.of(dimension, pos);
    }

    private static final class Entry {
        private SveWildTreeType type;
        private int stage;
        private boolean fertilized;
        private int lastProcessedDay;

        private Entry(SveWildTreeType type, int stage, boolean fertilized, int lastProcessedDay) {
            this.type = type;
            this.stage = stage;
            this.fertilized = fertilized;
            this.lastProcessedDay = lastProcessedDay;
        }
    }
}
