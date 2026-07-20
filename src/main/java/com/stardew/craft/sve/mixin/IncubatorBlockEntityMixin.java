package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.blockentity.IncubatorBlockEntity;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IncubatorBlockEntity.class, remap = false)
public abstract class IncubatorBlockEntityMixin {
    private static final ResourceLocation STARDEWCRAFTSVE$GOOSE_EGG =
            ResourceLocation.fromNamespaceAndPath("stardewcraftsve", "goose_egg");

    @Inject(method = "getContainingAnimalBuilding", at = @At("HEAD"), cancellable = true, require = 1)
    private void stardewcraftsve$preferContainingCoopForGooseEgg(
            ServerLevel level,
            CallbackInfoReturnable<AnimalBuildingRecord> cir
    ) {
        IncubatorBlockEntity incubator = (IncubatorBlockEntity) (Object) this;
        if (!SveAnimalRules.GOOSE_ID.equals(IncubatorBlockEntity.resolveAnimalTypeId(incubator.getInput()))) {
            return;
        }

        BlockPos incubatorPos = incubator.getBlockPos();
        String dimensionId = level.dimension().location().toString();
        AnimalBuildingRecord coop = AnimalWorldData.get(level).getBuildings().stream()
                .filter(AnimalBuildingRecord::active)
                .filter(building -> dimensionId.equals(building.dimensionId()))
                .filter(building -> "coop".equalsIgnoreCase(building.buildingType().family()))
                .filter(building -> building.isInBounds(incubatorPos))
                .findFirst()
                .orElse(null);
        cir.setReturnValue(coop);
    }

    @Inject(method = "resolveAnimalTypeId", at = @At("HEAD"), cancellable = true, require = 1)
    private static void stardewcraftsve$resolveGooseEgg(
            ItemStack stack,
            CallbackInfoReturnable<String> cir
    ) {
        if (!stack.isEmpty()
                && STARDEWCRAFTSVE$GOOSE_EGG.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            cir.setReturnValue(SveAnimalRules.GOOSE_ID);
        }
    }
}
