package com.stardew.craft.sve;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Client screen listing all SVE forage blocks. Clicking one sends the
 * selection back to the server via clickMenuButton.
 */
public class ForageSelectionScreen extends Screen implements MenuAccess<ForageSelectionMenu> {
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 22;
    private static final int ENTRIES_PER_PAGE = 8;

    private final ForageSelectionMenu menu;
    private final int containerId;
    private List<Block> blocks;
    private int page;

    public ForageSelectionScreen(ForageSelectionMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        this.containerId = menu.containerId;
    }

    @Override
    public ForageSelectionMenu getMenu() {
        return menu;
    }

    @Override
    protected void init() {
        blocks = ForageSelectionMenu.getAllForageBlocks();
        page = 0;
        rebuildButtons();
    }

    private static Component blockDisplayName(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        String path = key.getPath();
        // Block registry names are "forage_<item_name>" → use item translation key
        if (path.startsWith("forage_")) {
            return Component.translatable("item." + key.getNamespace() + "." + path.substring(7));
        }
        return Component.translatable(block.getDescriptionId());
    }

    private void rebuildButtons() {
        clearWidgets();

        int totalPages = (blocks.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE;
        int start = page * ENTRIES_PER_PAGE;
        int end = Math.min(start + ENTRIES_PER_PAGE, blocks.size());

        int centerX = width / 2;
        int listStartY = 40;

        for (int i = start; i < end; i++) {
            Block block = blocks.get(i);
            int index = i;
            int y = listStartY + (i - start) * (BUTTON_HEIGHT + 2);

            addRenderableWidget(Button.builder(
                blockDisplayName(block),
                btn -> select(index)
            ).bounds(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        // Page info
        if (totalPages > 1) {
            int navY = listStartY + ENTRIES_PER_PAGE * (BUTTON_HEIGHT + 2) + 8;
            if (page > 0) {
                addRenderableWidget(Button.builder(
                    Component.literal("◀ "),
                    btn -> { page--; rebuildButtons(); }
                ).bounds(centerX - 80, navY, 40, 20).build());
            }
            addRenderableWidget(Button.builder(
                Component.literal((page + 1) + "/" + totalPages),
                btn -> {}
            ).bounds(centerX - 20, navY, 40, 20).build());
            if (page < totalPages - 1) {
                addRenderableWidget(Button.builder(
                    Component.literal(" ▶"),
                    btn -> { page++; rebuildButtons(); }
                ).bounds(centerX + 40, navY, 40, 20).build());
            }
        }
    }

    private void select(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(containerId, index);
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
