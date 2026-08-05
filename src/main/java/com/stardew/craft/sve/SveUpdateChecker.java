package com.stardew.craft.sve;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Asynchronous CurseForge update lookup used by the client title screen. */
public final class SveUpdateChecker {
    static final int CURSEFORGE_PROJECT_ID = 1622068;
    static final String PROJECT_URL =
            "https://www.curseforge.com/minecraft/mc-mods/starfield-pastoral-sve";
    private static final String FILES_API =
            "https://api.curse.tools/v1/mods/" + CURSEFORGE_PROJECT_ID;
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String LOADER = "neoforge";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final long SUCCESS_CACHE_NANOS = TimeUnit.HOURS.toNanos(6);
    private static final long FAILURE_CACHE_NANOS = TimeUnit.MINUTES.toNanos(15);
    private static final Pattern FILE_VERSION = Pattern.compile(
            "(?i)^stardewcraftsve[-_ ]v?(.+?)\\.jar$");
    private static final Logger LOGGER = LoggerFactory.getLogger("stardewcraftsve/update-checker");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile CompletableFuture<VersionStatus> cachedCheck;
    private static volatile long cacheExpiresAtNanos;
    private static volatile boolean failureLogged;

    private SveUpdateChecker() {
    }

    public static CompletableFuture<VersionStatus> checkAsync() {
        long now = System.nanoTime();
        CompletableFuture<VersionStatus> current = cachedCheck;
        if (current != null && (!current.isDone() || now < cacheExpiresAtNanos)) {
            return current;
        }
        synchronized (SveUpdateChecker.class) {
            now = System.nanoTime();
            current = cachedCheck;
            if (current != null && (!current.isDone() || now < cacheExpiresAtNanos)) {
                return current;
            }

            String installedVersion = installedVersion();
            HttpRequest request = HttpRequest.newBuilder(URI.create(FILES_API))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", "StardewCraftSVE/" + installedVersion
                            + " (" + PROJECT_URL + ")")
                    .GET()
                    .build();
            cachedCheck = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> parseResponse(
                            installedVersion, response.statusCode(), response.body()))
                    .exceptionally(exception -> {
                        logFailureOnce(exception.getMessage());
                        return VersionStatus.unavailable(installedVersion);
                    });
            cachedCheck.thenAccept(status -> {
                if (status.state() == State.UNAVAILABLE) {
                    logFailureOnce("the service returned no compatible release");
                } else {
                    failureLogged = false;
                }
                cacheExpiresAtNanos = System.nanoTime()
                        + (status.state() == State.UNAVAILABLE
                        ? FAILURE_CACHE_NANOS : SUCCESS_CACHE_NANOS);
            });
            return cachedCheck;
        }
    }

    static VersionStatus parseResponse(String installedVersion, int statusCode, String responseBody) {
        if (statusCode != 200 || responseBody == null || responseBody.isBlank()) {
            return VersionStatus.unavailable(installedVersion);
        }
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject data = root.has("data") && root.get("data").isJsonObject()
                    ? root.getAsJsonObject("data") : root;
            JsonArray files = data.getAsJsonArray("latestFiles");
            if (files == null) {
                return VersionStatus.unavailable(installedVersion);
            }

            Release latest = files.asList().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .filter(SveUpdateChecker::isCompatibleRelease)
                    .map(SveUpdateChecker::toRelease)
                    .flatMap(Optional::stream)
                    .max(Comparator.comparing(Release::publishedAt))
                    .orElse(null);
            if (latest == null) {
                return VersionStatus.unavailable(installedVersion);
            }
            return VersionStatus.compare(installedVersion, latest.version(), latest.fileId());
        } catch (RuntimeException exception) {
            return VersionStatus.unavailable(installedVersion);
        }
    }

    static Optional<String> extractVersion(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        Matcher matcher = FILE_VERSION.matcher(fileName.trim());
        return matcher.matches() && !matcher.group(1).isBlank()
                ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static boolean isCompatibleRelease(JsonObject file) {
        if (!file.has("id") || !file.has("releaseType")
                || file.get("releaseType").getAsInt() != 1) {
            return false;
        }
        JsonArray versions = file.getAsJsonArray("gameVersions");
        if (versions == null) {
            return false;
        }
        boolean supportsGame = false;
        boolean supportsLoader = false;
        for (JsonElement element : versions) {
            String value = element.getAsString();
            supportsGame |= MINECRAFT_VERSION.equals(value);
            supportsLoader |= LOADER.equals(value.toLowerCase(Locale.ROOT));
        }
        return supportsGame && supportsLoader;
    }

    private static Optional<Release> toRelease(JsonObject file) {
        String fileName = stringValue(file, "fileName");
        if (fileName.isBlank()) {
            fileName = stringValue(file, "displayName");
        }
        Optional<String> version = extractVersion(fileName);
        if (version.isEmpty()) {
            return Optional.empty();
        }
        Instant publishedAt;
        try {
            publishedAt = Instant.parse(stringValue(file, "fileDate"));
        } catch (DateTimeParseException exception) {
            publishedAt = Instant.EPOCH;
        }
        return Optional.of(new Release(version.get(), file.get("id").getAsInt(), publishedAt));
    }

    private static String stringValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private static String installedVersion() {
        return ModList.get().getModContainerById(StardewcraftsveMod.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private static void logFailureOnce(String reason) {
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("Could not check StardewCraft SVE updates on CurseForge: {}", reason);
        }
    }

    private record Release(String version, int fileId, Instant publishedAt) {
    }

    public enum State {
        UP_TO_DATE,
        OUTDATED,
        AHEAD,
        UNAVAILABLE
    }

    public record VersionStatus(String installedVersion, String latestVersion, int fileId, State state) {
        static VersionStatus compare(String installedVersion, String latestVersion, int fileId) {
            try {
                int comparison = new ComparableVersion(normalize(installedVersion))
                        .compareTo(new ComparableVersion(normalize(latestVersion)));
                State state = comparison < 0
                        ? State.OUTDATED
                        : comparison > 0 ? State.AHEAD : State.UP_TO_DATE;
                return new VersionStatus(installedVersion, latestVersion, fileId, state);
            } catch (RuntimeException exception) {
                return unavailable(installedVersion);
            }
        }

        static VersionStatus unavailable(String installedVersion) {
            return new VersionStatus(installedVersion, "", -1, State.UNAVAILABLE);
        }

        public boolean isOutdated() {
            return state == State.OUTDATED;
        }

        public String fileUrl() {
            return fileId > 0 ? PROJECT_URL + "/files/" + fileId : PROJECT_URL + "/files";
        }

        private static String normalize(String version) {
            String normalized = version == null ? "" : version.trim();
            return normalized.startsWith("v") || normalized.startsWith("V")
                    ? normalized.substring(1) : normalized;
        }
    }
}
