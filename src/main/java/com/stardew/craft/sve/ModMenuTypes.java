package com.stardew.craft.sve;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, StardewcraftsveMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ForageSelectionMenu>> FORAGE_SELECTION =
        MENUS.register("forage_selection", () -> new MenuType<>(ForageSelectionMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenuTypes() {}
}
