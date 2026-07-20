package com.stardew.craft.sve;

import com.mojang.serialization.Codec;
import com.stardew.craft.api.v1.action.StardewActionResult;
import com.stardew.craft.api.v1.action.StardewActions;
import com.stardew.craft.mail.MailService;
import net.minecraft.resources.ResourceLocation;

/** Registers actions used by SVE data-driven content. */
public final class SveActions {
    private static final ResourceLocation QUEUE_MAIL = ResourceLocation.fromNamespaceAndPath(
            StardewcraftsveMod.MODID, "queue_mail");
    private static final Codec<String> QUEUE_MAIL_CODEC = Codec.STRING.fieldOf("mail").codec();

    private static boolean registered;

    private SveActions() {}

    public static synchronized void register() {
        if (registered) return;

        StardewActions.register(QUEUE_MAIL, QUEUE_MAIL_CODEC, (context, mailId) -> {
            if (mailId.isBlank()) {
                return StardewActionResult.failure("Mail ID must not be blank");
            }

            if (MailService.hasOrWillReceiveMail(context.player(), mailId)) {
                return StardewActionResult.ok();
            }

            MailService.addMailForTomorrow(context.player(), mailId);
            return MailService.hasOrWillReceiveMail(context.player(), mailId)
                    ? StardewActionResult.ok()
                    : StardewActionResult.failure("Unable to queue mail '" + mailId + "'");
        });
        registered = true;
    }
}
