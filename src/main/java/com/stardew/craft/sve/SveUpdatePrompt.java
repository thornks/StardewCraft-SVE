package com.stardew.craft.sve;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Shows one update confirmation per available version on the title screen. */
public final class SveUpdatePrompt {
    private static final Set<String> PROMPTED_VERSIONS = ConcurrentHashMap.newKeySet();

    private SveUpdatePrompt() {
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        SveUpdateChecker.checkAsync().thenAccept(status -> {
            if (!status.isOutdated()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> showIfStillOnTitleScreen(minecraft, status));
        });
    }

    private static void showIfStillOnTitleScreen(
            Minecraft minecraft, SveUpdateChecker.VersionStatus status) {
        if (!(minecraft.screen instanceof TitleScreen)
                || !PROMPTED_VERSIONS.add(status.latestVersion())) {
            return;
        }
        TitleScreen parent = (TitleScreen) minecraft.screen;
        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        Util.getPlatform().openUri(URI.create(status.fileUrl()));
                    }
                    minecraft.setScreen(parent);
                },
                Component.translatable("stardewcraftsve.update.title"),
                Component.translatable(
                        "stardewcraftsve.update.message",
                        status.installedVersion(),
                        status.latestVersion()),
                Component.translatable("stardewcraftsve.update.open"),
                Component.translatable("stardewcraftsve.update.later")
        ));
    }

    static boolean markPromptedForTest(String version) {
        return PROMPTED_VERSIONS.add(version);
    }

    static void clearPromptedForTest() {
        PROMPTED_VERSIONS.clear();
    }
}
