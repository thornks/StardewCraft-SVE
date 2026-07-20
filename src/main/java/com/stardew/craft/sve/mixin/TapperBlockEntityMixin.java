package com.stardew.craft.sve.mixin;

import com.stardew.craft.block.utility.TapperBlock;
import com.stardew.craft.blockentity.TapperBlockEntity;
import com.stardew.craft.blockentity.TimedProductionBlockEntity;
import com.stardew.craft.sve.tree.wild.SveWildTreeCompat;
import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import com.stardew.craft.tree.WildTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TapperBlockEntity.class)
public abstract class TapperBlockEntityMixin extends TimedProductionBlockEntity {
    @Shadow private String treeId;

    protected TapperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "startCycleIfEmpty(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$startFirWaxCycle(String requestedTreeId, CallbackInfo callback) {
        if (level == null || level.isClientSide() || !product.isEmpty()) {
            return;
        }
        WildTrees.Def support = TapperBlock.findValidProductionDef(level, worldPosition, getBlockState());
        if (support == null || SveWildTreeCompat.byId(support.id()) == null) return;
        SveWildTreeType type = SveWildTreeType.byId(support.id());
        treeId = type.id();
        product = new ItemStack(type.tapperProduct());
        readyAtAbsMinute = (getCurrentDayIndex() + type.tapperDays()) * EFFECTIVE_MINUTES_PER_DAY;
        ready = false;
        setChanged();
        syncToClient();
        callback.cancel();
    }
}
