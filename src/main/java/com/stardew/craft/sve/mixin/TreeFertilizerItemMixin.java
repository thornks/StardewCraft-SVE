package com.stardew.craft.sve.mixin;

import com.stardew.craft.item.TreeFertilizerItem;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeGrowthManager;
import com.stardew.craft.sve.tree.wild.SveWildTreeSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeFertilizerItem.class)
public abstract class TreeFertilizerItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$fertilizeFirSapling(
            UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SveWildTreeSaplingBlock) {
            if (level.isClientSide()) {
                callback.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
            ServerLevel serverLevel = (ServerLevel) level;
            SveWildTreeGrowthManager manager = SveWildTreeGrowthManager.get(serverLevel);
            boolean fertilized = manager.fertilize(serverLevel, pos);
            if (fertilized) {
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10, 0.3, 0.3, 0.3, 0.0);
            } else if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("stardewcraft.tree_fertilizer.already"), true);
            }
            callback.setReturnValue(InteractionResult.CONSUME);
            return;
        }
        if (state.getBlock() instanceof SveWildTreeBlock
                || state.getBlock() instanceof SveWildTreeExtensionBlock) {
            if (!level.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("stardewcraft.tree_fertilizer.mature"), true);
            }
            callback.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
