package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Shared block interaction and ticking behavior for SVE timed machines. */
public abstract class SveTimedMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private final VoxelShape shape;

    protected SveTimedMachineBlock(BlockBehaviour.Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WORKING, false));
    }

    protected abstract SveTimedMachineBlockEntity createMachineBlockEntity(BlockPos pos, BlockState state);

    protected abstract BlockEntityType<? extends SveTimedMachineBlockEntity> machineBlockEntityType();

    @Override
    protected final void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING);
    }

    @Nullable
    @Override
    public final BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WORKING, false);
    }

    @Override
    protected final VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected final RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public final <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != machineBlockEntityType()) return null;
        return (tickLevel, pos, tickState, blockEntity) ->
                ((SveTimedMachineBlockEntity) blockEntity).serverTick(tickLevel, pos, tickState);
    }

    @Override
    protected final ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SveTimedMachineBlockEntity machine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (com.stardew.craft.blockentity.UtilityDropHelper.tryHarvest(
                level, pos, player, machine::isReady, machine::harvestOne, 1)) {
            return ItemInteractionResult.SUCCESS;
        }
        int consumed = machine.tryInsert(stack);
        if (consumed <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!player.isCreative()) stack.shrink(consumed);
        if (!state.getValue(WORKING)) {
            level.setBlock(pos, state.setValue(WORKING, true), Block.UPDATE_ALL);
        }
        level.playSound(null, pos,
                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("stardewcraft:ship")),
                SoundSource.BLOCKS, 0.9F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected final InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof SveTimedMachineBlockEntity machine
                && com.stardew.craft.blockentity.UtilityDropHelper.tryHarvest(
                        level, pos, player, machine::isReady, machine::harvestOne, 1)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected final void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            com.stardew.craft.blockentity.UtilityDropHelper.dropAutomationContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
