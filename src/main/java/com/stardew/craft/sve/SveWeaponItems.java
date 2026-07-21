package com.stardew.craft.sve;

import com.stardew.craft.item.weapon.StardewClubItem;
import com.stardew.craft.item.weapon.StardewDaggerItem;
import com.stardew.craft.item.weapon.StardewWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Keeps legacy weapon stack data aligned with the public SVE definitions. */
final class SveSwordItem extends StardewWeaponItem {
    private final String path;

    SveSwordItem(String path, Item.Properties properties) {
        super(path, properties);
        this.path = path;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        SveWeaponData.ensureStackStats(stack, path);
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SveWeaponData.ensureStackStats(stack, path);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

final class SveDaggerItem extends StardewDaggerItem {
    private final String path;

    SveDaggerItem(String path, Item.Properties properties) {
        super(path, properties);
        this.path = path;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        SveWeaponData.ensureStackStats(stack, path);
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SveWeaponData.ensureStackStats(stack, path);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}

final class SveClubItem extends StardewClubItem {
    private final String path;

    SveClubItem(String path, Item.Properties properties) {
        super(path, properties);
        this.path = path;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        SveWeaponData.ensureStackStats(stack, path);
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SveWeaponData.ensureStackStats(stack, path);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
