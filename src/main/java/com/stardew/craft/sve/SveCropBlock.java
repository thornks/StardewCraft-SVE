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
    private final int[] seasonIds;        // stardew season ids: 0=spring 1=summer 2=fall 3=winter
    private final int[] phaseDays;        // 4-element array, one per growth stage
    private final boolean regrow;
    private final int regrowAge;          // stage to revert to on harvest
    private final int regrowDays;
    private final String displayName;
    private final int minHarvest;
    private final int maxHarvest;

    public SveCropBlock(BlockBehaviour.Properties props,
                        Supplier<Item> seedsItem,
                        Supplier<Item> cropItem,
                        int[] seasonIds,
                        int[] phaseDays,
                        boolean regrow,
                        int regrowAge,
                        int regrowDays,
                        String displayName) {
        this(props, seedsItem, cropItem, seasonIds, phaseDays,
             regrow, regrowAge, regrowDays, displayName, 1, 1);
    }

    public SveCropBlock(BlockBehaviour.Properties props,
                        Supplier<Item> seedsItem,
                        Supplier<Item> cropItem,
                        int[] seasonIds,
                        int[] phaseDays,
                        boolean regrow,
                        int regrowAge,
                        int regrowDays,
                        String displayName,
                        int minHarvest,
                        int maxHarvest) {
        super(props);
        this.seedsItem = seedsItem;
        this.cropItem = cropItem;
        this.seasonIds = seasonIds;
        this.phaseDays = phaseDays;
        this.regrow = regrow;
        this.regrowAge = regrowAge;
        this.regrowDays = regrowDays;
        this.displayName = displayName;
        this.minHarvest = minHarvest;
        this.maxHarvest = maxHarvest;
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
        for (int s : seasonIds) {
            if (s == current) return true;
        }
        return false;
    }

    @Override
    protected int[] getPhaseDays() {
        return phaseDays;
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
        return regrow;
    }

    @Override
    protected int getRegrowAge() {
        return regrowAge;
    }

    @Override
    protected int getRegrowDays() {
        return regrowDays;
    }

    @Override
    public String getCropDisplayNameKey() {
        Item item = cropItem.get();
        return item != null ? item.getDescriptionId() : displayName;
    }

    @Override
    protected int getHarvestMinStack() {
        return minHarvest;
    }

    @Override
    protected int getHarvestMaxStack() {
        return maxHarvest;
    }
}
