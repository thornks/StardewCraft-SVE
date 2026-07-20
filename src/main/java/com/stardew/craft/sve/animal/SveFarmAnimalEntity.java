package com.stardew.craft.sve.animal;

import com.stardew.craft.animal.data.AnimalWorldData;
import com.stardew.craft.animal.model.AnimalBuildingRecord;
import com.stardew.craft.animal.model.FarmAnimalRecord;
import com.stardew.craft.entity.animal.BaseCoopAnimalEntity;
import com.stardew.craft.sve.mixin.BaseCoopAnimalEntityAccessor;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public abstract class SveFarmAnimalEntity extends BaseCoopAnimalEntity {
    private static final int SLEEP_TIME_MINUTES = 20 * 60;
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(SveFarmAnimalEntity.class, EntityDataSerializers.BOOLEAN);

    protected SveFarmAnimalEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLEEPING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            setSveSleeping(resolveSleeping(serverLevel));
        }
    }

    public final boolean isSveSleeping() {
        return entityData.get(DATA_SLEEPING);
    }

    public final boolean isSveEating() {
        return ((BaseCoopAnimalEntityAccessor) this).stardewcraftsve$getEatAnimationTicks() > 0;
    }

    private void setSveSleeping(boolean sleeping) {
        if (entityData.get(DATA_SLEEPING) != sleeping) {
            entityData.set(DATA_SLEEPING, sleeping);
        }
    }

    private boolean resolveSleeping(ServerLevel level) {
        if (StardewTimeManager.get().getCurrentTime() < SLEEP_TIME_MINUTES || getManagedAnimalId() <= 0L) {
            return false;
        }

        AnimalWorldData worldData = AnimalWorldData.get(level);
        FarmAnimalRecord record = worldData.getAnimal(getManagedAnimalId()).orElse(null);
        if (record == null) {
            return false;
        }
        AnimalBuildingRecord building = worldData.getBuilding(record.buildingId()).orElse(null);
        return building != null && building.isInBounds(blockPosition());
    }
}
