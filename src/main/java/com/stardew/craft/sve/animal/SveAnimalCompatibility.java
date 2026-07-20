package com.stardew.craft.sve.animal;

import com.mojang.logging.LogUtils;
import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.AnimalAcquisitionSource;
import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.animal.model.AnimalTypeCatalog;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.animal.service.AnimalEntitySyncService;
import com.stardew.craft.animal.service.AnimalShopService;
import com.stardew.craft.api.v1.agriculture.StardewAgricultureDataApi;
import com.stardew.craft.api.v1.agriculture.StardewAnimalData;
import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/** Runtime bridge between SVE animal definitions and StardewCraft's internal animal services. */
public final class SveAnimalCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private SveAnimalCompatibility() {
    }

    public static Map<String, AnimalShopService.ShopAnimalRule> appendShopRules(
            Map<String, AnimalShopService.ShopAnimalRule> base
    ) {
        LinkedHashMap<String, AnimalShopService.ShopAnimalRule> rules = new LinkedHashMap<>(base);
        for (SveAnimalRules.Definition definition : SveAnimalRules.definitions()) {
            rules.put(definition.id(), shopRule(definition));
        }
        return Map.copyOf(rules);
    }

    public static List<String> appendShopOrder(List<String> base) {
        ArrayList<String> order = new ArrayList<>(base);
        for (SveAnimalRules.Definition definition : SveAnimalRules.definitions()) {
            if (!order.contains(definition.id())) order.add(definition.id());
        }
        return List.copyOf(order);
    }

    public static AnimalShopService.ShopAnimalRule shopRule(SveAnimalRules.Definition definition) {
        return new AnimalShopService.ShopAnimalRule(
                definition.id(),
                definition.buildingFamily(),
                definition.requiredBuildingTier(),
                definition.purchasePrice(),
                definition.displayName(),
                definition.descriptionKey(),
                definition.lockReasonKey()
        );
    }

    @Nullable
    public static AnimalTypeCatalog.AnimalTypeSpec typeSpec(String animalTypeId) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        return definition == null ? null : new AnimalTypeCatalog.AnimalTypeSpec(
                definition.id(), definition.buildingFamily(), definition.daysToMature());
    }

    public static Set<String> appendKnownTypeIds(Set<String> base) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(base);
        SveAnimalRules.definitions().stream().map(SveAnimalRules.Definition::id).forEach(ids::add);
        return Set.copyOf(ids);
    }

    @Nullable
    public static EntityType<? extends BaseCoopAnimalEntity> entityType(String animalTypeId) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        if (definition == null) return null;
        return switch (definition.id()) {
            case SveAnimalRules.GOOSE_ID -> SveAnimalEntities.GOOSE.get();
            case SveAnimalRules.CAMEL_ID -> SveAnimalEntities.CAMEL.get();
            default -> null;
        };
    }

    public static int variantIndex(String animalTypeId, int fallback) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        return definition == null ? fallback : definition.variantIndex();
    }

    @Nullable
    public static String animalTypeForIncubatorInput(ItemStack stack) {
        return stack.isEmpty() ? null : animalTypeForIncubatorInput(
                BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Nullable
    public static String animalTypeForIncubatorInput(ResourceLocation itemId) {
        if (itemId == null) return null;
        for (SveAnimalRules.Definition definition : SveAnimalRules.definitions()) {
            if (definition.incubationItemPath() == null) continue;
            ResourceLocation input = ResourceLocation.fromNamespaceAndPath(
                    StardewcraftsveMod.MODID, definition.incubationItemPath());
            if (input.equals(itemId)) return definition.id();
        }
        return null;
    }

    @Nullable
    public static AnimalBuildingRecord findContainingIncubatorBuilding(
            ServerLevel level,
            BlockPos incubatorPos,
            ItemStack input
    ) {
        String animalTypeId = animalTypeForIncubatorInput(input);
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        if (definition == null) return null;

        String dimensionId = level.dimension().location().toString();
        return AnimalWorldData.get(level).getBuildings().stream()
                .filter(AnimalBuildingRecord::active)
                .filter(building -> dimensionId.equals(building.dimensionId()))
                .filter(building -> definition.buildingFamily()
                        .equalsIgnoreCase(building.buildingType().family()))
                .filter(building -> building.isInBounds(incubatorPos))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static FarmAnimalRecord incubate(
            ServerLevel level,
            String animalTypeId,
            String customName,
            String buildingId
    ) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        if (definition == null || definition.incubationItemPath() == null) return null;

        AnimalWorldData worldData = AnimalWorldData.get(level);
        AnimalBuildingRecord building = worldData.getBuilding(buildingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown animal building: " + buildingId));
        validateBuilding(animalTypeId, building);
        if (!building.hasCapacity()) {
            throw new IllegalStateException("Animal building is full: " + buildingId);
        }

        String resolvedName = customName == null || customName.isBlank()
                ? definition.id()
                : customName;
        FarmAnimalRecord record = worldData.createAnimal(
                definition.id(),
                resolvedName,
                buildingId,
                AnimalAcquisitionSource.INCUBATION
        );

        try {
            AnimalEntitySyncService.ensurePresentNow(level, record);
        } catch (RuntimeException exception) {
            worldData.markChanged();
            LOGGER.warn(
                    "Created incubated SVE {} record {} but could not spawn its entity immediately; "
                            + "the managed-animal recovery pass will retry",
                    definition.id(),
                    record.animalId(),
                    exception
            );
        }
        return record;
    }

    public static boolean validateBuilding(String animalTypeId, AnimalBuildingRecord building) {
        SveAnimalRules.Definition definition = SveAnimalRules.definition(animalTypeId);
        if (definition == null) return false;

        String actualFamily = building.buildingType().family();
        if (!definition.buildingFamily().equals(actualFamily)) {
            throw new IllegalStateException(
                    "SVE animal " + definition.id() + " requires " + definition.buildingFamily()
                            + " but building " + building.buildingId() + " is " + actualFamily
            );
        }
        return true;
    }

    public static StardewAnimalData suppressBaseProduction(Entity entity) {
        StardewAnimalData resolved = StardewAgricultureDataApi.animal(entity);
        if (entity instanceof BaseCoopAnimalEntity animal) {
            return SveAnimalData.suppressBaseProduction(animal.getManagedAnimalType(), resolved);
        }
        return resolved;
    }

    public static boolean allowsRecordReproduction(FarmAnimalRecord record) {
        return SveAnimalRules.canReproduce(record.animalTypeId()) && record.allowReproduction();
    }

    @Nullable
    public static FarmAnimalRecord findRecord(Player player, long animalId) {
        return player.level() instanceof ServerLevel serverLevel
                ? AnimalWorldData.get(serverLevel).getAnimal(animalId).orElse(null)
                : null;
    }

    @Nullable
    public static String menuAnimalType(FarmAnimalRecord record, int variantIndex) {
        if (record != null && SveAnimalRules.isSveAnimal(record.animalTypeId())) {
            return SveAnimalRules.definition(record.animalTypeId()).id();
        }
        for (SveAnimalRules.Definition definition : SveAnimalRules.definitions()) {
            if (definition.variantIndex() == variantIndex) return definition.id();
        }
        return null;
    }

    public static boolean hasReproductionLocked(FarmAnimalRecord record, int variantIndex) {
        String animalTypeId = menuAnimalType(record, variantIndex);
        return animalTypeId != null && !SveAnimalRules.canReproduce(animalTypeId);
    }

    public static boolean enforceReproductionLock(
            Player player,
            long animalId,
            FarmAnimalRecord record,
            int variantIndex
    ) {
        if (!hasReproductionLocked(record, variantIndex)) return false;
        if (record != null && record.allowReproduction()
                && player.level() instanceof ServerLevel serverLevel) {
            AnimalWorldData.get(serverLevel).setAllowReproduction(animalId, false);
        }
        return true;
    }

    public static OptionalInt managedAnimalCount(
            ServerLevel level,
            String family,
            BlockPos managerPos
    ) {
        AnimalWorldData worldData = AnimalWorldData.get(level);
        AnimalBuildingRecord building = worldData.findBuildingByManagerAnyOwner(
                level.dimension().location().toString(), family, managerPos).orElse(null);
        if (building == null) return OptionalInt.empty();

        long recordCount = worldData.getAnimals().stream()
                .filter(record -> building.buildingId().equals(record.buildingId()))
                .count();
        return OptionalInt.of(Math.max(building.memberAnimalIds().size(), (int) recordCount));
    }
}
