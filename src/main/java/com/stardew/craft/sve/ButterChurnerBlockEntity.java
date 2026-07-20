package com.stardew.craft.sve;

import com.stardew.craft.blockentity.TimedProductionBlockEntity;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.item.quality.QualityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ButterChurnerBlockEntity extends TimedProductionBlockEntity {

    private static final String MACHINE_KEY = "butter_churner";

    public ButterChurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUTTER_CHURNER.get(), pos, state);
        this.input = ItemStack.EMPTY;
        this.product = ItemStack.EMPTY;
        this.readyAtAbsMinute = -1L;
        this.ready = false;
    }

    /**
     * Attempt to insert a milk item and start butter processing.
     *
     * @return the number of items consumed (0 if insertion failed)
     */
    public int tryInsert(ItemStack stack, Player player) {
        if (stack.isEmpty()) return 0;
        if (!product.isEmpty()) return 0;
        if (readyAtAbsMinute >= 0) return 0;

        var opt = ArtisanRecipeDataManager.getRecipe(MACHINE_KEY, stack);
        if (opt.isEmpty()) return 0;

        var recipe = opt.get();

        // Build output stack
        ItemStack output = new ItemStack(
            BuiltInRegistries.ITEM.get(recipe.outputId()),
            recipe.outputCount() > 0 ? recipe.outputCount() : 1
        );
        if (output.isEmpty()) return 0;

        // Apply quality rules
        if (recipe.keepInputQuality()) {
            QualityHelper.setQuality(output, QualityHelper.getQuality(stack));
        } else if (recipe.outputQuality() > 0) {
            QualityHelper.setQuality(output, recipe.outputQuality());
        }

        int consume = recipe.consumeCount() > 0 ? recipe.consumeCount() : 1;
        this.input = stack.copyWithCount(consume);
        this.product = output;
        this.readyAtAbsMinute = getCurrentAbsMinute() + 60;
        this.ready = false;

        setChanged();
        syncToClient();
        return consume;
    }

    /**
     * Harvest the finished product.
     */
    public ItemStack harvestOne() {
        if (!isReady()) return ItemStack.EMPTY;
        ItemStack result = product.copy();
        product = ItemStack.EMPTY;
        input = ItemStack.EMPTY;
        readyAtAbsMinute = -1;
        ready = false;
        setChanged();
        syncToClient();
        return result;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isWorking() {
        return !input.isEmpty() && !ready && readyAtAbsMinute > 0;
    }

    public boolean hasInput() {
        return !input.isEmpty();
    }

    // ===== Server ticking =====

    public static void serverTick(Level level, BlockPos pos, BlockState state, ButterChurnerBlockEntity be) {
        if (level.isClientSide) return;

        boolean currentlyReady = be.refreshReady();
        if (currentlyReady != be.ready) {
            be.ready = currentlyReady;
            be.setChanged();
            be.syncToClient();
        }

        // Update WORKING blockstate if needed
        boolean working = be.isWorking();
        if (state.hasProperty(ButterChurnerBlock.WORKING) && state.getValue(ButterChurnerBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(ButterChurnerBlock.WORKING, working), 3);
        }
    }

    // ===== Display data (for Jade) =====

    public ItemStack getDisplayInput() {
        return input;
    }

    public ItemStack getDisplayProduct() {
        return product;
    }

    public long getReadyAtAbsMinute() {
        return readyAtAbsMinute;
    }

    public long getCurrentAbsMinutePublic() {
        return getCurrentAbsMinute();
    }

    // ===== Automation =====

    public ItemStack getAutomationInput() {
        return input;
    }

    public ItemStack getAutomationOutput() {
        return ready ? product : ItemStack.EMPTY;
    }

    public ItemStack insertAutomation(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return stack;
        if (!product.isEmpty()) return stack;
        if (readyAtAbsMinute >= 0) return stack;

        var opt = ArtisanRecipeDataManager.getRecipe(MACHINE_KEY, stack);
        if (opt.isEmpty()) return stack;

        var recipe = opt.get();
        int consume = recipe.consumeCount() > 0 ? recipe.consumeCount() : 1;

        if (simulate) {
            return com.stardew.craft.blockentity.AutomationStackHelper.remainderAfterInsert(stack, consume);
        }

        ItemStack output = new ItemStack(
            BuiltInRegistries.ITEM.get(recipe.outputId()),
            recipe.outputCount() > 0 ? recipe.outputCount() : 1
        );
        if (output.isEmpty()) return stack;

        // Apply quality rules
        if (recipe.keepInputQuality()) {
            QualityHelper.setQuality(output, QualityHelper.getQuality(stack));
        } else if (recipe.outputQuality() > 0) {
            QualityHelper.setQuality(output, recipe.outputQuality());
        }

        this.input = stack.copyWithCount(consume);
        this.product = output;
        this.readyAtAbsMinute = getCurrentAbsMinute() + 60;
        this.ready = false;

        setChanged();
        syncToClient();

        return com.stardew.craft.blockentity.AutomationStackHelper.remainderAfterInsert(stack, consume);
    }

    public ItemStack extractAutomation(int maxCount, boolean simulate) {
        if (!ready || product.isEmpty()) return ItemStack.EMPTY;

        ItemStack extracted = com.stardew.craft.blockentity.AutomationStackHelper.extractUpTo(product, maxCount);
        if (simulate) return extracted;

        if (extracted.getCount() >= product.getCount()) {
            return harvestOne();
        }

        product.shrink(extracted.getCount());
        setChanged();
        syncToClient();
        return extracted;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) {
            tag.put("input", input.save(registries));
        }
        if (!product.isEmpty()) {
            tag.put("product", product.save(registries));
        }
        tag.putLong("readyAtAbsMinute", readyAtAbsMinute);
        tag.putBoolean("ready", ready);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = tag.contains("input")
                ? ItemStack.parse(registries, tag.getCompound("input")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        product = tag.contains("product")
                ? ItemStack.parse(registries, tag.getCompound("product")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        readyAtAbsMinute = tag.getLong("readyAtAbsMinute");
        ready = tag.getBoolean("ready");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
