package com.stardew.craft.sve;

import com.stardew.craft.item.weapon.StardewClubItem;
import com.stardew.craft.item.weapon.StardewDaggerItem;
import com.stardew.craft.item.weapon.StardewWeaponItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/** Keeps legacy weapon stack data aligned with the public SVE definitions. */
class SveSwordItem extends StardewWeaponItem {
    private final String path;

    SveSwordItem(String path, Item.Properties properties) {
        super(path, properties);
        this.path = path;
    }

    @Override
    public String getItemTypeKey() {
        return SveWeaponData.byPath(path).displayTypeKey();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return SveWeaponData.runtimeAttributes(path, super.getDefaultAttributeModifiers());
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

final class SveShieldItem extends SveSwordItem {
    private static final int USE_DURATION_TICKS = 72_000;

    SveShieldItem(String path, Item.Properties properties) {
        super(path, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(ability) || super.canPerformAction(stack, ability);
    }
}

final class SveDaggerItem extends StardewDaggerItem {
    private final String path;

    SveDaggerItem(String path, Item.Properties properties) {
        super(path, properties);
        this.path = path;
    }

    @Override
    public String getItemTypeKey() {
        return SveWeaponData.byPath(path).displayTypeKey();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return SveWeaponData.runtimeAttributes(path, super.getDefaultAttributeModifiers());
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
    public String getItemTypeKey() {
        return SveWeaponData.byPath(path).displayTypeKey();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return SveWeaponData.runtimeAttributes(path, super.getDefaultAttributeModifiers());
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
