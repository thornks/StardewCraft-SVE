package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class YarnSpoolerBlock extends SveTimedMachineBlock {
    public YarnSpoolerBlock(BlockBehaviour.Properties properties) {
        super(properties, Block.box(0.5, 0, 0.5, 15.5, 16, 15.5));
    }

    @Override
    protected SveTimedMachineBlockEntity createMachineBlockEntity(BlockPos pos, BlockState state) {
        return new YarnSpoolerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends SveTimedMachineBlockEntity> machineBlockEntityType() {
        return ModBlockEntities.YARN_SPOOLER.get();
    }
}
