package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ButterChurnerBlockEntity extends SveTimedMachineBlockEntity {
    public ButterChurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUTTER_CHURNER.get(), pos, state, "butter_churner", 60);
    }
}
