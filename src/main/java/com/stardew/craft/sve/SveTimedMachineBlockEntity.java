package com.stardew.craft.sve;

import com.stardew.craft.blockentity.AutomationStackHelper;
import com.stardew.craft.blockentity.TimedProductionBlockEntity;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.item.quality.QualityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared single-input timed processing runtime for SVE utility machines. */
public abstract class SveTimedMachineBlockEntity extends TimedProductionBlockEntity {
    private final String machineKey;
    private final int fallbackProcessingMinutes;

    protected SveTimedMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            String machineKey,
            int fallbackProcessingMinutes
    ) {
        super(type, pos, state);
        if (machineKey == null || machineKey.isBlank()) throw new IllegalArgumentException("machineKey");
        if (fallbackProcessingMinutes <= 0) throw new IllegalArgumentException("fallbackProcessingMinutes");
        this.machineKey = machineKey;
        this.fallbackProcessingMinutes = fallbackProcessingMinutes;
        input = ItemStack.EMPTY;
        product = ItemStack.EMPTY;
        readyAtAbsMinute = -1L;
        ready = false;
    }

    public final int tryInsert(ItemStack stack) {
        if (!canStart(stack)) return 0;
        var recipe = ArtisanRecipeDataManager.getRecipe(machineKey, stack);
        if (recipe.isEmpty()) return 0;
        return startRecipe(stack, recipe.get()) ? consumedCount(recipe.get()) : 0;
    }

    private boolean canStart(ItemStack stack) {
        return !stack.isEmpty() && product.isEmpty() && readyAtAbsMinute < 0;
    }

    private boolean startRecipe(ItemStack source, ArtisanRecipeDataManager.Recipe recipe) {
        ItemStack output = new ItemStack(
                BuiltInRegistries.ITEM.get(recipe.outputId()),
                recipe.outputCount() > 0 ? recipe.outputCount() : 1);
        if (output.isEmpty()) return false;

        if (recipe.keepInputQuality()) {
            QualityHelper.setQuality(output, QualityHelper.getQuality(source));
        } else if (recipe.outputQuality() >= 0) {
            QualityHelper.setQuality(output, recipe.outputQuality());
        }

        int consume = consumedCount(recipe);
        input = source.copyWithCount(consume);
        product = output;
        readyAtAbsMinute = getCurrentAbsMinute()
                + (recipe.minutes() > 0 ? recipe.minutes() : fallbackProcessingMinutes);
        ready = false;
        setChanged();
        syncToClient();
        return true;
    }

    private static int consumedCount(ArtisanRecipeDataManager.Recipe recipe) {
        return recipe.consumeCount() > 0 ? recipe.consumeCount() : 1;
    }

    public final ItemStack harvestOne() {
        if (!ready) return ItemStack.EMPTY;
        ItemStack result = product.copy();
        input = ItemStack.EMPTY;
        product = ItemStack.EMPTY;
        readyAtAbsMinute = -1L;
        ready = false;
        setChanged();
        syncToClient();
        return result;
    }

    public final boolean isReady() {
        return ready;
    }

    public final boolean isWorking() {
        return !input.isEmpty() && !ready && readyAtAbsMinute > 0;
    }

    public final boolean hasInput() {
        return !input.isEmpty();
    }

    public final ItemStack getDisplayInput() {
        return input;
    }

    public final ItemStack getDisplayProduct() {
        return product;
    }

    public final long getReadyAtAbsMinute() {
        return readyAtAbsMinute;
    }

    public final long getCurrentAbsMinutePublic() {
        return getCurrentAbsMinute();
    }

    public final void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean currentlyReady = refreshReady();
        if (currentlyReady != ready) {
            ready = currentlyReady;
            setChanged();
            syncToClient();
        }
        boolean working = isWorking();
        if (state.hasProperty(SveTimedMachineBlock.WORKING)
                && state.getValue(SveTimedMachineBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(SveTimedMachineBlock.WORKING, working), Block.UPDATE_ALL);
        }
    }

    @Override
    public final ItemStack getAutomationInput() {
        return input;
    }

    @Override
    public final ItemStack getAutomationOutput() {
        return ready ? product : ItemStack.EMPTY;
    }

    @Override
    public final ItemStack insertAutomation(ItemStack stack, boolean simulate) {
        if (!canStart(stack)) return stack;
        var recipe = ArtisanRecipeDataManager.getRecipe(machineKey, stack);
        if (recipe.isEmpty()) return stack;
        int consume = consumedCount(recipe.get());
        if (simulate) return AutomationStackHelper.remainderAfterInsert(stack, consume);
        if (!startRecipe(stack, recipe.get())) return stack;
        return AutomationStackHelper.remainderAfterInsert(stack, consume);
    }

    @Override
    public final ItemStack extractAutomation(int maxCount, boolean simulate) {
        if (!ready || product.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = AutomationStackHelper.extractUpTo(product, maxCount);
        if (simulate) return extracted;
        if (extracted.getCount() >= product.getCount()) return harvestOne();
        product.shrink(extracted.getCount());
        setChanged();
        syncToClient();
        return extracted;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) tag.put("input", input.save(registries));
        if (!product.isEmpty()) tag.put("product", product.save(registries));
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
        readyAtAbsMinute = tag.contains("readyAtAbsMinute") ? tag.getLong("readyAtAbsMinute") : -1L;
        ready = tag.getBoolean("ready");
    }

    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public final Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
