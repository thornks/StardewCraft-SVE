package com.stardew.craft.sve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Models survival acquisition as roots and transformations with item prerequisites. */
public final class SveContentAcquisitionGraph {
    private static final String NAMESPACE_PREFIX = StardewcraftsveMod.MODID + ":";

    private final Map<String, List<Route>> routes = new LinkedHashMap<>();

    public void addSource(String itemId, String kind, String detail) {
        addRouteWithRequirements(itemId, kind, detail, List.of());
    }

    public void addRoute(String itemId, String kind, String detail, Collection<String> prerequisites) {
        List<Requirement> requirements = prerequisites.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(Requirement::exact)
                .toList();
        addRouteWithRequirements(itemId, kind, detail, requirements);
    }

    public void addRouteWithRequirements(
            String itemId,
            String kind,
            String detail,
            Collection<Requirement> requirements
    ) {
        if (!isSveItem(itemId)) return;
        List<Requirement> normalizedRequirements = requirements.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Route route = new Route(kind, detail, normalizedRequirements);
        routes.computeIfAbsent(itemId.trim(), ignored -> new ArrayList<>()).add(route);
    }

    public Map<String, List<Route>> routes() {
        Map<String, List<Route>> copy = new LinkedHashMap<>();
        routes.forEach((item, itemRoutes) -> copy.put(item, List.copyOf(itemRoutes)));
        return Map.copyOf(copy);
    }

    public Evaluation evaluate(
            Set<String> registeredItems,
            Map<String, SveContentAcquisitionCatalog.Exclusion> exclusions
    ) {
        Set<String> registered = Set.copyOf(registeredItems);
        Set<String> rawReachable = resolveReachable(registered, Set.of());
        Set<String> reachable = resolveReachable(registered, exclusions.keySet());

        Set<String> unclassified = new LinkedHashSet<>();
        Map<String, Set<String>> blocked = new LinkedHashMap<>();
        for (String item : registered.stream().sorted().toList()) {
            if (reachable.contains(item) || exclusions.containsKey(item)) continue;
            List<Route> itemRoutes = routes.get(item);
            if (itemRoutes == null || itemRoutes.isEmpty()) {
                unclassified.add(item);
                continue;
            }
            Set<String> missing = new LinkedHashSet<>();
            for (Route route : itemRoutes) {
                for (Requirement requirement : route.requirements()) {
                    if (requirementReachable(requirement, reachable)) continue;
                    requirement.alternatives().stream()
                            .filter(SveContentAcquisitionGraph::isSveItem)
                            .filter(prerequisite -> !reachable.contains(prerequisite))
                            .forEach(missing::add);
                }
            }
            blocked.put(item, Set.copyOf(missing));
        }

        Set<String> unknownOutputs = new LinkedHashSet<>(routes.keySet());
        unknownOutputs.removeAll(registered);

        Set<String> missingDependencies = new LinkedHashSet<>();
        for (List<Route> itemRoutes : routes.values()) {
            for (Route route : itemRoutes) {
                for (Requirement requirement : route.requirements()) {
                    List<String> sveAlternatives = requirement.alternatives().stream()
                            .filter(SveContentAcquisitionGraph::isSveItem)
                            .toList();
                    boolean hasExternalAlternative = requirement.alternatives().stream()
                            .anyMatch(value -> !isSveItem(value));
                    boolean hasRegisteredAlternative = sveAlternatives.stream()
                            .anyMatch(registered::contains);
                    if (!hasExternalAlternative && !hasRegisteredAlternative) {
                        missingDependencies.addAll(sveAlternatives);
                    }
                }
            }
        }

        Set<String> staleExclusions = new LinkedHashSet<>(exclusions.keySet());
        staleExclusions.retainAll(rawReachable);
        Set<String> unknownExclusions = new LinkedHashSet<>(exclusions.keySet());
        unknownExclusions.removeAll(registered);

        return new Evaluation(
                Set.copyOf(reachable),
                Set.copyOf(unclassified),
                copyBlocked(blocked),
                Set.copyOf(unknownOutputs),
                Set.copyOf(missingDependencies),
                Set.copyOf(staleExclusions),
                Set.copyOf(unknownExclusions)
        );
    }

    private Set<String> resolveReachable(Set<String> registered, Set<String> excludedOutputs) {
        Set<String> reachable = new LinkedHashSet<>();
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<Route>> entry : routes.entrySet()) {
                if (!registered.contains(entry.getKey())
                        || excludedOutputs.contains(entry.getKey())
                        || reachable.contains(entry.getKey())) {
                    continue;
                }
                boolean hasReachableRoute = entry.getValue().stream()
                        .anyMatch(route -> prerequisitesReachable(route, reachable));
                if (hasReachableRoute) changed |= reachable.add(entry.getKey());
            }
        } while (changed);
        return reachable;
    }

    private static boolean prerequisitesReachable(Route route, Set<String> reachable) {
        return route.requirements().stream()
                .allMatch(requirement -> requirementReachable(requirement, reachable));
    }

    private static boolean requirementReachable(
            Requirement requirement,
            Set<String> reachable
    ) {
        return requirement.alternatives().stream()
                .anyMatch(value -> !isSveItem(value) || reachable.contains(value));
    }

    private static Map<String, Set<String>> copyBlocked(Map<String, Set<String>> blocked) {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        blocked.forEach((item, missing) -> copy.put(item, Set.copyOf(missing)));
        return Map.copyOf(copy);
    }

    private static boolean isSveItem(String value) {
        return value != null && value.trim().startsWith(NAMESPACE_PREFIX);
    }

    public record Requirement(String selector, List<String> alternatives) {
        public Requirement {
            selector = selector == null || selector.isBlank()
                    ? "unknown" : selector.trim();
            alternatives = alternatives == null ? List.of() : alternatives.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .distinct()
                    .sorted()
                    .toList();
        }

        public static Requirement exact(String itemId) {
            String normalized = itemId == null ? "" : itemId.trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Exact acquisition item id is required");
            }
            return new Requirement("item:" + normalized, List.of(normalized));
        }

        public static Requirement anyOf(
                String selector,
                Collection<String> alternatives
        ) {
            return new Requirement(selector,
                    alternatives == null ? List.of() : new ArrayList<>(alternatives));
        }
    }

    public record Route(String kind, String detail, List<Requirement> requirements) {
        public Route {
            kind = kind == null || kind.isBlank() ? "unknown" : kind.trim();
            detail = detail == null ? "" : detail.trim();
            requirements = List.copyOf(requirements);
        }

        public List<String> prerequisites() {
            return requirements.stream()
                    .flatMap(requirement -> requirement.alternatives().stream())
                    .distinct()
                    .toList();
        }
    }

    public record Evaluation(
            Set<String> reachable,
            Set<String> unclassified,
            Map<String, Set<String>> blocked,
            Set<String> unknownOutputs,
            Set<String> missingDependencies,
            Set<String> staleExclusions,
            Set<String> unknownExclusions
    ) {
        public boolean hasErrors() {
            return !unclassified.isEmpty()
                    || !blocked.isEmpty()
                    || !unknownOutputs.isEmpty()
                    || !missingDependencies.isEmpty()
                    || !staleExclusions.isEmpty()
                    || !unknownExclusions.isEmpty();
        }
    }
}
