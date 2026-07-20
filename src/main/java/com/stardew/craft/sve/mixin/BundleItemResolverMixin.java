package com.stardew.craft.sve.mixin;

import com.stardew.craft.communitycenter.data.BundleItemResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BundleItemResolver.class, remap = false)
public abstract class BundleItemResolverMixin {
    @Inject(method = "resolveItemStack", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveNamespacedItem(
            String itemId,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (itemId == null || !itemId.contains(":")) return;

        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        cir.setReturnValue(new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }
}
