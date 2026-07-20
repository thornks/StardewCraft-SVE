package com.stardew.craft.sve;

import com.mojang.brigadier.context.CommandContext;
import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.animal.model.AnimalTypeCatalog;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.animal.service.AnimalEntitySyncService;
import com.stardew.craft.animal.service.AnimalShopService;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.sve.animal.CamelEntity;
import com.stardew.craft.sve.animal.GooseEntity;
import com.stardew.craft.sve.animal.SveAnimalEntities;
import com.stardew.craft.sve.animal.SveAnimalRules;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read-only validation of SVE animal registrations and the current animal save data. */
public final class SveAnimalAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/animal-audit");
    private static final int MAX_REPORTED_ISSUES = 20;

    private SveAnimalAudit() {
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        if (level == null) {
            source.sendFailure(Component.literal("Stardew Valley dimension is not loaded"));
            return 0;
        }

        Audit audit = new Audit();
        validateRegistrations(audit);
        validateWorld(level, audit);

        ChatFormatting summaryColor = audit.errors == 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal(
                "SVE animal audit: geese=" + audit.geese
                        + ", camels=" + audit.camels
                        + ", buildings=" + audit.buildings
                        + ", errors=" + audit.errors
                        + ", warnings=" + audit.warnings
        ).withStyle(summaryColor), false);

        int shown = Math.min(MAX_REPORTED_ISSUES, audit.issues.size());
        for (int index = 0; index < shown; index++) {
            Issue issue = audit.issues.get(index);
            source.sendSystemMessage(Component.literal(issue.message)
                    .withStyle(issue.error ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (audit.issues.size() > shown) {
            source.sendSystemMessage(Component.literal(
                    "... " + (audit.issues.size() - shown) + " more issue(s); see server log"
            ).withStyle(ChatFormatting.GRAY));
        }

        for (Issue issue : audit.issues) {
            if (issue.error) {
                LOGGER.error(issue.message);
            } else {
                LOGGER.warn(issue.message);
            }
        }
        return audit.errors == 0 ? 1 : 0;
    }

    private static void validateRegistrations(Audit audit) {
        validateAnimalRegistration(audit, SveAnimalRules.GOOSE_ID, SveAnimalRules.GOOSE_PURCHASE_PRICE,
                "coop", SveAnimalRules.GOOSE_DAYS_TO_MATURE);
        validateAnimalRegistration(audit, SveAnimalRules.CAMEL_ID, SveAnimalRules.CAMEL_PURCHASE_PRICE,
                "barn", SveAnimalRules.CAMEL_DAYS_TO_MATURE);

        validateEntityType(audit, SveAnimalRules.GOOSE_ID, SveAnimalEntities.GOOSE.get());
        validateEntityType(audit, SveAnimalRules.CAMEL_ID, SveAnimalEntities.CAMEL.get());

        if (SveAnimalRules.sellPrice(SveAnimalRules.GOOSE_ID, 1_000) != 15_600) {
            audit.error("Goose full-friendship sale price is not 15600g");
        }
        if (SveAnimalRules.sellPrice(SveAnimalRules.CAMEL_ID, 1_000) != 37_200) {
            audit.error("Camel full-friendship sale price is not 37200g");
        }
        if (SveAnimalRules.canReproduce(SveAnimalRules.CAMEL_ID)) {
            audit.error("Camel reproduction rule is enabled");
        }
    }

    private static void validateAnimalRegistration(
            Audit audit,
            String animalTypeId,
            int expectedPrice,
            String expectedFamily,
            int expectedDaysToMature
    ) {
        AnimalShopService.ShopAnimalRule shopRule = AnimalShopService.getRule(animalTypeId);
        if (shopRule == null) {
            audit.error("Missing animal shop rule for " + animalTypeId);
        } else {
            if (shopRule.price() != expectedPrice) {
                audit.error(animalTypeId + " shop price is " + shopRule.price() + "g, expected " + expectedPrice + "g");
            }
            if (!expectedFamily.equals(shopRule.family()) || shopRule.requiredTier() != 3) {
                audit.error(animalTypeId + " shop building rule is " + shopRule.family() + " tier "
                        + shopRule.requiredTier() + ", expected " + expectedFamily + " tier 3");
            }
        }

        AnimalTypeCatalog.AnimalTypeSpec type = AnimalTypeCatalog.resolve(animalTypeId);
        if (!expectedFamily.equals(type.family()) || type.daysToMature() != expectedDaysToMature) {
            audit.error(animalTypeId + " type rule is " + type.family() + "/" + type.daysToMature()
                    + " days, expected " + expectedFamily + "/" + expectedDaysToMature + " days");
        }
    }

    private static void validateEntityType(Audit audit, String path, Object entityType) {
        ResourceLocation expected = ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
        ResourceLocation actual = BuiltInRegistries.ENTITY_TYPE.getKey(
                (net.minecraft.world.entity.EntityType<?>) entityType);
        if (!expected.equals(actual)) {
            audit.error("Entity type for " + path + " is " + actual + ", expected " + expected);
        }
    }

    private static void validateWorld(ServerLevel level, Audit audit) {
        AnimalWorldData worldData = AnimalWorldData.get(level);
        Map<Long, FarmAnimalRecord> records = new HashMap<>();
        for (FarmAnimalRecord record : worldData.getAnimals()) {
            records.put(record.animalId(), record);
            if (!SveAnimalRules.isSveAnimal(record.animalTypeId())) continue;

            if (SveAnimalRules.GOOSE_ID.equals(record.animalTypeId())) {
                audit.geese++;
            } else {
                audit.camels++;
            }
            validateRecord(level, worldData, record, audit);
        }

        for (AnimalBuildingRecord building : worldData.getBuildings()) {
            audit.buildings++;
            if (building.memberAnimalIds().size() > building.capacity()) {
                audit.error("Building " + building.buildingId() + " has " + building.memberAnimalIds().size()
                        + " members but capacity is " + building.capacity());
            }
            for (long memberId : building.memberAnimalIds()) {
                FarmAnimalRecord record = records.get(memberId);
                if (record == null) {
                    audit.error("Building " + building.buildingId() + " contains missing animal record #" + memberId);
                } else if (!building.buildingId().equals(record.buildingId())) {
                    audit.error("Animal #" + memberId + " points to building " + record.buildingId()
                            + " but is listed in " + building.buildingId());
                }
            }
        }
    }

    private static void validateRecord(
            ServerLevel level,
            AnimalWorldData worldData,
            FarmAnimalRecord record,
            Audit audit
    ) {
        String label = record.animalTypeId() + " #" + record.animalId();
        AnimalBuildingRecord building = worldData.getBuildingIncludingInactive(record.buildingId()).orElse(null);
        if (building == null) {
            audit.error(label + " references missing building " + record.buildingId());
            return;
        }

        String expectedFamily = SveAnimalRules.requiredBuildingFamily(record.animalTypeId());
        if (!expectedFamily.equals(building.buildingType().family())) {
            audit.error(label + " is in " + building.buildingType().family() + " building " + building.buildingId()
                    + ", expected " + expectedFamily);
        }
        if (!building.memberAnimalIds().contains(record.animalId())) {
            audit.error(label + " is absent from building " + building.buildingId() + " member set");
        }
        if (record.daysToMature() != SveAnimalRules.daysToMature(record.animalTypeId())) {
            audit.warning(label + " stores " + record.daysToMature() + " maturity days; current rule is "
                    + SveAnimalRules.daysToMature(record.animalTypeId()));
        }
        if (record.ageDays() < 0 || record.daysSinceLastProduce() < 0) {
            audit.error(label + " contains a negative age or production timer");
        }
        if (SveAnimalRules.CAMEL_ID.equals(record.animalTypeId()) && record.allowReproduction()) {
            audit.error(label + " has a legacy reproduction flag enabled");
        }

        BaseCoopAnimalEntity entity = AnimalEntitySyncService.findLoaded(level, record.animalId());
        if (entity == null) {
            if (building.active() && level.isLoaded(building.managerPos())) {
                audit.warning(label + " has no loaded entity while its building manager chunk is loaded");
            }
            return;
        }

        if (entity.getManagedAnimalId() != record.animalId()
                || !record.animalTypeId().equals(entity.getManagedAnimalType())) {
            audit.error(label + " loaded entity has mismatched managed identity");
        }
        if (entity.isBaby() != record.isBaby()) {
            audit.error(label + " entity baby state does not match its saved record");
        }
        if (SveAnimalRules.GOOSE_ID.equals(record.animalTypeId()) && !(entity instanceof GooseEntity)) {
            audit.error(label + " loaded entity is " + entity.getClass().getSimpleName() + ", expected GooseEntity");
        }
        if (SveAnimalRules.CAMEL_ID.equals(record.animalTypeId()) && !(entity instanceof CamelEntity)) {
            audit.error(label + " loaded entity is " + entity.getClass().getSimpleName() + ", expected CamelEntity");
        }
    }

    private static final class Audit {
        private final List<Issue> issues = new ArrayList<>();
        private int errors;
        private int warnings;
        private int geese;
        private int camels;
        private int buildings;

        private void error(String message) {
            errors++;
            issues.add(new Issue(true, message));
        }

        private void warning(String message) {
            warnings++;
            issues.add(new Issue(false, message));
        }
    }

    private record Issue(boolean error, String message) {
    }
}
