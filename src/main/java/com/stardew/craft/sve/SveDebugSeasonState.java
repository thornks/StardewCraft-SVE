package com.stardew.craft.sve;

import com.stardew.craft.farming.SeasonLocationRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persistent global override for crop and fruit-tree season restrictions. */
public final class SveDebugSeasonState extends SavedData {
    private static final String DATA_NAME = "stardewcraftsve_debug_season";
    private static final String ENABLED_TAG = "Enabled";
    private static final AtomicBoolean RULE_REGISTERED = new AtomicBoolean();

    private boolean enabled;

    public static void registerSeasonRule() {
        if (RULE_REGISTERED.compareAndSet(false, true)) {
            SeasonLocationRules.registerIgnoreSeasonsRule(
                    (level, pos) -> isEnabled(level));
        }
    }

    public static boolean isEnabled(Level level) {
        MinecraftServer server = level == null ? null : level.getServer();
        return server != null && get(server.overworld()).enabled;
    }

    public static boolean isEnabled(MinecraftServer server) {
        return server != null && get(server.overworld()).enabled;
    }

    public static boolean setEnabled(MinecraftServer server, boolean enabled) {
        return server != null && get(server.overworld()).setEnabled(enabled);
    }

    private boolean setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return false;
        }
        this.enabled = enabled;
        setDirty();
        return true;
    }

    public static SveDebugSeasonState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SveDebugSeasonState::new, SveDebugSeasonState::load),
                DATA_NAME);
    }

    static SveDebugSeasonState load(CompoundTag tag, HolderLookup.Provider registries) {
        SveDebugSeasonState state = new SveDebugSeasonState();
        state.enabled = tag.getBoolean(ENABLED_TAG);
        return state;
    }

    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag,
                            @Nonnull HolderLookup.Provider registries) {
        tag.putBoolean(ENABLED_TAG, enabled);
        return tag;
    }
}
