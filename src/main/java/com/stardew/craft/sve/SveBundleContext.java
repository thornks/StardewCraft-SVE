package com.stardew.craft.sve;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/** Supplies the active player to StardewCraft's static bundle catalog on the server thread. */
public final class SveBundleContext {
    private static final ThreadLocal<Deque<UUID>> PLAYERS = ThreadLocal.withInitial(ArrayDeque::new);

    private SveBundleContext() {}

    public static void enter(UUID player) {
        PLAYERS.get().push(player);
    }

    public static void exit() {
        Deque<UUID> players = PLAYERS.get();
        if (!players.isEmpty()) players.pop();
        if (players.isEmpty()) PLAYERS.remove();
    }

    public static UUID currentPlayer() {
        Deque<UUID> players = PLAYERS.get();
        return players.isEmpty() ? null : players.peek();
    }
}
