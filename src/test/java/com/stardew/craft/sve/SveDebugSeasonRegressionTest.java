package com.stardew.craft.sve;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.stardew.craft.command.StardewTeleportCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;

import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies debug-season persistence and its public command paths. */
public final class SveDebugSeasonRegressionTest {
    private SveDebugSeasonRegressionTest() {
    }

    public static void main(String[] args) {
        verifyPersistence();
        verifyCommands();
        verifySeasonSwitchMixinScope();
        System.out.println("SVE debug-season regression suite passed");
    }

    private static void verifyPersistence() {
        CompoundTag enabledTag = new CompoundTag();
        enabledTag.putBoolean("Enabled", true);
        SveDebugSeasonState enabled = SveDebugSeasonState.load(enabledTag, null);
        CompoundTag saved = enabled.save(new CompoundTag(), null);
        expect(saved.getBoolean("Enabled"), "enabled state survives save/load");

        SveDebugSeasonState disabled = SveDebugSeasonState.load(new CompoundTag(), null);
        CompoundTag disabledTag = disabled.save(new CompoundTag(), null);
        expect(!disabledTag.getBoolean("Enabled"), "missing state defaults to disabled");
    }

    private static void verifyCommands() {
        verifyMergedCommands(true);
        verifyMergedCommands(false);
    }

    private static void verifyMergedCommands(boolean hostFirst) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        if (hostFirst) {
            StardewTeleportCommand.register(dispatcher);
            SveDebugCommand.register(dispatcher);
        } else {
            SveDebugCommand.register(dispatcher);
            StardewTeleportCommand.register(dispatcher);
        }

        expectPath(dispatcher, "stardew", "time", "set", "season", "spring");
        expectPath(dispatcher, "stardew", "time", "set", "season", "debug");
        expectPath(dispatcher, "sve", "debugseason", "status");
        expectPath(dispatcher, "sve", "debugseason", "on");
        expectPath(dispatcher, "sve", "debugseason", "off");
    }

    private static void verifySeasonSwitchMixinScope() {
        try {
            String source = Files.readString(Path.of(
                    "src/main/java/com/stardew/craft/sve/mixin/StardewTeleportCommandMixin.java"));
            expect(source.contains("@Mixin(StardewTeleportCommand.class)"),
                    "debug season is disabled through the administrator command");
            expect(source.contains("method = \"setSeason\""),
                    "real-season command disables debug season");
            expect(source.contains("method = \"resetTime\""),
                    "time reset disables debug season");
            expect(!source.contains("StardewTimeManager.class"),
                    "client time synchronization must not disable debug season");
        } catch (Exception exception) {
            throw new AssertionError("failed to inspect season-switch Mixin", exception);
        }
    }

    private static void expectPath(CommandDispatcher<CommandSourceStack> dispatcher,
                                   String... path) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        for (String segment : path) {
            node = node.getChild(segment);
            expect(node != null, "missing command path segment: " + String.join(" ", path));
        }
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
