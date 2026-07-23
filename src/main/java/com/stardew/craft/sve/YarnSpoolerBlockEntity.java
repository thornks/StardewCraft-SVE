package com.stardew.craft.sve;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class YarnSpoolerBlockEntity extends SveTimedMachineBlockEntity implements GeoBlockEntity {
    private static final RawAnimation WORKING_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.winding_machine.working");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public YarnSpoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.YARN_SPOOLER.get(), pos, state, "yarn_spooler", 120);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "working", 0, this::workingAnimation));
    }

    private PlayState workingAnimation(AnimationState<YarnSpoolerBlockEntity> state) {
        return isWorking() ? state.setAndContinue(WORKING_ANIMATION) : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
