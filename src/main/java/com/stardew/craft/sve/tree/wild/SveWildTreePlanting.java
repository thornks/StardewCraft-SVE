package com.stardew.craft.sve.tree.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SveWildTreePlanting {
    public static InteractionResult tryPlant(UseOnContext context, SveWildTreeType type) {
        Level level = context.getLevel();
        BlockPos groundPos = context.getClickedPos();
        BlockState ground = level.getBlockState(groundPos);
        if (!isPlantableGround(ground)) return InteractionResult.PASS;

        BlockPos placePos = groundPos.above();
        BlockState sapling = type.saplingBlock().defaultBlockState();
        if (!level.getBlockState(placePos).canBeReplaced() || !sapling.canSurvive(level, placePos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(placePos, sapling, Block.UPDATE_ALL);
            level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.9F, 1.0F);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static boolean isPlantableGround(BlockState ground) {
        return !(ground.getBlock() instanceof FarmBlock)
                && (ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK));
    }

    private SveWildTreePlanting() {
    }
}
