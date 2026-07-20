package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.item.tool.StardewAxeItem;
import com.stardew.craft.player.PlayerStardewDataAPI;
import com.stardew.craft.player.ProfessionType;
import com.stardew.craft.player.SkillType;
import com.stardew.craft.sound.ModSounds;
import com.stardew.craft.sve.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

public final class SveWildTreeBlock extends Block implements EntityBlock {
    public static final BooleanProperty STUMP = BooleanProperty.create("stump");
    private static final VoxelShape TRUNK = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static boolean removingWholeTree;
    private final SveWildTreeType type;

    public SveWildTreeBlock(SveWildTreeType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(STUMP, false));
    }

    public SveWildTreeType getType() { return type; }

    public static boolean isLiveTree(BlockState state, SveWildTreeType type) {
        return state.getBlock() instanceof SveWildTreeBlock tree
                && tree.getType() == type
                && (!state.hasProperty(STUMP) || !state.getValue(STUMP));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return TRUNK; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return TRUNK; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SveWildTreeBlockEntity(pos, state); }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != ModBlockEntities.WILD_TREE.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) ->
                SveWildTreeBlockEntity.serverTick(tickLevel, pos, tickState, (SveWildTreeBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (state.getValue(STUMP)) return InteractionResult.PASS;
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SveWildTreeBlockEntity tree
                && tree.shake(serverLevel, pos)) {
            level.playSound(null, pos, ModSounds.LEAFRUSTLE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public InteractionResult useRootWithoutItem(BlockState state, Level level, BlockPos pos, Player player) {
        return useWithoutItem(state, level, pos, player,
                new BlockHitResult(player.position(), net.minecraft.core.Direction.UP, pos, false));
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player.isCreative()) return 1.0F;
        ItemStack tool = player.getMainHandItem();
        if (!isAxeLike(tool)) return 0.0F;
        float speed = tool.getItem() instanceof TieredItem tiered ? tiered.getTier().getSpeed() : 1.0F;
        return speed / Math.max(0.1F, state.getDestroySpeed(level, pos)) / 30.0F;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level.isClientSide()) return;
        if (state.getValue(STUMP)) {
            removeStump(level, pos, player);
        } else {
            fellTree(level, pos, player, type, state);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.getValue(STUMP) && !level.isClientSide() && !state.is(oldState.getBlock())
                && level instanceof ServerLevel serverLevel) {
            ensureExtensions(serverLevel, pos, type);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STUMP);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            removeExtensions(serverLevel, pos, type);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static boolean canMature(ServerLevel level, BlockPos root, SveWildTreeType type) {
        for (int y = 1; y <= type.requiredHeight(); y++) {
            BlockState state = level.getBlockState(root.above(y));
            if (!state.canBeReplaced() && !state.is(type.extensionBlock())) return false;
        }
        return true;
    }

    public static void ensureExtensions(ServerLevel level, BlockPos root, SveWildTreeType type) {
        BlockState rootState = level.getBlockState(root);
        if (rootState.hasProperty(STUMP) && rootState.getValue(STUMP)) return;
        BlockState extension = type.extensionBlock().defaultBlockState();
        for (int y = 1; y < type.trunkHeight(); y++) {
            BlockPos pos = root.above(y);
            if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, extension, Block.UPDATE_ALL);
        }
    }

    private static void removeExtensions(ServerLevel level, BlockPos root, SveWildTreeType type) {
        for (int y = 1; y < type.trunkHeight(); y++) {
            BlockPos pos = root.above(y);
            if (level.getBlockState(pos).is(type.extensionBlock())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Nullable
    public static BlockPos findRoot(LevelReader level, BlockPos pos, SveWildTreeType type) {
        if (isLiveTree(level.getBlockState(pos), type)) return pos;
        for (int y = 1; y < type.trunkHeight(); y++) {
            BlockPos candidate = pos.below(y);
            if (isLiveTree(level.getBlockState(candidate), type)) return candidate;
        }
        return null;
    }

    public static void fellTree(Level level, BlockPos root, Player player) {
        BlockState state = level.getBlockState(root);
        if (!(state.getBlock() instanceof SveWildTreeBlock tree) || state.getValue(STUMP)) return;
        fellTree(level, root, player, tree.getType(), state);
    }

    private static void fellTree(Level level, BlockPos root, Player player, SveWildTreeType type,
                                 BlockState originalState) {
        if (removingWholeTree) return;
        removingWholeTree = true;
        try {
            if (!player.isCreative()) {
                popResource(level, root, new ItemStack(
                        com.stardew.craft.item.ModItems.WOOD_NORMAL.get(), type.woodCount(level.random)));
                if (type.sapCount() > 0) {
                    popResource(level, root, new ItemStack(com.stardew.craft.item.ModItems.SAP.get(), type.sapCount()));
                }
                if (level.random.nextFloat() < SveWildTreeType.SEED_ON_CHOP_CHANCE) {
                    popResource(level, root, new ItemStack(type.seedItem(), 1 + level.random.nextInt(2)));
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    if (PlayerStardewDataAPI.hasProfession(serverPlayer, ProfessionType.LUMBERJACK)
                            && level.random.nextFloat() < 0.25F) {
                        popResource(level, root, new ItemStack(com.stardew.craft.item.ModItems.WOOD_HARD.get()));
                    }
                    PlayerStardewDataAPI.addExperience(serverPlayer, SkillType.FORAGING, 14);
                }
            }
            if (level instanceof ServerLevel serverLevel) removeExtensions(serverLevel, root, type);
            BlockState stumpState = type.matureBlock().defaultBlockState().setValue(STUMP, true);
            level.setBlock(root, stumpState, Block.UPDATE_ALL);
            if (level.getBlockEntity(root) instanceof SveWildTreeBlockEntity treeEntity) {
                treeEntity.beginFelling(computeFallDirection(player, root));
            }
            level.levelEvent(null, 2001, root, Block.getId(originalState));
        } finally {
            removingWholeTree = false;
        }
    }

    private static void removeStump(Level level, BlockPos root, Player player) {
        if (player.isCreative()) return;
        popResource(level, root, new ItemStack(
                com.stardew.craft.item.ModItems.WOOD_NORMAL.get(), 1 + level.random.nextInt(2)));
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerStardewDataAPI.addExperience(serverPlayer, SkillType.FORAGING, 1);
        }
    }

    private static Direction computeFallDirection(Player player, BlockPos root) {
        double dx = root.getX() + 0.5D - player.getX();
        double dz = root.getZ() + 0.5D - player.getZ();
        if (dx * dx + dz * dz < 0.01D) return player.getDirection();
        Direction direction = Direction.getNearest(dx, 0.0D, dz);
        return direction.getAxis().isHorizontal() ? direction : player.getDirection();
    }

    private static boolean isAxeLike(ItemStack tool) {
        return !tool.isEmpty() && (tool.is(ItemTags.AXES)
                || tool.getItem() instanceof StardewAxeItem
                || tool.canPerformAction(ItemAbilities.AXE_DIG));
    }
}
