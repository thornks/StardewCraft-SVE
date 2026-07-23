package com.stardew.craft.sve;

import com.stardew.craft.combat.WeaponStats;
import com.stardew.craft.item.weapon.WeaponData;
import com.stardew.craft.item.weapon.WeaponRegistry;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Regression checks for the six SVE 1.15.11 weapon definitions and legacy stack bridge. */
public final class SveWeaponRegressionTest {
    private static final Path ITEM_SOURCE = Path.of("src/main/java/com/stardew/craft/sve/ModItems.java");
    private static final Path MOD_SOURCE = Path.of("src/main/java/com/stardew/craft/sve/StardewcraftsveMod.java");
    private static final Path WEAPON_ITEM_SOURCE = Path.of("src/main/java/com/stardew/craft/sve/SveWeaponItems.java");
    private static final Path SHIELD_HANDLER_SOURCE = Path.of("src/main/java/com/stardew/craft/sve/SveHeavyShieldHandler.java");

    private SveWeaponRegressionTest() {}

    public static void main(String[] args) throws IOException {
        Map<String, SveWeaponData.Definition> definitions = definitions();
        validateGoldenStats(definitions);
        validateDisplayTypes(definitions);
        validateRuntimeAttackSpeeds(definitions);
        validateSkillInheritance(definitions);
        validateStackBridge(definitions);
        validateItemTypes();
        System.out.println("SVE weapon regression suite passed: 6 weapons, original stats and stack bridge");
    }

    private static void validateDisplayTypes(Map<String, SveWeaponData.Definition> definitions) {
        Map<String, String> expected = Map.of(
                "diamond_wand", "stardewcraft.type.weapon.wand",
                "heavy_shield", "stardewcraft.type.weapon.shield",
                "monster_splitter", "stardewcraft.type.weapon.great_sword",
                "tempered_galaxy_dagger", "stardewcraft.type.weapon.dagger",
                "tempered_galaxy_hammer", "stardewcraft.type.weapon.club",
                "tempered_galaxy_sword", "stardewcraft.type.weapon.sword");
        for (SveWeaponData.Definition definition : definitions.values()) {
            expectEquals(expected.get(definition.path()), definition.displayTypeKey(),
                    definition.path() + " display type");
        }
    }

    private static void validateRuntimeAttackSpeeds(Map<String, SveWeaponData.Definition> definitions) {
        expectFloat(0.5F, SveWeaponData.runtimeAttacksPerSecond(definitions.get("heavy_shield")),
                "heavy shield runtime attack speed floor");
        expectFloat(0.5F, SveWeaponData.runtimeAttacksPerSecond(definitions.get("monster_splitter")),
                "monster splitter runtime attack speed floor");
        expectFloat(2.2F, SveWeaponData.runtimeAttacksPerSecond(definitions.get("diamond_wand")),
                "positive sword speed remains unchanged");
        expectFloat(3.5F, SveWeaponData.runtimeAttacksPerSecond(definitions.get("tempered_galaxy_dagger")),
                "positive dagger speed remains unchanged");
        expectFloat(0.85F, SveWeaponData.runtimeAttacksPerSecond(definitions.get("tempered_galaxy_hammer")),
                "positive club speed remains unchanged");
        expectFloat(2.1F, SveWeaponData.runtimeAttacksPerSecond(definitions.get("tempered_galaxy_sword")),
                "positive sword speed remains unchanged");
    }

    private static void validateSkillInheritance(Map<String, SveWeaponData.Definition> definitions) {
        for (SveWeaponData.Definition definition : definitions.values()) {
            WeaponData actual = SveWeaponData.weaponDataByPath(definition.path());
            if (definition.skillSource() == null) {
                expect(actual.getSkill1() == null && actual.getSkill2() == null,
                        definition.path() + " must not invent weapon skills");
                continue;
            }
            WeaponData source = WeaponRegistry.get(definition.skillSource());
            expect(source != null, definition.path() + " skill source exists");
            expectEquals(skillId(source.getSkill1()), skillId(actual.getSkill1()),
                    definition.path() + " primary skill inheritance");
            expectEquals(skillId(source.getSkill2()), skillId(actual.getSkill2()),
                    definition.path() + " secondary skill inheritance");
        }
    }

    private static String skillId(com.stardew.craft.item.weapon.WeaponSkillData skill) {
        return skill == null ? "" : skill.getId();
    }

    private static Map<String, SveWeaponData.Definition> definitions() {
        Map<String, SveWeaponData.Definition> definitions = new LinkedHashMap<>();
        for (SveWeaponData.Definition definition : SveWeaponData.all()) {
            expect(definitions.put(definition.path(), definition) == null,
                    "duplicate weapon definition " + definition.path());
        }
        expectEquals(6, definitions.size(), "SVE weapon count");
        return definitions;
    }

    private static void validateGoldenStats(Map<String, SveWeaponData.Definition> definitions) {
        Map<String, String> expected = Map.ofEntries(
                stat("diamond_wand", "SWORD/1-2/31.0/6/800.0/0/1.0/1.0/null"),
                stat("heavy_shield", "SWORD/24-48/1.0/-80/100.0/35/0.0/0.0/null"),
                stat("monster_splitter", "CLUB/850-1000/2.0/-28/100.0/0/0.0/1.5/null"),
                stat("tempered_galaxy_dagger", "DAGGER/50-70/1.0/10/100.0/15/0.3/3.0/galaxy_dagger"),
                stat("tempered_galaxy_hammer", "CLUB/90-110/1.0/1/100.0/20/0.0/3.0/galaxy_hammer"),
                stat("tempered_galaxy_sword", "SWORD/80-120/1.0/5/100.0/10/0.15/3.0/galaxy_sword"));
        expectEquals(expected.keySet(), definitions.keySet(), "golden weapon set");
        for (SveWeaponData.Definition definition : definitions.values()) {
            String actual = definition.type() + "/" + definition.minDamage() + "-" + definition.maxDamage()
                    + "/" + definition.knockback() + "/" + definition.speed() + "/" + definition.precision()
                    + "/" + definition.defense() + "/" + definition.critChance() + "/"
                    + definition.critMultiplier() + "/" + definition.skillSource();
            expectEquals(expected.get(definition.path()), actual, definition.path() + " source stats");
        }
    }

    private static void validateStackBridge(Map<String, SveWeaponData.Definition> definitions) {
        for (SveWeaponData.Definition definition : definitions.values()) {
            WeaponStats stats = SveWeaponData.baseStats(definition);
            expectEquals(definition.type(), stats.getWeaponType(), definition.path() + " stack type");
            expectFloat(definition.minDamage(), stats.getMinDamage(), definition.path() + " stack min damage");
            expectFloat(definition.maxDamage(), stats.getMaxDamage(), definition.path() + " stack max damage");
            expectFloat(definition.knockback(), stats.getKnockback(), definition.path() + " stack knockback");
            expectFloat(definition.precision(), stats.getPrecision(), definition.path() + " stack precision");
            expectEquals(definition.speed(), stats.getSpeed(), definition.path() + " stack speed");
            expectEquals(definition.defense(), stats.getDefense(), definition.path() + " stack defense");
            expectFloat(definition.critChance(), stats.getCritChance(), definition.path() + " stack crit chance");
            expectFloat(SveWeaponData.critPowerBonus(definition),
                    stats.getBonusCritPower(), definition.path() + " stack crit power");

            CompoundTag root = new CompoundTag();
            CompoundTag legacyWeapon = new CompoundTag();
            legacyWeapon.putFloat(WeaponStats.TAG_PRECISION, 0.0F);
            root.put(WeaponStats.TAG_STARDEW_WEAPON, legacyWeapon);
            expect(SveWeaponData.patchExistingStackStats(root, definition),
                    definition.path() + " legacy stack recognized");
            expectFloat(definition.precision(), root.getCompound(WeaponStats.TAG_STARDEW_WEAPON)
                    .getFloat(WeaponStats.TAG_PRECISION), definition.path() + " migrated precision");
            expectFloat(definition.knockback(), root.getCompound(WeaponStats.TAG_STARDEW_WEAPON)
                    .getFloat(WeaponStats.TAG_KNOCKBACK), definition.path() + " migrated knockback");
            expectFloat(SveWeaponData.critPowerBonus(definition), root.getCompound(WeaponStats.TAG_STARDEW_WEAPON)
                    .getFloat(WeaponStats.TAG_CRIT_POWER), definition.path() + " migrated crit power");
        }
        expectFloat(-200.0F, SveWeaponData.critPowerBonus(definitions.get("diamond_wand")),
                "diamond wand final 1x crit conversion");
        expectFloat(0.0F, SveWeaponData.critPowerBonus(definitions.get("tempered_galaxy_sword")),
                "tempered galaxy final 3x crit conversion");
    }

    private static void validateItemTypes() throws IOException {
        String source = Files.readString(ITEM_SOURCE);
        expect(source.contains("new SveSwordItem(\"diamond_wand\""), "diamond wand sword behavior");
        expect(source.contains("new SveShieldItem(\"heavy_shield\""), "heavy shield hybrid behavior");
        expect(source.contains("new SveClubItem(\"monster_splitter\""), "monster splitter club behavior");
        expect(source.contains("new SveDaggerItem(\"tempered_galaxy_dagger\""), "tempered dagger behavior");
        expect(source.contains("new SveClubItem(\"tempered_galaxy_hammer\""), "tempered hammer behavior");
        expect(source.contains("new SveSwordItem(\"tempered_galaxy_sword\""), "tempered sword behavior");

        String weaponItems = Files.readString(WEAPON_ITEM_SOURCE);
        expect(weaponItems.contains("return UseAnim.BLOCK;"), "heavy shield block animation");
        expect(weaponItems.contains("player.startUsingItem(hand);"), "heavy shield starts active use");
        expect(weaponItems.contains("DEFAULT_SHIELD_ACTIONS.contains(ability)"), "heavy shield native item ability");

        String shieldHandler = Files.readString(SHIELD_HANDLER_SOURCE);
        expect(shieldHandler.contains("player.isDamageSourceBlocked(event.getSource())"),
                "heavy shield native direction and damage-tag rules");
        expect(shieldHandler.contains("event.setAmount(0.0F);"),
                "heavy shield blocks before StardewCraft health conversion");
        expect(shieldHandler.contains("attacker.canDisableShield()"), "heavy shield can be disabled natively");

        String modSource = Files.readString(MOD_SOURCE);
        expect(modSource.contains("SveHeavyShieldHandler.register();"), "heavy shield compatibility is registered");
    }

    private static Map.Entry<String, String> stat(String path, String value) {
        return Map.entry(path, value);
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectFloat(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001F) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
