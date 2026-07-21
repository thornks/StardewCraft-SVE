package com.stardew.craft.sve.mixin;

import com.stardew.craft.integration.jei.ArtisanJeiRecipe;
import com.stardew.craft.integration.jei.ArtisanJeiRecipeFactory;
import com.stardew.craft.integration.jei.MachineJeiRegistry;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.sve.StardewcraftsveMod;
import com.stardew.craft.sve.SveSeedMakerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppresses host JEI candidates that SVE handles separately or intentionally bans. */
@Mixin(value = ArtisanJeiRecipeFactory.class, remap = false)
public abstract class ArtisanJeiRecipeFactoryMixin {
    private static final ResourceLocation STARDEWCRAFTSVE$FISH_SMOKER =
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "fish_smoker");

    @Inject(
            method = "buildForItem(Lcom/stardew/craft/integration/jei/MachineJeiRegistry$Machine;"
                    + "Lcom/stardew/craft/item/artisan/ArtisanRecipeDataManager$Recipe;"
                    + "Lnet/minecraft/world/item/Item;)"
                    + "Lcom/stardew/craft/integration/jei/ArtisanJeiRecipe;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private static void stardewcraftsve$skipUnsupportedHostCandidate(
            MachineJeiRegistry.Machine machine,
            ArtisanRecipeDataManager.Recipe definition,
            Item input,
            CallbackInfoReturnable<ArtisanJeiRecipe> cir
    ) {
        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input);
        if (!StardewcraftsveMod.MODID.equals(inputId.getNamespace())) return;

        if (STARDEWCRAFTSVE$FISH_SMOKER.equals(machine.id())
                && definition.outputMode() == ArtisanRecipeDataManager.OutputMode.SMOKED) {
            // StardewCraft hardcodes smoked outputs to its own namespace. SVE registers
            // the correct quality-aware fish-smoker pages in StardewcraftsveJeiPlugin.
            cir.setReturnValue(null);
            return;
        }

        if (definition.outputMode() == ArtisanRecipeDataManager.OutputMode.SEEDMAKER
                && SveSeedMakerData.isBannedProduce(inputId.getPath())) {
            cir.setReturnValue(null);
        }
    }
}
