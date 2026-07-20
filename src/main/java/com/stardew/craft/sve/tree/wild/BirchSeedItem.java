package com.stardew.craft.sve.tree.wild;

import com.stardew.craft.item.SimpleStardewItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

public final class BirchSeedItem extends SimpleStardewItem {
    public BirchSeedItem(Properties properties) {
        super("stardewcraft.type.seed", 5, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return SveWildTreePlanting.tryPlant(context, SveWildTreeType.BIRCH);
    }
}
