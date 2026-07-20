package com.stardew.craft.sve;

import com.stardew.craft.block.crop.StardewCropBlock;
import com.stardew.craft.item.quality.QualityHelper;
import com.stardew.craft.time.StardewTimeManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

/**
 * Generic SVE crop block parameterized by its seeds, harvest, growth
 * schedule, and regrow behavior. Avoids one subclass-per-crop boilerplate
 * while hooking into StardewCropBlock's growth-engine (fertilizer, season
 * gating, quality tiers, trellis, giant variants).
 */
public class SveCropBlock extends StardewCropBlock {

    private final Supplier<Item> seedsItem;
    private final Supplier<Item> cropItem;
    private final SveCropData.Definition definition;

    public SveCropBlock(BlockBehaviour.Properties props,
                        Supplier<Item> seedsItem,
                        Supplier<Item> cropItem,
                        SveCropData.Definition definition) {
        super(props, requireDefinition(definition).raised());
        this.seedsItem = seedsItem;
        this.cropItem = cropItem;
        this.definition = definition;
    }

    private static SveCropData.Definition requireDefinition(SveCropData.Definition definition) {
        return java.util.Objects.requireNonNull(definition, "definition");
    }

    SveCropData.Definition definition() {
        return definition;
    }

    @Override
    protected Supplier<Item> getSeedsItem() {
        return seedsItem;
    }

    @Override
    protected Supplier<Item> getCropItem() {
        return cropItem;
    }

    @Override
    protected boolean isInSeason(Level level) {
        int current = StardewTimeManager.get().getCurrentSeason();
        return definition.isInSeason(current);
    }

    @Override
    protected int[] getPhaseDays() {
        return definition.phaseDaysArray();
    }

    @Override
    protected ItemStack getHarvestItem(int quality) {
        Item item = cropItem.get();
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        QualityHelper.setQuality(stack, quality);
        return stack;
    }

    @Override
    protected boolean canRegrow() {
        return definition.regrows();
    }

    @Override
    protected int getRegrowAge() {
        return definition.regrows() ? 1 : 0;
    }

    @Override
    protected int getRegrowDays() {
        return definition.regrowDays();
    }

    @Override
    public String getCropDisplayNameKey() {
        Item item = cropItem.get();
        return item != null ? item.getDescriptionId() : definition.producePath();
    }

    @Override
    protected int getHarvestMinStack() {
        return definition.minHarvest();
    }

    @Override
    protected int getHarvestMaxStack() {
        return definition.maxHarvest();
    }

    @Override
    protected float getHarvestMaxIncreasePerFarmingLevel() {
        return definition.harvestMaxIncreasePerFarmingLevel();
    }

    @Override
    protected double getExtraHarvestChance() {
        return definition.extraHarvestChance();
    }
}
