package com.stardew.craft.sve.mixin;

import com.stardew.craft.blockentity.CaskBlockEntity;
import com.stardew.craft.item.artisan.ArtisanDrinkItem;
import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/** Extends the base cask's internal aging whitelist to SVE quality wines. */
@Mixin(value = CaskBlockEntity.class, remap = false)
public abstract class CaskBlockEntityMixin {
    @Redirect(
            method = {"tryInsert", "insertAutomation"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;",
                    remap = false
            ),
            require = 2
    )
    private Object stardewcraftsve$resolveAgingRate(Map<?, ?> agingRates, Object key) {
        Object configuredRate = agingRates.get(key);
        if (configuredRate != null || !(key instanceof Item item)) return configuredRate;
        if (!(item instanceof ArtisanDrinkItem drink) || !drink.supportsQuality()) return null;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return StardewcraftsveMod.MODID.equals(itemId.getNamespace()) ? 1.0F : null;
    }
}
