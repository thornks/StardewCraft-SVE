package com.stardew.craft.sve.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(targets = "com.stardew.craft.communitycenter.state.CommunityCenterSavedData$PlayerProgress", remap = false)
public interface CommunityCenterPlayerProgressAccessor {
    @Accessor("bundleSlots")
    Map<Integer, boolean[]> stardewcraftsve$getBundleSlots();
}
