package com.stardew.craft.sve;

import com.stardew.craft.blockentity.TimedProductionBlockEntity;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.item.quality.QualityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class YarnSpoolerBlockEntity extends TimedProductionBlockEntity implements GeoBlockEntity {
    private static final String MACHINE_KEY = "yarn_spooler";
    private static final int FALLBACK_PROCESSING_MINUTES = 120;
    private static final RawAnimation WORKING_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.winding_machine.working");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public YarnSpoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.YARN_SPOOLER.get(), pos, state);
        input = ItemStack.EMPTY;
        product = ItemStack.EMPTY;
        readyAtAbsMinute = -1L;
        ready = false;
    }

    public int tryInsert(ItemStack stack) {
        if (!canStart(stack)) {
            return 0;
        }
        var recipe = ArtisanRecipeDataManager.getRecipe(MACHINE_KEY, stack);
        if (recipe.isEmpty()) {
            return 0;
        }
        return startRecipe(stack, recipe.get()) ? consumedCount(recipe.get()) : 0;
    }

    private boolean canStart(ItemStack stack) {
        return !stack.isEmpty() && product.isEmpty() && readyAtAbsMinute < 0;
    }

    private boolean startRecipe(ItemStack source, ArtisanRecipeDataManager.Recipe recipe) {
        ItemStack output = new ItemStack(
                BuiltInRegistries.ITEM.get(recipe.outputId()),
                recipe.outputCount() > 0 ? recipe.outputCount() : 1);
        if (output.isEmpty()) {
            return false;
        }
        if (recipe.keepInputQuality()) {
            QualityHelper.setQuality(output, QualityHelper.getQuality(source));
        } else if (recipe.outputQuality() > 0) {
            QualityHelper.setQuality(output, recipe.outputQuality());
        }

        int consume = consumedCount(recipe);
        input = source.copyWithCount(consume);
        product = output;
        readyAtAbsMinute = getCurrentAbsMinute()
                + (recipe.minutes() > 0 ? recipe.minutes() : FALLBACK_PROCESSING_MINUTES);
        ready = false;
        setChanged();
        syncToClient();
        return true;
    }

    private static int consumedCount(ArtisanRecipeDataManager.Recipe recipe) {
        return recipe.consumeCount() > 0 ? recipe.consumeCount() : 1;
    }

    public ItemStack harvestOne() {
        if (!isReady()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = product.copy();
        input = ItemStack.EMPTY;
        product = ItemStack.EMPTY;
        readyAtAbsMinute = -1L;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, YarnSpoolerBlockEntity spooler) {
        if (level.isClientSide) {
            return;
        }
        boolean currentlyReady = spooler.refreshReady();
        if (currentlyReady != spooler.ready) {
            spooler.ready = currentlyReady;
            spooler.setChanged();
            spooler.syncToClient();
        }
        boolean working = spooler.isWorking();
        if (state.getValue(YarnSpoolerBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(YarnSpoolerBlock.WORKING, working), Block.UPDATE_ALL);
        }
    }

    @Override
    public ItemStack getAutomationInput() {
        return input;
    }

    @Override
    public ItemStack getAutomationOutput() {
        return ready ? product : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertAutomation(ItemStack stack, boolean simulate) {
        if (!canStart(stack)) {
            return stack;
        }
        var recipe = ArtisanRecipeDataManager.getRecipe(MACHINE_KEY, stack);
        if (recipe.isEmpty()) {
            return stack;
        }
        int consume = consumedCount(recipe.get());
        if (simulate) {
            return com.stardew.craft.blockentity.AutomationStackHelper.remainderAfterInsert(stack, consume);
        }
        if (!startRecipe(stack, recipe.get())) {
            return stack;
        }
        return com.stardew.craft.blockentity.AutomationStackHelper.remainderAfterInsert(stack, consume);
    }

    @Override
    public ItemStack extractAutomation(int maxCount, boolean simulate) {
        if (!ready || product.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = com.stardew.craft.blockentity.AutomationStackHelper.extractUpTo(product, maxCount);
        if (simulate) {
            return extracted;
        }
        if (extracted.getCount() >= product.getCount()) {
            return harvestOne();
        }
        product.shrink(extracted.getCount());
        setChanged();
        syncToClient();
        return extracted;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "working", 0, this::workingAnimation));
    }

    private PlayState workingAnimation(AnimationState<YarnSpoolerBlockEntity> state) {
        return isWorking() ? state.setAndContinue(WORKING_ANIMATION) : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

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
        readyAtAbsMinute = tag.contains("readyAtAbsMinute") ? tag.getLong("readyAtAbsMinute") : -1L;
        ready = tag.getBoolean("ready");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>
            getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
