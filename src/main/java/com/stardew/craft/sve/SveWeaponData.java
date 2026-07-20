package com.stardew.craft.sve;

import com.stardew.craft.api.v1.equipment.StardewEquipmentData;
import com.stardew.craft.api.v1.equipment.StardewEquipmentDataApi;
import com.stardew.craft.combat.WeaponType;
import com.stardew.craft.item.weapon.WeaponData;
import com.stardew.craft.item.weapon.WeaponRegistry;
import com.stardew.craft.sve.mixin.WeaponRegistryAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Single source of truth for SVE weapon metadata. */
public final class SveWeaponData {
    private static final Map<String, WeaponData> WEAPONS = createDefinitions();

    private SveWeaponData() {}

    public static void register() {
        WEAPONS.values().forEach(WeaponRegistryAccessor::stardewcraftsve$register);
        StardewEquipmentDataApi.registerProvider(id("weapon_data"), 100, SveWeaponData::resolve);
    }

    private static StardewEquipmentData resolve(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!StardewcraftsveMod.MODID.equals(itemId.getNamespace())) return null;

        WeaponData data = WEAPONS.get(itemId.getPath());
        if (data == null) return null;

        StardewEquipmentData.Weapon weapon = new StardewEquipmentData.Weapon(
                data.getWeaponType().getName(),
                data.getDamageMin(),
                data.getDamageMax(),
                (float) data.getCritChance(),
                data.getSpeed(),
                data.getDefense(),
                0.0F,
                (float) data.getWeight(),
                Optional.empty(),
                Optional.empty());
        return new StardewEquipmentData(
                ResourceLocation.fromNamespaceAndPath("stardewcraft", "weapon"),
                0,
                0,
                0,
                0.0F,
                (float) Math.max(0.0D, (data.getCritPower() - 1.0D) * 100.0D),
                0,
                0.0F,
                0.0F,
                0,
                List.of(),
                Optional.of(weapon));
    }

    private static Map<String, WeaponData> createDefinitions() {
        Map<String, WeaponData> definitions = new LinkedHashMap<>();
        add(definitions, WeaponData.builder("diamond_wand")
                .type(WeaponType.SWORD)
                .level(6)
                .damage(1, 2)
                .speed(6)
                .defense(0)
                .critChance(1.0D)
                .critPower(1.0D)
                .build());
        add(definitions, WeaponData.builder("heavy_shield")
                .type(WeaponType.SWORD)
                .level(12)
                .damage(24, 48)
                .speed(-80)
                .defense(35)
                .critChance(0.0D)
                .critPower(0.0D)
                .build());
        add(definitions, WeaponData.builder("monster_splitter")
                .type(WeaponType.CLUB)
                .level(20)
                .damage(850, 1000)
                .speed(-28)
                .defense(0)
                .critChance(0.0D)
                .critPower(1.5D)
                .build());
        add(definitions, temperedGalaxy(
                WeaponData.builder("tempered_galaxy_dagger")
                        .type(WeaponType.DAGGER)
                        .level(17)
                        .damage(50, 70)
                        .speed(10)
                        .defense(15)
                        .critChance(0.3D)
                        .critPower(3.0D),
                "galaxy_dagger"));
        add(definitions, temperedGalaxy(
                WeaponData.builder("tempered_galaxy_hammer")
                        .type(WeaponType.CLUB)
                        .level(18)
                        .damage(90, 110)
                        .speed(1)
                        .defense(20)
                        .critChance(0.0D)
                        .critPower(3.0D),
                "galaxy_hammer"));
        add(definitions, temperedGalaxy(
                WeaponData.builder("tempered_galaxy_sword")
                        .type(WeaponType.SWORD)
                        .level(18)
                        .damage(80, 120)
                        .speed(5)
                        .defense(10)
                        .critChance(0.15D)
                        .critPower(3.0D),
                "galaxy_sword"));
        return Map.copyOf(definitions);
    }

    private static WeaponData temperedGalaxy(WeaponData.Builder builder, String sourceId) {
        WeaponData source = WeaponRegistry.get(sourceId);
        if (source != null) {
            if (source.getSkill1() != null) builder.skill1(source.getSkill1());
            if (source.getSkill2() != null) builder.skill2(source.getSkill2());
        }
        return builder.build();
    }

    private static void add(Map<String, WeaponData> definitions, WeaponData data) {
        definitions.put(data.getId(), data);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }
}
