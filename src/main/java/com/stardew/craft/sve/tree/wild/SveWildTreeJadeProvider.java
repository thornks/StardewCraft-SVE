package com.stardew.craft.sve.tree.wild;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SveWildTreeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            "stardewcraftsve", "wild_tree_info");

    @Override public ResourceLocation getUid() { return UID; }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel serverLevel)) return;
        BlockState state = accessor.getBlockState();
        BlockPos pos = accessor.getPosition();
        if (state.getBlock() instanceof SveWildTreeSaplingBlock sapling) {
            SveWildTreeGrowthManager manager = SveWildTreeGrowthManager.get(serverLevel);
            tag.putBoolean("Sapling", true);
            tag.putString("Type", sapling.getType().id());
            tag.putInt("Stage", manager.getGrowthStage(serverLevel, pos));
            tag.putBoolean("Fertilized", manager.isFertilized(serverLevel, pos));
            tag.putBoolean("Blocked", manager.isBlocked(serverLevel, pos));
            return;
        }
        SveWildTreeBlockEntity tree = resolveTree(accessor, pos);
        if (tree != null) {
            tag.putBoolean("Mature", true);
            tag.putString("Type", tree.getTreeType().id());
            tag.putBoolean("HasSeed", tree.hasSeed());
            tag.putBoolean("Shaken", tree.wasShakenToday());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.getBoolean("Sapling")) {
            tooltip.add(Component.translatable("stardewcraftsve.tooltip.wild_tree_stage",
                    data.getInt("Stage") + 1, SveWildTreeGrowthManager.MATURE_STAGE + 1));
            if (data.getBoolean("Fertilized")) {
                tooltip.add(Component.translatable("stardewcraftsve.tooltip.wild_tree_fertilized")
                        .withStyle(ChatFormatting.GREEN));
            }
            if (data.getBoolean("Blocked")) {
                tooltip.add(Component.translatable("stardewcraftsve.tooltip.wild_tree_blocked")
                        .withStyle(ChatFormatting.RED));
            }
        } else if (data.getBoolean("Mature") && data.getBoolean("HasSeed") && !data.getBoolean("Shaken")) {
            tooltip.add(Component.translatable("stardewcraftsve.tooltip.wild_tree_seed")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static SveWildTreeBlockEntity resolveTree(BlockAccessor accessor, BlockPos pos) {
        if (accessor.getBlockEntity() instanceof SveWildTreeBlockEntity tree) return tree;
        BlockState state = accessor.getBlockState();
        SveWildTreeType type;
        if (state.getBlock() instanceof SveWildTreeBlock tree) type = tree.getType();
        else if (state.getBlock() instanceof SveWildTreeExtensionBlock extension) type = extension.getType();
        else return null;
        BlockPos root = SveWildTreeBlock.findRoot(accessor.getLevel(), pos, type);
        if (root == null) return null;
        BlockEntity blockEntity = accessor.getLevel().getBlockEntity(root);
        return blockEntity instanceof SveWildTreeBlockEntity tree ? tree : null;
    }
}
