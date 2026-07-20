package com.stardew.craft.sve;

import com.stardew.craft.farm.FarmInstanceRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Persistent farm-level selection for SVE's hard community bundles. */
public final class SveBundleDifficultyData extends SavedData {
    private static final String DATA_NAME = "stardewcraftsve_bundle_difficulty";
    private final Set<UUID> hardFarmOwners = new HashSet<>();

    public boolean isHard(UUID farmOwner) {
        return hardFarmOwners.contains(farmOwner);
    }

    public void setHard(UUID farmOwner, boolean hard) {
        boolean changed = hard ? hardFarmOwners.add(farmOwner) : hardFarmOwners.remove(farmOwner);
        if (changed) setDirty();
    }

    public void transfer(UUID oldOwner, UUID newOwner) {
        if (hardFarmOwners.remove(oldOwner)) {
            hardFarmOwners.add(newOwner);
            setDirty();
        }
    }

    public void remove(UUID owner) {
        if (hardFarmOwners.remove(owner)) setDirty();
    }

    public static boolean isHardForPlayer(UUID player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        UUID owner = FarmInstanceRegistry.get().getOwnerForPlayer(player);
        return owner != null && get(server.overworld()).isHard(owner);
    }

    public static SveBundleDifficultyData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SveBundleDifficultyData::new, SveBundleDifficultyData::load), DATA_NAME);
    }

    private static SveBundleDifficultyData load(CompoundTag tag, HolderLookup.Provider registries) {
        SveBundleDifficultyData data = new SveBundleDifficultyData();
        ListTag owners = tag.getList("HardFarmOwners", Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            CompoundTag entry = owners.getCompound(i);
            if (entry.hasUUID("Owner")) data.hardFarmOwners.add(entry.getUUID("Owner"));
        }
        return data;
    }

    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        ListTag owners = new ListTag();
        for (UUID owner : hardFarmOwners) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", owner);
            owners.add(entry);
        }
        tag.put("HardFarmOwners", owners);
        return tag;
    }
}
