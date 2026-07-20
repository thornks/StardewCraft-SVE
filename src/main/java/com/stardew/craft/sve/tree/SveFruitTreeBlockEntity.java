package com.stardew.craft.sve.tree;

import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.agriculture.StardewTreeData;
import com.stardew.craft.item.quality.QualityHelper;
import com.stardew.craft.sound.ModSounds;
import com.stardew.craft.sve.ModBlockEntities;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class SveFruitTreeBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String TAG_TYPE = "Type";
    private static final String TAG_FRUIT_COUNT = "FruitCount";
    private static final String TAG_DAYS_SINCE_MATURE = "DaysSinceMature";
    private static final String TAG_LAST_PROCESSED_DAY = "LastProcessedDay";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private SveFruitTreeType type = SveFruitTreeType.PEAR;
    private int fruitCount;
    private int daysSinceMature;
    private int lastProcessedDay = -1;

    public SveFruitTreeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRUIT_TREE.get(), pos, state);
        if (state.getBlock() instanceof SveFruitTreeBlock treeBlock) {
            type = treeBlock.getType();
        }
    }

    public SveFruitTreeType getFruitTreeType() {
        if (getBlockState().getBlock() instanceof SveFruitTreeBlock treeBlock) {
            return treeBlock.getType();
        }
        return type;
    }

    public int getFruitCount() {
        return fruitCount;
    }

    public int getMaxStoredFruit() {
        StardewTreeData data = resolvePublicData();
        return data == null ? SveFruitTreeType.MAX_FRUIT : data.maxStoredProduct();
    }

    public int getDaysSinceMature() {
        return daysSinceMature;
    }

    public int getCurrentFruitQuality() {
        return getFruitQuality();
    }

    public void setNewlyMature(SveFruitTreeType type) {
        this.type = type;
        fruitCount = 0;
        daysSinceMature = 0;
        lastProcessedDay = absoluteDay();
        syncChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SveFruitTreeBlockEntity tree) {
        if (level.isClientSide() || level.getGameTime() % 20L != 0L) {
            return;
        }
        int today = absoluteDay();
        if (tree.lastProcessedDay < 0 || today < tree.lastProcessedDay) {
            tree.lastProcessedDay = today;
            tree.syncChanged();
            return;
        }
        if (today == tree.lastProcessedDay) {
            return;
        }

        int elapsedDays = Math.min(1120, today - tree.lastProcessedDay);
        int firstDay = tree.lastProcessedDay + 1;
        tree.dailyUpdate(level, pos, elapsedDays, firstDay);
        tree.lastProcessedDay = today;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SveFruitTreeBlock.ensureExtensions(serverLevel, pos, tree.getFruitTreeType());
        }
    }

    private void dailyUpdate(Level level, BlockPos pos, int elapsedDays, int firstDay) {
        StardewTreeData data = resolvePublicData();
        int dailyProduct = data == null ? 1 : data.productCount();
        int maxStored = data == null ? SveFruitTreeType.MAX_FRUIT : data.maxStoredProduct();
        for (int offset = 0; offset < elapsedDays; offset++) {
            daysSinceMature++;
            if (SveFruitTreeRules.canFruitOnSeason(level, pos, getFruitTreeType(), data,
                    SveFruitTreeRules.seasonOfAbsoluteDay(firstDay + offset)) && fruitCount < maxStored) {
                fruitCount = Math.min(maxStored, fruitCount + dailyProduct);
            }
        }
        syncChanged();
    }

    public void advanceOneDayForDebug(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        dailyUpdate(level, pos, 1, absoluteDay());
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SveFruitTreeBlock.ensureExtensions(serverLevel, pos, getFruitTreeType());
        }
    }

    public boolean harvestFruit(Player player) {
        if (level == null || level.isClientSide() || fruitCount <= 0) {
            return false;
        }
        Block.popResource(level, worldPosition, createHarvestStack(fruitCount));
        level.playSound(null, worldPosition, ModSounds.LEAFRUSTLE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        fruitCount = 0;
        syncChanged();
        return true;
    }

    public void dropStoredFruit(Level level, BlockPos pos) {
        if (fruitCount <= 0) {
            return;
        }
        Block.popResource(level, pos, createHarvestStack(fruitCount));
        fruitCount = 0;
        setChanged();
    }

    private ItemStack createHarvestStack(int count) {
        StardewTreeData data = resolvePublicData();
        net.minecraft.world.item.Item fruit = data == null
                ? getFruitTreeType().fruitItem()
                : BuiltInRegistries.ITEM.get(data.product());
        if (fruit == Items.AIR) {
            fruit = getFruitTreeType().fruitItem();
        }
        ItemStack stack = new ItemStack(fruit, count);
        QualityHelper.setQuality(stack, getFruitQuality());
        return stack;
    }

    @Nullable
    private StardewTreeData resolvePublicData() {
        return level == null
                ? null
                : StardewAgricultureDataApi.tree(level, worldPosition, getBlockState());
    }

    private int getFruitQuality() {
        if (daysSinceMature < SveFruitTreeType.QUALITY_STEP_DAYS) {
            return QualityHelper.NORMAL;
        }
        if (daysSinceMature >= SveFruitTreeType.QUALITY_STEP_DAYS * 3) {
            return QualityHelper.IRIDIUM;
        }
        if (daysSinceMature >= SveFruitTreeType.QUALITY_STEP_DAYS * 2) {
            return QualityHelper.GOLD;
        }
        return QualityHelper.SILVER;
    }

    private void syncChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    private static int absoluteDay() {
        StardewTimeManager time = StardewTimeManager.get();
        return (time.getCurrentYear() - 1) * 112
                + time.getCurrentSeason() * 28
                + time.getCurrentDay();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(4.0, 7.0, 4.0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_TYPE, getFruitTreeType().id());
        tag.putInt(TAG_FRUIT_COUNT, fruitCount);
        tag.putInt(TAG_DAYS_SINCE_MATURE, daysSinceMature);
        tag.putInt(TAG_LAST_PROCESSED_DAY, lastProcessedDay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = SveFruitTreeType.byId(tag.getString(TAG_TYPE));
        fruitCount = Math.max(0, tag.getInt(TAG_FRUIT_COUNT));
        daysSinceMature = Math.max(0, tag.getInt(TAG_DAYS_SINCE_MATURE));
        lastProcessedDay = tag.contains(TAG_LAST_PROCESSED_DAY) ? tag.getInt(TAG_LAST_PROCESSED_DAY) : -1;
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
