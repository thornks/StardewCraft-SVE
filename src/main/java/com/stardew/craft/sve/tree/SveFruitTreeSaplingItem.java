package com.stardew.craft.sve.tree;

import com.stardew.craft.item.StardewBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class SveFruitTreeSaplingItem extends StardewBlockItem {
    private final SveFruitTreeType type;
    private final Supplier<? extends Block> saplingBlock;

    public SveFruitTreeSaplingItem(SveFruitTreeType type,
                                   Supplier<? extends Block> saplingBlock,
                                   Properties properties) {
        super(saplingBlock.get(),
                "stardewcraft.type.seed",
                type.saplingSellPrice(),
                properties);
        this.type = type;
        this.saplingBlock = saplingBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos placePos = level.getBlockState(clicked).canBeReplaced()
                ? clicked
                : clicked.relative(context.getClickedFace());

        if (!SveFruitTreeRules.canPlantSapling(level, placePos)) {
            return InteractionResult.PASS;
        }

        BlockState saplingState = saplingBlock.get().defaultBlockState();
        if (!saplingState.canSurvive(level, placePos)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            level.setBlock(placePos, saplingState, Block.UPDATE_ALL);
            level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.9F, 1.0F);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                ItemStack stack = context.getItemInHand();
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public SveFruitTreeType getFruitTreeType() {
        return type;
    }
}
