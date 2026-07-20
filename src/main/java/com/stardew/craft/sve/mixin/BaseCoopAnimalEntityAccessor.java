package com.stardew.craft.sve.mixin;

import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseCoopAnimalEntity.class)
public interface BaseCoopAnimalEntityAccessor {
    @Accessor("eatAnimationTicks")
    int stardewcraftsve$getEatAnimationTicks();
}
