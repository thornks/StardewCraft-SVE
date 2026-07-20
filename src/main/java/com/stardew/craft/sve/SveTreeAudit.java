package com.stardew.craft.sve;

import com.mojang.brigadier.context.CommandContext;
import com.stardew.craft.block.ModBlocks;
import com.stardew.craft.block.utility.TapperBlock;
import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.sve.tree.SveFruitTreeBlock;
import com.stardew.craft.sve.tree.SveFruitTreeBlockEntity;
import com.stardew.craft.sve.tree.SveFruitTreeExtensionBlock;
import com.stardew.craft.sve.tree.SveFruitTreeGrowthManager;
import com.stardew.craft.sve.tree.SveFruitTreeSaplingBlock;
import com.stardew.craft.sve.tree.SveFruitTreeType;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeBlockEntity;
import com.stardew.craft.sve.tree.wild.SveWildTreeExtensionBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeGrowthManager;
import com.stardew.craft.sve.tree.wild.SveWildTreeSaplingBlock;
import com.stardew.craft.sve.tree.wild.SveWildTreeType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Read-only consistency checks for SVE trees in saved data and loaded player areas. */
public final class SveTreeAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/tree-audit");
    private static final int MAX_REPORTED_ISSUES = 24;
    private static final int SCAN_HORIZONTAL_RADIUS = 24;
    private static final int SCAN_VERTICAL_RADIUS = 40;

    private SveTreeAudit() {
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        if (level == null) {
            source.sendFailure(net.minecraft.network.chat.Component.literal(
                    "Stardew Valley dimension is not loaded"));
            return 0;
        }

        Audit audit = audit(level);
        ChatFormatting color = audit.errors == 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "SVE tree audit: fruit_saplings=" + audit.fruitSaplings
                        + ", wild_saplings=" + audit.wildSaplings
                        + ", loaded_blocks=" + audit.loadedBlocks
                        + ", errors=" + audit.errors
                        + ", warnings=" + audit.warnings
        ).withStyle(color), false);

        int shown = Math.min(MAX_REPORTED_ISSUES, audit.issues.size());
        for (int index = 0; index < shown; index++) {
            Issue issue = audit.issues.get(index);
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal(issue.message)
                    .withStyle(issue.error ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (audit.issues.size() > shown) {
            source.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "... " + (audit.issues.size() - shown) + " more issue(s); see server log"
            ).withStyle(ChatFormatting.GRAY));
        }
        for (Issue issue : audit.issues) {
            if (issue.error) LOGGER.error(issue.message);
            else LOGGER.warn(issue.message);
        }
        return audit.errors == 0 ? 1 : 0;
    }

    public static Audit audit(ServerLevel level) {
        Audit audit = new Audit();
        auditSavedSaplings(level, audit);
        scanLoadedPlayerAreas(level, audit);
        return audit;
    }

    private static void auditSavedSaplings(ServerLevel level, Audit audit) {
        SveFruitTreeGrowthManager fruitManager = SveFruitTreeGrowthManager.get(level);
        for (SveFruitTreeGrowthManager.SaplingSnapshot snapshot : fruitManager.snapshots()) {
            if (!snapshot.pos().dimension().equals(level.dimension())) continue;
            audit.fruitSaplings++;
            BlockPos pos = snapshot.pos().pos();
            if (snapshot.daysRemaining() < 0
                    || snapshot.daysRemaining() > SveFruitTreeType.DAYS_TO_MATURE) {
                audit.error("Fruit sapling " + format(pos) + " has invalid days remaining "
                        + snapshot.daysRemaining());
            }
            if (!level.isLoaded(pos)) {
                audit.warning("Fruit sapling " + format(pos) + " is in an unloaded chunk; skipped world check");
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SveFruitTreeSaplingBlock sapling)) {
                audit.error("Fruit sapling entry " + format(pos) + " is not a sapling block");
                continue;
            }
            if (sapling.getType() != snapshot.type()) {
                audit.error("Fruit sapling " + format(pos) + " type mismatch: data="
                        + snapshot.type().id() + ", block=" + sapling.getType().id());
            }
            if (state.getValue(SveFruitTreeSaplingBlock.HALF) != DoubleBlockHalf.LOWER) {
                audit.error("Fruit sapling entry " + format(pos) + " is not the lower half");
            }
            int expectedStage = snapshot.type().visualStageFromDaysRemaining(snapshot.daysRemaining());
            if (state.getValue(SveFruitTreeSaplingBlock.AGE) != expectedStage) {
                audit.warning("Fruit sapling " + format(pos) + " visual stage "
                        + state.getValue(SveFruitTreeSaplingBlock.AGE) + ", expected " + expectedStage);
            }
            BlockState upper = level.getBlockState(pos.above());
            if (!(upper.getBlock() instanceof SveFruitTreeSaplingBlock)
                    || upper.getValue(SveFruitTreeSaplingBlock.HALF) != DoubleBlockHalf.UPPER
                    || upper.getValue(SveFruitTreeSaplingBlock.AGE) != state.getValue(SveFruitTreeSaplingBlock.AGE)) {
                audit.error("Fruit sapling " + format(pos) + " is missing a matching upper half");
            }
        }

        SveWildTreeGrowthManager wildManager = SveWildTreeGrowthManager.get(level);
        for (SveWildTreeGrowthManager.SaplingSnapshot snapshot : wildManager.snapshots()) {
            if (!snapshot.pos().dimension().equals(level.dimension())) continue;
            audit.wildSaplings++;
            BlockPos pos = snapshot.pos().pos();
            if (snapshot.stage() < 0 || snapshot.stage() > SveWildTreeGrowthManager.MATURE_STAGE) {
                audit.error("Wild sapling " + format(pos) + " has invalid stage " + snapshot.stage());
            }
            if (!level.isLoaded(pos)) {
                audit.warning("Wild sapling " + format(pos) + " is in an unloaded chunk; skipped world check");
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SveWildTreeSaplingBlock sapling)) {
                audit.error("Wild sapling entry " + format(pos) + " is not a sapling block");
                continue;
            }
            if (sapling.getType() != snapshot.type()) {
                audit.error("Wild sapling " + format(pos) + " type mismatch: data="
                        + snapshot.type().id() + ", block=" + sapling.getType().id());
            }
            int expectedStage = Math.min(SveWildTreeGrowthManager.MATURE_STAGE - 1, snapshot.stage());
            if (state.getValue(SveWildTreeSaplingBlock.STAGE) != expectedStage) {
                audit.warning("Wild sapling " + format(pos) + " visual stage "
                        + state.getValue(SveWildTreeSaplingBlock.STAGE) + ", expected " + expectedStage);
            }
        }
    }

    private static void scanLoadedPlayerAreas(ServerLevel level, Audit audit) {
        Set<BlockPos> scanned = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            int minX = center.getX() - SCAN_HORIZONTAL_RADIUS;
            int maxX = center.getX() + SCAN_HORIZONTAL_RADIUS;
            int minZ = center.getZ() - SCAN_HORIZONTAL_RADIUS;
            int maxZ = center.getZ() + SCAN_HORIZONTAL_RADIUS;
            int minY = Math.max(level.getMinBuildHeight(), center.getY() - SCAN_VERTICAL_RADIUS);
            int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + SCAN_VERTICAL_RADIUS);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!level.isLoaded(new BlockPos(x, center.getY(), z))) continue;
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (scanned.add(pos)) inspectBlock(level, pos, audit);
                    }
                }
            }
        }
    }

    private static void inspectBlock(ServerLevel level, BlockPos pos, Audit audit) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SveFruitTreeBlock tree) {
            audit.loadedBlocks++;
            if (level.getBlockEntity(pos) == null) {
                audit.error("Fruit tree " + format(pos) + " is missing its block entity");
            }
            for (int y = 1; y <= tree.getType().trunkTopY(); y++) {
                BlockState extension = level.getBlockState(pos.above(y));
                if (!(extension.getBlock() instanceof SveFruitTreeExtensionBlock part)
                        || part.getType() != tree.getType()) {
                    audit.warning("Fruit tree " + format(pos) + " is missing extension at y+" + y);
                }
            }
        } else if (state.getBlock() instanceof SveFruitTreeExtensionBlock extension
                && SveFruitTreeBlock.findRoot(level, pos, extension.getType()) == null) {
            audit.warning("Orphan fruit tree extension at " + format(pos));
        } else if (state.getBlock() instanceof SveWildTreeBlock tree) {
            audit.loadedBlocks++;
            if (!state.getValue(SveWildTreeBlock.STUMP)) {
                for (int y = 1; y < tree.getType().trunkHeight(); y++) {
                    BlockState extension = level.getBlockState(pos.above(y));
                    if (!(extension.getBlock() instanceof SveWildTreeExtensionBlock part)
                            || part.getType() != tree.getType()) {
                        audit.warning("Wild tree " + format(pos) + " is missing extension at y+" + y);
                    }
                }
            }
            if (level.getBlockEntity(pos) == null) {
                audit.error("Wild tree " + format(pos) + " is missing its block entity");
            }
        } else if (state.getBlock() instanceof SveWildTreeExtensionBlock extension
                && SveWildTreeBlock.findRoot(level, pos, extension.getType()) == null) {
            audit.warning("Orphan wild tree extension at " + format(pos));
        } else if (state.is(ModBlocks.TAPPER.get())) {
            inspectTapper(level, pos, state, audit);
        }
    }

    private static void inspectTapper(ServerLevel level, BlockPos pos, BlockState state, Audit audit) {
        if (!state.hasProperty(TapperBlock.FACING)) return;
        BlockPos support = pos.relative(state.getValue(TapperBlock.FACING));
        BlockState supportState = level.getBlockState(support);
        if (supportState.getBlock() instanceof SveWildTreeBlock tree
                && SveWildTreeBlock.findRoot(level, support, tree.getType()) == null) {
            audit.warning("Tapper " + format(pos) + " is attached to an invalid SVE tree root");
        }
        if (supportState.getBlock() instanceof SveWildTreeExtensionBlock extension
                && SveWildTreeBlock.findRoot(level, support, extension.getType()) == null) {
            audit.warning("Tapper " + format(pos) + " is attached to an orphan SVE extension");
        }
    }

    private static String format(BlockPos pos) {
        return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
    }

    public static final class Audit {
        private final List<Issue> issues = new ArrayList<>();
        private int fruitSaplings;
        private int wildSaplings;
        private int loadedBlocks;
        private int errors;
        private int warnings;

        private void error(String message) {
            errors++;
            issues.add(new Issue(true, message));
        }

        private void warning(String message) {
            warnings++;
            issues.add(new Issue(false, message));
        }
    }

    private record Issue(boolean error, String message) {
    }
}
