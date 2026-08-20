package com.stardew.craft.sve;

import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.shop.SaloonService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.stardew.craft.sve.tree.SveFruitTreeGrowthManager;
import com.stardew.craft.sve.tree.wild.SveWildTreeGrowthManager;
import com.stardew.craft.sve.animal.SveAnimalData;
import com.stardew.craft.sve.animal.SveAnimalEntities;
import com.stardew.craft.sve.network.SveNetwork;
import com.stardew.craft.npc.runtime.NpcSpawnManager;

@Mod(StardewcraftsveMod.MODID)
public class StardewcraftsveMod {
    public static final String MODID = "stardewcraftsve";
    private static final ResourceManagerReloadListener BUNDLE_RELOAD_LISTENER =
            resourceManager -> SveCommunityBundles.apply();

    public StardewcraftsveMod(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SveClientEvents.register(modEventBus);
        }

        SveActions.register();
        verifyRecipeShopNamespaceCompatibility();
        SveWeaponData.register();
        SveHeavyShieldHandler.register();
        SveCropData.register();
        SveAnimalData.register();
        SveDebugSeasonState.registerSeasonRule();
        SveInteractionHintProvider.register();

        ModItems.ITEMS.register(modEventBus);
        ModItems.STARDEWCRAFT_ITEMS.register(modEventBus);
        ModItems.SMOKED_FISH_ITEMS.register(modEventBus);
        ModItems.ARTISAN_ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        SveAnimalEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(SveAnimalEntities::onEntityAttributeCreation);
        modEventBus.addListener(SveNetwork::register);

        NeoForge.EVENT_BUS.addListener(
                PlayerEvent.PlayerLoggedInEvent.class,
                SveFriendshipRewards::onPlayerLoggedIn);
        // Run after StardewCraft's own login handler. 0.5.6 keeps NPC existence
        // maintenance separate from time simulation; re-running the idempotent
        // entry sweep here recovers entities when the host listener observed a
        // stale process-local player state during a world switch.
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                PlayerEvent.PlayerLoggedInEvent.class,
                event -> {
                    if (!(event.getEntity() instanceof ServerPlayer player)) return;
                    ServerLevel level = player.serverLevel();
                    if (level.dimension().equals(ModDimensions.STARDEW_VALLEY)) {
                        NpcSpawnManager.onPlayerEntered(level);
                        NpcSpawnManager.tick(level);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                PlayerEvent.PlayerLoggedOutEvent.class,
                event -> SveBundleSelectionPending.remove(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                StardewcraftsveMod::onAddReloadListeners);

        // Service NPCs route to shops before the generic gift flow.
        SveNpcGiftInteractionProvider.register();
        MorrisDialogueInterceptor.register();

        // The 0.5 public world data API covers loot, but not custom artifact-spot zones.
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().getTickCount() % 20 != 0) return;
            ServerLevel svLevel = event.getServer().getLevel(ModDimensions.STARDEW_VALLEY);
            if (svLevel != null) {
                SveArtifactSpotSpawnService.tick(svLevel);
                SveFruitTreeGrowthManager.get(svLevel).tick(svLevel);
                SveWildTreeGrowthManager.get(svLevel).tick(svLevel);
            }
        });

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                SveDebugCommand.register(event.getDispatcher()));
    }

    private static void verifyRecipeShopNamespaceCompatibility() {
        String expected = MODID + ":compatibility_probe";
        String actual = SaloonService.extractRecipeId("recipe:" + expected);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "StardewCraft recipe shops discarded the addon namespace: " + actual);
        }
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(BUNDLE_RELOAD_LISTENER);
    }
}
