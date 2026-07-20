package com.stardew.craft.sve.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Compact checkbox drawn with the same palette and checkmark as StardewCraft's farm list. */
public final class SveFarmDifficultyCheckbox extends AbstractButton {
    private static final int TEXT_COLOR = 0x5D4037;
    private static final int TEXT_HOVER_COLOR = 0x582A11;
    private static final int CHECK_COLOR = 0x2E7D32;

    private final Font font;
    private final int boxSize;
    private final Consumer<Boolean> onChanged;
    private boolean selected;

    public SveFarmDifficultyCheckbox(int x, int y, int width, Font font, Component label,
                                     boolean selected, Consumer<Boolean> onChanged) {
        super(x, y, width, Math.max(13, font.lineHeight + 4), label);
        this.font = font;
        this.boxSize = Math.max(11, font.lineHeight + 3);
        this.selected = selected;
        this.onChanged = onChanged;
    }

    public boolean selected() {
        return selected;
    }

    @Override
    public void onPress() {
        selected = !selected;
        onChanged.accept(selected);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        if (highlighted) {
            graphics.fill(getX() - 2, getY() - 1,
                    getX() + width, getY() + height + 1, 0x22EADB8C);
        }

        int boxY = getY() + (height - boxSize) / 2;
        int border = highlighted ? 0xCC582A11 : 0xAA8A4B20;
        graphics.fill(getX(), boxY, getX() + boxSize, boxY + boxSize, border);
        graphics.fill(getX() + 1, boxY + 1,
                getX() + boxSize - 1, boxY + boxSize - 1,
                selected ? 0xAAF6E3A5 : 0x66F6E3A5);

        if (selected) {
            String check = "\u2714";
            int checkX = getX() + (boxSize - font.width(check)) / 2;
            int checkY = boxY + (boxSize - font.lineHeight) / 2;
            graphics.drawString(font, check, checkX, checkY, CHECK_COLOR, false);
        }

        int textX = getX() + boxSize + 6;
        int textY = getY() + (height - font.lineHeight) / 2;
        graphics.drawString(font, getMessage(), textX, textY,
                highlighted ? TEXT_HOVER_COLOR : TEXT_COLOR, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
