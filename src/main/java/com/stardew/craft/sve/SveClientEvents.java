package com.stardew.craft.sve;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.FoliageColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stardew.craft.sve.tree.SveFruitTreeBlockEntityRenderer;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlockEntityRenderer;
import com.stardew.craft.sve.animal.SveAnimalEntities;
import com.stardew.craft.sve.animal.CamelRenderer;
import com.stardew.craft.sve.animal.GooseRenderer;

/**
 * Client-side setup for SVE blocks and items.
 */
public final class SveClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/client-models");

    private SveClientEvents() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SveClientEvents::onRegisterMenuScreens);
        modEventBus.addListener(SveClientEvents::onClientSetup);
        modEventBus.addListener(SveClientEvents::onRegisterRenderers);
        modEventBus.addListener(SveClientEvents::onRegisterAdditional);
        modEventBus.addListener(SveClientEvents::onRegisterBlockColors);
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FORAGE_SELECTION.get(), ForageSelectionScreen::new);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.BUTTER_CHURNER.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.YARN_SPOOLER.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.PEAR_SAPLING.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.NECTARINE_SAPLING.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.PERSIMMON_SAPLING.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.FIR_SAPLING.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.BIRCH_SAPLING.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.HEDGE_FENCE.get(), RenderType.cutout());
        });
    }

    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return FoliageColor.getDefaultColor();
            }
            return BiomeColors.getAverageFoliageColor(level, pos);
        }, ModBlocks.HEDGE_FENCE.get());
    }

    /** Callback for {@link ModelEvent.RegisterAdditional} — mod bus. */
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.BUTTER_CHURNER.get(),
                ButterChurnerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.YARN_SPOOLER.get(),
                YarnSpoolerBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FRUIT_TREE.get(),
                SveFruitTreeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WILD_TREE.get(),
                SveWildTreeBlockEntityRenderer::new);
        event.registerEntityRenderer(SveAnimalEntities.GOOSE.get(), GooseRenderer::new);
        event.registerEntityRenderer(SveAnimalEntities.CAMEL.get(), CamelRenderer::new);
    }

    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        int count = 0;
        for (String fish : SveFishData.SVE_FISH) {
            String smoked = "smoked_" + fish;
            // Register the 5 standalone variants per fish, matching the
            // pattern in stardewcraft's ModClientModels.
            event.register(standalone("stardewcraftsve", "item/" + smoked));
            event.register(standalone("stardewcraftsve", "item/" + smoked + "_base"));
            event.register(standalone("stardewcraftsve", "item/" + smoked + "_base_silver"));
            event.register(standalone("stardewcraftsve", "item/" + smoked + "_base_gold"));
            event.register(standalone("stardewcraftsve", "item/" + smoked + "_base_iridium"));
            count++;
        }
        if (count > 0) {
            LOGGER.info("Registered standalone model variants for {} SVE smoked fish", count);
        }
    }

    private static ModelResourceLocation standalone(String namespace, String path) {
        return new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(namespace, path),
            "standalone"
        );
    }
}
