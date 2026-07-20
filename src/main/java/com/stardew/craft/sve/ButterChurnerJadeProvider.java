package com.stardew.craft.sve;

import com.stardew.craft.integration.jade.RemainingTimeTooltip;
import com.stardew.craft.item.quality.QualityHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

public enum ButterChurnerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String NBT_READY = "ready";
    private static final String NBT_INPUT_ITEM = "inputItem";
    private static final String NBT_PRODUCT_ITEM = "productItem";
    private static final String NBT_PRODUCT_QUALITY = "productQuality";
    private static final String NBT_DAYS = "days";
    private static final String NBT_HOURS = "hours";
    private static final String NBT_MINUTES = "minutes";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ButterChurnerBlockEntity churner)) return;

        tag.putBoolean(NBT_READY, churner.isReady());

        ItemStack input = churner.getDisplayInput();
        if (!input.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(input.getItem());
            if (id != null) {
                tag.putString(NBT_INPUT_ITEM, id.toString());
            }
        }

        ItemStack product = churner.getDisplayProduct();
        if (!product.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(product.getItem());
            if (id != null) {
                tag.putString(NBT_PRODUCT_ITEM, id.toString());
            }
            tag.putInt(NBT_PRODUCT_QUALITY, QualityHelper.getQuality(product));
        }

        // Remaining time
        long readyAt = churner.getReadyAtAbsMinute();
        if (readyAt >= 0 && !churner.isReady()) {
            long remaining = readyAt - churner.getCurrentAbsMinutePublic();
            if (remaining > 0) {
                int days = (int) (remaining / (24 * 60));
                int hours = (int) ((remaining % (24 * 60)) / 60);
                int minutes = (int) (remaining % 60);
                tag.putInt(NBT_DAYS, days);
                tag.putInt(NBT_HOURS, hours);
                tag.putInt(NBT_MINUTES, minutes);
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(NBT_READY)) return;

        boolean ready = data.getBoolean(NBT_READY);
        String productItemId = data.getString(NBT_PRODUCT_ITEM);
        String inputItemId = data.getString(NBT_INPUT_ITEM);

        IElementHelper helper = IElementHelper.get();

        if (ready && !productItemId.isEmpty()) {
            // --- Ready state: show product + ready text ---
            ItemStack productStack = stackFromId(productItemId);
            if (!productStack.isEmpty()) {
                int quality = data.getInt(NBT_PRODUCT_QUALITY);
                if (quality > 0) {
                    QualityHelper.setQuality(productStack, quality);
                }
                tooltip.add(java.util.List.of(
                    helper.item(productStack, 1.0f),
                    helper.spacer(4, 0),
                    helper.text(
                        Component.translatable("stardewcraft.tooltip.tapper.product")
                            .append(":")
                            .append(productStack.getHoverName())
                            .withStyle(ChatFormatting.WHITE)
                    )
                ));
            }
            tooltip.add(
                Component.translatable("stardewcraftsve.tooltip.butter_churner.ready")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            );
            return;
        }

        if (inputItemId.isEmpty()) {
            // --- Empty: show "Input: None" ---
            tooltip.add(
                Component.translatable("stardewcraftsve.tooltip.butter_churner.input")
                    .append(":")
                    .append(Component.translatable("stardewcraftsve.tooltip.butter_churner.input.none"))
                    .withStyle(ChatFormatting.WHITE)
            );
            return;
        }

        // --- Has input: show input item + remaining time ---
        ItemStack inputStack = stackFromId(inputItemId);
        if (!inputStack.isEmpty()) {
            tooltip.add(java.util.List.of(
                helper.item(inputStack, 1.0f),
                helper.spacer(4, 0),
                helper.text(
                    Component.translatable("stardewcraftsve.tooltip.butter_churner.input")
                        .append(":")
                        .append(inputStack.getHoverName())
                        .withStyle(ChatFormatting.WHITE)
                )
            ));
        } else {
            tooltip.add(
                Component.translatable("stardewcraftsve.tooltip.butter_churner.input")
                    .append(":")
                    .append(Component.literal(inputItemId))
                    .withStyle(ChatFormatting.WHITE)
            );
        }

        // Remaining time (always shown when input is present)
        int days = data.getInt(NBT_DAYS);
        int hours = data.getInt(NBT_HOURS);
        int minutes = data.getInt(NBT_MINUTES);
        tooltip.add(
            RemainingTimeTooltip.build("stardewcraftsve.tooltip.butter_churner.remaining", days, hours, minutes)
                .withStyle(ChatFormatting.GRAY)
        );
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath("stardewcraftsve", "butter_churner");
    }

    private static ItemStack stackFromId(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return ItemStack.EMPTY;
        if (!BuiltInRegistries.ITEM.containsKey(location)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(location));
    }
}
