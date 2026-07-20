package com.stardew.craft.sve.animal;

import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.agriculture.StardewAnimalData;
import com.stardew.craft.api.v1.agriculture.StardewBuildingData;
import com.stardew.craft.data.StardewDataMaps;
import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class SveAnimalData {
    private static final ResourceLocation COOP = ResourceLocation.fromNamespaceAndPath("stardewcraft", "coop");
    private static final ResourceLocation BARN = ResourceLocation.fromNamespaceAndPath("stardewcraft", "barn");
    private static final ResourceLocation GOOSE_ENTITY =
            ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, SveAnimalRules.GOOSE_ID);
    private static final ResourceLocation CAMEL_ENTITY =
            ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, SveAnimalRules.CAMEL_ID);

    private static final StardewAnimalData GOOSE = new StardewAnimalData(
            COOP,
            definition(SveAnimalRules.GOOSE_ID).purchasePrice(),
            definition(SveAnimalRules.GOOSE_ID).daysToMature(),
            ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, definition(SveAnimalRules.GOOSE_ID).produceItemPath()),
            definition(SveAnimalRules.GOOSE_ID).produceIntervalDays()
    );

    private static final StardewAnimalData CAMEL = new StardewAnimalData(
            BARN,
            definition(SveAnimalRules.CAMEL_ID).purchasePrice(),
            definition(SveAnimalRules.CAMEL_ID).daysToMature(),
            ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, definition(SveAnimalRules.CAMEL_ID).produceItemPath()),
            definition(SveAnimalRules.CAMEL_ID).produceIntervalDays()
    );

    private SveAnimalData() {
    }

    public static void register() {
        StardewAgricultureDataApi.registerAnimalProvider(
                ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "farm_animals"),
                100,
                entity -> {
                    if (entity.getType() == SveAnimalEntities.GOOSE.get()) {
                        return GOOSE;
                    }
                    if (entity.getType() == SveAnimalEntities.CAMEL.get()) {
                        return CAMEL;
                    }
                    return null;
                }
        );

        StardewAgricultureDataApi.registerBuildingProvider(
                ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "farm_animal_buildings"),
                100,
                (level, pos, state) -> extendAcceptedAnimals(
                        state.getBlock().builtInRegistryHolder().getData(StardewDataMaps.BUILDING_DATA)
                )
        );
    }

    private static StardewBuildingData extendAcceptedAnimals(StardewBuildingData base) {
        if (base == null) {
            return null;
        }

        ResourceLocation addition;
        if (COOP.equals(base.type())) {
            addition = GOOSE_ENTITY;
        } else if (BARN.equals(base.type())) {
            addition = CAMEL_ENTITY;
        } else {
            return null;
        }

        if (base.acceptedAnimals().contains(addition)) {
            return base;
        }

        List<ResourceLocation> acceptedAnimals = new ArrayList<>(base.acceptedAnimals());
        acceptedAnimals.add(addition);
        return new StardewBuildingData(
                base.type(),
                base.capacity(),
                List.copyOf(acceptedAnimals),
                base.requiredInteriorBlocks()
        );
    }

    public static StardewAnimalData suppressBaseProduction(String animalTypeId, StardewAnimalData resolved) {
        StardewAnimalData data = forType(animalTypeId);
        if (data == null) return resolved;
        return new StardewAnimalData(
                data.buildingType(),
                data.purchasePrice(),
                data.daysToMature(),
                data.produce(),
                Integer.MAX_VALUE
        );
    }

    public static StardewAnimalData forType(String animalTypeId) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        if (definition == null) return null;
        return switch (definition.id()) {
            case SveAnimalRules.GOOSE_ID -> GOOSE;
            case SveAnimalRules.CAMEL_ID -> CAMEL;
            default -> null;
        };
    }

    private static SveAnimalRules.Definition definition(String animalTypeId) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        if (definition == null) throw new IllegalStateException("Missing SVE animal definition: " + animalTypeId);
        return definition;
    }
}
