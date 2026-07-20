package com.stardew.craft.sve;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("stardewcraftsve")
public class ButterChurnerJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                ButterChurnerJadeProvider.INSTANCE,
                ButterChurnerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                ButterChurnerJadeProvider.INSTANCE,
                ButterChurnerBlock.class);
    }
}
