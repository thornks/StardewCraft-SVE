package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.service.AnimalShopService;
import com.stardew.craft.sve.animal.SveAnimalRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(AnimalShopService.class)
public abstract class AnimalShopServiceMixin {
    @Shadow @Final @Mutable
    private static Map<String, AnimalShopService.ShopAnimalRule> SHOP_RULES;

    @Shadow @Final @Mutable
    private static List<String> SHOP_ORDER;

    @Inject(method = "<clinit>", at = @At("TAIL"), require = 1)
    private static void stardewcraftsve$installShopRules(CallbackInfo ci) {
        if (!SveAnimalRules.SHOP_MODELS_READY) {
            return;
        }

        LinkedHashMap<String, AnimalShopService.ShopAnimalRule> rules = new LinkedHashMap<>(SHOP_RULES);
        rules.put(SveAnimalRules.GOOSE_ID, SveAnimalRules.gooseShopRule());
        rules.put(SveAnimalRules.CAMEL_ID, SveAnimalRules.camelShopRule());
        SHOP_RULES = Map.copyOf(rules);

        ArrayList<String> order = new ArrayList<>(SHOP_ORDER);
        order.add(SveAnimalRules.GOOSE_ID);
        order.add(SveAnimalRules.CAMEL_ID);
        SHOP_ORDER = List.copyOf(order);
    }
}
