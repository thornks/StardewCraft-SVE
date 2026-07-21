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
    private static final Map<String, String> LOCATION_KEYS = Map.ofEntries(
            location("adventurer_summit"),
            location("blue_moon_vineyard"),
            location("crimson_badlands"),
            location("diamond_cavern"),
            location("fable_reef"),
            location("forbidden_maze"),
            location("forest_west"),
            location("highlands"),
            location("highlands_cavern"),
            location("joja_town_after_event"),
            location("junimo_woods"),
            location("morris_property"),
            location("shearwater_bridge"),
            location("sprite_spring"),
            tag("is_forest_river", "forest"),
            tag("is_freshwater", "freshwater"),
            tag("is_ginger_island_ocean", "ginger_island"),
            tag("is_mutant_bug_lair", "mutant_bug_lair"),
            tag("is_town_river", "town")
    );

    private static Map.Entry<String, String> location(String path) {
        return Map.entry("stardewcraftsve:" + path, "stardewcraftsve.jei.location." + path);
    }

    private static Map.Entry<String, String> tag(String path, String translationPath) {
        return Map.entry("#stardewcraft:" + path,
                "stardewcraftsve.jei.location." + translationPath);
    }

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
