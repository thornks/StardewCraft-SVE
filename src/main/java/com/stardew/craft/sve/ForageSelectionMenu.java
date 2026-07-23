package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Server-side menu for the debug forage wand.
 * Forage block list is deterministic from registry, so no data sync needed.
 */
public class ForageSelectionMenu extends AbstractContainerMenu {
    private final Inventory inventory;

    protected ForageSelectionMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.FORAGE_SELECTION.get(), containerId);
        this.inventory = inventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().is(ModItems.DEBUG_WAND.get())
            || player.getOffhandItem().is(ModItems.DEBUG_WAND.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        List<Block> blocks = getAllForageBlocks();
        if (id < 0 || id >= blocks.size()) return false;
        if (!(inventory.player instanceof ServerPlayer serverPlayer)) return false;

        Block block = blocks.get(id);
        BlockPos pos = SveForageSpawnService.spawnOneForBlock(
            serverPlayer.serverLevel(), block
        );
        if (pos != null) {
            Component name = blockDisplayName(block);
            serverPlayer.sendSystemMessage(Component.translatable(
                "debug.stardewcraftsve.forage_spawned", name, pos.getX(), pos.getY(), pos.getZ()
            ));
        } else {
            serverPlayer.sendSystemMessage(Component.translatable(
                "debug.stardewcraftsve.forage_spawn_failed", blockDisplayName(block)
            ));
        }
        return true;
    }

    private static Component blockDisplayName(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        String path = key.getPath();
        if (path.startsWith("forage_")) {
            return Component.translatable("item." + key.getNamespace() + "." + path.substring(7));
        }
        return Component.translatable(block.getDescriptionId());
    }

    /** Returns all SVE forage blocks in a deterministic order. */
    public static List<Block> getAllForageBlocks() {
        return List.of(
            ModBlocks.FORAGE_GREEN_MUSHROOM.get(),
            ModBlocks.FORAGE_MEGA_PURPLE_MUSHROOM.get(),
            ModBlocks.FORAGE_MUSHROOM_COLONY.get(),
            ModBlocks.FORAGE_POISON_MUSHROOM.get(),
            ModBlocks.FORAGE_DIAMOND_FLOWER.get(),
            ModBlocks.FORAGE_FERNGILL_PRIMROSE.get(),
            ModBlocks.FORAGE_GOLDENROD.get(),
            ModBlocks.FORAGE_GOLDEN_OCEAN_FLOWER.get(),
            ModBlocks.FORAGE_SMELLY_RAFFLESIA.get(),
            ModBlocks.FORAGE_SWAMP_FLOWER.get(),
            ModBlocks.FORAGE_THISTLE.get(),
            ModBlocks.FORAGE_WINTER_STAR_ROSE.get(),
            ModBlocks.FORAGE_BIG_CONCH.get(),
            ModBlocks.FORAGE_DRIED_SAND_DOLLAR.get(),
            ModBlocks.FORAGE_DULSE_SEAWEED.get(),
            ModBlocks.FORAGE_SHARK_TOOTH.get(),
            ModBlocks.FORAGE_BEARBERRYS.get(),
            ModBlocks.FORAGE_LUCKY_FOUR_LEAF_CLOVER.get(),
            ModBlocks.FORAGE_RED_BANEBERRY.get(),
            ModBlocks.FORAGE_PEAR.get(),
            ModBlocks.FORAGE_NECTARINE.get(),
            ModBlocks.FORAGE_PERSIMMON.get()
        );
    }
}
