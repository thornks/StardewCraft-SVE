package com.stardew.craft.sve.animal;

import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SveAnimalEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StardewcraftsveMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GooseEntity>> GOOSE = ENTITY_TYPES.register(
            SveAnimalRules.GOOSE_ID,
            () -> EntityType.Builder.<GooseEntity>of(GooseEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.9F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(SveAnimalRules.GOOSE_ID)
    );

    public static final DeferredHolder<EntityType<?>, EntityType<CamelEntity>> CAMEL = ENTITY_TYPES.register(
            SveAnimalRules.CAMEL_ID,
            () -> EntityType.Builder.<CamelEntity>of(CamelEntity::new, MobCategory.CREATURE)
                    .sized(1.1F, 1.8F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(SveAnimalRules.CAMEL_ID)
    );

    private SveAnimalEntities() {
    }

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(GOOSE.get(), BaseCoopAnimalEntity.createAttributes().build());
        event.put(CAMEL.get(), BaseCoopAnimalEntity.createAttributes().build());
    }
}
