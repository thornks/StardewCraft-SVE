package com.stardew.craft.sve.mixin;

import com.stardew.craft.network.payload.ShopPurchasePayload;
import com.stardew.craft.network.payload.ShopPurchaseResultPayload;
import com.stardew.craft.player.PlayerDataEventHandler;
import com.stardew.craft.player.PlayerDataManager;
import com.stardew.craft.player.PlayerStardewData;
import com.stardew.craft.player.PlayerStardewDataAPI;
import com.stardew.craft.shop.SaloonService;
import com.stardew.craft.shop.ShopItemEntry;
import com.stardew.craft.shop.ShopStockTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Allows the fair's star-token shop to sell data-driven recipe entries. */
@Mixin(ShopPurchasePayload.class)
public abstract class FairRecipePurchaseMixin {
    private static final String RECIPE_PREFIX = "recipe:";

    @Inject(method = "handleFairStarTokenPurchase", at = @At("HEAD"), cancellable = true, require = 1)
    private static void sve$handleRecipePurchase(ServerPlayer player,
                                                  ShopPurchasePayload payload,
                                                  ShopItemEntry entry,
                                                  int quantity,
                                                  int totalPrice,
                                                  CallbackInfo ci) {
        if (!entry.itemId().startsWith(RECIPE_PREFIX)) {
            return;
        }

        ci.cancel();
        int tokens = PlayerStardewDataAPI.getFairStarTokens(player);
        PlayerStardewData data = PlayerDataManager.getPlayerData(player);
        String recipeId = SaloonService.extractRecipeId(entry.itemId());

        if (quantity != 1 || totalPrice <= 0 || tokens < totalPrice
                || entry.requiresTrade() || data == null || data.isRecipeUnlocked(recipeId)) {
            sendResult(player, false, payload.shopId(), tokens, "", 0, payload.itemIndex());
            return;
        }

        if (!PlayerStardewDataAPI.consumeFairStarTokens(player, totalPrice)) {
            sendResult(player, false, payload.shopId(), tokens, "", 0, payload.itemIndex());
            return;
        }

        data.unlockRecipe(recipeId);
        PlayerDataEventHandler.syncPlayerData(player, data);
        if (entry.stock() != Integer.MAX_VALUE) {
            ShopStockTracker.recordPurchase(
                    player.getUUID(), payload.shopId(), entry.itemId(), quantity);
        }

        sendResult(player, true, payload.shopId(), PlayerStardewDataAPI.getFairStarTokens(player),
                entry.itemId(), quantity, payload.itemIndex());
    }

    private static void sendResult(ServerPlayer player,
                                   boolean success,
                                   String shopId,
                                   int tokens,
                                   String itemId,
                                   int quantity,
                                   int itemIndex) {
        PacketDistributor.sendToPlayer(player,
                new ShopPurchaseResultPayload(success, shopId, tokens, itemId, quantity, itemIndex));
    }
}
