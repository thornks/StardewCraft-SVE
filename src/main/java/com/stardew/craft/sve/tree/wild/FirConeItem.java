package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.item.SimpleStardewItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A fir seed which can be planted directly on valid wild-tree ground. */
public final class FirConeItem extends SimpleStardewItem {
    public FirConeItem(Properties properties) {
        super("stardewcraft.type.seed", 5, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return SveWildTreePlanting.tryPlant(context, SveWildTreeType.FIR);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide()) return;

        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("Quality")) {
            tag.remove("Quality");
            if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
            else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null && modelData.value() >= 1 && modelData.value() <= 3) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }
}
