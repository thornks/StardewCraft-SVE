package com.stardew.craft.sve.tree;

import com.stardew.craft.sve.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class SveFruitTreeBlock extends Block implements EntityBlock {
    private static final VoxelShape TRUNK_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static boolean removingWholeTree;

    private final SveFruitTreeType type;

    public SveFruitTreeBlock(SveFruitTreeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public SveFruitTreeType getType() {
        return type;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TRUNK_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TRUNK_SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return com.stardew.craft.tree.fruit.FruitTreeRules.isValidGround(level.getBlockState(pos.below()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SveFruitTreeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != ModBlockEntities.FRUIT_TREE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                SveFruitTreeBlockEntity.serverTick(tickLevel, pos, tickState,
                        (SveFruitTreeBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return useRootWithoutItem(state, level, pos, player);
    }

    public InteractionResult useRootWithoutItem(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof SveFruitTreeBlockEntity tree && tree.harvestFruit(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            fellTree(level, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock()) && level instanceof ServerLevel serverLevel) {
            placeExtensions(serverLevel, pos, type);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            removeExtensions(serverLevel, pos, type);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void fellTree(Level level, BlockPos rootPos, Player player) {
        if (removingWholeTree) {
            return;
        }
        BlockState rootState = level.getBlockState(rootPos);
        if (!(rootState.getBlock() instanceof SveFruitTreeBlock treeBlock)) {
            return;
        }

        removingWholeTree = true;
        try {
            if (!player.isCreative()) {
                if (level.getBlockEntity(rootPos) instanceof SveFruitTreeBlockEntity tree) {
                    tree.dropStoredFruit(level, rootPos);
                }
                popResource(level, rootPos,
                        new ItemStack(com.stardew.craft.item.ModItems.WOOD_NORMAL.get(), 12));
            }
            if (level instanceof ServerLevel serverLevel) {
                removeExtensions(serverLevel, rootPos, treeBlock.getType());
            }
            level.setBlock(rootPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.levelEvent(null, 2001, rootPos, Block.getId(rootState));
        } finally {
            removingWholeTree = false;
        }
    }

    @Nullable
    public static BlockPos findRoot(LevelReader level, BlockPos pos, SveFruitTreeType type) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SveFruitTreeBlock treeBlock && treeBlock.getType() == type) {
            return pos;
        }
        for (int dy = 1; dy <= type.trunkTopY(); dy++) {
            BlockPos candidate = pos.below(dy);
            if (level.getBlockState(candidate).getBlock() instanceof SveFruitTreeBlock treeBlock
                    && treeBlock.getType() == type) {
                return candidate;
            }
        }
        return null;
    }

    public static VoxelShape extensionShape(LevelReader level, BlockPos pos, SveFruitTreeType type) {
        return findRoot(level, pos, type) == null ? Shapes.empty() : TRUNK_SHAPE;
    }

    public static void ensureExtensions(ServerLevel level, BlockPos rootPos, SveFruitTreeType type) {
        placeExtensions(level, rootPos, type);
    }

    private static void placeExtensions(ServerLevel level, BlockPos rootPos, SveFruitTreeType type) {
        BlockState extension = type.extensionBlock().defaultBlockState();
        for (int y = 1; y <= type.trunkTopY(); y++) {
            BlockPos pos = rootPos.above(y);
            if (level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, extension, Block.UPDATE_ALL);
            }
        }
    }

    private static void removeExtensions(ServerLevel level, BlockPos rootPos, SveFruitTreeType type) {
        for (int y = 1; y <= type.trunkTopY(); y++) {
            BlockPos pos = rootPos.above(y);
            if (level.getBlockState(pos).getBlock() instanceof SveFruitTreeExtensionBlock extension
                    && extension.getType() == type) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
