package com.stardew.craft.sve.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SveNetwork {
    private SveNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BundleDifficultySelectionPayload.TYPE,
                BundleDifficultySelectionPayload.STREAM_CODEC,
                BundleDifficultySelectionPayload::handle);
    }
}
