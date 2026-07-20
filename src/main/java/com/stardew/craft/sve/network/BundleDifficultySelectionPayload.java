package com.stardew.craft.sve.network;

import com.stardew.craft.farm.FarmInstanceRegistry;
import com.stardew.craft.sve.StardewcraftsveMod;
import com.stardew.craft.sve.SveBundleSelectionPending;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BundleDifficultySelectionPayload(boolean hard) implements CustomPacketPayload {
    public static final Type<BundleDifficultySelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "bundle_difficulty_selection"));
    public static final StreamCodec<ByteBuf, BundleDifficultySelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BundleDifficultySelectionPayload::hard,
            BundleDifficultySelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BundleDifficultySelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && !FarmInstanceRegistry.get().hasFarm(player.getUUID())) {
                SveBundleSelectionPending.put(player.getUUID(), payload.hard());
            }
        });
    }
}
