package com.stardew.craft.sve;

import com.mojang.brigadier.context.CommandContext;
import com.stardew.craft.fishpond.service.FishPondDataService;
import com.stardew.craft.fishpond.service.FishPondQualifiedItemService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only validation of SVE fish pond definitions after data reload. */
public final class SveFishPondAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/fishpond-audit");
    private static final int MAX_REPORTED_ISSUES = 24;
    private static final Set<String> FIXED_SINGLE_FISH_PONDS = Set.of("turretfish", "wolf_snapper");

    private SveFishPondAudit() {
    }

    public static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Audit audit = audit();
        ChatFormatting color = audit.errors == 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal(
                "SVE fish pond audit: definitions=" + audit.definitions
                        + ", productions=" + audit.productions
                        + ", gates=" + audit.gates
                        + ", errors=" + audit.errors
                        + ", warnings=" + audit.warnings
        ).withStyle(color), false);

        int shown = Math.min(MAX_REPORTED_ISSUES, audit.issues.size());
        for (int index = 0; index < shown; index++) {
            Issue issue = audit.issues.get(index);
            source.sendSystemMessage(Component.literal(issue.message)
                    .withStyle(issue.error ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (audit.issues.size() > shown) {
            source.sendSystemMessage(Component.literal(
                    "... " + (audit.issues.size() - shown) + " more issue(s); see server log"
            ).withStyle(ChatFormatting.GRAY));
        }
        for (Issue issue : audit.issues) {
            if (issue.error) LOGGER.error(issue.message);
            else LOGGER.warn(issue.message);
        }
        return audit.errors == 0 ? 1 : 0;
    }

    public static Audit audit() {
        Audit audit = new Audit();
        FishPondDataService service = FishPondDataService.get();
        for (String fishId : SveFishData.SVE_FISH) {
            ResourceLocation fishKey = ResourceLocation.fromNamespaceAndPath(StardewcraftsveMod.MODID, fishId);
            if (!BuiltInRegistries.ITEM.containsKey(fishKey)) {
                audit.error("Missing fish pond item " + fishKey);
                continue;
            }
            Item fishItem = BuiltInRegistries.ITEM.get(fishKey);
            if (fishItem == Items.AIR) {
                audit.error("Fish pond item resolves to air: " + fishKey);
                continue;
            }

            ItemStack fish = new ItemStack(fishItem);
            FishPondDataService.PondData data = service.resolve(fish).orElse(null);
            if (data == null) {
                audit.error("No fish pond definition resolved for " + fishKey);
                continue;
            }
            if (!data.requiredTags().contains("item_id:" + fishKey)) {
                audit.error("No SVE-specific fish pond definition resolved for " + fishKey
                        + "; matched host fallback " + data.id());
                continue;
            }
            audit.definitions++;
            validatePopulation(fishId, fish, data, service, audit);
            validateProductions(fishId, fish, data, service, audit);
            validateGates(fishId, data.populationGates(), audit);
        }
        return audit;
    }

    private static void validatePopulation(
            String fishId,
            ItemStack fish,
            FishPondDataService.PondData data,
            FishPondDataService service,
            Audit audit
    ) {
        boolean fixedSingle = FIXED_SINGLE_FISH_PONDS.contains(fishId);
        if (fixedSingle && data.maxPopulation() != 1) {
            audit.error(fishId + " must have a fixed maximum population of 1");
        } else if (!fixedSingle && data.maxPopulation() > 0) {
            audit.warning(fishId + " uses explicit maximum population " + data.maxPopulation()
                    + " instead of population gates");
        }
        int initialMaximum = service.resolveMaxPopulation(fish);
        if (initialMaximum < 1 || initialMaximum > 10) {
            audit.error(fishId + " resolves invalid initial maximum population " + initialMaximum);
        }
        if (data.spawnTime() < -1) {
            audit.error(fishId + " has invalid spawn time " + data.spawnTime());
        }
        if (!validChance(data.baseMinProduceChance()) || !validChance(data.baseMaxProduceChance())
                || data.baseMinProduceChance() > data.baseMaxProduceChance()) {
            audit.error(fishId + " has invalid daily production chance range");
        }
    }

    private static void validateProductions(
            String fishId,
            ItemStack fish,
            FishPondDataService.PondData data,
            FishPondDataService service,
            Audit audit
    ) {
        if (data.producedItems().isEmpty()) {
            audit.error(fishId + " has no fish pond productions");
            return;
        }
        for (FishPondDataService.ProducedItem production : data.producedItems()) {
            audit.productions++;
            if (production.requiredPopulation() < 0 || production.requiredPopulation() > 10) {
                audit.error(fishId + " production " + production.itemId()
                        + " has invalid required population " + production.requiredPopulation());
            }
            if (!validChance(production.chance())) {
                audit.error(fishId + " production " + production.itemId()
                        + " has invalid chance " + production.chance());
            }
            if (production.minStack() < 1 || production.maxStack() < production.minStack()) {
                audit.error(fishId + " production " + production.itemId() + " has invalid count range");
            }
            if (FishPondQualifiedItemService.createItemStack(production.itemId(), 1).isEmpty()) {
                audit.error(fishId + " production references missing item " + production.itemId());
            }
        }
        if (service.getDisplayProductions(fish).isEmpty()) {
            audit.error(fishId + " has no JEI-visible fish pond production");
        }
    }

    private static void validateGates(
            String fishId,
            Map<Integer, List<String>> gates,
            Audit audit
    ) {
        int previous = 0;
        for (Map.Entry<Integer, List<String>> gate : gates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            audit.gates++;
            int population = gate.getKey();
            if (population <= previous || population < 2 || population > 10) {
                audit.error(fishId + " has invalid population gate " + population);
            }
            previous = population;
            if (gate.getValue().isEmpty()) {
                audit.error(fishId + " population gate " + population + " has no requested items");
            }
            for (String request : gate.getValue()) {
                String itemId = request == null ? "" : request.trim().split("\\s+")[0];
                if (itemId.isBlank()
                        || FishPondQualifiedItemService.createItemStack(itemId, 1).isEmpty()) {
                    audit.error(fishId + " population gate " + population
                            + " references missing item " + itemId);
                }
            }
        }
    }

    private static boolean validChance(double chance) {
        return Double.isFinite(chance) && chance >= 0.0D && chance <= 1.0D;
    }

    public static final class Audit {
        private final List<Issue> issues = new ArrayList<>();
        private int definitions;
        private int productions;
        private int gates;
        private int errors;
        private int warnings;

        private void error(String message) {
            errors++;
            issues.add(new Issue(true, message));
        }

        private void warning(String message) {
            warnings++;
            issues.add(new Issue(false, message));
        }
    }

    private record Issue(boolean error, String message) {
    }
}
