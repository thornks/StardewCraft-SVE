package com.stardew.craft.sve.mixin;

import com.google.gson.JsonElement;
import com.stardew.craft.communitycenter.data.BundleDataManager;
import com.stardew.craft.sve.SveCommunityBundles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = BundleDataManager.ReloadListener.class, remap = false)
public abstract class BundleDataReloadMixin {
    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void stardewcraftsve$applyBundles(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        SveCommunityBundles.apply();
    }
}
