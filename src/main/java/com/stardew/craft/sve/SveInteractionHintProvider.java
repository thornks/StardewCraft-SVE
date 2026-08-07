package com.stardew.craft.sve;

import com.stardew.craft.api.v1.interaction.StardewInteractionHint;
import com.stardew.craft.api.v1.interaction.StardewInteractionHintContext;
import com.stardew.craft.api.v1.interaction.StardewInteractionHintDecision;
import com.stardew.craft.api.v1.interaction.StardewInteractionHintType;
import com.stardew.craft.api.v1.interaction.StardewInteractionHints;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.event.FarmAreaProtectionEvents;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Supplies the host-style crosshair hint for SVE wild trees. */
public final class SveInteractionHintProvider {
    private static final ResourceLocation PROVIDER_ID = ResourceLocation.fromNamespaceAndPath(
            StardewcraftsveMod.MODID, "wild_tree_interaction_hint");
    private static final ResourceLocation WILD_TREE_ID = ResourceLocation.fromNamespaceAndPath(
            "stardewcraft", "wild_tree");

    private SveInteractionHintProvider() {
    }

    public static void register() {
        StardewInteractionHints.register(PROVIDER_ID, 100,
                SveInteractionHintProvider::resolve);
    }

    private static StardewInteractionHintDecision resolve(StardewInteractionHintContext context) {
        if (context.isEntityTarget() || !context.player().getMainHandItem().isEmpty()) {
            return StardewInteractionHintDecision.pass();
        }

        BlockState state = context.blockState();
        SveWildTreeType type = typeOf(state);
        if (type == null) {
            return StardewInteractionHintDecision.pass();
        }

        BlockPos root = SveWildTreeBlock.findRoot(
                context.player().serverLevel(), context.pos(), type);
        if (root == null || !canModify(context, root)) {
            return StardewInteractionHintDecision.pass();
        }

        return StardewInteractionHintDecision.show(new StardewInteractionHint(
                StardewInteractionHintType.GRAB, false, WILD_TREE_ID));
    }

    private static SveWildTreeType typeOf(BlockState state) {
        if (state.getBlock() instanceof SveWildTreeBlock tree) {
            return state.hasProperty(SveWildTreeBlock.STUMP)
                    && state.getValue(SveWildTreeBlock.STUMP)
                    ? null : tree.getType();
        }
        if (state.getBlock() instanceof com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock extension) {
            return extension.getType();
        }
        return null;
    }

    private static boolean canModify(StardewInteractionHintContext context, BlockPos root) {
        return context.player().serverLevel().dimension() != ModDimensions.STARDEW_VALLEY
                || context.player().isCreative()
                || FarmAreaProtectionEvents.canModifyAt(context.player(), root);
    }
}
