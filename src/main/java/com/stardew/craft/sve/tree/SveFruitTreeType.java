package com.stardew.craft.sve.tree;

import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum SveFruitTreeType {
    PEAR("pear", 0, 900, 90, 3),
    NECTARINE("nectarine", 1, 1500, 150, 3),
    PERSIMMON("persimmon", 2, 2000, 200, 3);

    public static final int DAYS_TO_MATURE = 28;
    public static final int MAX_FRUIT = 3;
    public static final int QUALITY_STEP_DAYS = 112;

    private final String id;
    private final int fruitSeason;
    private final int saplingSellPrice;
    private final int fruitSellPrice;
    private final int trunkTopY;

    SveFruitTreeType(String id, int fruitSeason, int saplingSellPrice, int fruitSellPrice, int trunkTopY) {
        this.id = id;
        this.fruitSeason = fruitSeason;
        this.saplingSellPrice = saplingSellPrice;
        this.fruitSellPrice = fruitSellPrice;
        this.trunkTopY = trunkTopY;
    }

    public String id() {
        return id;
    }

    public int fruitSeason() {
        return fruitSeason;
    }

    public int saplingSellPrice() {
        return saplingSellPrice;
    }

    public int fruitSellPrice() {
        return fruitSellPrice;
    }

    public int trunkTopY() {
        return trunkTopY;
    }

    public String saplingBlockId() {
        return id + "_sapling";
    }

    public String matureBlockId() {
        return id + "_tree";
    }

    public String extensionBlockId() {
        return id + "_tree_extension";
    }

    public ResourceLocation matureModel() {
        return location("geo/block/tree/fruit/" + id + "_tree.geo.json");
    }

    public ResourceLocation matureTexture() {
        return location("textures/block/tree/fruit/" + id + "_tree.png");
    }

    public Item saplingItem() {
        return BuiltInRegistries.ITEM.get(location(id + "_sapling"));
    }

    public Item fruitItem() {
        return BuiltInRegistries.ITEM.get(location(id));
    }

    public Block saplingBlock() {
        return BuiltInRegistries.BLOCK.get(location(saplingBlockId()));
    }

    public Block matureBlock() {
        return BuiltInRegistries.BLOCK.get(location(matureBlockId()));
    }

    public Block extensionBlock() {
        return BuiltInRegistries.BLOCK.get(location(extensionBlockId()));
    }

    public int visualStageFromDaysRemaining(int daysRemaining) {
        int grownDays = Math.max(0, DAYS_TO_MATURE - Math.max(0, daysRemaining));
        return Math.min(3, grownDays / 7);
    }

    public static SveFruitTreeType byId(String id) {
        for (SveFruitTreeType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return PEAR;
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }
}
