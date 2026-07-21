package com.stardew.craft.sve;

import java.util.List;

/** Seed-maker eligibility derived from SVE's original object metadata. */
public final class SveSeedMakerData {
    public static final int PROCESSING_MINUTES = 20;

    private static final List<SveCropData.Definition> BANNED = SveCropData.all().stream()
            .filter(crop -> crop == SveCropData.GOLD_CARROT)
            .toList();
    private static final List<SveCropData.Definition> ALLOWED = SveCropData.all().stream()
            .filter(crop -> !BANNED.contains(crop))
            .toList();

    private SveSeedMakerData() {
    }

    public static List<SveCropData.Definition> allowed() {
        return ALLOWED;
    }

    public static List<SveCropData.Definition> banned() {
        return BANNED;
    }
}
