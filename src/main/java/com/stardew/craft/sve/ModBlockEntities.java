package com.stardew.craft.sve;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.stardew.craft.sve.tree.SveFruitTreeBlockEntity;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StardewcraftsveMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ButterChurnerBlockEntity>> BUTTER_CHURNER =
            BLOCK_ENTITIES.register("butter_churner", () ->
                    BlockEntityType.Builder.of(ButterChurnerBlockEntity::new, ModBlocks.BUTTER_CHURNER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<YarnSpoolerBlockEntity>> YARN_SPOOLER =
            BLOCK_ENTITIES.register("yarn_spooler", () ->
                    BlockEntityType.Builder.of(YarnSpoolerBlockEntity::new, ModBlocks.YARN_SPOOLER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SveFruitTreeBlockEntity>> FRUIT_TREE =
            BLOCK_ENTITIES.register("fruit_tree", () ->
                    BlockEntityType.Builder.of(SveFruitTreeBlockEntity::new,
                                    ModBlocks.PEAR_TREE.get(),
                                    ModBlocks.NECTARINE_TREE.get(),
                                    ModBlocks.PERSIMMON_TREE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SveWildTreeBlockEntity>> WILD_TREE =
            BLOCK_ENTITIES.register("fir_tree", () ->
                    BlockEntityType.Builder.of(SveWildTreeBlockEntity::new,
                                    ModBlocks.FIR_TREE.get(), ModBlocks.BIRCH_TREE.get())
                            .build(null));

    private ModBlockEntities() {}
}
