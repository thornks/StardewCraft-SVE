package com.stardew.craft.sve;

import com.stardew.craft.farming.SeasonLocationRules;
import com.stardew.craft.item.IStardewItem;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.function.Supplier;

/**
 * Generic seed item that plants a crop block when used on farmland.
 * Mirrors the pattern from stardewcraft's ParsnipSeedItem/AmaranthSeedItem.
 */
public class SveSeedItem extends Item implements IStardewItem {

    private final Supplier<Block> cropBlock;
    private final int[] allowedSeasons;
    private final int sellPrice;

    public SveSeedItem(Properties properties, Supplier<Block> cropBlock, int[] allowedSeasons, int sellPrice) {
        super(properties);
        this.cropBlock = cropBlock;
        this.allowedSeasons = allowedSeasons;
        this.sellPrice = sellPrice;
    }

    @Override
    public String getItemTypeKey() {
        return "stardewcraft.type.seed";
    }

    @Override
    public int getSellPrice(ItemStack stack) {
        return sellPrice;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(pos);

        if (!isFarmland(clickedState)) {
            return InteractionResult.PASS;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (!aboveState.isAir()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int season = StardewTimeManager.get().getCurrentSeason();
        if (!SeasonLocationRules.isPlantingSeasonAllowed(level, pos, season, allowedSeasons)) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("stardewcraft.message.seed.wrong_season"), true);
            }
            return InteractionResult.FAIL;
        }

        Block block = cropBlock.get();
        if (block == null) return InteractionResult.FAIL;

        level.setBlock(abovePos, block.defaultBlockState(), 3);
        level.playSound(null, abovePos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        context.getItemInHand().shrink(1);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isFarmland(BlockState state) {
        return state.getBlock() instanceof FarmBlock;
    }
}
