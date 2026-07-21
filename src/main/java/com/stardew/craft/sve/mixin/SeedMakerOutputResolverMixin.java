package com.stardew.craft.sve.mixin;

import com.stardew.craft.item.artisan.SeedMakerOutputResolver;
import com.stardew.craft.sve.StardewcraftsveMod;
import com.stardew.craft.sve.SveSeedMakerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolves SVE's nonstandard produce-to-seed names for gameplay and JEI. */
@Mixin(value = SeedMakerOutputResolver.class, remap = false)
public abstract class SeedMakerOutputResolverMixin {
    @Inject(
            method = "resolve(Lnet/minecraft/world/item/Item;)Lnet/minecraft/world/item/Item;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$resolveSeed(
            Item input,
            CallbackInfoReturnable<Item> cir
    ) {
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input);
        if (!StardewcraftsveMod.MODID.equals(inputId.getNamespace())) return;

        String seedPath = SveSeedMakerData.seedPathForProduce(inputId.getPath());
        if (seedPath != null) {
            ResourceLocation seedId = ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, seedPath);
            cir.setReturnValue(BuiltInRegistries.ITEM.containsKey(seedId)
                    ? BuiltInRegistries.ITEM.get(seedId)
                    : null);
        } else if (SveSeedMakerData.isBannedProduce(inputId.getPath())) {
            cir.setReturnValue(null);
        }
    }
}
