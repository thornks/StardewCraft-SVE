package com.stardew.craft.sve;

import com.stardew.craft.item.cooking.CookingDishItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** The nausea is an on-consume consequence, not a displayed Stardew food buff. */
public final class VoidMayoSandwichItem extends CookingDishItem {
    static final int NAUSEA_DURATION_TICKS = 10 * 20;

    public VoidMayoSandwichItem(Properties properties) {
        super(100, 1, List.of(), properties, false);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide()) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION_TICKS));
        }
        return result;
    }
}
