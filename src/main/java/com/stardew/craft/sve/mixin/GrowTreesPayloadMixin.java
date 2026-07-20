package com.stardew.craft.sve.mixin;

import com.stardew.craft.network.GrowTreesPayload;
import com.stardew.craft.sve.tree.SveFruitTreeBlock;
import com.stardew.craft.sve.tree.SveFruitTreeExtensionBlock;
import com.stardew.craft.sve.tree.SveFruitTreeGrowthManager;
import com.stardew.craft.sve.tree.SveFruitTreeSaplingBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeGrowthManager;
import com.stardew.craft.sve.tree.wild.SveWildTreeSaplingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(GrowTreesPayload.class)
public abstract class GrowTreesPayloadMixin {
    @Inject(method = "handle", at = @At("TAIL"), require = 1)
    private static void stardewcraftsve$growSveFruitTrees(
            GrowTreesPayload payload, IPayloadContext context, CallbackInfo callback) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!player.isCreative() || !player.hasPermissions(2)) {
                return;
            }
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            SveFruitTreeGrowthManager manager = SveFruitTreeGrowthManager.get(serverLevel);
            SveWildTreeGrowthManager wildManager = SveWildTreeGrowthManager.get(serverLevel);
            BlockPos playerPos = player.blockPosition();
            Set<BlockPos> advancedTrees = new HashSet<>();
            for (int x = -5; x <= 5; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.getBlock() instanceof SveFruitTreeSaplingBlock
                                && state.getValue(SveFruitTreeSaplingBlock.HALF) == DoubleBlockHalf.LOWER) {
                            manager.growOneDay(serverLevel, pos);
                        } else if (state.getBlock() instanceof SveFruitTreeBlock) {
                            if (advancedTrees.add(pos.immutable())) {
                                manager.growOneDay(serverLevel, pos);
                            }
                        } else if (state.getBlock() instanceof SveFruitTreeExtensionBlock extension) {
                            BlockPos root = SveFruitTreeBlock.findRoot(serverLevel, pos, extension.getType());
                            if (root != null && advancedTrees.add(root.immutable())) {
                                manager.growOneDay(serverLevel, root);
                            }
                        } else if (state.getBlock() instanceof SveWildTreeSaplingBlock) {
                            wildManager.growOneDay(serverLevel, pos);
                        } else if (state.getBlock() instanceof SveWildTreeBlock) {
                            advancedTrees.add(pos.immutable());
                        } else if (state.getBlock() instanceof SveWildTreeExtensionBlock extension) {
                            BlockPos root = SveWildTreeBlock.findRoot(serverLevel, pos, extension.getType());
                            if (root != null) {
                                advancedTrees.add(root.immutable());
                            }
                        }
                    }
                }
            }
        });
    }
}
