package com.stardew.craft.sve.mixin;

import com.stardew.craft.item.weapon.WeaponData;
import com.stardew.craft.item.weapon.WeaponRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Typed bridge for the 0.5.1 weapon constructor, which still reads WeaponRegistry. */
@Mixin(value = WeaponRegistry.class, remap = false)
public interface WeaponRegistryAccessor {
    @Invoker("register")
    static void stardewcraftsve$register(WeaponData data) {
        throw new AssertionError("Mixin was not applied");
    }
}
