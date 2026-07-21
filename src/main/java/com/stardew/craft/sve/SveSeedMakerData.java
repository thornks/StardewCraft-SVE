package com.stardew.craft.sve;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Seed-maker eligibility derived from SVE's original object metadata. */
public final class SveSeedMakerData {
    public static final int PROCESSING_MINUTES = 20;

    private static final List<SveCropData.Definition> BANNED = SveCropData.all().stream()
            .filter(crop -> crop == SveCropData.GOLD_CARROT)
            .toList();
    private static final List<SveCropData.Definition> ALLOWED = SveCropData.all().stream()
            .filter(crop -> !BANNED.contains(crop))
            .toList();
    private static final Map<String, String> SEED_BY_PRODUCE = ALLOWED.stream()
            .collect(Collectors.toUnmodifiableMap(
                    SveCropData.Definition::producePath,
                    SveCropData.Definition::seedPath));
    private static final Set<String> BANNED_PRODUCE = BANNED.stream()
            .map(SveCropData.Definition::producePath)
            .collect(Collectors.toUnmodifiableSet());

    private SveSeedMakerData() {
    }

    public static List<SveCropData.Definition> allowed() {
        return ALLOWED;
    }

    public static List<SveCropData.Definition> banned() {
        return BANNED;
    }

    /** Returns the registered seed path for an eligible produce item, or null otherwise. */
    public static String seedPathForProduce(String producePath) {
        return SEED_BY_PRODUCE.get(producePath);
    }

    public static boolean isBannedProduce(String producePath) {
        return BANNED_PRODUCE.contains(producePath);
    }
}
