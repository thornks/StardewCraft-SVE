package com.stardew.craft.sve;

import java.util.LinkedHashMap;
import java.util.Map;

/** Documented acquisition routes for every SVE ingredient used by either bundle catalog. */
public final class SveBundleAcquisitionCatalog {
    private static final Map<String, String> ROUTES = createRoutes();

    private SveBundleAcquisitionCatalog() {
    }

    public static Map<String, String> routes() {
        return ROUTES;
    }

    public static String routeFor(String itemId) {
        return ROUTES.get(itemId);
    }

    private static Map<String, String> createRoutes() {
        Map<String, String> routes = new LinkedHashMap<>();
        route(routes, "cucumber", "Pierre seed shop; summer crop");
        route(routes, "butternut_squash", "Pierre seed shop; fall crop");
        route(routes, "sweet_potato", "Pierre seed shop; fall crop");
        route(routes, "butter", "Butter churner processing milk");
        route(routes, "nectarine", "Pierre sapling shop; summer fruit tree");
        route(routes, "pear", "Pierre sapling shop; spring fruit tree");
        route(routes, "persimmon", "Pierre sapling shop; fall fruit tree");
        route(routes, "red_baneberry", "Secret Woods summer forage");
        route(routes, "mushroom_colony", "Forest and Secret Woods fall forage");
        route(routes, "bearberrys", "Secret Woods winter forage");
        route(routes, "lucky_four_leaf_clover", "Secret Woods spring and summer forage");
        route(routes, "poison_mushroom", "Secret Woods summer and fall forage");
        route(routes, "fir_wax", "Fir-tree tapper or Bear vendor");
        route(routes, "birch_water", "Birch-tree tapper or Bear vendor");
        route(routes, "minnow", "Fishing");
        route(routes, "goldfish", "Fishing");
        route(routes, "tadpole", "Fishing");
        route(routes, "starfish", "Fishing");
        route(routes, "frog", "Fishing");
        route(routes, "king_salmon", "Fishing after Western Cindersap Forest is ported");
        route(routes, "butterfish", "Fishing after its SVE locations are ported");
        route(routes, "shark", "Fishing at Ginger Island or Fable Reef");
        route(routes, "shiny_lunaloo", "Fishing at Ginger Island or Fable Reef");
        route(routes, "candy", "Saloon recipe; cooking");
        route(routes, "mixed_berry_pie", "Saloon recipe; cooking");
        route(routes, "big_bark_burger", "Saloon recipe; requires Puppyfish location");
        route(routes, "glazed_butterfish", "Saloon recipe; requires Butterfish location");
        route(routes, "amber", "Railroad, Backwoods, Mountain, and Forest artifact spots");
        return Map.copyOf(routes);
    }

    private static void route(Map<String, String> routes, String path, String description) {
        routes.put(StardewcraftsveMod.MODID + ":" + path, description);
    }
}
