package com.stardew.craft.sve.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SveFruitTreeExtensionBlock extends Block {
    private final SveFruitTreeType type;

    public SveFruitTreeExtensionBlock(SveFruitTreeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public SveFruitTreeType getType() {
        return type;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level instanceof LevelReader reader
                ? SveFruitTreeBlock.extensionShape(reader, pos, type)
                : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level instanceof LevelReader reader
                ? SveFruitTreeBlock.extensionShape(reader, pos, type)
                : Shapes.empty();
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level instanceof LevelReader reader) {
            BlockPos root = SveFruitTreeBlock.findRoot(reader, pos, type);
            if (root != null) {
                return reader.getBlockState(root).getDestroyProgress(player, level, root);
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockPos root = SveFruitTreeBlock.findRoot(level, pos, type);
        if (root == null) {
            return InteractionResult.PASS;
        }
        BlockState rootState = level.getBlockState(root);
        if (rootState.getBlock() instanceof SveFruitTreeBlock treeBlock) {
            return treeBlock.useRootWithoutItem(rootState, level, root, player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos root = SveFruitTreeBlock.findRoot(level, pos, type);
            if (root != null) {
                SveFruitTreeBlock.fellTree(level, root, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos root = SveFruitTreeBlock.findRoot(level, pos, type);
        if (root != null) {
            return level.getBlockState(root).getBlock().asItem().getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }
}
