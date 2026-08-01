package com.stardew.craft.sve;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.stardew.craft.npc.runtime.NpcFriendshipDataManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/** Development-only SVE commands. */
public final class SveDebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/debug");

    private SveDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sve")
                .then(Commands.literal("debugseason")
                        .requires(source -> source.hasPermission(2))
                        .executes(SveDebugCommand::debugSeasonStatus)
                        .then(Commands.literal("status")
                                .executes(SveDebugCommand::debugSeasonStatus))
                        .then(Commands.literal("on")
                                .executes(context -> setDebugSeason(context, true)))
                        .then(Commands.literal("off")
                                .executes(context -> setDebugSeason(context, false))))
                .then(Commands.literal("animalaudit")
                        .requires(source -> source.hasPermission(2))
                        .executes(SveAnimalAudit::run))
                .then(Commands.literal("bundleaudit")
                        .requires(source -> source.hasPermission(2))
                        .executes(SveBundleAudit::run))
                .then(Commands.literal("treeaudit")
                        .requires(source -> source.hasPermission(2))
                        .executes(SveTreeAudit::run))
                .then(Commands.literal("fishpondaudit")
                        .requires(source -> source.hasPermission(2))
                        .executes(SveFishPondAudit::run))
                .then(Commands.literal("contentaudit")
                        .requires(source -> source.hasPermission(2))
                        .executes(SveContentAudit::run))
                .then(Commands.literal("setfriendship")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("npc", StringArgumentType.word())
                                        .then(Commands.argument("points", IntegerArgumentType.integer(0, 5000))
                                                .executes(SveDebugCommand::setFriendship))))));

        // Brigadier merges this branch into StardewCraft's existing command tree.
        dispatcher.register(Commands.literal("stardew")
                .then(Commands.literal("time")
                        .then(Commands.literal("set")
                                .then(Commands.literal("season")
                                        .then(Commands.literal("debug")
                                                .requires(source -> source.hasPermission(2))
                                                .executes(context -> setDebugSeason(context, true)))))));
    }

    private static int setDebugSeason(CommandContext<CommandSourceStack> context,
                                      boolean enabled) {
        SveDebugSeasonState.setEnabled(context.getSource().getServer(), enabled);
        String key = enabled
                ? "command.stardewcraftsve.debug_season.enabled"
                : "command.stardewcraftsve.debug_season.disabled";
        context.getSource().sendSuccess(() -> Component.translatable(key), true);
        return 1;
    }

    private static int debugSeasonStatus(CommandContext<CommandSourceStack> context) {
        boolean enabled = SveDebugSeasonState.isEnabled(context.getSource().getServer());
        String key = enabled
                ? "command.stardewcraftsve.debug_season.status.enabled"
                : "command.stardewcraftsve.debug_season.status.disabled";
        context.getSource().sendSuccess(() -> Component.translatable(key), false);
        return 1;
    }

    private static int setFriendship(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            String npcId = StringArgumentType.getString(context, "npc");
            int targetPoints = IntegerArgumentType.getInteger(context, "points");

            int count = 0;
            for (ServerPlayer player : targets) {
                NpcFriendshipDataManager dataManager =
                        NpcFriendshipDataManager.get(player.serverLevel());
                if (dataManager == null) {
                    context.getSource().sendFailure(
                            Component.literal("Failed to get friendship data manager"));
                    return 0;
                }

                NpcFriendshipDataManager.FriendshipState state =
                        dataManager.getOrCreate(player.getUUID(), npcId);
                int currentPoints = state.points();
                state.addPoints(targetPoints - currentPoints, 5000);
                dataManager.setDirty();
                SveFriendshipRewards.applyEligible(player, npcId, targetPoints);

                context.getSource().sendSuccess(() -> Component.literal(
                        "Set " + player.getName().getString() + " friendship with " + npcId
                                + " to " + targetPoints + " (" + currentPoints
                                + " -> " + targetPoints + ")"), true);
                count++;
            }
            return count;
        } catch (Exception exception) {
            LOGGER.error("Failed to set friendship", exception);
            context.getSource().sendFailure(Component.literal("Error: " + exception.getMessage()));
            return 0;
        }
    }
}
