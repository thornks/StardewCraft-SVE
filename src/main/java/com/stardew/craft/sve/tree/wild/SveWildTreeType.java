package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.sve.StardewcraftsveMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum SveWildTreeType {
    FIR("fir", 7, 11, 15, 15, 0, 11, "fir_wax"),
    BIRCH("birch", 13, 15, 12, 16, 5, 3, "birch_water");

    public static final float GROWTH_CHANCE = 0.20F;
    public static final float FERTILIZED_GROWTH_CHANCE = 1.0F;
    public static final float SEED_SPREAD_CHANCE = 0.15F;
    public static final float SEED_ON_SHAKE_CHANCE = 0.05F;
    public static final float SEED_ON_CHOP_CHANCE = 0.75F;

    private final String id;
    private final int trunkHeight;
    private final int requiredHeight;
    private final int minWood;
    private final int maxWood;
    private final int sapCount;
    private final int tapperDays;
    private final String tapperProduct;

    SveWildTreeType(String id, int trunkHeight, int requiredHeight, int minWood, int maxWood,
                    int sapCount, int tapperDays, String tapperProduct) {
        this.id = id;
        this.trunkHeight = trunkHeight;
        this.requiredHeight = requiredHeight;
        this.minWood = minWood;
        this.maxWood = maxWood;
        this.sapCount = sapCount;
        this.tapperDays = tapperDays;
        this.tapperProduct = tapperProduct;
    }

    public String id() { return id; }
    public int trunkHeight() { return trunkHeight; }
    public int requiredHeight() { return requiredHeight; }
    public int sapCount() { return sapCount; }
    public int tapperDays() { return tapperDays; }
    public int fallDurationTicks() { return this == FIR ? 24 : 28; }

    public int woodCount(net.minecraft.util.RandomSource random) {
        return minWood == maxWood ? minWood : net.minecraft.util.Mth.nextInt(random, minWood, maxWood);
    }

    public Block saplingBlock() { return block(id + "_sapling"); }
    public Block matureBlock() { return block(id + "_tree"); }
    public Block extensionBlock() { return block(id + "_tree_extension"); }
    public Item seedItem() { return item(id.equals("fir") ? "fir_cone" : "birch_seed"); }
    public Item tapperProduct() { return item(tapperProduct); }

    public ResourceLocation model() {
        return location("geo/block/tree/wild/" + id + "_tree.geo.json");
    }

    public ResourceLocation texture() {
        return location("textures/block/tree/wild/" + id + "_tree.png");
    }

    public ResourceLocation animation() {
        return location("animations/block/tree/wild/" + id + "_tree.animation.json");
    }

    public static SveWildTreeType byId(String id) {
        for (SveWildTreeType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return FIR;
    }

    private static Block block(String path) { return BuiltInRegistries.BLOCK.get(location(path)); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(location(path)); }
    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, path);
    }
}
