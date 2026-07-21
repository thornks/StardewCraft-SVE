package com.stardew.craft.sve;

import com.stardew.craft.effect.ModMobEffects;
import com.stardew.craft.item.cooking.CookingDishItem;
import com.stardew.craft.item.cooking.CookingDishItem.BuffType;
import com.stardew.craft.item.cooking.CookingDishItem.DishBuff;
import com.stardew.craft.player.PlayerStardewDataAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;

import java.util.List;

/** Authoritative non-recipe consumable rules sourced from SVE 1.15.11 Objects data. */
public final class SveConsumableData {
    public static final int MINECRAFT_TICKS_PER_SDV_DURATION_UNIT = 14;

    private static final List<Definition> ALL = List.of(
            consumable("aged_blue_moon_wine", 10_000, 100, true, 600, buff(BuffType.LUCK, 7)),
            consumable("blue_moon_wine", 700, 10, true, 180, buff(BuffType.LUCK, 2)),
            consumable("dewdrop_berry", 45, 90, false, 5_143,
                    buff(BuffType.LUCK, 2), buff(BuffType.MAX_ENERGY, 50),
                    buff(BuffType.MAGNETIC_RADIUS, 75), buff(BuffType.SPEED, 2)),
            consumable("green_mushroom", 1_250, 500, false, 1_600,
                    buff(BuffType.LUCK, 1), buff(BuffType.MAX_ENERGY, 200),
                    buff(BuffType.MAGNETIC_RADIUS, 25)),
            consumable("grampleton_orange_chicken", 400, 65, false, 720,
                    buff(BuffType.FARMING, 3)),
            consumable("seed_cookie", 35, 30, false, 600, buff(BuffType.MAX_ENERGY, 30)),
            consumable("aegis_elixir", 12_000, 1, false, 25, buff(BuffType.DEFENSE, 255)),
            consumable("armor_elixir", 2_000, 2, false, 400, buff(BuffType.DEFENSE, 15)),
            consumable("barbarian_elixir", 10_000, 1, false, 100, buff(BuffType.ATTACK, 99)),
            consumable("bombardier_elixir", 5_000, 50, true, 250, buff(BuffType.ATTACK, 50)),
            consumable("gravity_elixir", 1_500, 1, true, 5_143, buff(BuffType.MAGNETIC_RADIUS, 999)),
            consumable("haste_elixir", 2_000, 2, true, 400, buff(BuffType.SPEED, 3)),
            consumable("hero_elixir", 2_650, 2, false, 400, buff(BuffType.ATTACK, 20)),
            consumable("lightning_elixir", 5_000, 1, true, 88, buff(BuffType.SPEED, 8)),
            consumable("marsh_tonic", 750, 50, true, 550,
                    buff(BuffType.SPEED, 1), buff(BuffType.DEFENSE, 5), buff(BuffType.ATTACK, 10)),
            consumable("sports_drink", 300, 60, true, 1_000, buff(BuffType.MAX_ENERGY, 50)),
            consumable("stamina_capsule", 1_000, 4, false, 1_600,
                    buff(BuffType.MAX_ENERGY, 150), buff(BuffType.SPEED, 1)),
            consumable("super_joja_cola", 350, 150, true, 1_600,
                    buff(BuffType.LUCK, 2), buff(BuffType.MAGNETIC_RADIUS, 50), buff(BuffType.SPEED, 2))
    );

    private SveConsumableData() {}

    public static List<Definition> all() { return ALL; }

    public static Definition byPath(String path) {
        return ALL.stream().filter(definition -> definition.path().equals(path)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SVE consumable: " + path));
    }

    public static int durationTicks(int sveDuration) {
        if (sveDuration <= 0) throw new IllegalArgumentException("sveDuration");
        return Math.multiplyExact(sveDuration, MINECRAFT_TICKS_PER_SDV_DURATION_UNIT);
    }

    static void applyBuff(ServerPlayer player, DishBuff buff) {
        if (buff.amount() == 0 || buff.durationTicks() <= 0) return;
        switch (buff.type()) {
            case MAX_ENERGY -> PlayerStardewDataAPI.applyMaxEnergyBuff(player, buff.amount(), buff.durationTicks());
            case FISHING -> PlayerStardewDataAPI.applyFishingLevelBuff(player, buff.amount(), buff.durationTicks());
            case LUCK -> PlayerStardewDataAPI.applyLuckBuff(player, buff.amount(), buff.durationTicks());
            case SPEED -> player.addEffect(new MobEffectInstance(
                    ModMobEffects.SPEED, buff.durationTicks(), Math.max(0, buff.amount() - 1)));
            case FARMING -> PlayerStardewDataAPI.applyFarmingLevelBuff(player, buff.amount(), buff.durationTicks());
            case FORAGING -> PlayerStardewDataAPI.applyForagingLevelBuff(player, buff.amount(), buff.durationTicks());
            case MINING -> PlayerStardewDataAPI.applyMiningLevelBuff(player, buff.amount(), buff.durationTicks());
            case ATTACK -> PlayerStardewDataAPI.applyAttackBuff(player, buff.amount(), buff.durationTicks());
            case DEFENSE -> PlayerStardewDataAPI.applyDefenseBuff(player, buff.amount(), buff.durationTicks());
            case MAGNETIC_RADIUS -> PlayerStardewDataAPI.applyMagneticRadiusBuff(player, buff.amount(), buff.durationTicks());
            case AVOID_MONSTERS -> player.addEffect(new MobEffectInstance(
                    ModMobEffects.AVOID_MONSTERS, buff.durationTicks(), Math.max(0, buff.amount() - 1),
                    false, true, true));
        }
    }

    private static Definition consumable(
            String path, int price, int edibility, boolean drink, int sveDuration, SourceBuff... buffs
    ) {
        return new Definition(path, price, edibility, drink, sveDuration, List.of(buffs));
    }

    private static SourceBuff buff(BuffType type, int amount) { return new SourceBuff(type, amount); }

    public record SourceBuff(BuffType type, int amount) {
        public SourceBuff {
            if (type == null) throw new IllegalArgumentException("type");
            if (amount == 0) throw new IllegalArgumentException("amount");
        }

        public DishBuff toDishBuff(int sveDuration) {
            return new DishBuff(type, amount, durationTicks(sveDuration));
        }
    }

    public record Definition(
            String path, int price, int edibility, boolean drink, int sveDuration, List<SourceBuff> sourceBuffs
    ) {
        public Definition {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("path");
            if (price < 0) throw new IllegalArgumentException("price");
            if (sveDuration <= 0) throw new IllegalArgumentException("sveDuration");
            sourceBuffs = List.copyOf(sourceBuffs);
            if (sourceBuffs.isEmpty()) throw new IllegalArgumentException("sourceBuffs");
        }

        public List<DishBuff> buffs() {
            return sourceBuffs.stream().map(buff -> buff.toDishBuff(sveDuration)).toList();
        }

        public CookingDishItem createItem(Item.Properties properties) {
            return new CookingDishItem(price, edibility, buffs(), properties, drink);
        }

        public SveBuffedQualityItem createQualityItem(
                String typeKey, boolean supportsQuality, Item.Properties properties
        ) {
            return new SveBuffedQualityItem(
                    typeKey, price, edibility, supportsQuality, buffs(), properties, drink);
        }
    }
}
