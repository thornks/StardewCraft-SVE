package com.stardew.craft.sve;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("stardewcraftsve")
public class YarnSpoolerJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(YarnSpoolerJadeProvider.INSTANCE, YarnSpoolerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(YarnSpoolerJadeProvider.INSTANCE, YarnSpoolerBlock.class);
    }
}
