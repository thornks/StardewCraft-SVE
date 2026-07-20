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

public enum YarnSpoolerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String READY = "ready";
    private static final String INPUT_ITEM = "inputItem";
    private static final String INPUT_QUALITY = "inputQuality";
    private static final String PRODUCT_ITEM = "productItem";
    private static final String PRODUCT_QUALITY = "productQuality";
    private static final String DAYS = "days";
    private static final String HOURS = "hours";
    private static final String MINUTES = "minutes";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof YarnSpoolerBlockEntity spooler)) {
            return;
        }
        tag.putBoolean(READY, spooler.isReady());
        putStack(tag, INPUT_ITEM, INPUT_QUALITY, spooler.getDisplayInput());
        putStack(tag, PRODUCT_ITEM, PRODUCT_QUALITY, spooler.getDisplayProduct());

        long readyAt = spooler.getReadyAtAbsMinute();
        if (readyAt >= 0 && !spooler.isReady()) {
            long remaining = readyAt - spooler.getCurrentAbsMinutePublic();
            if (remaining > 0) {
                tag.putInt(DAYS, (int) (remaining / (24 * 60)));
                tag.putInt(HOURS, (int) ((remaining % (24 * 60)) / 60));
                tag.putInt(MINUTES, (int) (remaining % 60));
            }
        }
    }

    private static void putStack(CompoundTag tag, String itemKey, String qualityKey, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            tag.putString(itemKey, id.toString());
            tag.putInt(qualityKey, QualityHelper.getQuality(stack));
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(READY)) {
            return;
        }
        IElementHelper helper = IElementHelper.get();
        String productId = data.getString(PRODUCT_ITEM);
        if (data.getBoolean(READY) && !productId.isEmpty()) {
            ItemStack product = stackFromData(data, PRODUCT_ITEM, PRODUCT_QUALITY);
            if (!product.isEmpty()) {
                tooltip.add(java.util.List.of(
                        helper.item(product, 1.0F),
                        helper.spacer(4, 0),
                        helper.text(Component.translatable("stardewcraft.tooltip.tapper.product")
                                .append(":")
                                .append(product.getHoverName())
                                .withStyle(ChatFormatting.WHITE))));
            }
            tooltip.add(Component.translatable("stardewcraftsve.tooltip.yarn_spooler.ready")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            return;
        }

        String inputId = data.getString(INPUT_ITEM);
        if (inputId.isEmpty()) {
            tooltip.add(Component.translatable("stardewcraftsve.tooltip.yarn_spooler.input")
                    .append(":")
                    .append(Component.translatable("stardewcraftsve.tooltip.yarn_spooler.input.none"))
                    .withStyle(ChatFormatting.WHITE));
            return;
        }

        ItemStack input = stackFromData(data, INPUT_ITEM, INPUT_QUALITY);
        if (!input.isEmpty()) {
            tooltip.add(java.util.List.of(
                    helper.item(input, 1.0F),
                    helper.spacer(4, 0),
                    helper.text(Component.translatable("stardewcraftsve.tooltip.yarn_spooler.input")
                            .append(":")
                            .append(input.getHoverName())
                            .withStyle(ChatFormatting.WHITE))));
        }
        tooltip.add(RemainingTimeTooltip.build(
                "stardewcraftsve.tooltip.yarn_spooler.remaining",
                data.getInt(DAYS), data.getInt(HOURS), data.getInt(MINUTES))
                .withStyle(ChatFormatting.GRAY));
    }

    private static ItemStack stackFromData(CompoundTag data, String itemKey, String qualityKey) {
        ResourceLocation id = ResourceLocation.tryParse(data.getString(itemKey));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        QualityHelper.setQuality(stack, data.getInt(qualityKey));
        return stack;
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "yarn_spooler");
    }
}
