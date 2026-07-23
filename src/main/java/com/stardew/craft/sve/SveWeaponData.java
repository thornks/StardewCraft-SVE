package com.stardew.craft.sve;

import com.stardew.craft.StardewCraft;
import com.stardew.craft.api.v1.equipment.StardewEquipmentData;
import com.stardew.craft.api.v1.equipment.StardewEquipmentDataApi;
import com.stardew.craft.combat.WeaponStats;
import com.stardew.craft.combat.WeaponType;
import com.stardew.craft.item.weapon.WeaponData;
import com.stardew.craft.item.weapon.WeaponRegistry;
import com.stardew.craft.item.weapon.WeaponSkillData;
import com.stardew.craft.sve.mixin.WeaponRegistryAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Single source of truth for the six weapons defined by SVE 1.15.11. */
public final class SveWeaponData {
    private static final float MIN_RUNTIME_ATTACKS_PER_SECOND = 0.5F;
    private static final Map<String, Definition> DEFINITIONS = createDefinitions();
    private static final Map<String, WeaponData> WEAPONS = createWeaponData();

    private SveWeaponData() {}

    public static void register() {
        WEAPONS.values().forEach(WeaponRegistryAccessor::stardewcraftsve$register);
        StardewEquipmentDataApi.registerProvider(id("weapon_data"), 100, SveWeaponData::resolve);
    }

    static List<Definition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    static Definition byPath(String path) {
        Definition definition = DEFINITIONS.get(path);
        if (definition == null) throw new IllegalArgumentException("Unknown SVE weapon: " + path);
        return definition;
    }

    static WeaponData weaponDataByPath(String path) {
        WeaponData data = WEAPONS.get(path);
        if (data == null) throw new IllegalArgumentException("Unknown SVE weapon: " + path);
        return data;
    }

    /**
     * StardewCraft's legacy weapon items persist stats before consulting the public provider.
     * Seed new stacks from the audited definition and refresh existing base stats without touching forge data.
     */
    static void ensureStackStats(ItemStack stack, String path) {
        Definition definition = byPath(path);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag root = customData.copyTag();
            if (root.contains(WeaponStats.TAG_STARDEW_WEAPON)) {
                if (patchExistingStackStats(root, definition)) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                }
                return;
            }
        }

        baseStats(definition)
                .writeToItemStack(stack);
    }

    static WeaponStats baseStats(Definition definition) {
        return WeaponStats.builder()
                .weaponType(definition.type())
                .minDamage(definition.minDamage())
                .maxDamage(definition.maxDamage())
                .critChance(definition.critChance())
                .bonusCritPower(critPowerBonus(definition))
                .speed(definition.speed())
                .defense(definition.defense())
                .precision(definition.precision())
                .knockback(definition.knockback())
                .build();
    }

    /**
     * Stardew's extreme negative speed values can become zero APS under StardewCraft's linear conversion.
     * Keep the source stat intact for tooltips and stack data while ensuring Minecraft can finish its
     * attack cooldown and first-person equip animation.
     */
    static ItemAttributeModifiers runtimeAttributes(String path, ItemAttributeModifiers inherited) {
        Definition definition = byPath(path);
        float convertedAttacksPerSecond = convertedAttacksPerSecond(definition);
        if (convertedAttacksPerSecond >= MIN_RUNTIME_ATTACKS_PER_SECOND) return inherited;

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                StardewCraft.MODID, "weapon." + path + ".attack_speed");
        return inherited.withModifierAdded(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(modifierId, MIN_RUNTIME_ATTACKS_PER_SECOND - 4.0F,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    static float runtimeAttacksPerSecond(Definition definition) {
        return Math.max(MIN_RUNTIME_ATTACKS_PER_SECOND, convertedAttacksPerSecond(definition));
    }

    private static float convertedAttacksPerSecond(Definition definition) {
        return definition.type().getAttackSpeed() + definition.speed() * 0.1F;
    }

    static boolean patchExistingStackStats(CompoundTag root, Definition definition) {
        if (!root.contains(WeaponStats.TAG_STARDEW_WEAPON)) return false;
        CompoundTag weapon = root.getCompound(WeaponStats.TAG_STARDEW_WEAPON);
        CompoundTag updated = weapon.copy();
        updated.putInt(WeaponStats.TAG_WEAPON_TYPE, definition.type().getId());
        updated.putFloat(WeaponStats.TAG_MIN_DAMAGE, definition.minDamage());
        updated.putFloat(WeaponStats.TAG_MAX_DAMAGE, definition.maxDamage());
        updated.putFloat(WeaponStats.TAG_CRIT_CHANCE, definition.critChance());
        updated.putFloat(WeaponStats.TAG_CRIT_POWER, critPowerBonus(definition));
        updated.putInt(WeaponStats.TAG_SPEED, definition.speed());
        updated.putInt(WeaponStats.TAG_DEFENSE, definition.defense());
        updated.putFloat(WeaponStats.TAG_PRECISION, definition.precision());
        updated.putFloat(WeaponStats.TAG_KNOCKBACK, definition.knockback());
        if (updated.equals(weapon)) return false;
        root.put(WeaponStats.TAG_STARDEW_WEAPON, updated);
        return true;
    }

    static float critPowerBonus(Definition definition) {
        if (definition.critChance() <= 0.0F) return 0.0F;
        return (definition.critMultiplier() - 3.0F) * 100.0F;
    }

    private static StardewEquipmentData resolve(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!StardewcraftsveMod.MODID.equals(itemId.getNamespace())) return null;

        Definition definition = DEFINITIONS.get(itemId.getPath());
        if (definition == null) return null;

        WeaponData weaponData = WEAPONS.get(definition.path());
        StardewEquipmentData.Weapon weapon = new StardewEquipmentData.Weapon(
                definition.type().getName(),
                definition.minDamage(),
                definition.maxDamage(),
                definition.critChance(),
                definition.speed(),
                definition.defense(),
                definition.precision(),
                definition.knockback(),
                skillId(weaponData.getSkill1()),
                skillId(weaponData.getSkill2()));
        return new StardewEquipmentData(
                ResourceLocation.fromNamespaceAndPath("stardewcraft", "weapon"),
                0,
                0,
                0,
                0.0F,
                critPowerBonus(definition),
                0,
                0.0F,
                0.0F,
                0,
                List.of(),
                Optional.of(weapon));
    }

    private static Optional<ResourceLocation> skillId(WeaponSkillData skill) {
        if (skill == null || skill.getId() == null || skill.getId().isBlank()) return Optional.empty();
        ResourceLocation result = skill.getId().indexOf(':') >= 0
                ? ResourceLocation.tryParse(skill.getId())
                : ResourceLocation.tryBuild("stardewcraft", skill.getId());
        return Optional.ofNullable(result);
    }

    private static Map<String, Definition> createDefinitions() {
        Map<String, Definition> definitions = new LinkedHashMap<>();
        add(definitions, new Definition("diamond_wand", WeaponType.SWORD,
                "stardewcraft.type.weapon.wand", 6,
                1, 2, 31.0F, 6, 800.0F, 0, 1.0F, 1.0F, null));
        add(definitions, new Definition("heavy_shield", WeaponType.SWORD,
                "stardewcraft.type.weapon.shield", 12,
                24, 48, 1.0F, -80, 100.0F, 35, 0.0F, 0.0F, null));
        add(definitions, new Definition("monster_splitter", WeaponType.CLUB,
                "stardewcraft.type.weapon.great_sword", 20,
                850, 1000, 2.0F, -28, 100.0F, 0, 0.0F, 1.5F, null));
        add(definitions, new Definition("tempered_galaxy_dagger", WeaponType.DAGGER,
                "stardewcraft.type.weapon.dagger", 17,
                50, 70, 1.0F, 10, 100.0F, 15, 0.3F, 3.0F, "galaxy_dagger"));
        add(definitions, new Definition("tempered_galaxy_hammer", WeaponType.CLUB,
                "stardewcraft.type.weapon.club", 18,
                90, 110, 1.0F, 1, 100.0F, 20, 0.0F, 3.0F, "galaxy_hammer"));
        add(definitions, new Definition("tempered_galaxy_sword", WeaponType.SWORD,
                "stardewcraft.type.weapon.sword", 18,
                80, 120, 1.0F, 5, 100.0F, 10, 0.15F, 3.0F, "galaxy_sword"));
        return Map.copyOf(definitions);
    }

    private static Map<String, WeaponData> createWeaponData() {
        Map<String, WeaponData> weapons = new LinkedHashMap<>();
        for (Definition definition : DEFINITIONS.values()) {
            WeaponData.Builder builder = WeaponData.builder(definition.path())
                    .type(definition.type())
                    .level(definition.level())
                    .damage(definition.minDamage(), definition.maxDamage())
                    .weight(definition.knockback())
                    .speed(definition.speed())
                    .defense(definition.defense())
                    .critChance(definition.critChance())
                    .critPower(definition.critMultiplier());
            inheritSkills(builder, definition.skillSource());
            weapons.put(definition.path(), builder.build());
        }
        return Map.copyOf(weapons);
    }

    private static void inheritSkills(WeaponData.Builder builder, String sourceId) {
        if (sourceId == null) return;
        WeaponData source = WeaponRegistry.get(sourceId);
        if (source == null) {
            throw new IllegalStateException("Missing StardewCraft weapon skill source: " + sourceId);
        }
        if (source.getSkill1() != null) builder.skill1(source.getSkill1());
        if (source.getSkill2() != null) builder.skill2(source.getSkill2());
    }

    private static void add(Map<String, Definition> definitions, Definition definition) {
        if (definitions.put(definition.path(), definition) != null) {
            throw new IllegalStateException("Duplicate SVE weapon definition: " + definition.path());
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }

    record Definition(
            String path,
            WeaponType type,
            String displayTypeKey,
            int level,
            int minDamage,
            int maxDamage,
            float knockback,
            int speed,
            float precision,
            int defense,
            float critChance,
            float critMultiplier,
            String skillSource
    ) {}
}
