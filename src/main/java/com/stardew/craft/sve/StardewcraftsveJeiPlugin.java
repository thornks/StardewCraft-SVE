package com.stardew.craft.sve;

import com.stardew.craft.integration.jei.ArtisanJeiRecipe;
import com.stardew.craft.integration.jei.ArtisanJeiRecipeFactory;
import com.stardew.craft.integration.jei.ArtisanRecipeCategory;
import com.stardew.craft.integration.jei.FishPondInfoCategory;
import com.stardew.craft.integration.jei.MachineJeiRegistry;
import com.stardew.craft.fishpond.service.FishPondDataService;
import com.stardew.craft.item.IStardewItem;
import com.stardew.craft.item.StardewQualityItem;
import com.stardew.craft.item.artisan.ArtisanRecipeDataManager;
import com.stardew.craft.item.artisan.PreserveType;
import com.stardew.craft.item.artisan.PreservesItem;
import com.stardew.craft.item.artisan.SmokedFishItem;
import com.stardew.craft.item.catalog.StardewItemCatalog;
import com.stardew.craft.item.catalog.StardewItemDisplayStacks;
import com.stardew.craft.item.quality.QualityHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class StardewcraftsveJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/jei");
    private static final int[] QUALITY_LEVELS = {
            QualityHelper.NORMAL,
            QualityHelper.SILVER,
            QualityHelper.GOLD,
            QualityHelper.IRIDIUM
    };

    public static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, "jei_plugin");

    private static final MachineJeiRegistry.Machine FISH_SMOKER = MachineJeiRegistry.find(
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "fish_smoker")
    ).orElseThrow();
    private static final MachineJeiRegistry.Machine PRESERVES_JAR = MachineJeiRegistry.find(
            ResourceLocation.fromNamespaceAndPath("stardewcraft", "preserves_jar")
    ).orElseThrow();
    private static final MachineJeiRegistry.Machine BUTTER_CHURNER = addonMachine(
            "butter_churner", "butter_churner");
    private static final MachineJeiRegistry.Machine YARN_SPOOLER = addonMachine(
            "yarn_spooler", "yarn_spooler");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        addArtisanCategory(registration, BUTTER_CHURNER);
        addArtisanCategory(registration, YARN_SPOOLER);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ArtisanJeiRecipe> smokedFish = buildSveSmokedFishRecipes();
        if (!smokedFish.isEmpty()) {
            registration.addRecipes(FISH_SMOKER.recipeType(), smokedFish);
        }

        List<ArtisanJeiRecipe> roeRecipes = buildSveRoeRecipes();
        if (!roeRecipes.isEmpty()) {
            registration.addRecipes(PRESERVES_JAR.recipeType(), roeRecipes);
        }

        List<FishPondInfoCategory.DisplayEntry> fishPondRecipes = buildSveFishPondRecipes();
        if (!fishPondRecipes.isEmpty()) {
            registration.addRecipes(FishPondInfoCategory.RECIPE_TYPE, fishPondRecipes);
        }

        registerAddonMachineRecipes(registration, BUTTER_CHURNER);
        registerAddonMachineRecipes(registration, YARN_SPOOLER);

        registration.addItemStackInfo(
                List.of(new ItemStack(ModItems.JOJA_BERRY_STARTER.get()),
                        new ItemStack(ModItems.JOJA_VEGGIE_SEEDS.get())),
                Component.translatable("jei.stardewcraftsve.joja_seeds.purchase")
        );
        registration.addItemStackInfo(
                List.of(new ItemStack(ModItems.FIR_CONE.get())),
                Component.translatable("jei.stardewcraftsve.fir_cone.growth")
        );
        registration.addItemStackInfo(
                List.of(new ItemStack(ModItems.BIRCH_SEED.get())),
                Component.translatable("jei.stardewcraftsve.birch_seed.growth")
        );
        registerArtifactSpotInfo(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.BUTTER_CHURNER.get()), BUTTER_CHURNER.recipeType());
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.YARN_SPOOLER.get()), YARN_SPOOLER.recipeType());
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registerBaseQualitySubtypes(registration);
        registerQualitySubtypes(registration, ModItems.ITEMS.getEntries());
        registerQualitySubtypes(registration, ModItems.SMOKED_FISH_ITEMS.getEntries());
        registerQualitySubtypes(registration, ModItems.STARDEWCRAFT_ITEMS.getEntries());
        registerQualitySubtypes(registration, ModItems.ARTISAN_ITEMS.getEntries());
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        List<ItemStack> extras = new ArrayList<>();
        addQualityVariants(extras, ModItems.ITEMS.getEntries());
        addQualityVariants(extras, ModItems.SMOKED_FISH_ITEMS.getEntries());
        addQualityVariants(extras, ModItems.STARDEWCRAFT_ITEMS.getEntries());
        addQualityVariants(extras, ModItems.ARTISAN_ITEMS.getEntries());
        for (SveRoePair pair : buildSveRoePairs()) {
            extras.add(pair.roe());
            extras.add(pair.agedRoe());
        }
        if (!extras.isEmpty()) {
            registration.addExtraItemStacks(extras);
        }
    }

    private static void addArtisanCategory(
            IRecipeCategoryRegistration registration,
            MachineJeiRegistry.Machine machine
    ) {
        ItemStack icon = ArtisanRecipeCategory.itemStack(machine.itemId().toString());
        if (icon.isEmpty()) {
            LOGGER.warn("Cannot register JEI category for missing machine item {}", machine.itemId());
            return;
        }
        registration.addRecipeCategories(new ArtisanRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), machine, icon));
    }

    private static void registerAddonMachineRecipes(
            IRecipeRegistration registration,
            MachineJeiRegistry.Machine machine
    ) {
        List<ArtisanJeiRecipe> recipes = ArtisanJeiRecipeFactory.build(machine);
        if (recipes.isEmpty()) {
            recipes = fallbackRecipes(machine);
        }
        if (!recipes.isEmpty()) {
            registration.addRecipes(machine.recipeType(), recipes);
            LOGGER.info("Registered {} recipes for SVE machine {}", recipes.size(), machine.id());
        }
    }

    private static List<ArtisanJeiRecipe> buildSveSmokedFishRecipes() {
        List<ArtisanJeiRecipe> recipes = new ArrayList<>();
        for (String fishId : SveFishData.SVE_FISH) {
            Item fish = item("stardewcraftsve", fishId);
            Item smokedFish = item("stardewcraftsve", "smoked_" + fishId);
            if (fish == Items.AIR || smokedFish == Items.AIR) {
                continue;
            }
            recipes.add(new ArtisanJeiRecipe(
                    ResourceLocation.fromNamespaceAndPath(
                            StardewcraftsveMod.MODID, "jei/fish_smoker/" + fishId),
                    FISH_SMOKER,
                    List.of(
                            new ArtisanJeiRecipe.Input(qualityStacks(fish), 1, false),
                            new ArtisanJeiRecipe.Input(List.of(new ItemStack(
                                    com.stardew.craft.item.ModItems.COAL.get())), 1, true)
                    ),
                    List.of(new ArtisanJeiRecipe.Output(
                            qualityStacks(smokedFish), 1, 1, 1.0D)),
                    50,
                    true,
                    -1
            ));
        }
        return List.copyOf(recipes);
    }

    private static List<ArtisanJeiRecipe> buildSveRoeRecipes() {
        ArtisanRecipeDataManager.Recipe definition = ArtisanRecipeDataManager
                .getRecipes(PRESERVES_JAR.id().toString())
                .stream()
                .filter(recipe -> recipe.preserveType() == PreserveType.AGED_ROE)
                .findFirst()
                .orElse(null);
        if (definition == null) {
            LOGGER.warn("Cannot register SVE roe recipes: aged roe definition is missing");
            return List.of();
        }

        List<ArtisanJeiRecipe> recipes = new ArrayList<>();
        for (SveRoePair pair : buildSveRoePairs()) {
            ItemStack input = pair.roe();
            input.setCount(Math.max(1, definition.consumeCount()));
            ItemStack output = pair.agedRoe();
            output.setCount(Math.max(1, definition.outputCount()));
            recipes.add(new ArtisanJeiRecipe(
                    ResourceLocation.fromNamespaceAndPath(
                            StardewcraftsveMod.MODID, "jei/preserves_jar/roe/" + pair.fishId()),
                    PRESERVES_JAR,
                    List.of(new ArtisanJeiRecipe.Input(
                            List.of(input), Math.max(1, definition.consumeCount()), false)),
                    List.of(new ArtisanJeiRecipe.Output(
                            List.of(output), output.getCount(), output.getCount(), 1.0D)),
                    definition.minutes(),
                    definition.keepInputQuality(),
                    definition.outputQuality()
            ));
        }
        return List.copyOf(recipes);
    }

    private static List<FishPondInfoCategory.DisplayEntry> buildSveFishPondRecipes() {
        FishPondDataService pondData = FishPondDataService.get();
        List<FishPondInfoCategory.DisplayEntry> recipes = new ArrayList<>();
        for (String fishId : SveFishData.SVE_FISH) {
            Item fishItem = item(StardewcraftsveMod.MODID, fishId);
            if (fishItem == Items.AIR) {
                continue;
            }
            ItemStack fish = new ItemStack(fishItem);
            for (FishPondDataService.DisplayProduction production
                    : pondData.getDisplayProductions(fish)) {
                recipes.add(new FishPondInfoCategory.DisplayEntry(
                        fish,
                        production.output(),
                        production.requiredPopulation(),
                        production.outputChance(),
                        production.dailyMinChance(),
                        production.dailyMaxChance(),
                        production.minCount(),
                        production.maxCount(),
                        production.bonusCountPossible()
                ));
            }
        }
        return List.copyOf(recipes);
    }

    private static List<SveRoePair> buildSveRoePairs() {
        List<SveRoePair> pairs = new ArrayList<>();
        for (String fishId : SveFishData.SVE_FISH) {
            Item fishItem = item(StardewcraftsveMod.MODID, fishId);
            if (fishItem == Items.AIR) {
                continue;
            }
            ItemStack fish = new ItemStack(fishItem);
            ItemStack roe = new ItemStack(com.stardew.craft.item.ModItems.ROE.get());
            ItemStack agedRoe = new ItemStack(com.stardew.craft.item.ModItems.AGED_ROE.get());
            PreservesItem.createFlavored(PreserveType.ROE, fish, roe);
            PreservesItem.createFlavored(PreserveType.AGED_ROE, fish, agedRoe);
            pairs.add(new SveRoePair(fishId, roe, agedRoe));
        }
        return List.copyOf(pairs);
    }

    private static List<ArtisanJeiRecipe> fallbackRecipes(MachineJeiRegistry.Machine machine) {
        if (machine == BUTTER_CHURNER) {
            return List.of(
                    qualityRecipe(machine, "milk", ModItems.BUTTER.get(), 60),
                    qualityRecipe(machine, "goat_milk", ModItems.BUTTER.get(), 60),
                    qualityRecipe(machine, "large_milk", ModItems.BUTTER.get(), 60),
                    qualityRecipe(machine, "large_goat_milk", ModItems.BUTTER.get(), 60)
            );
        }
        if (machine == YARN_SPOOLER) {
            return List.of(new ArtisanJeiRecipe(
                    ResourceLocation.fromNamespaceAndPath(
                            StardewcraftsveMod.MODID, "jei/yarn_spooler/camel_wool"),
                    machine,
                    List.of(new ArtisanJeiRecipe.Input(
                            qualityStacks(ModItems.CAMEL_WOOL.get()), 1, false)),
                    List.of(new ArtisanJeiRecipe.Output(
                            qualityStacks(ModItems.YARN.get()), 1, 1, 1.0D)),
                    120,
                    true,
                    -1
            ));
        }
        return List.of();
    }

    private static ArtisanJeiRecipe qualityRecipe(
            MachineJeiRegistry.Machine machine,
            String inputPath,
            Item outputItem,
            int minutes
    ) {
        Item inputItem = item("stardewcraft", inputPath);
        return new ArtisanJeiRecipe(
                ResourceLocation.fromNamespaceAndPath(
                        StardewcraftsveMod.MODID, "jei/butter_churner/" + inputPath),
                machine,
                List.of(new ArtisanJeiRecipe.Input(
                        qualityStacks(inputItem), 1, false)),
                List.of(new ArtisanJeiRecipe.Output(qualityStacks(outputItem), 1, 1, 1.0D)),
                minutes,
                true,
                -1
        );
    }

    private static MachineJeiRegistry.Machine addonMachine(String machinePath, String itemPath) {
        ResourceLocation machineId = ResourceLocation.fromNamespaceAndPath("stardewcraft", machinePath);
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, itemPath);
        RecipeType<ArtisanJeiRecipe> recipeType = RecipeType.create(
                StardewcraftsveMod.MODID, "machine/" + machinePath, ArtisanJeiRecipe.class);
        return new MachineJeiRegistry.Machine(
                machineId, itemId, recipeType, MachineJeiRegistry.Layout.STANDARD, true);
    }

    private static List<ItemStack> qualityStacks(Item item) {
        List<ItemStack> stacks = new ArrayList<>(QUALITY_LEVELS.length);
        for (int quality : QUALITY_LEVELS) {
            stacks.add(QualityHelper.createWithQuality(new ItemStack(item), quality));
        }
        return List.copyOf(stacks);
    }

    private static void addQualityVariants(
            List<ItemStack> extras,
            Iterable<? extends DeferredHolder<Item, ? extends Item>> holders
    ) {
        for (DeferredHolder<Item, ? extends Item> holder : holders) {
            Item item = holder.get();
            if (isQualityItem(item)) {
                extras.addAll(qualityStacks(item));
            }
        }
    }

    private static void registerQualitySubtypes(
            ISubtypeRegistration registration,
            Iterable<? extends DeferredHolder<Item, ? extends Item>> holders
    ) {
        for (DeferredHolder<Item, ? extends Item> holder : holders) {
            Item item = holder.get();
            if (isQualityItem(item)) {
                registration.registerSubtypeInterpreter(
                        item, QualityDisplaySubtypeInterpreter.INSTANCE);
            }
        }
    }

    private static void registerBaseQualitySubtypes(ISubtypeRegistration registration) {
        int registered = 0;
        for (Item item : StardewItemCatalog.visibleItems()) {
            List<ItemStack> displayStacks = StardewItemDisplayStacks.stacksForItem(item);
            boolean hasMultipleQualities = displayStacks.stream()
                    .mapToInt(QualityHelper::getQuality)
                    .distinct()
                    .count() > 1;
            if (!hasMultipleQualities) {
                continue;
            }
            registration.registerSubtypeInterpreter(
                    item, BaseQualityDisplaySubtypeInterpreter.INSTANCE);
            registered++;
        }
        LOGGER.info("Registered JEI display-only quality subtypes for {} StardewCraft items", registered);
    }

    private static Item item(String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
    }

    private static void registerArtifactSpotInfo(IRecipeRegistration registration) {
        addItemInfo(registration, ModItems.FADED_BUTTON.get(),
                "jei.stardewcraftsve.artifact_spot.faded_button");
        addItemInfo(registration, ModItems.OLD_COIN.get(),
                "jei.stardewcraftsve.artifact_spot.old_coin");
        addItemInfo(registration, ModItems.FOSSILIZED_APPLE.get(),
                "jei.stardewcraftsve.artifact_spot.fossilized_apple");
        addItemInfo(registration, ModItems.STONE_OF_YOBA.get(),
                "jei.stardewcraftsve.artifact_spot.stone_of_yoba");
        addItemInfo(registration, ModItems.AMBER.get(),
                "jei.stardewcraftsve.artifact_spot.amber");
        addItemInfo(registration, ModItems.BOOMERANG.get(),
                "jei.stardewcraftsve.artifact_spot.boomerang");
        addItemInfo(registration, ModItems.RUSTY_SHIELD.get(),
                "jei.stardewcraftsve.artifact_spot.rusty_shield");
        addItemInfo(registration, ModItems.SALAL_BERRY_SEED.get(),
                "jei.stardewcraftsve.artifact_spot.salal_berry_seed");
        addItemInfo(registration, ModItems.ANCIENT_FERNS_SEED.get(),
                "jei.stardewcraftsve.artifact_spot.ancient_ferns_seed");
    }

    private static void addItemInfo(
            IRecipeRegistration registration,
            Item item,
            String translationKey
    ) {
        registration.addItemStackInfo(
                List.of(new ItemStack(item)), Component.translatable(translationKey));
    }

    private static boolean isQualityItem(Item item) {
        if (item instanceof SmokedFishItem) {
            return true;
        }
        if (!(item instanceof IStardewItem stardew)) {
            return false;
        }
        if (item instanceof StardewQualityItem qualityItem && qualityItem.supportsQuality()) {
            return true;
        }
        return "stardewcraft.type.fish".equals(stardew.getItemTypeKey());
    }

    private static final class QualityDisplaySubtypeInterpreter
            implements ISubtypeInterpreter<ItemStack> {
        private static final QualityDisplaySubtypeInterpreter INSTANCE =
                new QualityDisplaySubtypeInterpreter();

        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            return context == UidContext.Ingredient
                    ? Integer.valueOf(QualityHelper.getQuality(stack))
                    : null;
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            return context == UidContext.Ingredient
                    ? String.valueOf(QualityHelper.getQuality(stack))
                    : "";
        }
    }

    private static final class BaseQualityDisplaySubtypeInterpreter
            implements ISubtypeInterpreter<ItemStack> {
        private static final BaseQualityDisplaySubtypeInterpreter INSTANCE =
                new BaseQualityDisplaySubtypeInterpreter();

        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            if (context != UidContext.Ingredient) {
                return null;
            }
            Integer flowerColor = StardewItemDisplayStacks.getFlowerColor(stack);
            return new BaseDisplaySubtype(
                    QualityHelper.getQuality(stack),
                    flowerColor == null ? -1 : flowerColor);
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            if (context != UidContext.Ingredient) {
                return "";
            }
            Integer flowerColor = StardewItemDisplayStacks.getFlowerColor(stack);
            return QualityHelper.getQuality(stack) + ":" + (flowerColor == null ? -1 : flowerColor);
        }
    }

    private record BaseDisplaySubtype(int quality, int flowerColor) {
    }

    private record SveRoePair(String fishId, ItemStack roe, ItemStack agedRoe) {
        private SveRoePair {
            roe = roe.copy();
            agedRoe = agedRoe.copy();
        }

        @Override
        public ItemStack roe() {
            return roe.copy();
        }

        @Override
        public ItemStack agedRoe() {
            return agedRoe.copy();
        }
    }
}
