package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.time.StardewTimeManager;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SveWildTreeGrowthManager extends SavedData {
    public static final int MATURE_STAGE = 4;
    // Keep the original name so worlds created with the fir implementation migrate in place.
    private static final String DATA_NAME = "stardewcraftsve_fir_trees";
    private final Map<GlobalPos, Entry> saplings = new HashMap<>();

    public void addSapling(ServerLevel level, BlockPos pos, SveWildTreeType type) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos.immutable());
        BlockState state = level.getBlockState(pos);
        int visualStage = state.getBlock() instanceof SveWildTreeSaplingBlock
                ? state.getValue(SveWildTreeSaplingBlock.STAGE) : 0;
        Entry existing = saplings.get(globalPos);
        if (existing == null) {
            saplings.put(globalPos, new Entry(type, visualStage, false, absoluteDay()));
            setDirty();
            return;
        }
        boolean changed = existing.type != type;
        existing.type = type;
        if (existing.stage < visualStage) {
            existing.stage = visualStage;
            changed = true;
        }
        if (changed) setDirty();
    }

    public void removeSapling(ServerLevel level, BlockPos pos) {
        if (saplings.remove(GlobalPos.of(level.dimension(), pos.immutable())) != null) setDirty();
    }

    public List<SaplingSnapshot> snapshots() {
        return saplings.entrySet().stream()
                .map(entry -> new SaplingSnapshot(entry.getKey(), entry.getValue().type,
                        entry.getValue().stage, entry.getValue().fertilized,
                        entry.getValue().lastProcessedDay))
                .toList();
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
        for (Map.Entry<GlobalPos, Entry> mapEntry : new ArrayList<>(saplings.entrySet())) {
            GlobalPos globalPos = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (!globalPos.dimension().equals(level.dimension()) || entry.lastProcessedDay >= today) continue;
            int elapsed = Math.min(1120, today - entry.lastProcessedDay);
            if (!level.isLoaded(globalPos.pos())) continue;
            int firstDay = entry.lastProcessedDay + 1;
            for (int offset = 0; offset < elapsed && saplings.containsKey(globalPos); offset++) {
                processDay(level, globalPos.pos(), entry, seasonOfAbsoluteDay(firstDay + offset));
            }
            entry.lastProcessedDay = today;
            changed = true;
        }
        if (changed) setDirty();
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
        return tag;
    }

    public static SveWildTreeGrowthManager load(CompoundTag tag,
                                                net.minecraft.core.HolderLookup.Provider registries) {
        SveWildTreeGrowthManager manager = new SveWildTreeGrowthManager();
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

    public record SaplingSnapshot(GlobalPos pos, SveWildTreeType type, int stage,
                                  boolean fertilized, int lastProcessedDay) {
    }
}
