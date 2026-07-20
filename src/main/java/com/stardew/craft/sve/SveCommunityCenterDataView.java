package com.stardew.craft.sve;

import com.stardew.craft.sve.mixin.CommunityCenterPlayerProgressAccessor;

import java.util.Map;
import java.util.UUID;

/** Read-only bridge used by the bundle audit without creating or normalizing progress entries. */
public interface SveCommunityCenterDataView {
    Map<UUID, ?> stardewcraftsve$getPlayerDataView();

    static CommunityCenterPlayerProgressAccessor progress(Object value) {
        return value instanceof CommunityCenterPlayerProgressAccessor accessor ? accessor : null;
    }
}
