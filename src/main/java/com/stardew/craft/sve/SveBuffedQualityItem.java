package com.stardew.craft.sve;

import com.stardew.craft.item.StardewQualityItem;
import com.stardew.craft.item.cooking.CookingDishItem.BuffType;
import com.stardew.craft.item.cooking.CookingDishItem.DishBuff;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** A quality-scaled forage item whose SVE buffs remain independent of item quality. */
public final class SveBuffedQualityItem extends StardewQualityItem {
    private final List<DishBuff> buffs;

    public SveBuffedQualityItem(
            String typeKey, int basePrice, int edibility, boolean supportsQuality,
            List<DishBuff> buffs, Properties properties, boolean drinkAnimation
    ) {
        super(typeKey, basePrice, edibility, supportsQuality, properties, drinkAnimation);
        this.buffs = List.copyOf(buffs);
    }

    public List<DishBuff> getBuffs() { return buffs; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
            buffs.forEach(buff -> SveConsumableData.applyBuff(player, buff));
        }
        return result;
    }

    @Override
    public List<Component> getAfterEatTooltipLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>(buffs.size());
        for (DishBuff buff : buffs) {
            MutableComponent title = Component.literal("[")
                    .append(Component.translatable(effectKey(buff.type())))
                    .append("]")
                    .withStyle(color(buff.type()));
            lines.add(Component.literal(icon(buff.type()) + " ")
                    .append(title)
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(valueKey(buff.type()), buff.amount())
                            .withStyle(ChatFormatting.GRAY)));
        }
        return List.copyOf(lines);
    }

    private static String icon(BuffType type) {
        return switch (type) {
            case MAX_ENERGY -> "\uE010";
            case FISHING -> "\uE011";
            case LUCK -> "\uE012";
            case SPEED -> "\uE013";
            case FARMING -> "\uE014";
            case FORAGING -> "\uE015";
            case MINING -> "\uE016";
            case ATTACK -> "\uE017";
            case DEFENSE -> "\uE018";
            case MAGNETIC_RADIUS -> "\uE019";
            case AVOID_MONSTERS -> "\uE01A";
        };
    }

    private static String effectKey(BuffType type) {
        return switch (type) {
            case MAX_ENERGY -> "effect.stardewcraft.vigorous";
            case FISHING -> "effect.stardewcraft.sea_king_blessing";
            case LUCK -> "effect.stardewcraft.spirit_blessing";
            case SPEED -> "effect.stardewcraft.speed";
            case FARMING -> "effect.stardewcraft.farmer_blessing";
            case FORAGING -> "effect.stardewcraft.forager_blessing";
            case MINING -> "effect.stardewcraft.miner_blessing";
            case ATTACK -> "effect.stardewcraft.warrior_blessing";
            case DEFENSE -> "effect.stardewcraft.guardian_blessing";
            case MAGNETIC_RADIUS -> "effect.stardewcraft.magnetism";
            case AVOID_MONSTERS -> "effect.stardewcraft.avoid_monsters";
        };
    }

    private static String valueKey(BuffType type) {
        return switch (type) {
            case MAX_ENERGY -> "stardewcraft.tooltip.buff.max_energy";
            case FISHING -> "stardewcraft.tooltip.buff.fishing_level";
            case LUCK -> "stardewcraft.tooltip.buff.luck";
            case SPEED -> "stardewcraft.tooltip.buff.speed";
            case FARMING -> "stardewcraft.tooltip.buff.farming_level";
            case FORAGING -> "stardewcraft.tooltip.buff.foraging_level";
            case MINING -> "stardewcraft.tooltip.buff.mining_level";
            case ATTACK -> "stardewcraft.tooltip.buff.attack";
            case DEFENSE -> "stardewcraft.tooltip.buff.defense";
            case MAGNETIC_RADIUS -> "stardewcraft.tooltip.buff.magnetic_radius";
            case AVOID_MONSTERS -> "stardewcraft.tooltip.buff.avoid_monsters";
        };
    }

    private static ChatFormatting color(BuffType type) {
        return switch (type) {
            case FISHING -> ChatFormatting.AQUA;
            case LUCK -> ChatFormatting.GOLD;
            case SPEED -> ChatFormatting.BLUE;
            case MAX_ENERGY, FARMING, FORAGING, MINING -> ChatFormatting.GREEN;
            case ATTACK -> ChatFormatting.RED;
            case DEFENSE -> ChatFormatting.DARK_AQUA;
            case MAGNETIC_RADIUS -> ChatFormatting.YELLOW;
            case AVOID_MONSTERS -> ChatFormatting.DARK_GREEN;
        };
    }
}
