package com.stardew.craft.sve;

import com.stardew.craft.core.ModDimensions;
import com.stardew.craft.item.SimpleStardewItem;
import com.stardew.craft.sound.ModSounds;
import com.stardew.craft.time.StardewTimeManager;
import com.stardew.craft.weather.WeatherManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WindTotemItem extends SimpleStardewItem {
    private static final String TYPE_KEY = "stardewcraft.type.magic";

    public WindTotemItem(int sellPrice, Item.Properties properties) {
        super(TYPE_KEY, sellPrice, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!player.isAlive()) {
            return InteractionResultHolder.pass(stack);
        }

        // Wind totem has no effect in winter
        if (StardewTimeManager.get().getCurrentSeason() == 3) {
            player.displayClientMessage(Component.translatable("message.stardewcraftsve.wind_totem_winter"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (level.dimension() != ModDimensions.STARDEW_VALLEY) {
            player.displayClientMessage(Component.translatable("message.stardewcraftsve.wind_totem_denied"), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel svLevel = level.getServer().getLevel(ModDimensions.STARDEW_VALLEY);
        if (svLevel != null) {
            WeatherManager.setTomorrowWeather(svLevel, "Wind");
            svLevel.playSound(null, player.blockPosition(), ModSounds.LEAFRUSTLE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            svLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 200, 0.5, 0.5, 0.5, 0.0);
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.stardewcraftsve.wind_totem_used"), true);
        return InteractionResultHolder.consume(stack);
    }
}
