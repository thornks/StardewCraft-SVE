package com.stardew.craft.sve.mixin;

import com.stardew.craft.animal.service.AnimalShopService;
import com.stardew.craft.sve.animal.SveAnimalCompatibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        SHOP_RULES = SveAnimalCompatibility.appendShopRules(SHOP_RULES);
        SHOP_ORDER = SveAnimalCompatibility.appendShopOrder(SHOP_ORDER);
    }
}
