package com.stardew.craft.sve.mixin;

import com.stardew.craft.blockentity.FishSmokerBlockEntity;
import com.stardew.craft.item.artisan.SmokedFishItem;
import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the host fish smoker resolve smoked variants in the SVE namespace. */
@Mixin(value = FishSmokerBlockEntity.class, remap = false)
public abstract class FishSmokerBlockEntityMixin {
    @Inject(method = "createSmokedOutput", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$createSmokedOutput(
            ItemStack input,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (!StardewcraftsveMod.MODID.equals(inputId.getNamespace())) return;

        ResourceLocation outputId = ResourceLocation.fromNamespaceAndPath(
                StardewcraftsveMod.MODID, "smoked_" + inputId.getPath());
        if (!BuiltInRegistries.ITEM.containsKey(outputId)) return;
        Item output = BuiltInRegistries.ITEM.get(outputId);
        if (output instanceof SmokedFishItem) {
            cir.setReturnValue(new ItemStack(output));
        }
    }
}
