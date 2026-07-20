package com.stardew.craft.sve.tree;

import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeJadeProvider;
import com.stardew.craft.sve.tree.wild.SveWildTreeSaplingBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("stardewcraftsve")
public class SveFruitTreeJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SveFruitTreeJadeProvider.INSTANCE, SveFruitTreeSaplingBlock.class);
        registration.registerBlockDataProvider(SveFruitTreeJadeProvider.INSTANCE, SveFruitTreeBlock.class);
        registration.registerBlockDataProvider(SveFruitTreeJadeProvider.INSTANCE, SveFruitTreeExtensionBlock.class);
        registration.registerBlockDataProvider(SveWildTreeJadeProvider.INSTANCE, SveWildTreeSaplingBlock.class);
        registration.registerBlockDataProvider(SveWildTreeJadeProvider.INSTANCE, SveWildTreeBlock.class);
        registration.registerBlockDataProvider(SveWildTreeJadeProvider.INSTANCE, SveWildTreeExtensionBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SveFruitTreeJadeProvider.INSTANCE, SveFruitTreeSaplingBlock.class);
        registration.registerBlockComponent(SveFruitTreeJadeProvider.INSTANCE, SveFruitTreeBlock.class);
        registration.registerBlockComponent(SveFruitTreeJadeProvider.INSTANCE, SveFruitTreeExtensionBlock.class);
        registration.registerBlockComponent(SveWildTreeJadeProvider.INSTANCE, SveWildTreeSaplingBlock.class);
        registration.registerBlockComponent(SveWildTreeJadeProvider.INSTANCE, SveWildTreeBlock.class);
        registration.registerBlockComponent(SveWildTreeJadeProvider.INSTANCE, SveWildTreeExtensionBlock.class);
    }
}
