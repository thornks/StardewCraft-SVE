package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.sve.ModBlockEntities;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class SveWildTreeBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private SveWildTreeType type = SveWildTreeType.FIR;
    private int lastProcessedDay = -1;
    private int lastShakenDay = Integer.MIN_VALUE;
    private boolean hasSeed;
    private boolean felled;
    private boolean fallComplete;
    private int fallDirection = Direction.NORTH.get2DDataValue();
    private long fallStartedTick;

    public SveWildTreeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WILD_TREE.get(), pos, state);
        if (state.getBlock() instanceof SveWildTreeBlock tree) type = tree.getType();
        if (state.hasProperty(SveWildTreeBlock.STUMP) && state.getValue(SveWildTreeBlock.STUMP)) {
            felled = true;
            fallComplete = true;
        }
    }

    public SveWildTreeType getTreeType() {
        return getBlockState().getBlock() instanceof SveWildTreeBlock tree ? tree.getType() : type;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SveWildTreeBlockEntity tree) {
        if (tree.felled || state.getValue(SveWildTreeBlock.STUMP)) {
            if (!tree.fallComplete && level.getGameTime() - tree.fallStartedTick >= tree.getTreeType().fallDurationTicks()) {
                tree.fallComplete = true;
                tree.syncChanged();
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 20L != 0L) return;
        int today = absoluteDay();
        if (tree.lastProcessedDay < 0 || today < tree.lastProcessedDay) {
            tree.lastProcessedDay = today;
            tree.rollSeed(serverLevel);
            tree.syncChanged();
            return;
        }
        if (today == tree.lastProcessedDay) {
            SveWildTreeBlock.ensureExtensions(serverLevel, pos, tree.getTreeType());
            return;
        }
        int elapsed = Math.min(1120, today - tree.lastProcessedDay);
        for (int day = 0; day < elapsed; day++) {
            tree.rollSeed(serverLevel);
            if (serverLevel.random.nextFloat() < SveWildTreeType.SEED_SPREAD_CHANCE) {
                SveWildTreeGrowthManager.trySpreadSapling(serverLevel, pos, tree.getTreeType());
            }
        }
        tree.lastProcessedDay = today;
        SveWildTreeBlock.ensureExtensions(serverLevel, pos, tree.getTreeType());
        tree.syncChanged();
    }

    public boolean shake(ServerLevel level, BlockPos pos) {
        int today = absoluteDay();
        if (lastShakenDay == today) return false;
        lastShakenDay = today;
        if (hasSeed) {
            hasSeed = false;
            Block.popResource(level, pos, new ItemStack(getTreeType().seedItem()));
        }
        syncChanged();
        return true;
    }

    public boolean hasSeed() { return hasSeed; }
    public boolean wasShakenToday() { return lastShakenDay == absoluteDay(); }
    public boolean isFelled() { return felled; }
    public boolean isFallComplete() { return fallComplete; }

    public void beginFelling(Direction direction) {
        felled = true;
        fallComplete = false;
        fallDirection = direction.get2DDataValue();
        fallStartedTick = level == null ? 0L : level.getGameTime();
        syncChanged();
    }

    private void rollSeed(ServerLevel level) {
        hasSeed = level.random.nextFloat() < SveWildTreeType.SEED_ON_SHAKE_CHANCE;
    }

    private void syncChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    private static int absoluteDay() {
        StardewTimeManager time = StardewTimeManager.get();
        return (time.getCurrentYear() - 1) * 112 + time.getCurrentSeason() * 28 + time.getCurrentDay();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "felling", 0, state -> {
            if (!felled) return PlayState.STOP;
            String animation = fallComplete
                    ? "stump"
                    : "fall_" + Direction.from2DDataValue(fallDirection).getName();
            return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(animation));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(6.0, getTreeType().requiredHeight() + 1.0, 6.0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Type", getTreeType().id());
        tag.putInt("LastProcessedDay", lastProcessedDay);
        tag.putInt("LastShakenDay", lastShakenDay);
        tag.putBoolean("HasSeed", hasSeed);
        tag.putBoolean("Felled", felled);
        tag.putBoolean("FallComplete", fallComplete);
        tag.putInt("FallDirection", fallDirection);
        tag.putLong("FallStartedTick", fallStartedTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = SveWildTreeType.byId(tag.getString("Type"));
        lastProcessedDay = tag.contains("LastProcessedDay") ? tag.getInt("LastProcessedDay") : -1;
        lastShakenDay = tag.contains("LastShakenDay") ? tag.getInt("LastShakenDay") : Integer.MIN_VALUE;
        hasSeed = tag.getBoolean("HasSeed");
        felled = tag.getBoolean("Felled")
                || getBlockState().hasProperty(SveWildTreeBlock.STUMP)
                && getBlockState().getValue(SveWildTreeBlock.STUMP);
        fallComplete = tag.contains("FallComplete") ? tag.getBoolean("FallComplete") : felled;
        fallDirection = tag.contains("FallDirection")
                ? tag.getInt("FallDirection")
                : Direction.NORTH.get2DDataValue();
        fallStartedTick = tag.getLong("FallStartedTick");
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
