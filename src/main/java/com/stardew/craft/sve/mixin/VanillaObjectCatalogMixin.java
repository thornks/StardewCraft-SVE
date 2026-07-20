package com.stardew.craft.sve.mixin;

import com.stardew.craft.data.VanillaObjectCatalog;
import com.stardew.craft.sve.collection.SveCollectionCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = VanillaObjectCatalog.class, remap = false)
public abstract class VanillaObjectCatalogMixin {
    @Inject(method = "entriesForCollection", at = @At("RETURN"), cancellable = true, require = 1)
    private static void stardewcraftsve$appendCollectionEntries(
            int collectionTab,
            CallbackInfoReturnable<List<VanillaObjectCatalog.Entry>> cir
    ) {
        List<VanillaObjectCatalog.Entry> additions = SveCollectionCatalog.entriesForTab(collectionTab);
        if (additions.isEmpty()) {
            return;
        }

        List<VanillaObjectCatalog.Entry> combined = new ArrayList<>(cir.getReturnValue().size() + additions.size());
        combined.addAll(cir.getReturnValue());
        combined.addAll(additions);
        cir.setReturnValue(List.copyOf(combined));
    }

    @Inject(method = "stackFor", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveCollectionStack(
            VanillaObjectCatalog.Entry entry,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!SveCollectionCatalog.isSveEntry(entry)) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(entry.key());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        cir.setReturnValue(new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    @Inject(method = "matchesItemId", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$matchNamespacedCollectionItem(
            VanillaObjectCatalog.Entry entry,
            String itemId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!SveCollectionCatalog.isSveEntry(entry)) {
            return;
        }

        ResourceLocation expected = ResourceLocation.tryParse(entry.key());
        ResourceLocation actual = ResourceLocation.tryParse(itemId);
        cir.setReturnValue(expected != null && expected.equals(actual));
    }
}
