package com.stardew.craft.sve;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Makes native shield blocking authoritative before StardewCraft converts incoming player damage. */
final class SveHeavyShieldHandler {
    private SveHeavyShieldHandler() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, SveHeavyShieldHandler::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) return;
        if (!player.getUseItem().is(ModItems.HEAVY_SHIELD.get())) return;
        if (!player.isDamageSourceBlocked(event.getSource())) return;

        float blockedDamage = event.getAmount();
        event.setAmount(0.0F);
        player.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(blockedDamage * 10.0F));
        player.level().broadcastEntityEvent(player, (byte) 29);

        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            attacker.knockback(0.5, attacker.getX() - player.getX(), attacker.getZ() - player.getZ());
            if (attacker.canDisableShield()) {
                player.disableShield();
            }
        }
    }
}
