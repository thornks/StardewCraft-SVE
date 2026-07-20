package com.stardew.craft.sve;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Debug item that opens a forage selection GUI on right-click.
 * Selecting a block spawns it with proper surface checks near the player.
 * Uses the stick texture for convenience.
 */
public class SveDebugWandItem extends Item {
    public SveDebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ForageSelectionMenu(id, inv),
                Component.translatable("debug.stardewcraftsve.forage_selection.title")
            ));
        }

        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
}
