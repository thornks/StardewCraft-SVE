package com.stardew.craft.sve.collection;

import com.stardew.craft.data.VanillaObjectCatalog;
import com.stardew.craft.sve.SveItemCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class SveCollectionCatalog {
    public static final String NAMESPACE = "stardewcraftsve";

    private SveCollectionCatalog() {
    }

    public static List<VanillaObjectCatalog.Entry> entriesForTab(int tab) {
        return switch (tab) {
            case 0 -> staticEntries(SveItemCatalog.SHIPPING, "Basic", -79);
            case 1 -> staticEntries(SveItemCatalog.FISH, "Fish", -4);
            case 2 -> staticEntries(SveItemCatalog.ARTIFACTS, "Arch", 0);
            case 3 -> staticEntries(SveItemCatalog.MINERALS, "Minerals", -2);
            case 4 -> staticEntries(SveItemCatalog.COOKING, "Cooking", -7);
            default -> List.of();
        };
    }

    public static boolean isSveEntry(VanillaObjectCatalog.Entry entry) {
        return entry != null && entry.key().startsWith(NAMESPACE + ":");
    }

    /** Static collection IDs used by validation without requiring a live item registry. */
    public static List<String> configuredItemIdsForTab(int tab) {
        List<String> paths = switch (tab) {
            case 0 -> SveItemCatalog.SHIPPING;
            case 1 -> SveItemCatalog.FISH;
            case 2 -> SveItemCatalog.ARTIFACTS;
            case 3 -> SveItemCatalog.MINERALS;
            case 4 -> SveItemCatalog.COOKING;
            default -> List.of();
        };
        return paths.stream().map(path -> NAMESPACE + ":" + path).toList();
    }

    private static List<VanillaObjectCatalog.Entry> staticEntries(
            List<String> paths,
            String type,
            int category
    ) {
        List<VanillaObjectCatalog.Entry> entries = new ArrayList<>(paths.size());
        for (String path : paths) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                entries.add(entry(id, type, category));
            }
        }
        return List.copyOf(entries);
    }

    private static VanillaObjectCatalog.Entry entry(
            ResourceLocation id,
            String type,
            int category
    ) {
        return new VanillaObjectCatalog.Entry(
                id.toString(),
                id.getPath(),
                type,
                category,
                "",
                0,
                false,
                false
        );
    }
}
