package com.stardew.craft.sve.mixin;

import com.stardew.craft.integration.jei.FishingInfoCategory;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** Renders unavailable SVE fishing biomes with their original location names. */
@Mixin(value = FishingInfoCategory.class, remap = false)
public abstract class FishingInfoCategoryMixin {
    private static final Map<String, String> LOCATION_KEYS = Map.of(
            "stardewcraftsve:crimson_badlands", "stardewcraftsve.jei.location.crimson_badlands",
            "stardewcraftsve:highlands_cavern", "stardewcraftsve.jei.location.highlands_cavern"
    );

    @Inject(
            method = "location(Ljava/lang/String;)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$describeLocation(
            String raw,
            CallbackInfoReturnable<Component> cir
    ) {
        if (raw == null) return;

        String translationKey = LOCATION_KEYS.get(raw.toLowerCase(java.util.Locale.ROOT));
        if (translationKey != null) {
            cir.setReturnValue(Component.translatable(translationKey));
        }
    }
}
