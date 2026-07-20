package com.stardew.craft.sve.animal;

import com.stardew.craft.entity.animal.CoopAnimalVariant;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class GooseEntity extends SveFarmAnimalEntity {
    private static final Ingredient BREED_INGREDIENT = Ingredient.of(
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.MELON_SEEDS
    );
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EAT = RawAnimation.begin().thenLoop("eat");
    private static final RawAnimation SLEEP = RawAnimation.begin()
            .thenPlay("sleep_start")
            .thenLoop("sleep");

    public GooseEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public CoopAnimalVariant getVariant() {
        return CoopAnimalVariant.DUCK;
    }

    @Override
    protected Ingredient getBreedIngredient() {
        return BREED_INGREDIENT;
    }

    @Override
    protected EntityType<? extends Animal> getOffspringType() {
        return SveAnimalEntities.GOOSE.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (isSveSleeping()) {
                state.setAndContinue(SLEEP);
                return PlayState.CONTINUE;
            }
            if (isSveEating()) {
                state.setAndContinue(EAT);
                return PlayState.CONTINUE;
            }
            if (state.isMoving()) {
                state.setAndContinue(WALK);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }));
    }
}
