package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ButterChurnerBlock extends SveTimedMachineBlock {
    public ButterChurnerBlock(BlockBehaviour.Properties properties) {
        super(properties, Block.box(1, 0, 1, 15, 16, 15));
    }

    @Override
    protected SveTimedMachineBlockEntity createMachineBlockEntity(BlockPos pos, BlockState state) {
        return new ButterChurnerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends SveTimedMachineBlockEntity> machineBlockEntityType() {
        return ModBlockEntities.BUTTER_CHURNER.get();
    }
}
