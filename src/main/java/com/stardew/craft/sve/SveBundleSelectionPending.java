package com.stardew.craft.sve;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived selections sent immediately before StardewCraft's farm creation payload. */
public final class SveBundleSelectionPending {
    private static final Map<UUID, Boolean> PENDING = new ConcurrentHashMap<>();

    private SveBundleSelectionPending() {}

    public static void put(UUID player, boolean hard) {
        PENDING.put(player, hard);
    }

    public static Boolean consume(UUID player) {
        return PENDING.remove(player);
    }

    public static void remove(UUID player) {
        PENDING.remove(player);
    }

    static void clear() {
        PENDING.clear();
    }
}
