package com.stardew.craft.sve.tree;

import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public final class SveFruitTreeGrowthManager extends SavedData {
    private static final String DATA_NAME = "stardewcraftsve_fruit_trees";

    private final Map<GlobalPos, SaplingEntry> saplings = new HashMap<>();

    public void addSapling(ServerLevel level, BlockPos pos, SveFruitTreeType type) {
        GlobalPos globalPos = toGlobalPos(level, pos);
        BlockState state = level.getBlockState(pos);
        int visualStage = state.getBlock() instanceof SveFruitTreeSaplingBlock
                ? state.getValue(SveFruitTreeSaplingBlock.AGE) : 0;
        int stageDays = Math.min(SveFruitTreeType.DAYS_TO_MATURE, visualStage * 7);
        SaplingEntry existing = saplings.get(globalPos);
        if (existing == null) {
            saplings.put(globalPos,
                    new SaplingEntry(type, SveFruitTreeType.DAYS_TO_MATURE - stageDays, absoluteDay()));
            setDirty();
            return;
        }
        boolean changed = existing.type != type;
        existing.type = type;
        if (existing.daysRemaining == SveFruitTreeType.DAYS_TO_MATURE && stageDays > 0) {
            existing.daysRemaining = SveFruitTreeType.DAYS_TO_MATURE - stageDays;
            changed = true;
        }
        if (changed) setDirty();
    }

    public void removeSapling(ServerLevel level, BlockPos pos) {
        if (saplings.remove(toGlobalPos(level, pos)) != null) {
            setDirty();
        }
    }

    public int getDaysGrown(ServerLevel level, BlockPos pos) {
        BlockPos lowerPos = resolveSaplingLowerPos(level, pos);
        SaplingEntry entry = saplings.get(toGlobalPos(level, lowerPos));
        if (entry != null) {
            return Math.max(0, SveFruitTreeType.DAYS_TO_MATURE - entry.daysRemaining);
        }
        BlockState state = level.getBlockState(lowerPos);
        return state.getBlock() instanceof SveFruitTreeSaplingBlock
                ? state.getValue(SveFruitTreeSaplingBlock.AGE) * 7
                : 0;
    }

    public int getDaysRemaining(ServerLevel level, BlockPos pos) {
        return Math.max(0, SveFruitTreeType.DAYS_TO_MATURE - getDaysGrown(level, pos));
    }

    public int getGrowthStage(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(resolveSaplingLowerPos(level, pos));
        return state.getBlock() instanceof SveFruitTreeSaplingBlock
                ? state.getValue(SveFruitTreeSaplingBlock.AGE)
                : 0;
    }

    public boolean isBlockedNow(ServerLevel level, BlockPos pos) {
        return SveFruitTreeRules.isGrowthBlocked(level, resolveSaplingLowerPos(level, pos));
    }

    public void growOneDay(ServerLevel level, BlockPos pos) {
        BlockPos lowerPos = resolveSaplingLowerPos(level, pos);
        BlockState state = level.getBlockState(lowerPos);
        if (state.getBlock() instanceof SveFruitTreeBlock) {
            if (level.getBlockEntity(lowerPos) instanceof SveFruitTreeBlockEntity tree) {
                tree.advanceOneDayForDebug(level, lowerPos);
            }
            return;
        }
        if (!(state.getBlock() instanceof SveFruitTreeSaplingBlock sapling)
                || state.getValue(SveFruitTreeSaplingBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        addSapling(level, lowerPos, sapling.getType());
        SaplingEntry entry = saplings.get(toGlobalPos(level, lowerPos));
        if (entry != null) {
            processSaplingDay(level, lowerPos, entry);
        }
    }

    public void tick(ServerLevel level) {
        int today = absoluteDay();
        boolean changed = false;
        for (Map.Entry<GlobalPos, SaplingEntry> mapEntry : new java.util.ArrayList<>(saplings.entrySet())) {
            GlobalPos globalPos = mapEntry.getKey();
            SaplingEntry entry = mapEntry.getValue();
            if (!globalPos.dimension().equals(level.dimension()) || entry.lastProcessedDay >= today) {
                continue;
            }

            BlockPos pos = globalPos.pos();
            if (!level.isLoaded(pos)) {
                continue;
            }
            int elapsedDays = Math.min(1120, today - entry.lastProcessedDay);
            for (int day = 0; day < elapsedDays && saplings.containsKey(globalPos); day++) {
                processSaplingDay(level, pos, entry);
            }
            entry.lastProcessedDay = today;
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    private void processSaplingDay(ServerLevel level, BlockPos pos, SaplingEntry entry) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SveFruitTreeSaplingBlock sapling)
                || state.getValue(SveFruitTreeSaplingBlock.HALF) != DoubleBlockHalf.LOWER) {
            removeSapling(level, pos);
            return;
        }

        SveFruitTreeType type = sapling.getType();
        if (entry.daysRemaining <= 0) {
            matureSapling(level, pos, type);
            return;
        }
        if (SveFruitTreeRules.isGrowthBlocked(level, pos)) {
            updateVisualStage(level, pos, state, type.visualStageFromDaysRemaining(entry.daysRemaining));
            return;
        }

        entry.daysRemaining--;
        updateVisualStage(level, pos, state, type.visualStageFromDaysRemaining(entry.daysRemaining));
        if (entry.daysRemaining <= 0) {
            matureSapling(level, pos, type);
        }
        setDirty();
    }

    private void updateVisualStage(ServerLevel level, BlockPos pos, BlockState lowerState, int visualStage) {
        if (lowerState.getValue(SveFruitTreeSaplingBlock.AGE) == visualStage) {
            return;
        }
        BlockState nextLower = lowerState.setValue(SveFruitTreeSaplingBlock.AGE, visualStage)
                .setValue(SveFruitTreeSaplingBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(pos, nextLower, Block.UPDATE_ALL);
        BlockPos above = pos.above();
        if (level.getBlockState(above).getBlock() == lowerState.getBlock()) {
            level.setBlock(above,
                    nextLower.setValue(SveFruitTreeSaplingBlock.HALF, DoubleBlockHalf.UPPER),
                    Block.UPDATE_ALL);
        }
    }

    private void matureSapling(ServerLevel level, BlockPos pos, SveFruitTreeType type) {
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, type.matureBlock().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SveFruitTreeBlockEntity tree) {
            tree.setNewlyMature(type);
        }
        removeSapling(level, pos);
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<GlobalPos, SaplingEntry> mapEntry : saplings.entrySet()) {
            CompoundTag entryTag = writeGlobalPos(mapEntry.getKey());
            SaplingEntry entry = mapEntry.getValue();
            entryTag.putString("Type", entry.type.id());
            entryTag.putInt("DaysRemaining", entry.daysRemaining);
            entryTag.putInt("LastProcessedDay", entry.lastProcessedDay);
            list.add(entryTag);
        }
        tag.put("Saplings", list);
        return tag;
    }

    public static SveFruitTreeGrowthManager load(CompoundTag tag,
                                                 net.minecraft.core.HolderLookup.Provider registries) {
        SveFruitTreeGrowthManager manager = new SveFruitTreeGrowthManager();
        if (!tag.contains("Saplings", Tag.TAG_LIST)) {
            return manager;
        }
        ListTag list = tag.getList("Saplings", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            GlobalPos globalPos = readGlobalPos(entryTag);
            if (globalPos == null) {
                continue;
            }
            SveFruitTreeType type = SveFruitTreeType.byId(entryTag.getString("Type"));
            int daysRemaining = Math.max(0,
                    Math.min(SveFruitTreeType.DAYS_TO_MATURE, entryTag.getInt("DaysRemaining")));
            int lastProcessedDay = entryTag.contains("LastProcessedDay")
                    ? entryTag.getInt("LastProcessedDay")
                    : absoluteDay();
            manager.saplings.put(globalPos, new SaplingEntry(type, daysRemaining, lastProcessedDay));
        }
        return manager;
    }

    public static SveFruitTreeGrowthManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SveFruitTreeGrowthManager::new, SveFruitTreeGrowthManager::load),
                DATA_NAME);
    }

    private static int absoluteDay() {
        StardewTimeManager time = StardewTimeManager.get();
        return (time.getCurrentYear() - 1) * 112
                + time.getCurrentSeason() * 28
                + time.getCurrentDay();
    }

    private static GlobalPos toGlobalPos(Level level, BlockPos pos) {
        return GlobalPos.of(
                Objects.requireNonNull(level.dimension(), "dimension"),
                Objects.requireNonNull(pos.immutable(), "pos"));
    }

    private static BlockPos resolveSaplingLowerPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SveFruitTreeSaplingBlock
                && state.getValue(SveFruitTreeSaplingBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    private static CompoundTag writeGlobalPos(GlobalPos globalPos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", globalPos.dimension().location().toString());
        tag.put("Pos", NbtUtils.writeBlockPos(globalPos.pos()));
        return tag;
    }

    @Nullable
    private static GlobalPos readGlobalPos(CompoundTag tag) {
        if (!tag.contains("Dimension", Tag.TAG_STRING) || !tag.contains("Pos", Tag.TAG_COMPOUND)) {
            return null;
        }
        ResourceKey<Level> dimension = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(tag.getString("Dimension")));
        BlockPos pos = NbtUtils.readBlockPos(tag, "Pos").orElse(null);
        return pos == null ? null : GlobalPos.of(dimension, pos);
    }

    private static final class SaplingEntry {
        private SveFruitTreeType type;
        private int daysRemaining;
        private int lastProcessedDay;

        private SaplingEntry(SveFruitTreeType type, int daysRemaining, int lastProcessedDay) {
            this.type = type;
            this.daysRemaining = daysRemaining;
            this.lastProcessedDay = lastProcessedDay;
        }
    }
}
